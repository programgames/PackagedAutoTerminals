package fr.julien.packagedautoterminals.network;

import fr.julien.packagedautoterminals.Reference;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class PatNetwork {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);

    private PatNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(PacketProviderList.Handler.class, PacketProviderList.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(PacketRecipeAction.Handler.class, PacketRecipeAction.class, 1, Side.SERVER);
        CHANNEL.registerMessage(PacketEditorSlot.Handler.class, PacketEditorSlot.class, 2, Side.SERVER);
        CHANNEL.registerMessage(PacketEditorFill.Handler.class, PacketEditorFill.class, 3, Side.SERVER);
        CHANNEL.registerMessage(PacketRenameGroup.Handler.class, PacketRenameGroup.class, 4, Side.SERVER);
    }
}
