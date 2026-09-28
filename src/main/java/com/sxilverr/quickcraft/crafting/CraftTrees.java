package com.sxilverr.quickcraft.crafting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class CraftTrees {
    private CraftTrees() {
    }

    public static Station missingStation(CraftNode node) {
        if (node.isBlockedByStation()) return node.requiredStation();
        for (CraftNode child : node.children) {
            Station station = missingStation(child);
            if (station != null) return station;
        }
        return null;
    }

    public static boolean truncated(CraftNode node) {
        if (node.truncated) return true;
        for (CraftNode child : node.children) {
            if (truncated(child)) return true;
        }
        return false;
    }

    public static Map<ItemKey, Integer> leafTotals(CraftNode root) {
        Map<ItemKey, Integer> totals = new LinkedHashMap<ItemKey, Integer>();
        collect(root, root.requiredCount, totals, null, new HashSet<ItemKey>());
        return totals;
    }

    public static Map<ItemKey, CraftNode> leafSamples(CraftNode root) {
        Map<ItemKey, CraftNode> samples = new HashMap<ItemKey, CraftNode>();
        collect(root, root.requiredCount, new HashMap<ItemKey, Integer>(), samples, new HashSet<ItemKey>());
        return samples;
    }

    private static void collect(CraftNode node, int required, Map<ItemKey, Integer> totals, Map<ItemKey, CraftNode> samples,
                                Set<ItemKey> catalysts) {
        if (node.reference != null) {
            collect(node.reference, required, totals, samples, catalysts);
            return;
        }
        if (node.catalyst && !catalysts.add(ItemKey.of(node.output))) return;
        if (!hasRealChild(node)) {
            ItemKey key = ItemKey.of(node.output);
            totals.put(key, clamp((long) totals.getOrDefault(key, 0) + required));
            if (samples != null) samples.putIfAbsent(key, node);
            return;
        }
        int crafts = ceilDiv(required, Math.max(1, node.resultPerCraft));
        int ownCrafts = Math.max(1, node.craftsNeeded);
        for (CraftNode child : node.children) {
            if (child.isMobSource()) continue;
            int childNeed = child.catalyst ? child.requiredCount : clamp((long) (child.requiredCount / ownCrafts) * crafts);
            collect(child, childNeed, totals, samples, catalysts);
        }
    }

    private static boolean hasRealChild(CraftNode node) {
        for (CraftNode child : node.children) {
            if (!child.isMobSource()) return true;
        }
        return false;
    }

    private static int ceilDiv(int a, int b) {
        return clamp(((long) a + b - 1) / b);
    }

    private static int clamp(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }
}
