package com.invsortbuttons.client;

import com.invsortbuttons.InvSortButtons;
import com.invsortbuttons.network.NetworkHandler;
import com.invsortbuttons.network.SyncConfigPacket;
import com.invsortbuttons.sort.PlayerSortConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;

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
        return FMLPaths.CONFIGDIR.get().resolve(RULES_NAME);
    }

    public static Path treeFile() {
        return FMLPaths.CONFIGDIR.get().resolve(TREE_NAME);
    }

    public static Path shortcutsFile() {
        return FMLPaths.CONFIGDIR.get().resolve(SHORTCUTS_NAME);
    }

    public static void ensureDefaults() {
        writeIfMissing(rulesFile(), "default_rules.txt");
        writeIfMissing(treeFile(), "default_tree.xml");
        writeIfMissing(shortcutsFile(), "default_shortcuts.txt");
    }

    private static void writeIfMissing(Path file, String resource) {
        try {
            if (!Files.exists(file)) {
                Files.writeString(file, PlayerSortConfig.readResource(resource));
            }
        } catch (Exception e) {
            InvSortButtons.LOGGER.warn("Could not write default config {}: {}", file, e.toString());
        }
    }

    public static void openRules() {
        ensureDefaults();
        Util.getPlatform().openFile(rulesFile().toFile());
    }

    public static void openTree() {
        ensureDefaults();
        Util.getPlatform().openFile(treeFile().toFile());
    }

    public static void openShortcuts() {
        ensureDefaults();
        Util.getPlatform().openFile(shortcutsFile().toFile());
    }

    /** Send the current files to the server if they changed (or when forced on login). */
    public static void syncToServer(boolean force) {
        Minecraft mc = Minecraft.getInstance();
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
                    Files.readString(rulesFile()), Files.readString(treeFile())));
            InvSortButtons.LOGGER.info("Sorting config sent to server");
        } catch (Exception e) {
            InvSortButtons.LOGGER.warn("Could not sync sorting config: {}", e.toString());
        }
    }
}
