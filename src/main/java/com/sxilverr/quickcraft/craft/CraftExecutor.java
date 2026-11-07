package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.ForeignRecipe;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CraftExecutor {
    private CraftExecutor() {
    }

    private static final int MAX_PASSES = 256;

    public static void simulate(CraftNode root, VirtualPool pool) {
        ItemKey rootKey = ItemKey.of(root.output);
        int want = root.requiredCount;
        VirtualPool best = attempt(root, pool, rootKey, want);
        if (best == null) {
            int lo = 0;
            int hi = want;
            for (int k = 1; k < want; k = (int) Math.min(want, 2L * k)) {
                VirtualPool trial = attempt(root, pool, rootKey, k);
                if (trial == null) {
                    hi = k;
                    break;
                }
                best = trial;
                lo = k;
            }
            while (hi - lo > 1) {
                int mid = lo + (hi - lo) / 2;
                VirtualPool trial = attempt(root, pool, rootKey, mid);
                if (trial == null) {
                    hi = mid;
                } else {
                    best = trial;
                    lo = mid;
                }
            }
        }
        if (best != null) pool.restore(best);
    }

    public static Map<ItemKey, Integer> collect(CraftNode root, VirtualPool pool, Map<ItemKey, ForeignRecipe> foreign) {
        Map<ItemKey, Integer> out = new LinkedHashMap<ItemKey, Integer>();
        gather(root, clamp((long) pool.count(ItemKey.of(root.output)) + root.requiredCount), pool, foreign, out);
        return out;
    }

    private static void gather(CraftNode node, int need, VirtualPool pool, Map<ItemKey, ForeignRecipe> foreign,
                               Map<ItemKey, Integer> out) {
        if (node.reference != null) {
            gather(node.reference, need, pool, foreign, out);
            return;
        }
        int missing = need - pool.count(ItemKey.of(node.output));
        if (missing <= 0) return;
        if (node.children.isEmpty()) {
            ForeignRecipe recipe = foreign.get(ItemKey.of(node.output));
            if (recipe != null) takeInputs(recipe, missing, pool, out);
            return;
        }
        int crafts = Math.max(1, node.craftsNeeded);
        int wanted = ceilDiv(missing, Math.max(1, node.resultPerCraft));
        for (CraftNode child : node.children) {
            int amount = demand(child, crafts, wanted);
            ItemKey key = ItemKey.of(child.output);
            for (int pass = 0; pass < MAX_PASSES && pool.count(key) < amount; pass++) {
                if (ensure(child, amount, pool) == 0) break;
            }
            int have = Math.min(amount, pool.count(key));
            int got = takeInto(pool, key, amount, out);
            if (got == 0) got = takeInto(pool, key, have, out);
            if (got < amount) gather(child, amount - got, pool, foreign, out);
        }
    }

    private static void takeInputs(ForeignRecipe recipe, int missing, VirtualPool pool, Map<ItemKey, Integer> out) {
        int crafts = ceilDiv(missing, Math.max(1, recipe.made()));
        for (List<ItemStack> slot : recipe.inputs()) {
            if (slot.isEmpty()) continue;
            ItemStack pick = slot.get(0);
            for (ItemStack option : slot) {
                if (pool.count(ItemKey.of(option)) > pool.count(ItemKey.of(pick))) pick = option;
            }
            ItemKey key = ItemKey.of(pick);
            takeInto(pool, key, Math.min(clamp((long) Math.max(1, pick.getCount()) * crafts), pool.count(key)), out);
        }
    }

    private static int takeInto(VirtualPool pool, ItemKey key, int amount, Map<ItemKey, Integer> out) {
        if (amount <= 0) return 0;
        Map<ItemKey, Integer> before = new HashMap<ItemKey, Integer>();
        for (Map.Entry<ItemKey, Integer> entry : pool.counts().entrySet()) {
            if (entry.getKey().sameItem(key)) before.put(entry.getKey(), entry.getValue());
        }
        if (!pool.take(key, amount)) return 0;
        int fromStock = 0;
        for (Map.Entry<ItemKey, Integer> entry : before.entrySet()) {
            int taken = entry.getValue() - pool.exact(entry.getKey());
            if (taken <= 0) continue;
            add(out, entry.getKey(), taken);
            fromStock += taken;
        }
        if (amount > fromStock) add(out, key, amount - fromStock);
        return amount;
    }

    private static void add(Map<ItemKey, Integer> out, ItemKey key, int amount) {
        Integer existing = out.get(key);
        out.put(key, existing == null ? amount : clamp((long) existing + amount));
    }

    private static VirtualPool attempt(CraftNode root, VirtualPool pool, ItemKey rootKey, int amount) {
        VirtualPool trial = pool.copy();
        int goal = clamp((long) trial.count(rootKey) + amount);
        for (int pass = 0; pass < MAX_PASSES && trial.count(rootKey) < goal; pass++) {
            if (ensure(root, goal, trial) == 0) break;
        }
        return trial.count(rootKey) >= goal ? trial : null;
    }

    private static int ensure(CraftNode node, int need, VirtualPool pool) {
        if (node.reference != null) return ensure(node.reference, need, pool);
        if (!node.fitsStation || node.children.isEmpty()) return 0;

        ItemKey outputKey = ItemKey.of(node.output);
        int crafts = Math.max(1, node.craftsNeeded);
        int resultPer = Math.max(1, node.resultPerCraft);
        int done = 0;
        while (pool.count(outputKey) < need) {
            int before = pool.count(outputKey);
            int wanted = ceilDiv(need - before, resultPer);
            for (CraftNode child : node.children) {
                done += ensure(child, demand(child, crafts, wanted), pool);
            }
            int batch = craftable(node, pool, crafts, wanted);
            if (batch <= 0) break;
            doCraft(node, pool, crafts, resultPer, outputKey, batch);
            done += batch;
            if (pool.count(outputKey) <= before) break;
        }
        return done;
    }

    private static int perCraft(CraftNode child, int crafts) {
        return child.catalyst ? child.requiredCount : child.requiredCount / crafts;
    }

    private static int demand(CraftNode child, int crafts, int wanted) {
        if (child.catalyst) return child.requiredCount;
        return clamp((long) perCraft(child, crafts) * wanted);
    }

    private static int craftable(CraftNode node, VirtualPool pool, int crafts, int wanted) {
        int max = wanted;
        for (CraftNode child : node.children) {
            ItemKey key = ItemKey.of(child.output);
            int per = perCraft(child, crafts);
            if (per <= 0) continue;
            if (pool.hasEmc() && pool.emcValue(key) > 0L) continue;
            int have = pool.count(key);
            int limit = child.catalyst ? (have >= per ? wanted : 0) : have / per;
            max = Math.min(max, limit);
            if (max <= 0) return 0;
        }
        if (!pool.hasEmc()) return max;
        int lo = 0;
        int hi = max;
        while (lo < hi) {
            int mid = lo + (hi - lo + 1) / 2;
            if (pool.emcAfford(cost(node, pool, crafts, mid))) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    private static BigInteger cost(CraftNode node, VirtualPool pool, int crafts, int batch) {
        BigInteger total = BigInteger.ZERO;
        for (CraftNode child : node.children) {
            ItemKey key = ItemKey.of(child.output);
            int per = perCraft(child, crafts);
            if (per <= 0) continue;
            long value = pool.emcValue(key);
            if (value <= 0L) continue;
            long required = child.catalyst ? per : (long) per * batch;
            long have = pool.count(key);
            if (have >= required) continue;
            total = total.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(required - have)));
        }
        return total;
    }

    private static void doCraft(CraftNode node, VirtualPool pool, int crafts, int resultPer, ItemKey outputKey, int batch) {
        for (CraftNode child : node.children) {
            ItemKey key = ItemKey.of(child.output);
            if (child.catalyst) {
                if (pool.count(key) <= 0 && pool.take(key, child.requiredCount)) pool.add(key, child.requiredCount);
                continue;
            }
            int occ = clamp((long) perCraft(child, crafts) * batch);
            pool.take(key, occ);
            ItemStack remainder = ForgeHooks.getContainerItem(child.output);
            if (remainder != null && !remainder.isEmpty()) {
                pool.add(ItemKey.of(remainder), clamp((long) occ * remainder.getCount()));
            }
        }
        pool.produce(outputKey, clamp((long) resultPer * batch));
    }

    private static int ceilDiv(int a, int b) {
        return clamp(((long) a + b - 1) / b);
    }

    private static int clamp(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }
}
