package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;

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
            ItemStack remainder = Services.PLATFORM.getCraftingRemainder(child.output);
            if (!remainder.isEmpty()) {
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
