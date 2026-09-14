package fr.julien.packagedautoterminals;

/** Mod identity constants. The version is injected at build time by ForgeGradle. */
public final class Reference {

    public static final String MOD_ID = "packagedautoterminals";
    public static final String MOD_NAME = "PackagedAuto Terminals";
    public static final String VERSION = "@MOD_VERSION@";

    /**
     * Network channel name. It must never exceed 20 characters: on 1.12.2,
     * CPacketCustomPayload reads the channel name with buf.readString(20). A longer
     * name disconnects the player on a dedicated server with
     * "The received string length is longer than maximum allowed".
     * MOD_ID is 21 characters long, so it cannot be reused here.
     */
    public static final String CHANNEL = "pat_terminals";

    /** Modid of AE2 Unofficial Extended Life. Same as the official AE2 one. */
    public static final String AE2 = "appliedenergistics2";
    public static final String PACKAGED_AUTO = "packagedauto";

    /** Optional addons. Each integration is detected by its modid (decision D07). */
    public static final String PACKAGED_EX_CRAFTING = "packagedexcrafting";
    public static final String PACKAGED_AVARITIA = "packagedavaritia";
    public static final String PACKAGED_FLUID_CRAFTING = "packagedfluidcrafting";
    public static final String PACKAGING_PROVIDER = "packagingprovider";
    public static final String AE2WUT = "ae2wut";

    private Reference() {}
}
