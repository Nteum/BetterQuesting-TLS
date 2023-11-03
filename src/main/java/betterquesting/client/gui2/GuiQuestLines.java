package betterquesting.client.gui2;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.enums.EnumQuestVisibility;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api.questing.IQuestLineEntry;
import betterquesting.api2.cache.QuestCache;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.IPanelButton;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.controls.PanelButtonQuest;
import betterquesting.api2.client.gui.controls.PanelButtonStorage;
import betterquesting.api2.client.gui.events.IPEventListener;
import betterquesting.api2.client.gui.events.PEventBroadcaster;
import betterquesting.api2.client.gui.events.PanelEvent;
import betterquesting.api2.client.gui.events.types.PEventButton;
import betterquesting.api2.client.gui.misc.GuiAlign;
import betterquesting.api2.client.gui.misc.GuiPadding;
import betterquesting.api2.client.gui.misc.GuiRectangle;
import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelGeneric;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasHoverTray;
import betterquesting.api2.client.gui.panels.lists.CanvasQuestLine;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.resources.colors.GuiColorPulse;
import betterquesting.api2.client.gui.resources.colors.GuiColorStatic;
import betterquesting.api2.client.gui.resources.textures.GuiTextureColored;
import betterquesting.api2.client.gui.resources.textures.OreDictTexture;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetIcon;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.api2.utils.Tuple2;
import betterquesting.client.gui2.editors.GuiQuestLinesEditor;
import betterquesting.client.gui2.editors.designer.GuiDesigner;
import betterquesting.network.handlers.NetQuestAction;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLineDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GuiQuestLines extends GuiScreenCanvas implements IPEventListener, INeedsRefresh
{
    private IQuestLine selectedLine = null;
    private static int selectedLineId = -1;

    private final List<Tuple2<DBEntry<IQuestLine>, Integer>> visChapters = new ArrayList<>();

    private CanvasQuestLine cvQuest;    //显示在页面上的章节图
    //这里不太好翻译，tray的字面意义是“浅盘”，这里应该是指页面的可动子界面
    // Keep these separate for now
    private static CanvasHoverTray cvChapterTray;   //显示任务章节，点击按钮从左侧弹出
    private static CanvasHoverTray cvDescTray;  //显示描述，点击按钮从左侧弹出
    private static CanvasHoverTray cvFrame; //显示任务路线图

    private CanvasScrolling cvDesc; //章节描述
    private PanelVScrollBar scDesc;
    private CanvasScrolling cvLines;
    private PanelVScrollBar scLines;

    private PanelGeneric icoChapter;
    private PanelTextBox txTitle;
    private PanelTextBox txDesc;

    private PanelButton claimAll;   //一键让所有任务都完成

    private static boolean trayLock = false;

    private final List<PanelButtonStorage<DBEntry<IQuestLine>>> btnListRef = new ArrayList<>();

    public GuiQuestLines(GuiScreen parent)
    {
        super(parent);
    }

    @Override
    public void refreshGui()
    {
        refreshChapterVisibility();
        refreshContent();
    }

    @Override
    public void initPanel()
    {
        super.initPanel();

        if(selectedLineId >= 0)
        {
            selectedLine = QuestLineDatabase.INSTANCE.getValue(selectedLineId);
            if(selectedLine == null) selectedLineId = -1;
        } else
        {
            selectedLine = null;
        }

        boolean canEdit = QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(mc.thePlayer);  //目前是否可编辑
        boolean preOpen = false;
        if(trayLock && cvChapterTray != null && cvChapterTray.isTrayOpen()) preOpen = true;
        if(trayLock && cvDescTray != null && cvDescTray.isTrayOpen()) preOpen = true;

        PEventBroadcaster.INSTANCE.register(this, PEventButton.class);

        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
        //退出按钮
        PanelButton btnExit = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, 8, -24, 32, 16, 0), -1, "").setIcon(PresetIcon.ICON_PG_PREV.getTexture());
        btnExit.setClickAction((b) -> mc.displayGuiScreen(parent));
        btnExit.setTooltip(Collections.singletonList(QuestTranslation.translate("gui.back")));
        cvBackground.addPanel(btnExit);
        //如果可编辑，就在界面左下添加两个小按钮
        if(canEdit)
        {
            PanelButton btnEdit = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, 8, -40, 16, 16, 0), -1, "").setIcon(PresetIcon.ICON_GEAR.getTexture());
            btnEdit.setClickAction((b) -> mc.displayGuiScreen(new GuiQuestLinesEditor(this)));
            btnEdit.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.edit")));
            cvBackground.addPanel(btnEdit);

            PanelButton btnDesign = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, 24, -40, 16, 16, 0), -1, "").setIcon(PresetIcon.ICON_SORT.getTexture());
            btnDesign.setClickAction((b) -> {
                if(selectedLine != null) mc.displayGuiScreen(new GuiDesigner(this, selectedLine));
            });
            btnDesign.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.designer")));
            cvBackground.addPanel(btnDesign);
        }
        //应该是左上角显示任务章节名称的区域，不选中任务章节则不显示
        txTitle = new PanelTextBox(new GuiTransform(new Vector4f(0F, 0F, 0.5F, 0F), new GuiPadding(60, 12, 0, -24), 0), "");
        txTitle.setColor(PresetColor.TEXT_HEADER.getColor());
        cvBackground.addPanel(txTitle);

        icoChapter = new PanelGeneric(new GuiTransform(GuiAlign.TOP_LEFT, 40, 8, 16, 16, 0), null);
        cvBackground.addPanel(icoChapter);
        //显示任务的主界面
        cvFrame = new CanvasHoverTray(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(40 + 150 + 24, 24, 8, 8), 0), new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(40, 24, 8, 8), 0), PresetTexture.AUX_FRAME_0.getTexture());
        cvFrame.setManualOpen(true);
        //CanvasTextured cvFrame = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(40, 24, 8, 8), 0), PresetTexture.AUX_FRAME_0.getTexture());
        cvBackground.addPanel(cvFrame);
        cvFrame.setTrayState(!preOpen, 1);
        // These would probably be more annoying than useful if you just wanted to check a tray but not lose your position
        //cvFrame.setOpenAction(() -> cvQuest.fitToWindow());
        //cvFrame.setCloseAction(() -> cvQuest.fitToWindow());

        // === CHAPTER TRAY === 任务章节显示区域

        boolean oldState1 = trayLock && cvChapterTray != null && cvChapterTray.isTrayOpen();
        cvChapterTray = new CanvasHoverTray(new GuiTransform(GuiAlign.LEFT_EDGE, new GuiPadding(40, 24, -24, 8), -1), new GuiTransform(GuiAlign.LEFT_EDGE, new GuiPadding(40, 24, -40 - 150 - 24, 8), -1), PresetTexture.PANEL_INNER.getTexture());
        cvChapterTray.setManualOpen(true);
        cvChapterTray.setOpenAction(() -> {
            cvDescTray.setTrayState(false, 200);
            cvFrame.setTrayState(false, 200);
            buildChapterList();
        });
        cvBackground.addPanel(cvChapterTray);
        //展示任务章节的可滑动区域
        cvLines = new CanvasScrolling(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(8, 8, 16, 8), 0));
        cvChapterTray.getCanvasOpen().addPanel(cvLines);
        //任务章节的滑块
        scLines = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-16, 8, 8, 8), 0));
        cvLines.setScrollDriverY(scLines);
        cvChapterTray.getCanvasOpen().addPanel(scLines);

        // === DESCRIPTION TRAY === 章节描述显示区块

        boolean oldState2 = trayLock && cvDescTray != null && cvDescTray.isTrayOpen();
        cvDescTray = new CanvasHoverTray(new GuiTransform(GuiAlign.LEFT_EDGE, new GuiPadding(40, 24, -24, 8), -1), new GuiTransform(GuiAlign.LEFT_EDGE, new GuiPadding(40, 24, -40 - 150 - 24, 8), -1), PresetTexture.PANEL_INNER.getTexture());
        cvDescTray.setManualOpen(true);
        cvDescTray.setOpenAction(() -> {
            cvChapterTray.setTrayState(false, 200);
            cvFrame.setTrayState(false, 200);
            cvDesc.resetCanvas();
            if(selectedLine != null)
            {
                txDesc = new PanelTextBox(new GuiRectangle(0, 0, cvDesc.getTransform().getWidth(), 0, 0), QuestTranslation.translate(selectedLine.getUnlocalisedDescription()), true);
                txDesc.setColor(PresetColor.TEXT_AUX_0.getColor());//.setFontSize(10);
                cvDesc.addCulledPanel(txDesc, false);
                cvDesc.refreshScrollBounds();
                scDesc.setEnabled(cvDesc.getScrollBounds().getHeight() > 0);
            } else
            {
                scDesc.setEnabled(false);
            }
        });
        cvBackground.addPanel(cvDescTray);

        cvDesc = new CanvasScrolling(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(8, 8, 20, 8), 0));
        cvDescTray.getCanvasOpen().addPanel(cvDesc);

        scDesc = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-16, 8, 8, 8), 0));
        cvDesc.setScrollDriverY(scDesc);
        cvDescTray.getCanvasOpen().addPanel(scDesc);

        // === LEFT SIDEBAR === 左侧工作区
        //打开章节表的按钮
        PanelButton btnTrayToggle = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, 8, 24, 32, 16, 0), -1, "");
        btnTrayToggle.setIcon(PresetIcon.ICON_BOOKMARK.getTexture(), selectedLineId < 0 ? new GuiColorPulse(0xFFFFFFFF, 0xFF444444, 2F, 0F) : new GuiColorStatic(0xFFFFFFFF), 0);
        btnTrayToggle.setClickAction((b) -> {
            cvFrame.setTrayState(cvChapterTray.isTrayOpen(), 200);
            cvChapterTray.setTrayState(!cvChapterTray.isTrayOpen(), 200);
            btnTrayToggle.setIcon(PresetIcon.ICON_BOOKMARK.getTexture());
        });
        btnTrayToggle.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.title.quest_lines")));
        cvBackground.addPanel(btnTrayToggle);
        //打开章节描述的按钮
        PanelButton btnDescToggle = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, 8, 40, 32, 16, 0), -1, "").setIcon(PresetIcon.ICON_DESC.getTexture());
        btnDescToggle.setClickAction((b) -> {
            cvFrame.setTrayState(cvDescTray.isTrayOpen(), 200);
            cvDescTray.setTrayState(!cvDescTray.isTrayOpen(), 200);
        });
        btnDescToggle.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.gui.description")));
        cvBackground.addPanel(btnDescToggle);
        //将任务图放在中间的按钮
        PanelButton fitView = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, 8, 72, 32, 16, -2), 5, "");
        fitView.setIcon(PresetIcon.ICON_BOX_FIT.getTexture());
        fitView.setClickAction((b) -> {
            if(cvQuest.getQuestLine() != null) cvQuest.fitToWindow();
        });
        fitView.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.zoom_fit")));
        cvBackground.addPanel(fitView);
        //
        claimAll = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, 8, 56, 32, 16, -2), -1, "");
        claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture());
        claimAll.setClickAction((b) -> {
            if(cvQuest.getQuestButtons().size() <= 0) return;
            List<Integer> claimIdList = new ArrayList<>();
            for(PanelButtonQuest pbQuest : cvQuest.getQuestButtons())
            {
                IQuest q = pbQuest.getStoredValue().getValue();
                if(q.getRewards().size() > 0 && q.canClaim(mc.thePlayer)) claimIdList.add(pbQuest.getStoredValue().getID());
            }

            int[] cIDs = new int[claimIdList.size()];
            for(int i = 0; i < cIDs.length; i++)
            {
                cIDs[i] = claimIdList.get(i);
            }

            NetQuestAction.requestClaim(cIDs);
            claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
        });
        claimAll.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.claim_all")));
        cvBackground.addPanel(claimAll);

        // The Jester1147 button 锁定tray的按钮（这个有什么用暂时没看懂）
        PanelButton btnTrayLock = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, 8, 88, 32, 16, -2), -1, "").setIcon(trayLock ? PresetIcon.ICON_LOCKED.getTexture() : PresetIcon.ICON_UNLOCKED.getTexture());
        btnTrayLock.setClickAction((b) -> {
            trayLock = !trayLock;
            b.setIcon(trayLock ? PresetIcon.ICON_LOCKED.getTexture() : PresetIcon.ICON_UNLOCKED.getTexture());
        });
        btnTrayLock.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.lock_tray")));
        cvBackground.addPanel(btnTrayLock);

        // === CHAPTER VIEWPORT === 显示任务章节图

        CanvasQuestLine oldCvQuest = cvQuest;
        cvQuest = new CanvasQuestLine(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), 2);
        cvFrame.addPanel(cvQuest);

        if(selectedLine != null)
        {
            cvQuest.setQuestLine(selectedLine);

            if(oldCvQuest != null)
            {
                cvQuest.setZoom(oldCvQuest.getZoom());
                cvQuest.setScrollX(oldCvQuest.getScrollX());
                cvQuest.setScrollY(oldCvQuest.getScrollY());
                cvQuest.refreshScrollBounds();
                cvQuest.updatePanelScroll();
            }

            txTitle.setText(QuestTranslation.translate(selectedLine.getUnlocalisedName()));
            icoChapter.setTexture(new OreDictTexture(1F, selectedLine.getProperty(NativeProps.ICON), false, true), null);
        }

        // === MISC ===

        cvChapterTray.setTrayState(oldState1, 1);
        cvDescTray.setTrayState(oldState2, 1);

        refreshChapterVisibility();
        refreshClaimAll();
    }

    @Override
    public void onPanelEvent(PanelEvent event)
    {
        if(event instanceof PEventButton)
        {
            onButtonPress((PEventButton)event);
        }
    }

    // TODO: Change CanvasQuestLine to NOT need these panel events anymore
    private void onButtonPress(PEventButton event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        IPanelButton btn = event.getButton();

        if(btn.getButtonID() == 2 && btn instanceof PanelButtonStorage) // Quest Instance Select
        {
            @SuppressWarnings("unchecked")
            DBEntry<IQuest> quest = ((PanelButtonStorage<DBEntry<IQuest>>)btn).getStoredValue();
            GuiHome.bookmark = new GuiQuest(this, quest.getID());

            mc.displayGuiScreen(GuiHome.bookmark);
        }
    }

    private void refreshChapterVisibility()
    {
        boolean canEdit = QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(mc.thePlayer);
        List<DBEntry<IQuestLine>> lineList = QuestLineDatabase.INSTANCE.getSortedEntries();
        this.visChapters.clear();
        UUID playerID = QuestingAPI.getQuestingUUID(mc.thePlayer);

        for(DBEntry<IQuestLine> dbEntry : lineList)
        {
            IQuestLine ql = dbEntry.getValue();
            EnumQuestVisibility vis = ql.getProperty(NativeProps.VISIBILITY);
            if(!canEdit && vis == EnumQuestVisibility.HIDDEN) continue;

            boolean show = false;
            boolean unlocked = false;
            boolean complete = false;
            boolean allComplete = true;
            boolean pendingClaim = false;

            if(canEdit)
            {
                show = true;
                unlocked = true;
                complete = true;
            }

            for(DBEntry<IQuestLineEntry> qID : ql.getEntries())
            {
                IQuest q = QuestDatabase.INSTANCE.getValue(qID.getID());
                if(q == null) continue;

                if(allComplete && !q.isComplete(playerID)) allComplete = false;
                if(!pendingClaim && q.isComplete(playerID) && !q.hasClaimed(playerID)) pendingClaim = true;
                if(!unlocked && q.isUnlocked(playerID)) unlocked = true;
                if(!complete && q.isComplete(playerID)) complete = true;
                if(!show && QuestCache.isQuestShown(q, playerID, mc.thePlayer)) show = true;
                if(unlocked && complete && show && pendingClaim && !allComplete) break;
            }

            if(vis == EnumQuestVisibility.COMPLETED && !complete)
            {
                continue;
            } else if(vis == EnumQuestVisibility.UNLOCKED && !unlocked)
            {
                continue;
            }

            int val = pendingClaim ? 1 : 0;
            if(allComplete) val |= 2;
            if(!show) val |= 4;

            visChapters.add(new Tuple2<>(dbEntry, val));
        }

        if(cvChapterTray.isTrayOpen()) buildChapterList();
    }

    private void buildChapterList()
    {
        cvLines.resetCanvas();
        btnListRef.clear();

        int listW = cvLines.getTransform().getWidth();

        for(int n = 0; n < visChapters.size(); n++)
        {
            DBEntry<IQuestLine> entry = visChapters.get(n).getFirst();
            int vis = visChapters.get(n).getSecond();

            cvLines.addPanel(new PanelGeneric(new GuiRectangle(0, n * 16, 16, 16, 0), new OreDictTexture(1F, entry.getValue().getProperty(NativeProps.ICON), false, true)));

            if((vis & 1) > 0)
            {
                cvLines.addPanel(new PanelGeneric(new GuiRectangle(8, n * 16 + 8, 8, 8, -1), new GuiTextureColored(PresetIcon.ICON_NOTICE.getTexture(), new GuiColorStatic(0xFFFFFF00))));
            } else if((vis & 2) > 0)
            {
                cvLines.addPanel(new PanelGeneric(new GuiRectangle(8, n * 16 + 8, 8, 8, -1), new GuiTextureColored(PresetIcon.ICON_TICK.getTexture(), new GuiColorStatic(0xFF00FF00))));
            }
            PanelButtonStorage<DBEntry<IQuestLine>> btnLine = new PanelButtonStorage<>(new GuiRectangle(16, n * 16, listW - 16, 16, 0), 1, QuestTranslation.translate(entry.getValue().getUnlocalisedName()), entry);
            btnLine.setTextAlignment(0);
            btnLine.setActive((vis & 4) == 0 && entry.getID() != selectedLineId);
            btnLine.setCallback((q) -> {
                btnListRef.forEach((b) -> {if(b.getStoredValue().getID() == selectedLineId) b.setActive(true);});
                btnLine.setActive(false);
                selectedLine = q.getValue();
                selectedLineId = q.getID();
                cvQuest.setQuestLine(q.getValue());
                icoChapter.setTexture(new OreDictTexture(1F, q.getValue().getProperty(NativeProps.ICON), false, true), null);
                txTitle.setText(QuestTranslation.translate(q.getValue().getUnlocalisedName()));
                if(!trayLock)
                {
                    cvFrame.setTrayState(true, 200);
                    cvChapterTray.setTrayState(false, 200);
                    cvQuest.fitToWindow();
                }
                refreshClaimAll();
            });
            cvLines.addPanel(btnLine);
            btnListRef.add(btnLine);
        }

        cvLines.refreshScrollBounds();
        scLines.setEnabled(cvLines.getScrollBounds().getHeight() > 0);
    }

    private void refreshContent()
    {
        if(selectedLineId >= 0)
        {
            selectedLine = QuestLineDatabase.INSTANCE.getValue(selectedLineId);
            if(selectedLine == null) selectedLineId = -1;
        } else
        {
            selectedLine = null;
        }

        float zoom = cvQuest.getZoom();
        int sx = cvQuest.getScrollX();
        int sy = cvQuest.getScrollY();
        /*if(cvQuest.getQuestLine() != selectedLine)*/ cvQuest.setQuestLine(selectedLine);
        cvQuest.setZoom(zoom);
        cvQuest.setScrollX(sx);
        cvQuest.setScrollY(sy);
        cvQuest.refreshScrollBounds();
        cvQuest.updatePanelScroll();

        if(selectedLine != null)
        {
            txTitle.setText(QuestTranslation.translate(selectedLine.getUnlocalisedName()));
            icoChapter.setTexture(new OreDictTexture(1F, selectedLine.getProperty(NativeProps.ICON), false, true), null);
        } else
        {
            txTitle.setText("");
            icoChapter.setTexture(null, null);
        }

        refreshClaimAll();
    }

    private void refreshClaimAll()
    {
        if(cvQuest.getQuestLine() == null || cvQuest.getQuestButtons().size() <= 0)
        {
            claimAll.setActive(false);
            claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
            return;
        }

        for(PanelButtonQuest btn : cvQuest.getQuestButtons())
        {
            if(btn.getStoredValue().getValue().canClaim(mc.thePlayer))
            {
                claimAll.setActive(true);
                claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorPulse(0xFFFFFFFF, 0xFF444444, 2F, 0F), 0);
                return;
            }
        }

        claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
        claimAll.setActive(false);
    }
}
