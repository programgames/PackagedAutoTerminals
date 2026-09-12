package fr.julien.packagedautoterminals.client;

import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Vérifie que le fichier de langue du mod est bien chargé. Côté client seulement. */
@SideOnly(Side.CLIENT)
public final class ClientDiagnostics {

    private ClientDiagnostics() {}

    public static void logTranslations() {
        String key = "item.packagedautoterminals.pat_terminal.name";
        String value = I18n.format(key);
        if (key.equals(value)) {
            PackagedAutoTerminals.LOGGER.error(
                    "Les traductions du mod NE sont PAS chargees : {} reste brut.", key);
        } else {
            PackagedAutoTerminals.LOGGER.info("Traductions chargees : {} = {}", key, value);
        }
    }
}
