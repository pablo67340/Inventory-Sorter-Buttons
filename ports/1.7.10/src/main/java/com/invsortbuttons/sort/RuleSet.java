package com.invsortbuttons.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Port of InvTweaksConfigInventoryRuleset / InvTweaksConfigSortingRule (MIT).
 *
 * The inventory is a 36-slot grid, 9 per row: rows a-c are the main inventory,
 * row d is the hotbar. Constraints: "a1" tile, "a" row, "1" column, "a1-c4"
 * rectangle ("v" fills vertically, "r" reverses). Keywords come from the item
 * tree, or are raw registry names / #tags. "LOCKED"/"FROZEN" pin slots.
 */
public final class RuleSet {
    public static final int GRID_SIZE = 36;
    public static final int ROW_SIZE = 9;

    // Rule specificity, like the original: tile > column > row > rectangle
    private static final int PRIO_RECTANGLE = 1_000_000;
    private static final int PRIO_ROW = 2_000_000;
    private static final int PRIO_COLUMN = 3_000_000;
    private static final int PRIO_TILE = 4_000_000;

    public static final class Rule {
        private final int[] preferredSlots;
        private final String keyword;
        private final int priority;

        public Rule(int[] preferredSlots, String keyword, int priority) {
            this.preferredSlots = preferredSlots;
            this.keyword = keyword;
            this.priority = priority;
        }

        public int[] preferredSlots() {
            return this.preferredSlots;
        }

        public String keyword() {
            return this.keyword;
        }

        public int priority() {
            return this.priority;
        }
    }

    private final List<Rule> rules = new ArrayList<>();
    private final boolean[] frozen = new boolean[GRID_SIZE];

    public List<Rule> getRules() {
        return this.rules;
    }

    public boolean isFrozen(int gridSlot) {
        return this.frozen[gridSlot];
    }

    public static RuleSet parse(String text, ItemTree tree) {
        RuleSet set = new RuleSet();
        for (String rawLine : text.split("\r?\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("|")
                    || line.startsWith("=") || line.startsWith("/")) {
                continue;
            }
            set.registerLine(line, tree);
        }
        set.rules.sort(Comparator.comparingInt(Rule::priority).reversed());
        return set;
    }

    private void registerLine(String rawLine, ItemTree tree) {
        String lineText = rawLine.replaceAll("\\s+", " ").toLowerCase();
        String[] words = lineText.split(" ");
        if (words.length != 2) {
            return;
        }
        boolean validConstraint = lineText.matches("^([a-d]|[1-9]|[r]){1,2} [\\w:.#/-]*$")
                || lineText.matches("^[a-d][1-9]-[a-d][1-9][rv]?[rv]? [\\w:.#/-]*$");
        if (!validConstraint) {
            return;
        }
        String constraint = words[0];
        String keyword = words[1];

        if (keyword.equals("locked") || keyword.equals("frozen")) {
            for (int slot : getPreferredPositions(constraint)) {
                this.frozen[slot] = true;
            }
            return;
        }

        boolean valid = tree.isKeywordValid(keyword)
                || keyword.contains(":") || keyword.startsWith("#");
        if (!valid) {
            // The original's keyword variants: plural and wood/gold spellings
            for (String variant : keywordVariants(keyword)) {
                if (tree.isKeywordValid(variant)) {
                    keyword = variant;
                    valid = true;
                    break;
                }
            }
        }
        if (!valid) {
            return;
        }

        int typePriority = getTypePriority(constraint);
        int priority = typePriority + 100000
                + tree.getKeywordDepth(keyword) * 1000 - tree.getKeywordOrder(keyword);
        this.rules.add(new Rule(getPreferredPositions(constraint), keyword, priority));
    }

    private static List<String> keywordVariants(String keyword) {
        List<String> variants = new ArrayList<>();
        if (keyword.endsWith("es")) {
            variants.add(keyword.substring(0, keyword.length() - 2));
        }
        if (keyword.endsWith("s")) {
            variants.add(keyword.substring(0, keyword.length() - 1));
        }
        if (keyword.contains("en")) {
            variants.add(keyword.replaceAll("en", ""));
        } else {
            if (keyword.contains("wood")) {
                variants.add(keyword.replaceAll("wood", "wooden"));
            }
            if (keyword.contains("gold")) {
                variants.add(keyword.replaceAll("gold", "golden"));
            }
        }
        return variants;
    }

    private static int getTypePriority(String constraint) {
        String bare = constraint.replace("r", "");
        if (constraint.length() == 1 || (constraint.length() == 2 && constraint.contains("r"))) {
            return bare.charAt(0) >= '1' && bare.charAt(0) <= '9' ? PRIO_COLUMN : PRIO_ROW;
        }
        if (constraint.length() > 4) {
            return PRIO_RECTANGLE;
        }
        return PRIO_TILE;
    }

    /** Port of getRulePreferredPositions for the 36/9 inventory grid. */
    static int[] getPreferredPositions(String constraint) {
        int columnCount = GRID_SIZE / ROW_SIZE;

        if (constraint.length() >= 5) {
            boolean vertical = constraint.contains("v");
            boolean reverse = constraint.contains("r");
            String bare = constraint.replaceAll("[rv]", "");
            String[] parts = bare.split("-");
            int[] a = getPreferredPositions(parts[0]);
            int[] b = getPreferredPositions(parts[1]);
            int x1 = a[0] % ROW_SIZE;
            int y1 = a[0] / ROW_SIZE;
            int x2 = b[0] % ROW_SIZE;
            int y2 = b[0] / ROW_SIZE;
            if (vertical) {
                int t1 = x1;
                x1 = y1;
                y1 = t1;
                int t2 = x2;
                x2 = y2;
                y2 = t2;
            }
            int[] result = new int[(Math.abs(y2 - y1) + 1) * (Math.abs(x2 - x1) + 1)];
            int idx = 0;
            int y = y1;
            while (y1 < y2 ? y <= y2 : y >= y2) {
                int x = x1;
                while (x1 < x2 ? x <= x2 : x >= x2) {
                    result[idx++] = vertical ? x * ROW_SIZE + y : y * ROW_SIZE + x;
                    x += x1 < x2 ? 1 : -1;
                }
                y += y1 < y2 ? 1 : -1;
            }
            if (reverse) {
                reverseArray(result);
            }
            return result;
        }

        int column = -1;
        int row = -1;
        boolean reverse = false;
        for (int i = 0; i < constraint.length(); i++) {
            char c = constraint.charAt(i);
            if (c >= '1' && c <= '9') {
                column = c - '1';
            } else if (c >= 'a' && c <= 'd') {
                row = c - 'a';
            } else if (c == 'r') {
                reverse = true;
            }
        }
        if (column != -1 && row != -1) {
            return new int[]{row * ROW_SIZE + column};
        }
        if (row != -1) {
            int[] result = new int[ROW_SIZE];
            for (int i = 0; i < ROW_SIZE; i++) {
                result[i] = row * ROW_SIZE + (reverse ? ROW_SIZE - 1 - i : i);
            }
            return result;
        }
        // Column rules fill bottom-to-top by default, like the original
        int[] result = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            result[i] = (reverse ? i : columnCount - 1 - i) * ROW_SIZE + column;
        }
        return result;
    }

    private static void reverseArray(int[] data) {
        for (int left = 0, right = data.length - 1; left < right; left++, right--) {
            int t = data[left];
            data[left] = data[right];
            data[right] = t;
        }
    }
}
