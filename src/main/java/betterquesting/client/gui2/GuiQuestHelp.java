package betterquesting.client.gui2;

import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.controls.PanelButtonStorage;
import betterquesting.api2.client.gui.help.HelpRegistry;
import betterquesting.api2.client.gui.help.HelpTopic;
import betterquesting.api2.client.gui.misc.GuiAlign;
import betterquesting.api2.client.gui.misc.GuiPadding;
import betterquesting.api2.client.gui.misc.GuiRectangle;
import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.utils.QuestTranslation;
import net.minecraft.client.gui.GuiScreen;
/*应该是那个指导书的页面*/
public class GuiQuestHelp extends GuiScreenCanvas
{
    public GuiQuestHelp(GuiScreen parent)
    {
        super(parent);
    }

	@Override
	public void initPanel()
    {
        super.initPanel();

        // Background panel
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
        //指导书名
        PanelTextBox panTxt = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 16, 0, -32), 0), QuestTranslation.translate("item.betterquesting.guide.name")).setAlignment(1);
        panTxt.setColor(PresetColor.TEXT_HEADER.getColor());
        cvBackground.addPanel(panTxt);
        //返回按钮
        PanelButton btnBack = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -16, 200, 16, 0), 0, QuestTranslation.translate("gui.back"))
        {
            @Override
            public void onButtonClick()
            {
                mc.displayGuiScreen(parent);
            }
        };
        cvBackground.addPanel(btnBack);
        //按钮列表区域
        CanvasScrolling cvTopics = new CanvasScrolling(new GuiTransform(GuiAlign.LEFT_EDGE, new GuiPadding(16, 32, -108, 24), 0));
        cvBackground.addPanel(cvTopics);
        //按钮列表的滑块
        PanelVScrollBar scTopic = new PanelVScrollBar(new GuiTransform(GuiAlign.LEFT_EDGE, new GuiPadding(108, 32, -116, 24), 0));
        cvBackground.addPanel(scTopic);
        cvTopics.setScrollDriverY(scTopic);
        //说明文字区域
        CanvasScrolling cvDesc = new CanvasScrolling(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(124, 48, 24, 24), 0)).enableBlocking(false);
        cvBackground.addPanel(cvDesc);
        //说明文字区域的滑块
        PanelVScrollBar scDesc = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-24, 48, 16, 24), 0));
        cvBackground.addPanel(scDesc);
        cvDesc.setScrollDriverY(scDesc);
        //说明文字的标题
        PanelTextBox txtTitle = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(124, 36, 16, -48), 0), "").setAlignment(1);
        txtTitle.setColor(PresetColor.TEXT_HEADER.getColor());
        cvBackground.addPanel(txtTitle);
        //说明文字的内容
        PanelTextBox txtDesc = new PanelTextBox(new GuiRectangle(0, 0, cvDesc.getTransform().getWidth(), 16, 0), "", true);
        txtDesc.setColor(PresetColor.TEXT_MAIN.getColor());
        cvDesc.addPanel(txtDesc);
        //加载所有说明文字列表到cvTopics并生成对应按钮
        int width = cvTopics.getTransform().getWidth();
        HelpTopic[] topics = HelpRegistry.INSTANCE.getTopics();
        for(int i = 0; i < topics.length; i++)
        {
            HelpTopic ht = topics[i];
            cvTopics.addPanel(new PanelButtonStorage<>(new GuiRectangle(0, i * 16, width, 16, 0), -1, ht.getTitle(), ht).setCallback(value -> {
                txtTitle.setText(value.getTitle());
                txtDesc.setText(value.getDescription());
                cvDesc.refreshScrollBounds();
            }));
        }
    }
}
