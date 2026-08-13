package com.invsortbuttons.sort;

import net.minecraft.util.ResourceLocation;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BoatItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MinecartItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraftforge.registries.ForgeRegistries;
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
 * ({@code id="minecraft:stone"}), item tag ({@code tag="forge:ores"}), or item
 * class ({@code class="sword"}). Categories are just nested elements; their
 * names become the keywords usable in the rules file, and the depth-first
 * order of leaves defines the sort order.
 */
public final class ItemTree {
    private final Node root;
    private final List<Node> leaves = new ArrayList<>();
    private final Map<String, Node> byName = new HashMap<>();
    private final Map<net.minecraft.item.Item, Integer> orderCache = new ConcurrentHashMap<>();

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
        if (keyword.startsWith("#")) {
            ResourceLocation rl = ResourceLocation.tryParse(keyword.substring(1));
            return rl != null && stack.getItem().getTags().contains(rl);
        }
        if (keyword.contains(":")) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id != null && id.toString().equals(keyword);
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
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key != null && key.toString().equals(this.id)) {
                    return true;
                }
            }
            if (this.tag != null) {
                ResourceLocation rl = ResourceLocation.tryParse(this.tag);
                if (rl != null && stack.getItem().getTags().contains(rl)) {
                    return true;
                }
            }
            if (this.clazz != null) {
                return matchesClass(stack, this.clazz);
            }
            return false;
        }

        private static boolean matchesClass(ItemStack stack, String clazz) {
            net.minecraft.item.Item item = stack.getItem();
            switch (clazz) {
                case "sword":
                    return item instanceof SwordItem;
                case "bow":
                    return item instanceof BowItem;
                case "crossbow":
                    return item instanceof CrossbowItem;
                case "trident":
                    return item instanceof TridentItem;
                case "pickaxe":
                    return item instanceof PickaxeItem;
                case "shovel":
                    return item instanceof ShovelItem;
                case "axe":
                    return item instanceof AxeItem;
                case "hoe":
                    return item instanceof HoeItem;
                case "tool":
                    return item instanceof ToolItem;
                case "shears":
                    return item instanceof ShearsItem;
                case "fishingrod":
                    return item instanceof FishingRodItem;
                case "flintandsteel":
                    return item instanceof FlintAndSteelItem;
                case "helmet":
                    return isArmorFor(item, EquipmentSlotType.HEAD);
                case "chestplate":
                    return isArmorFor(item, EquipmentSlotType.CHEST);
                case "leggings":
                    return isArmorFor(item, EquipmentSlotType.LEGS);
                case "boots":
                    return isArmorFor(item, EquipmentSlotType.FEET);
                case "armor":
                    return item instanceof ArmorItem;
                case "shield":
                    return item instanceof ShieldItem;
                case "food":
                    return item.isEdible();
                case "potion":
                    return item instanceof PotionItem;
                case "bucket":
                    return item instanceof BucketItem;
                case "minecart":
                    return item instanceof MinecartItem;
                case "boat":
                    return item instanceof BoatItem;
                case "record":
                    return item instanceof MusicDiscItem;
                case "block":
                    return item instanceof BlockItem;
                default:
                    return false;
            }
        }

        private static boolean isArmorFor(net.minecraft.item.Item item, EquipmentSlotType slot) {
            return item instanceof ArmorItem && ((ArmorItem) item).getSlot() == slot;
        }
    }
}
