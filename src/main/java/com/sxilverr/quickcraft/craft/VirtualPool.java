package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VirtualPool {
    private final Map<ItemKey, Integer> counts = new HashMap<>();
    private final Map<ItemKey, Integer> byLoose = new HashMap<>();
    private final Map<ItemKey, Integer> produced = new HashMap<>();
    private final boolean loose;
    private EmcBank emc;

    public VirtualPool() {
        this(false);
    }

    public VirtualPool(boolean loose) {
        this.loose = loose;
    }

    public void setEmc(EmcBank emc) {
        this.emc = emc;
    }

    public boolean hasEmc() {
        return emc != null;
    }

    public long emcValue(ItemKey key) {
        return emc == null ? 0L : emc.value(key);
    }

    public boolean emcAfford(BigInteger cost) {
        return emc != null && emc.canAfford(cost);
    }

    public void add(ItemKey key, int amount) {
        if (amount <= 0) return;
        counts.merge(key, amount, VirtualPool::saturatingAdd);
        byLoose.merge(key.loose(), amount, VirtualPool::saturatingAdd);
    }

    private static int saturatingAdd(int a, int b) {
        return (int) Math.min(Integer.MAX_VALUE, (long) a + b);
    }

    public void addStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        add(ItemKey.of(stack), stack.getCount());
    }

    public void produce(ItemKey key, int amount) {
        if (amount <= 0) return;
        add(key, amount);
        produced.merge(key, amount, VirtualPool::saturatingAdd);
    }

    public Set<ItemKey> producedKeys() {
        return produced.keySet();
    }

    public int count(ItemKey key) {
        if (loose && key.isLoose()) return byLoose.getOrDefault(key, 0);
        return counts.getOrDefault(key, 0);
    }

    public int exact(ItemKey key) {
        return counts.getOrDefault(key, 0);
    }

    public void limit(ItemKey key, int max) {
        int have = exact(key);
        if (have > max) takeExact(key, have - Math.max(0, max));
    }

    public boolean take(ItemKey key, int amount) {
        int have = count(key);
        if (have >= amount) {
            drain(key, amount);
            return true;
        }
        if (emc != null && emc.buy(key, amount - have)) {
            drain(key, have);
            return true;
        }
        return false;
    }

    private void drain(ItemKey key, int amount) {
        int left = amount - takeExact(key, amount);
        if (left <= 0 || !loose || !key.isLoose()) return;
        for (ItemKey other : new ArrayList<>(counts.keySet())) {
            if (left <= 0) break;
            if (other.equals(key) || !other.sameItem(key)) continue;
            left -= takeExact(other, left);
        }
    }

    private int takeExact(ItemKey key, int amount) {
        int have = counts.getOrDefault(key, 0);
        int taken = Math.min(have, amount);
        if (taken <= 0) return 0;
        if (have == taken) counts.remove(key);
        else counts.put(key, have - taken);
        ItemKey looseKey = key.loose();
        int total = byLoose.getOrDefault(looseKey, 0) - taken;
        if (total <= 0) byLoose.remove(looseKey);
        else byLoose.put(looseKey, total);
        return taken;
    }

    public Map<ItemKey, Integer> counts() {
        return counts;
    }

    public VirtualPool copy() {
        VirtualPool other = new VirtualPool(loose);
        other.counts.putAll(this.counts);
        other.byLoose.putAll(this.byLoose);
        other.produced.putAll(this.produced);
        other.emc = this.emc == null ? null : this.emc.copy();
        return other;
    }

    void restore(VirtualPool other) {
        counts.clear();
        counts.putAll(other.counts);
        byLoose.clear();
        byLoose.putAll(other.byLoose);
        produced.clear();
        produced.putAll(other.produced);
        if (emc != null && other.emc != null) emc.restore(other.emc);
    }
}
