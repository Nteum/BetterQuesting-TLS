package betterquesting.items;

import betterquesting.api2.client.gui.GuiScreenTest;
import betterquesting.api2.client.gui.themes.gui_args.GArgsNone;
import betterquesting.api2.client.gui.themes.presets.PresetGUIs;
import betterquesting.client.gui2.GuiHome;
import betterquesting.api.storage.BQ_Settings;
import betterquesting.client.themes.ThemeRegistry;
import betterquesting.core.BetterQuesting;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
/**任务指导书
 * 这本书右键可以打开任务界面，主要用于打开任务按键有冲突的情况。
 * 注：这本指导书之前是一个单独的mod，链接在这儿：https://github.com/Funwayguy/BetterQuesting
 * */
public class ItemQuestBook extends Item {

    public static boolean hasEffect;

    public ItemQuestBook() {
        this.setTextureName("betterquesting:questbook");
        this.setUnlocalizedName("betterquesting.quest_book");
        this.setCreativeTab(BetterQuesting.tabQuesting);
        hasEffect = true;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            Minecraft mc = Minecraft.getMinecraft();
            if(mc.thePlayer.isSneaking() && mc.thePlayer.getCommandSenderName().equalsIgnoreCase("Funwayguy"))
            {
                mc.displayGuiScreen(new GuiScreenTest(mc.currentScreen));
            } else
            {
                if(BQ_Settings.useBookmark && GuiHome.bookmark != null)
                {
                    mc.displayGuiScreen(GuiHome.bookmark);
                } else
                {
                    mc.displayGuiScreen(ThemeRegistry.INSTANCE.getGui(PresetGUIs.HOME, GArgsNone.NONE));
                }
            }
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return hasEffect;
    }

    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register)
    {
        itemIcon = register.registerIcon(this.getIconString());
    }
}
