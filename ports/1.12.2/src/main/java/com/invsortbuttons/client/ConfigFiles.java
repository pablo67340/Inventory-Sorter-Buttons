package com.invsortbuttons.client;

import com.invsortbuttons.InvSortButtons;
import com.invsortbuttons.network.NetworkHandler;
import com.invsortbuttons.network.SyncConfigPacket;
import com.invsortbuttons.sort.PlayerSortConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The editable config files, like the original's InvTweaksRules.txt and
 * InvTweaksTree.txt. Defaults regenerate when a file is missing, and edits are
 * synced to the server on the next sort (checked by modification time).
 */
public final class ConfigFiles {
    private static final String RULES_NAME = "InvSortButtonsRules.txt";
    private static final String TREE_NAME = "InvSortButtonsTree.txt";
    private static final String SHORTCUTS_NAME = "InvSortButtonsShortcuts.txt";

    private static long lastRulesModified = -1;
    private static long lastTreeModified = -1;

    private ConfigFiles() {
    }

    public static Path rulesFile() {
        return Loader.instance().getConfigDir().toPath().resolve(RULES_NAME);
    }

    public static Path treeFile() {
        return Loader.instance().getConfigDir().toPath().resolve(TREE_NAME);
    }

    public static Path shortcutsFile() {
        return Loader.instance().getConfigDir().toPath().resolve(SHORTCUTS_NAME);
    }

    public static void ensureDefaults() {
        writeIfMissing(rulesFile(), "default_rules.txt");
        writeIfMissing(treeFile(), "default_tree.xml");
        writeIfMissing(shortcutsFile(), "default_shortcuts.txt");
    }

    private static void writeIfMissing(Path file, String resource) {
        try {
            if (!Files.exists(file)) {
                Files.write(file, PlayerSortConfig.readResource(resource)
                        .getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            InvSortButtons.LOGGER.warn("Could not write default config {}: {}", file, e.toString());
        }
    }

    public static void openRules() {
        ensureDefaults();
        openInEditor(rulesFile());
    }

    public static void openTree() {
        ensureDefaults();
        openInEditor(treeFile());
    }

    public static void openShortcuts() {
        ensureDefaults();
        openInEditor(shortcutsFile());
    }

    private static void openInEditor(Path file) {
        try {
            java.awt.Desktop.getDesktop().open(file.toFile());
        } catch (Exception e) {
            InvSortButtons.LOGGER.warn("Could not open {}: {}", file, e.toString());
        }
    }

    /** Send the current files to the server if they changed (or when forced on login). */
    public static void syncToServer(boolean force) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getConnection() == null) {
            return;
        }
        ensureDefaults();
        try {
            long rulesModified = Files.getLastModifiedTime(rulesFile()).toMillis();
            long treeModified = Files.getLastModifiedTime(treeFile()).toMillis();
            if (!force && rulesModified == lastRulesModified && treeModified == lastTreeModified) {
                return;
            }
            lastRulesModified = rulesModified;
            lastTreeModified = treeModified;
            NetworkHandler.CHANNEL.sendToServer(new SyncConfigPacket(
                    readFile(rulesFile()), readFile(treeFile())));
            InvSortButtons.LOGGER.info("Sorting config sent to server");
        } catch (Exception e) {
            InvSortButtons.LOGGER.warn("Could not sync sorting config: {}", e.toString());
        }
    }

    private static String readFile(Path file) throws java.io.IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
