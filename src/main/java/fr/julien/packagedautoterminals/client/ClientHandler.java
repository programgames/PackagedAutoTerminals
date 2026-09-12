package fr.julien.packagedautoterminals.client;

import java.util.ArrayList;
import java.util.List;

import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.container.ContainerPatTerminal;
import fr.julien.packagedautoterminals.network.PacketProviderList;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Application côté client de l'instantané envoyé par le serveur. */
@SideOnly(Side.CLIENT)
public final class ClientHandler {

    private ClientHandler() {}

    public static void applyProviderList(PacketProviderList message) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null
                || !(minecraft.player.openContainer instanceof ContainerPatTerminal)) {
            return;
        }
        ContainerPatTerminal container = (ContainerPatTerminal) minecraft.player.openContainer;
        List<ProviderSnapshot> providers = new ArrayList<>();
        NBTTagList list = message.payload.getTagList("Providers", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            providers.add(ProviderSnapshot.readFromNBT(tag));
        }
        container.providers = providers;
        container.lastPayloadBytes = message.payloadBytes;
    }
}
