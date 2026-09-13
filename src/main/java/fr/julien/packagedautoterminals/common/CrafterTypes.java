package fr.julien.packagedautoterminals.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;
import thelm.packagedauto.api.IRecipeType;

/**
 * Correspondance « type de recette → machine qui l'exécute ».
 *
 * <p>Cette table est **écrite à la main**, et c'est assumé. La révision R1 en donne la
 * preuve : l'API ne permet pas de la déduire.
 *
 * <ul>
 *   <li>{@code IPackageCraftingMachine.acceptPackage(…, boolean)} n'est pas un mode
 *       simulation. Sa version par défaut délègue à la méthode à trois arguments, qui
 *       exécute réellement le craft. Aucun essai à blanc n'est donc possible.
 *   <li>{@code IRecipeType.getRepresentation()} désigne la station d'origine du craft, par
 *       exemple la table de travail vanilla ou celle d'Extended Crafting, et non le Package
 *       Crafter correspondant.
 * </ul>
 *
 * <p>Les machines sont désignées par le **nom** de leur classe, jamais par la classe
 * elle-même : le mod compile et tourne donc sans les addons. Un type inconnu ne produit
 * aucun diagnostic, plutôt qu'un diagnostic faux.
 */
public final class CrafterTypes {

    private static final Map<ResourceLocation, String> MACHINE_BY_TYPE = new HashMap<>();

    static {
        map("packagedauto", "crafting", "thelm.packagedauto.tile.TileCrafter");

        map("packagedexcrafting", "basic", "thelm.packagedexcrafting.tile.TileBasicCrafter");
        map("packagedexcrafting", "advanced", "thelm.packagedexcrafting.tile.TileAdvancedCrafter");
        map("packagedexcrafting", "elite", "thelm.packagedexcrafting.tile.TileEliteCrafter");
        map("packagedexcrafting", "ultimate", "thelm.packagedexcrafting.tile.TileUltimateCrafter");
        map("packagedexcrafting", "combination", "thelm.packagedexcrafting.tile.TileCombinationCrafter");
        map("packagedexcrafting", "ender", "thelm.packagedexcrafting.tile.TileEnderCrafter");

        map("packagedavaritia", "extreme", "thelm.packagedavaritia.tile.TileExtremeCrafter");
    }

    /**
     * Machines qui **transmettent** un colis sans rien fabriquer.
     *
     * <ul>
     *   <li>le Positioned Package Distributor envoie chaque colis à une position marquée,
     *       pour le type de recette {@code Positioned} ;
     *   <li>le Package Crafting Machine Proxy relaie vers une machine désignée par un
     *       marqueur, souvent trop loin pour toucher l'Unpackager.
     * </ul>
     *
     * <p>Les deux acceptent des colis, donc l'API les donne pour des machines d'exécution.
     * Les mêler aux crafters ferait croire qu'ils savent produire.
     */
    private static final java.util.Set<String> ROUTERS = new java.util.HashSet<>(
            java.util.Arrays.asList(
                    "thelm.packagedauto.tile.TileDistributor",
                    "thelm.packagedauto.tile.TileCraftingProxy"));

    private CrafterTypes() {}

    /** Cette machine se contente-t-elle d'aiguiller les colis ? */
    public static boolean isRouter(String machineClass) {
        return ROUTERS.contains(machineClass);
    }

    private static void map(String modid, String type, String machineClass) {
        MACHINE_BY_TYPE.put(new ResourceLocation(modid, type), machineClass);
    }

    /** Nom de la classe de machine attendue, ou {@code null} si le type est inconnu. */
    public static String machineClassFor(IRecipeType type) {
        return type == null ? null : MACHINE_BY_TYPE.get(type.getName());
    }

    /**
     * Le type exige-t-il une machine que nous sachions reconnaître ?
     *
     * <p>Un type sans machine, comme {@code processing}, envoie ses colis vers un inventaire
     * quelconque : aucun diagnostic n'a de sens pour lui.
     */
    public static boolean isDiagnosable(IRecipeType type) {
        return type != null && type.hasMachine() && MACHINE_BY_TYPE.containsKey(type.getName());
    }

    /** Table complète, en lecture seule. Utile aux tests et au journal de démarrage. */
    public static Map<ResourceLocation, String> all() {
        return Collections.unmodifiableMap(MACHINE_BY_TYPE);
    }
}
