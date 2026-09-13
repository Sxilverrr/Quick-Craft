package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.Availability;
import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.CraftTrees;
import com.sxilverr.quickcraft.crafting.EmcLookup;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.crafting.TreeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CraftPlanner {
    private CraftPlanner() {
    }

    public enum Reason {
        NO_RECIPE, NOT_LEARNED, NOT_ENOUGH_EMC, CATALYST, STATION, TREE_LIMIT, LOOP, MANUAL
    }

    public record Blocker(ItemKey key, int missing, Reason reason) {
    }

    public record Plan(CraftNode root, VirtualPool initial, VirtualPool working, EmcBank bank, Set<ItemKey> keys,
                       int requested, int craftable, boolean truncated, List<Blocker> blockers) {
        public boolean full() {
            return requested > 0 && craftable >= requested;
        }

        public Map<ItemKey, Integer> supplied() {
            return bank == null ? Map.of() : bank.purchased();
        }

        public BigInteger spentEmc() {
            return bank == null ? BigInteger.ZERO : bank.spentEmc();
        }

        public CraftPreview.Result toResult() {
            ItemKey targetKey = ItemKey.of(root.output);
            List<CraftPreview.Gain> gained = new ArrayList<>();
            for (ItemKey key : working.counts().keySet()) {
                int delta = working.exact(key) - initial.exact(key);
                if (delta > 0) gained.add(new CraftPreview.Gain(key, delta));
            }
            gained.sort(Comparator
                    .comparingInt((CraftPreview.Gain g) -> g.key().equals(targetKey) ? 0 : 1)
                    .thenComparing(g -> -g.count()));
            return new CraftPreview.Result(craftable, requested, gained, blockers);
        }
    }

    public static Plan plan(TreeBuilder builder, ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                            Map<String, Item> ingredientChoices, VirtualPool initial, Availability availability,
                            Stations stations, boolean collapseOwned, boolean hideLooping, EmcSource emc, BigInteger budget) {
        int qty = Math.max(1, quantity);
        builder.setEmcLookup(emc == null ? EmcLookup.NONE : emc);
        CraftNode root = builder.build(target, qty, overrides, ingredientChoices, availability, stations, collapseOwned, hideLooping);
        Set<ItemKey> keys = collectKeys(root, new HashSet<>());
        keys.addAll(builder.loopIngredientKeys());
        EmcBank bank = null;
        if (emc != null) bank = budget == null ? emc.bank(keys) : emc.bank(keys, budget);
        VirtualPool working = initial.copy();
        working.setEmc(bank);
        CraftExecutor.simulate(root, working);
        ItemKey targetKey = ItemKey.of(target);
        buyTarget(bank, initial, working, targetKey, qty);
        int craftable = Math.max(0, Math.min(qty, working.count(targetKey) - initial.count(targetKey)));
        List<Blocker> blockers = craftable >= qty ? List.of() : diagnose(root, initial, bank, emc);
        return new Plan(root, initial, working, bank, keys, qty, craftable, builder.truncated(), blockers);
    }

    public static Set<ItemKey> collectKeys(CraftNode node, Set<ItemKey> out) {
        out.add(ItemKey.of(node.output));
        for (CraftNode child : node.children) collectKeys(child, out);
        return out;
    }

    private static void buyTarget(EmcBank bank, VirtualPool initial, VirtualPool working, ItemKey targetKey, int qty) {
        if (bank == null || !bank.supplies(targetKey)) return;
        int made = Math.max(0, working.count(targetKey) - initial.count(targetKey));
        if (made >= qty) return;
        int buy = Math.min(qty - made, bank.affordable(targetKey));
        if (buy > 0 && bank.buy(targetKey, buy)) working.produce(targetKey, buy);
    }

    private static List<Blocker> diagnose(CraftNode root, VirtualPool initial, EmcBank bank, EmcSource emc) {
        Map<ItemKey, Integer> totals = CraftTrees.leafTotals(root);
        Map<ItemKey, CraftNode> samples = CraftTrees.leafSamples(root);
        List<Blocker> out = new ArrayList<>();
        List<Blocker> unbought = new ArrayList<>();
        BigInteger unboughtCost = BigInteger.ZERO;
        boolean stationMissing = CraftTrees.missingStation(root) != null;
        for (Map.Entry<ItemKey, Integer> entry : totals.entrySet()) {
            ItemKey key = entry.getKey();
            int bought = bank == null ? 0 : bank.purchased().getOrDefault(key, 0);
            int missing = entry.getValue() - initial.count(key) - bought;
            if (missing <= 0) continue;
            Reason reason = reasonFor(samples.get(key), key, emc, stationMissing);
            if (reason != Reason.NOT_ENOUGH_EMC) {
                out.add(new Blocker(key, missing, reason));
                continue;
            }
            long unit = bank == null ? 0L : bank.value(key);
            unboughtCost = unboughtCost.add(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(missing)));
            unbought.add(new Blocker(key, missing, reason));
        }
        if (bank == null || !bank.canAfford(unboughtCost)) out.addAll(unbought);
        return out;
    }

    private static Reason reasonFor(CraftNode node, ItemKey key, EmcSource emc, boolean stationMissing) {
        if (node != null) {
            if (node.truncated) return Reason.TREE_LIMIT;
            if (node.cyclic) return Reason.LOOP;
            if (node.isBlockedByStation()) return Reason.STATION;
            if (node.isCraftable() && node.selected() == null) return Reason.MANUAL;
        }
        if (emc != null) {
            ItemStack stack = key.toStack(1);
            if (emc.value(stack) > 0L) {
                if (!emc.learned(stack)) return Reason.NOT_LEARNED;
                return stationMissing ? Reason.STATION : Reason.NOT_ENOUGH_EMC;
            }
        }
        if (node != null && node.catalyst) return Reason.CATALYST;
        return Reason.NO_RECIPE;
    }
}
