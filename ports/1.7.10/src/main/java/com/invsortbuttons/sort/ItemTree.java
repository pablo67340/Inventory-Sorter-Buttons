package com.invsortbuttons.sort;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBoat;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraftforge.oredict.OreDictionary;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Port of the InvTweaks item tree (MIT). Same XML format as InvTweaksTree.txt,
 * modernized: instead of numeric IDs, leaves match by registry name
 * ({@code id="minecraft:stone"}), ore dictionary name ({@code tag="ingotIron"}),
 * or item class ({@code class="sword"}). Categories are just nested elements;
 * their names become the keywords usable in the rules file, and the depth-first
 * order of leaves defines the sort order.
 */
public final class ItemTree {
    private final Node root;
    private final List<Node> leaves = new ArrayList<>();
    private final Map<String, Node> byName = new HashMap<>();
    private final Map<Item, Integer> orderCache = new ConcurrentHashMap<>();

    private ItemTree(Node root) {
        this.root = root;
    }

    public static ItemTree parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Element rootEl = doc.getDocumentElement();
        ItemTree tree = new ItemTree(new Node(rootEl.getTagName().toLowerCase(), null, null, null, 0));
        tree.register(tree.root);
        tree.buildChildren(rootEl, tree.root);
        return tree;
    }

    private void buildChildren(Element parentEl, Node parent) {
        NodeList children = parentEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) {
                continue;
            }
            Element el = (Element) children.item(i);
            Node node = new Node(el.getTagName().toLowerCase(),
                    emptyToNull(el.getAttribute("id")),
                    emptyToNull(el.getAttribute("tag")),
                    emptyToNull(el.getAttribute("class")),
                    parent.depth + 1);
            parent.children.add(node);
            this.register(node);
            if (node.isLeaf()) {
                node.leafOrder = this.leaves.size();
                this.leaves.add(node);
            }
            this.buildChildren(el, node);
        }
    }

    private void register(Node node) {
        node.keywordOrder = this.byName.size();
        this.byName.putIfAbsent(node.name, node);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    /** Sort order for a stack: DFS index of the first matching leaf, or MAX_VALUE. */
    public int getItemOrder(ItemStack stack) {
        return this.orderCache.computeIfAbsent(stack.getItem(), item -> {
            ItemStack probe = new ItemStack(item);
            for (Node leaf : this.leaves) {
                if (leaf.matchesItem(probe)) {
                    return leaf.leafOrder;
                }
            }
            return Integer.MAX_VALUE;
        });
    }

    /** True if the stack belongs to the keyword's subtree (or matches a raw id/#tag keyword). */
    public boolean matches(ItemStack stack, String keyword) {
        if (stack == null) {
            return false;
        }
        if (keyword.startsWith("#")) {
            return oreDictMatches(stack, keyword.substring(1));
        }
        if (keyword.contains(":")) {
            String id = registryName(stack.getItem());
            return id != null && id.equals(keyword);
        }
        Node node = this.byName.get(keyword);
        if (node == null) {
            return false;
        }
        if (node == this.root) {
            return true;
        }
        return this.subtreeMatches(node, stack);
    }

    private static String registryName(Item item) {
        return item == null ? null : Item.itemRegistry.getNameForObject(item);
    }

    /** 1.7.10: "tags" are ore dictionary names ("ingotIron"); a namespace prefix is ignored. */
    private static boolean oreDictMatches(ItemStack stack, String tagName) {
        if (stack == null) {
            return false;
        }
        String bare = tagName.contains(":") ? tagName.substring(tagName.indexOf(':') + 1) : tagName;
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            if (OreDictionary.getOreName(oreId).equalsIgnoreCase(bare)) {
                return true;
            }
        }
        return false;
    }

    private boolean subtreeMatches(Node node, ItemStack stack) {
        if (node.isLeaf() && node.matchesItem(stack)) {
            return true;
        }
        for (Node child : node.children) {
            if (this.subtreeMatches(child, stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isKeywordValid(String keyword) {
        return this.byName.containsKey(keyword);
    }

    public int getKeywordDepth(String keyword) {
        Node node = this.byName.get(keyword);
        return node == null ? 0 : node.depth;
    }

    public int getKeywordOrder(String keyword) {
        Node node = this.byName.get(keyword);
        return node == null ? 0 : node.keywordOrder;
    }

    private static final class Node {
        final String name;
        final String id;
        final String tag;
        final String clazz;
        final int depth;
        final List<Node> children = new ArrayList<>();
        int leafOrder = -1;
        int keywordOrder;

        Node(String name, String id, String tag, String clazz, int depth) {
            this.name = name;
            this.id = id;
            this.tag = tag;
            this.clazz = clazz;
            this.depth = depth;
        }

        boolean isLeaf() {
            return this.id != null || this.tag != null || this.clazz != null;
        }

        boolean matchesItem(ItemStack stack) {
            if (this.id != null) {
                String key = registryName(stack.getItem());
                if (key != null && key.equals(this.id)) {
                    return true;
                }
            }
            if (this.tag != null && oreDictMatches(stack, this.tag)) {
                return true;
            }
            if (this.clazz != null) {
                return matchesClass(stack, this.clazz);
            }
            return false;
        }

        private static boolean matchesClass(ItemStack stack, String clazz) {
            Item item = stack.getItem();
            switch (clazz) {
                case "sword":
                    return item instanceof ItemSword;
                case "bow":
                    return item instanceof ItemBow;
                case "pickaxe":
                    return item instanceof ItemPickaxe;
                case "shovel":
                    return item instanceof ItemSpade;
                case "axe":
                    return item instanceof ItemAxe;
                case "hoe":
                    return item instanceof ItemHoe;
                case "tool":
                    return item instanceof ItemTool;
                case "shears":
                    return item instanceof ItemShears;
                case "fishingrod":
                    return item instanceof ItemFishingRod;
                case "flintandsteel":
                    return item instanceof ItemFlintAndSteel;
                case "helmet":
                    return isArmorFor(item, 0);
                case "chestplate":
                    return isArmorFor(item, 1);
                case "leggings":
                    return isArmorFor(item, 2);
                case "boots":
                    return isArmorFor(item, 3);
                case "armor":
                    return item instanceof ItemArmor;
                case "food":
                    return item instanceof ItemFood;
                case "potion":
                    return item instanceof ItemPotion;
                case "bucket":
                    return item instanceof ItemBucket;
                case "minecart":
                    return item instanceof ItemMinecart;
                case "boat":
                    return item instanceof ItemBoat;
                case "record":
                    return item instanceof ItemRecord;
                case "block":
                    return item instanceof ItemBlock;
                default:
                    // shield/crossbow/trident etc. don't exist in 1.7.10
                    return false;
            }
        }

        private static boolean isArmorFor(Item item, int armorType) {
            return item instanceof ItemArmor && ((ItemArmor) item).armorType == armorType;
        }
    }
}
