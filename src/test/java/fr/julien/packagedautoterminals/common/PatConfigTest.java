package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import fr.julien.packagedautoterminals.Reference;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** A config file written by an older version of the mod, with a setting removed since, still loads. */
public class PatConfigTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void aKeyWithNoFieldLeftInTheFileIsIgnored() throws Exception {
        File file = folder.newFile(Reference.MOD_ID + ".cfg");
        Files.write(file.toPath(), Arrays.asList(
                "general {",
                "    I:refreshTicks=40",
                "    I:removedSetting=8",
                "}"), StandardCharsets.UTF_8);
        int refreshTicks = PatConfig.refreshTicks;
        try {
            sync(file);
            assertEquals(40, PatConfig.refreshTicks);
        } finally {
            PatConfig.refreshTicks = refreshTicks;
        }
    }

    /**
     * What {@code ConfigManager.sync(modid, type)} does at startup. The public method needs the
     * FML loader to find the file, and the {@code Configuration} constructor needs the game
     * folder, which only the launcher sets.
     */
    private void sync(File file) throws Exception {
        Field gameFolder = FMLInjectionData.class.getDeclaredField("minecraftHome");
        gameFolder.setAccessible(true);
        gameFolder.set(null, folder.getRoot());
        Configuration config = new Configuration(file);
        Method sync = ConfigManager.class.getDeclaredMethod("sync", Configuration.class, Class.class,
                String.class, String.class, boolean.class, Object.class);
        sync.setAccessible(true);
        sync.invoke(null, config, PatConfig.class, Reference.MOD_ID, Configuration.CATEGORY_GENERAL, true, null);
    }
}
