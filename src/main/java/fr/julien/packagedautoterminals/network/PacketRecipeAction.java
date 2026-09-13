package fr.julien.packagedautoterminals.network;

import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import fr.julien.packagedautoterminals.container.ContainerPatTerminal;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Ordre d'édition, du client vers le serveur.
 *
 * <p>Le client envoie une **intention**, jamais un NBT de recette (décision D05). Il désigne
 * la machine par sa position, et non par un identifiant de session : l'ordre d'un scan peut
 * changer entre deux rafraîchissements. Le serveur vérifie ensuite que cette machine est
 * bien sur la grille du terminal, et que le joueur a le droit d'y toucher.
 */
public class PacketRecipeAction implements IMessage {

    /** Supprime la recette d'indice {@link #index}. */
    public static final byte ACTION_REMOVE = 0;
    /** Ouvre l'éditeur sur la recette d'indice {@link #index}. */
    public static final byte ACTION_EDIT = 1;
    /** Écrit la recette de l'éditeur, puis revient au terminal. */
    public static final byte ACTION_SAVE = 2;
    /** Change le type de recette dans l'éditeur. {@link #index} vaut 1 en avant, 0 en arrière. */
    public static final byte ACTION_CYCLE_TYPE = 3;
    /** Ajoute une recette à cette machine, et ouvre l'éditeur dessus. */
    public static final byte ACTION_NEW = 4;
    /** Retire le porte-recettes de cette machine, et le range dans le réseau. */
    public static final byte ACTION_REMOVE_HOLDER = 5;
    /** Referme l'éditeur et rouvre le terminal. */
    public static final byte ACTION_BACK = 6;
    /** Supprime la recette en cours d'édition. */
    public static final byte ACTION_DELETE = 7;
    /** Vide la grille de l'éditeur, sans rien écrire. */
    public static final byte ACTION_CLEAR = 8;
    /** Déplace la rangée d'onglets. {@link #index} vaut 1 en avant, 0 en arrière. */
    public static final byte ACTION_SCROLL_TABS = 9;

    public int dimension;
    public BlockPos pos = BlockPos.ORIGIN;
    public int index;
    public byte action;

    public PacketRecipeAction() {}

    public PacketRecipeAction(int dimension, BlockPos pos, int index, byte action) {
        this.dimension = dimension;
        this.pos = pos;
        this.index = index;
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        pos = BlockPos.fromLong(buf.readLong());
        index = buf.readInt();
        action = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeLong(pos.toLong());
        buf.writeInt(index);
        buf.writeByte(action);
    }

    public static class Handler implements IMessageHandler<PacketRecipeAction, IMessage> {
        @Override
        public IMessage onMessage(PacketRecipeAction message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerPatTerminal) {
                    ContainerPatTerminal container = (ContainerPatTerminal) player.openContainer;
                    if (message.action == ACTION_REMOVE) {
                        container.removeRecipe(message.dimension, message.pos, message.index);
                    } else if (message.action == ACTION_EDIT) {
                        container.openEditor(message.dimension, message.pos, message.index);
                    } else if (message.action == ACTION_NEW) {
                        container.newRecipe(message.dimension, message.pos);
                    } else if (message.action == ACTION_REMOVE_HOLDER) {
                        container.removeHolder(message.dimension, message.pos);
                    }
                    return;
                }
                if (player.openContainer instanceof ContainerPatEditor) {
                    ContainerPatEditor editor = (ContainerPatEditor) player.openContainer;
                    if (message.action == ACTION_SAVE) {
                        // On reste dans l'éditeur : le joueur doit voir le message, et
                        // pouvoir enchaîner une seconde modification.
                        editor.save();
                    } else if (message.action == ACTION_BACK) {
                        editor.backToTerminal();
                    } else if (message.action == ACTION_CYCLE_TYPE) {
                        editor.cycleRecipeType(message.index == 1);
                    } else if (message.action == ACTION_DELETE) {
                        editor.deleteCurrent();
                    } else if (message.action == ACTION_CLEAR) {
                        editor.clearGrid();
                    } else if (message.action == ACTION_SCROLL_TABS) {
                        editor.scrollTabs(message.index == 1);
                    }
                }
            });
            return null;
        }
    }
}
