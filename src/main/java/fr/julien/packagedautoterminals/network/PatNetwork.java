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
    }
}
