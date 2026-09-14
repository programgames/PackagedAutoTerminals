package fr.julien.packagedautoterminals.common;

import appeng.api.AEApi;
import appeng.api.util.AEColor;
import appeng.client.render.StaticItemColor;
import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.item.ItemPatTerminal;
import fr.julien.packagedautoterminals.item.ItemWirelessPatTerminal;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class PatItems {

    public static final CreativeTabs TAB = new CreativeTabs(Reference.MOD_ID) {
        // Mappings snapshot_20171003: the method is named getTabIconItem, not createIcon.
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(TERMINAL);
        }
    };

    public static final ItemPatTerminal TERMINAL = new ItemPatTerminal();
    public static final ItemWirelessPatTerminal WIRELESS_TERMINAL = new ItemWirelessPatTerminal();

    private PatItems() {}

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        TERMINAL.setRegistryName(Reference.MOD_ID, "pat_terminal");
        TERMINAL.setUnlocalizedName(Reference.MOD_ID + ".pat_terminal");
        TERMINAL.setCreativeTab(TAB);
        event.getRegistry().register(TERMINAL);

        WIRELESS_TERMINAL.setRegistryName(Reference.MOD_ID, "wireless_pat_terminal");
        WIRELESS_TERMINAL.setUnlocalizedName(Reference.MOD_ID + ".wireless_pat_terminal");
        WIRELESS_TERMINAL.setCreativeTab(TAB);
        event.getRegistry().register(WIRELESS_TERMINAL);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(TERMINAL, 0,
                new ModelResourceLocation(TERMINAL.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(WIRELESS_TERMINAL, 0,
                new ModelResourceLocation(WIRELESS_TERMINAL.getRegistryName(), "inventory"));
    }

    /**
     * Gives the terminal item its colour.
     *
     * <p>The model inherits from the AE2 {@code item/part/display}. That model paints
     * nothing: it stacks three white layers and asks the game for the colour of tint indexes
     * 1 to 4. Without this handler the game answers "no tint", and the item comes out
     * **all white**.
     *
     * <p>{@code AEColor.TRANSPARENT} is the tint of unpainted parts: fluix purple.
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerItemColors(ColorHandlerEvent.Item event) {
        event.getItemColors().registerItemColorHandler(
                new StaticItemColor(AEColor.TRANSPARENT), TERMINAL);
    }

    /**
     * Registers the wireless terminal with the AE2 registry.
     *
     * <p>This registration drives the binding, the energy and the range. It does **not**
     * open the screen: see the explanation in {@link ItemWirelessPatTerminal}.
     */
    public static void registerWirelessHandler() {
        AEApi.instance().registries().wireless().registerWirelessHandler(WIRELESS_TERMINAL);
    }

    /**
     * AE2 only bakes the part models that are declared to it. Without this call the part is
     * invisible in the world, with no error at all in the log.
     */
    public static void registerPartModels() {
        AEApi.instance().registries().partModels()
                .registerModels(PartPatTerminal.MODEL_OFF, PartPatTerminal.MODEL_ON);
    }
}
