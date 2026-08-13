package com.invsortbuttons.sort;

import com.invsortbuttons.InvSortButtons;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** A player's parsed sorting configuration: item tree + inventory rules. */
public final class PlayerSortConfig {
    private static PlayerSortConfig defaultConfig;

    public final ItemTree tree;
    public final RuleSet rules;

    private PlayerSortConfig(ItemTree tree, RuleSet rules) {
        this.tree = tree;
        this.rules = rules;
    }

    /** Parse user texts; anything broken falls back to the bundled defaults. */
    public static PlayerSortConfig parse(String rulesText, String treeText) {
        PlayerSortConfig fallback = getDefault();
        ItemTree tree;
        try {
            tree = ItemTree.parse(treeText);
        } catch (Exception e) {
            InvSortButtons.LOGGER.warn("Broken item tree file, using default: {}", e.toString());
            tree = fallback.tree;
        }
        RuleSet rules;
        try {
            rules = RuleSet.parse(rulesText, tree);
        } catch (Exception e) {
            InvSortButtons.LOGGER.warn("Broken rules file, using default: {}", e.toString());
            rules = fallback.rules;
        }
        return new PlayerSortConfig(tree, rules);
    }

    public static synchronized PlayerSortConfig getDefault() {
        if (defaultConfig == null) {
            try {
                ItemTree tree = ItemTree.parse(readResource("default_tree.xml"));
                RuleSet rules = RuleSet.parse(readResource("default_rules.txt"), tree);
                defaultConfig = new PlayerSortConfig(tree, rules);
            } catch (Exception e) {
                throw new IllegalStateException("Bundled default sorting config is broken", e);
            }
        }
        return defaultConfig;
    }

    public static String readResource(String name) {
        try (InputStream in = PlayerSortConfig.class.getResourceAsStream(
                "/assets/" + InvSortButtons.MOD_ID + "/config/" + name)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Missing bundled config resource " + name, e);
        }
    }
}
