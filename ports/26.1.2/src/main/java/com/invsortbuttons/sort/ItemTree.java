package com.invsortbuttons.sort;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
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
    private final Map<net.minecraft.world.item.Item, Integer> orderCache = new ConcurrentHashMap<>();

    private ItemTree(Node root) {
        this.root = root;
    }

    public static ItemTree parse(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
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
            if (!(children.item(i) instanceof Element el)) {
                continue;
            }
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
            Identifier rl = Identifier.tryParse(keyword.substring(1));
            return rl != null && stack.is(TagKey.create(Registries.ITEM, rl));
        }
        if (keyword.contains(":")) {
            Identifier id = ForgeRegistries.ITEMS.getKey(stack.getItem());
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
                Identifier key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key != null && key.toString().equals(this.id)) {
                    return true;
                }
            }
            if (this.tag != null) {
                Identifier rl = Identifier.tryParse(this.tag);
                if (rl != null && stack.is(TagKey.create(Registries.ITEM, rl))) {
                    return true;
                }
            }
            if (this.clazz != null) {
                return matchesClass(stack, this.clazz);
            }
            return false;
        }

        private static boolean matchesClass(ItemStack stack, String clazz) {
            var item = stack.getItem();
            return switch (clazz) {
                // Since 1.21.5 the tool/armor item classes are gone; tags and
                // data components identify them instead
                case "sword" -> stack.is(ItemTags.SWORDS);
                case "bow" -> item instanceof BowItem;
                case "crossbow" -> item instanceof CrossbowItem;
                case "trident" -> item instanceof TridentItem;
                case "pickaxe" -> stack.is(ItemTags.PICKAXES);
                case "shovel" -> stack.is(ItemTags.SHOVELS);
                case "axe" -> stack.is(ItemTags.AXES);
                case "hoe" -> stack.is(ItemTags.HOES);
                case "tool" -> stack.has(DataComponents.TOOL);
                case "shears" -> item instanceof ShearsItem;
                case "fishingrod" -> item instanceof FishingRodItem;
                case "flintandsteel" -> item instanceof FlintAndSteelItem;
                case "helmet" -> equipsTo(stack, EquipmentSlot.HEAD);
                case "chestplate" -> equipsTo(stack, EquipmentSlot.CHEST);
                case "leggings" -> equipsTo(stack, EquipmentSlot.LEGS);
                case "boots" -> equipsTo(stack, EquipmentSlot.FEET);
                case "armor" -> {
                    var eq = stack.get(DataComponents.EQUIPPABLE);
                    yield eq != null && eq.slot().isArmor();
                }
                case "shield" -> item instanceof ShieldItem;
                case "food" -> stack.has(DataComponents.FOOD);
                case "potion" -> item instanceof PotionItem;
                case "bucket" -> item instanceof BucketItem;
                case "minecart" -> item instanceof MinecartItem;
                case "boat" -> item instanceof BoatItem;
                case "record" -> stack.has(DataComponents.JUKEBOX_PLAYABLE);
                case "block" -> item instanceof BlockItem;
                default -> false;
            };
        }

        private static boolean equipsTo(ItemStack stack, EquipmentSlot slot) {
            var eq = stack.get(DataComponents.EQUIPPABLE);
            return eq != null && eq.slot() == slot;
        }
    }
}
