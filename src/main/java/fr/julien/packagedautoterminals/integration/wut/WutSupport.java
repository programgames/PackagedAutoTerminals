package fr.julien.packagedautoterminals.integration.wut;

import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.common.PatConfig;
import fr.julien.packagedautoterminals.common.PatItems;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Loader;

/**
 * Pont vers AE2 Wireless Universal Terminal, le mod qui fond plusieurs terminaux sans fil
 * en un seul objet.
 *
 * <p>AE2WUT n'expose **aucune** interface d'extension. La preuve est dans son bytecode, et
 * la révision R4 de {@code docs/DECISIONS.md} la consigne :
 *
 * <ul>
 *   <li>{@code getAllMode()} construit une liste d'entiers écrite en dur, 0 à 9 ;
 *   <li>{@code getWirelessName(int)} est un {@code tableswitch} de 1 à 9 ;
 *   <li>{@code AllWUTRecipe.getIngredient()} remplit une table écrite en dur.
 * </ul>
 *
 * <p>Trois greffes suffisent pourtant, car les trois méthodes rendent une valeur que l'on
 * peut compléter. Le reste d'AE2WUT est déjà générique : la molette lit le tableau
 * {@code modes} de l'objet, et la recette d'assemblage se construit à partir de la table des
 * ingrédients.
 */
public final class WutSupport {

    /** Nom enregistré de l'objet d'AE2WUT. */
    private static final ResourceLocation ITEM =
            new ResourceLocation(Reference.AE2WUT, "wireless_universal_terminal");

    /** Classe de l'objet d'AE2WUT, atteinte par réflexion seulement. */
    private static final String ITEM_CLASS =
            "com.circulation.ae2wut.item.ItemWirelessUniversalTerminal";

    private static final String KEY_MODE = "mode";
    private static final String KEY_MODES = "modes";

    /**
     * Identifiant de notre mode, lu une seule fois.
     *
     * <p>PIÈGE : les greffes tournent pendant les événements de registre, qui précèdent le
     * {@code preInit} de notre mod. Or Forge ne remplit {@link PatConfig} qu'au {@code
     * preInit}. Lire le réglage sans précaution donnerait la valeur par défaut au moment de
     * l'enregistrement, puis la valeur réglée plus tard : deux identifiants pour un seul
     * terminal. On force donc la lecture du fichier, puis on retient le résultat.
     */
    private static Integer mode;

    private WutSupport() {}

    public static boolean isLoaded() {
        return Loader.isModLoaded(Reference.AE2WUT);
    }

    public static synchronized int mode() {
        if (mode == null) {
            try {
                ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
            } catch (Throwable ignored) {
                // Le fichier n'existe pas encore : la valeur par défaut fera l'affaire.
            }
            mode = PatConfig.wutModeId;
        }
        return mode;
    }

    /** Un exemplaire de notre terminal sans fil, pour la recette d'assemblage. */
    public static ItemStack terminal() {
        return new ItemStack(PatItems.WIRELESS_TERMINAL);
    }

    /**
     * Nom affiché à la suite de celui de l'objet, dans le style d'AE2WUT.
     *
     * <p>La traduction passe par {@code util.text.translation.I18n}, et non par la classe
     * du même nom dans {@code client.resources}. Cette dernière n'existe pas sur un serveur
     * dédié : la charger là-bas arrêterait le jeu. Celle-ci vit des deux côtés.
     */
    @SuppressWarnings("deprecation")
    public static String modeName() {
        return "§6("
                + I18n.translateToLocal("item.packagedautoterminals.wireless_pat_terminal.name")
                + ")";
    }

    public static boolean isUniversalTerminal(ItemStack stack) {
        return !stack.isEmpty() && ITEM.equals(stack.getItem().getRegistryName());
    }

    /**
     * Cet objet est-il un terminal universel réglé sur notre mode, et qui le porte vraiment ?
     *
     * <p>Les deux questions comptent. {@code mode} dit ce que le joueur a choisi à la
     * molette ; {@code modes} dit ce que l'objet a réellement absorbé. Sans la seconde, un
     * terminal universel neuf ouvrirait notre fenêtre sans jamais avoir avalé notre terminal.
     */
    public static boolean isOurMode(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return hasOurMode(stack) && tag.getInteger(KEY_MODE) == mode();
    }

    /**
     * Ce terminal universel **affiche-t-il** notre mode ?
     *
     * <p>Le rendu ne regarde que le mode courant, et non le tableau {@code modes}. Un objet
     * de création, réglé sur notre mode sans nous avoir absorbés, doit porter notre image
     * plutôt qu'un modèle manquant.
     */
    public static boolean showsOurMode(ItemStack stack) {
        if (!isUniversalTerminal(stack)) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getInteger(KEY_MODE) == mode();
    }

    /**
     * Ce terminal universel a-t-il **absorbé** notre terminal, quel que soit son mode courant ?
     *
     * <p>La touche d'ouverture s'appuie sur cette question, et non sur {@link #isOurMode}. Le
     * joueur qui appuie sur notre touche veut notre terminal, sans avoir à tourner la
     * molette d'abord.
     */
    public static boolean hasOurMode(ItemStack stack) {
        if (!isUniversalTerminal(stack)) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return false;
        }
        for (int absorbed : tag.getIntArray(KEY_MODES)) {
            if (absorbed == mode()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Bascule un terminal universel sur notre mode.
     *
     * <p>Écrire {@code mode} dans le NBT ne suffit pas. AE2WUT range la grille de craft du
     * mode quitté dans son cache, par {@code nbtChangeB}, puis restaure celle du mode
     * demandé, par {@code nbtChange}. Sauter ces deux appels ferait perdre au joueur la
     * grille du terminal de craft qu'il quitte.
     *
     * <p>Les deux méthodes sont appelées par réflexion. AE2WUT n'est pas sur notre chemin de
     * compilation, et ne doit pas y être : voir la décision D33. Si elles disparaissent, on
     * se replie sur l'écriture simple, et le mod continue de fonctionner.
     */
    public static void switchToOurMode(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return;
        }
        if (!callWut(stack)) {
            tag.setInteger(KEY_MODE, mode());
        }
    }

    /** Vrai si les deux méthodes d'AE2WUT ont bien tourné. */
    private static boolean callWut(ItemStack stack) {
        try {
            Class<?> type = Class.forName(ITEM_CLASS);
            Field instanceField = type.getField("INSTANCE");
            Object instance = instanceField.get(null);
            Method stash = type.getMethod("nbtChangeB", ItemStack.class);
            Method apply = type.getMethod("nbtChange", ItemStack.class, int.class);
            stash.invoke(instance, stack);
            apply.invoke(instance, stack, mode());
            return true;
        } catch (Throwable missing) {
            PackagedAutoTerminals.LOGGER.warn(
                    "AE2WUT a change de forme : bascule de mode simplifiee.", missing);
            return false;
        }
    }
}
