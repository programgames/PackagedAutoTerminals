package fr.julien.packagedautoterminals;

/** Constantes d'identité du mod. La version est injectée au build par ForgeGradle. */
public final class Reference {

    public static final String MOD_ID = "packagedautoterminals";
    public static final String MOD_NAME = "PackagedAuto Terminals";
    public static final String VERSION = "@MOD_VERSION@";

    /**
     * Nom du canal reseau. Il ne doit jamais depasser 20 caracteres : en 1.12.2,
     * CPacketCustomPayload lit le nom du canal avec buf.readString(20). Un nom plus
     * long deconnecte le joueur sur un serveur dedie avec
     * "The received string length is longer than maximum allowed".
     * MOD_ID fait 21 caracteres, donc on ne peut pas le reutiliser ici.
     */
    public static final String CHANNEL = "pat_terminals";

    /** Modid d'AE2 Unofficial Extended Life. Identique à celui d'AE2 officiel. */
    public static final String AE2 = "appliedenergistics2";
    public static final String PACKAGED_AUTO = "packagedauto";

    /** Addons facultatifs. Chaque intégration se détecte par son modid (décision D07). */
    public static final String PACKAGED_EX_CRAFTING = "packagedexcrafting";
    public static final String PACKAGED_AVARITIA = "packagedavaritia";
    public static final String PACKAGED_FLUID_CRAFTING = "packagedfluidcrafting";
    public static final String PACKAGING_PROVIDER = "packagingprovider";
    public static final String AE2WUT = "ae2wut";

    private Reference() {}
}
