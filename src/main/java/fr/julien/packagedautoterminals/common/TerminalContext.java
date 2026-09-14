package fr.julien.packagedautoterminals.common;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.helpers.WirelessTerminalGuiObject;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

/**
 * D'où vient le terminal ouvert : une part posée sur un câble, ou un objet sans fil.
 *
 * <p>Cette classe existe pour que les conteneurs ignorent la différence. Les deux sources
 * implémentent {@link IActionHost}, donc donnent leur nœud de grille de la même façon. Seule
 * la réouverture de la fenêtre diffère, car {@code openGui} ne transporte que trois entiers.
 *
 * <p>PIÈGE d'AE2, contourné ici : {@code WirelessRegistry.openWirelessTerminalGui} caste le
 * gestionnaire rendu par {@code IWirelessTermHandler.getGuiHandler} en {@code GuiBridge},
 * une énumération interne. Un mod tiers qui passerait par cette méthode recevrait une
 * {@code ClassCastException}. Le terminal sans fil ouvre donc sa fenêtre lui-même.
 */
public final class TerminalContext {

    private final IActionHost host;
    /** Part câblée : position de la tuile hôte. */
    private final BlockPos pos;
    /** Part câblée : face de la part. */
    private final int side;
    /** Objet sans fil : emplacement dans l'inventaire du joueur. Sinon -1. */
    private final int inventorySlot;

    private TerminalContext(IActionHost host, BlockPos pos, int side, int inventorySlot) {
        this.host = host;
        this.pos = pos;
        this.side = side;
        this.inventorySlot = inventorySlot;
    }

    public static TerminalContext ofPart(PartPatTerminal part) {
        return new TerminalContext(part, part.getTile().getPos(), part.getSide().ordinal(), -1);
    }

    public static TerminalContext ofWireless(WirelessTerminalGuiObject object) {
        return new TerminalContext(object, BlockPos.ORIGIN, 0, object.getInventorySlot());
    }

    public boolean isWireless() {
        return inventorySlot >= 0;
    }

    public IActionHost host() {
        return host;
    }

    /** Grille du terminal, ou {@code null} s'il n'est relié à rien. */
    public IGrid grid() {
        IGridNode node = host.getActionableNode();
        return node == null ? null : node.getGrid();
    }

    /**
     * Raison de fermer la fenêtre, ou {@code null} si tout va bien.
     *
     * <p>PIÈGE corrigé, premier temps : les contrôles de portée et d'énergie existaient,
     * mais aucun conteneur ne les appelait. Sorti de portée, le terminal sans fil restait
     * ouvert et vide, et il ne consommait jamais d'énergie.
     *
     * <p>PIÈGE corrigé, second temps : **l'ordre des trois questions compte**. Le bytecode
     * d'AE2UEL montre que {@code WirelessTerminalGuiObject.getActionableNode} appelle
     * {@code rangeCheck} puis rend {@code null} quand aucun point d'accès n'est à portée.
     * Interroger la grille en premier faisait donc annoncer une absence de liaison à chaque
     * fois que le joueur s'éloignait. La portée passe devant.
     *
     * <p>L'énergie vient en dernier, donc elle n'est prélevée que si le reste est bon.
     * Facturer un terminal hors de portée n'aurait aucun sens.
     *
     * @param ticks nombre de ticks écoulés depuis le dernier appel.
     */
    public String refusal(int ticks) {
        if (host instanceof WirelessTerminalGuiObject
                && !((WirelessTerminalGuiObject) host).rangeCheck()) {
            return "gui.packagedautoterminals.out_of_range";
        }
        if (grid() == null) {
            return "gui.packagedautoterminals.not_linked";
        }
        if (!drainPower(PatConfig.wirelessPowerPerTick * Math.max(1, ticks))) {
            return "gui.packagedautoterminals.no_power";
        }
        return null;
    }

    /**
     * Prévient le joueur, puis laisse AE2 refermer la fenêtre.
     *
     * <p>Nous n'appelons pas {@code closeScreen} nous-mêmes, en pleine mise à jour du
     * conteneur. `AEBaseContainer.canInteractWith` rend faux dès que le conteneur est
     * déclaré invalide, et le serveur referme alors la fenêtre à son propre rythme.
     */
    public static void refuse(net.minecraft.entity.player.EntityPlayerMP player, String key) {
        player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentTranslation(key), false);
    }

    /**
     * Prélève l'énergie d'un terminal sans fil.
     *
     * <p>Le terminal câblé, lui, ne consomme rien de plus : la grille paie déjà son nœud.
     *
     * @return vrai si l'énergie était disponible.
     */
    public boolean drainPower(double amount) {
        if (!(host instanceof WirelessTerminalGuiObject)) {
            return true;
        }
        WirelessTerminalGuiObject wireless = (WirelessTerminalGuiObject) host;
        return wireless.extractAEPower(amount, Actionable.MODULATE, PowerMultiplier.CONFIG)
                >= amount - 0.01;
    }

    public void openTerminal(EntityPlayer player) {
        open(player, isWireless() ? PatGuiHandler.WIRELESS : PatGuiHandler.TERMINAL + side);
    }

    public void openEditor(EntityPlayer player) {
        open(player, isWireless()
                ? PatGuiHandler.WIRELESS_EDITOR
                : PatGuiHandler.EDITOR + side);
    }

    /**
     * Pour la part, {@code openGui} transporte la position du bloc. Pour le sans-fil, il n'y
     * a pas de bloc : l'emplacement d'inventaire prend la place de la coordonnée x.
     */
    private void open(EntityPlayer player, int id) {
        if (isWireless()) {
            player.openGui(PackagedAutoTerminals.instance, id, player.world, inventorySlot, 0, 0);
        } else {
            player.openGui(PackagedAutoTerminals.instance, id, player.world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
