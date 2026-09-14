package fr.julien.packagedautoterminals.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
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

    /**
     * Bloc à montrer pour un type de recette.
     *
     * <p>La classe de la tuile sert au diagnostic ; elle ne donne aucune image. Le nom
     * enregistré du bloc, lui, donne un {@link ItemStack} même quand aucune machine de ce
     * genre n'est posée sur le réseau. Ces noms viennent des fichiers de langue des mods,
     * lus dans leurs jars, et non d'un souvenir.
     */
    private static final Map<ResourceLocation, ResourceLocation> BLOCK_BY_TYPE = new HashMap<>();

    /** Images déjà construites. Le dessin passe ici à chaque image de jeu. */
    private static final Map<ResourceLocation, ItemStack> ICON_CACHE = new HashMap<>();

    static {
        map("packagedauto", "crafting", "thelm.packagedauto.tile.TileCrafter",
                "packagedauto", "crafter");

        map("packagedexcrafting", "basic", "thelm.packagedexcrafting.tile.TileBasicCrafter",
                "packagedexcrafting", "basic_crafter");
        map("packagedexcrafting", "advanced", "thelm.packagedexcrafting.tile.TileAdvancedCrafter",
                "packagedexcrafting", "advanced_crafter");
        map("packagedexcrafting", "elite", "thelm.packagedexcrafting.tile.TileEliteCrafter",
                "packagedexcrafting", "elite_crafter");
        map("packagedexcrafting", "ultimate", "thelm.packagedexcrafting.tile.TileUltimateCrafter",
                "packagedexcrafting", "ultimate_crafter");
        map("packagedexcrafting", "combination", "thelm.packagedexcrafting.tile.TileCombinationCrafter",
                "packagedexcrafting", "combination_crafter");
        map("packagedexcrafting", "ender", "thelm.packagedexcrafting.tile.TileEnderCrafter",
                "packagedexcrafting", "ender_crafter");

        map("packagedavaritia", "extreme", "thelm.packagedavaritia.tile.TileExtremeCrafter",
                "packagedavaritia", "extreme_crafter");
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

    private static void map(String modid, String type, String machineClass,
                            String blockModid, String blockName) {
        ResourceLocation key = new ResourceLocation(modid, type);
        MACHINE_BY_TYPE.put(key, machineClass);
        BLOCK_BY_TYPE.put(key, new ResourceLocation(blockModid, blockName));
    }

    /**
     * Image de la machine qui exécute ce type, ou {@link ItemStack#EMPTY}.
     *
     * <p>Le résultat est vide dans deux cas : le type n'exige aucune machine connue, comme
     * {@code processing}, ou l'addon qui porte la machine n'est pas installé. L'appelant
     * revient alors au nom écrit du type.
     */
    public static ItemStack iconFor(IRecipeType type) {
        if (type == null) {
            return ItemStack.EMPTY;
        }
        ResourceLocation key = type.getName();
        ItemStack cached = ICON_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        ResourceLocation block = BLOCK_BY_TYPE.get(key);
        ItemStack icon = ItemStack.EMPTY;
        if (block != null) {
            Item item = ForgeRegistries.ITEMS.getValue(block);
            if (item != null) {
                icon = new ItemStack(item);
            }
        }
        ICON_CACHE.put(key, icon);
        return icon;
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
