package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.ItemKey;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public final class EmcBank {
    private static final BigInteger INT_CAP = BigInteger.valueOf(Integer.MAX_VALUE);

    private final Map<ItemKey, Long> values;
    private BigInteger budget;
    private final Map<ItemKey, Integer> purchased = new HashMap<ItemKey, Integer>();
    private BigInteger gained = BigInteger.ZERO;

    public EmcBank(Map<ItemKey, Long> values, BigInteger budget) {
        this.values = values;
        this.budget = budget == null ? BigInteger.ZERO : budget;
    }

    public EmcBank copy() {
        EmcBank other = new EmcBank(values, budget);
        other.purchased.putAll(purchased);
        other.gained = gained;
        return other;
    }

    void restore(EmcBank other) {
        budget = other.budget;
        purchased.clear();
        purchased.putAll(other.purchased);
        gained = other.gained;
    }

    public boolean supplies(ItemKey key) {
        return value(key) > 0L;
    }

    public long value(ItemKey key) {
        return values.getOrDefault(key, 0L);
    }

    public boolean canAfford(BigInteger cost) {
        return cost.compareTo(budget) <= 0;
    }

    public int affordable(ItemKey key) {
        long v = value(key);
        if (v <= 0L) return 0;
        BigInteger max = budget.divide(BigInteger.valueOf(v));
        return max.compareTo(INT_CAP) >= 0 ? Integer.MAX_VALUE : max.intValue();
    }

    public boolean buy(ItemKey key, int amount) {
        long v = value(key);
        if (v <= 0L || amount <= 0) return false;
        BigInteger cost = BigInteger.valueOf(v).multiply(BigInteger.valueOf(amount));
        if (cost.compareTo(budget) > 0) return false;
        budget = budget.subtract(cost);
        Integer existing = purchased.get(key);
        purchased.put(key, existing == null ? amount : (int) Math.min(Integer.MAX_VALUE, (long) existing + amount));
        return true;
    }

    public void gain(BigInteger amount) {
        if (amount == null || amount.signum() <= 0) return;
        budget = budget.add(amount);
        gained = gained.add(amount);
    }

    public BigInteger remaining() {
        return budget;
    }

    public boolean changed() {
        return !purchased.isEmpty() || gained.signum() > 0;
    }

    public Map<ItemKey, Integer> purchased() {
        return purchased;
    }

    public BigInteger spentEmc() {
        BigInteger total = BigInteger.ZERO;
        for (Map.Entry<ItemKey, Integer> entry : purchased.entrySet()) {
            total = total.add(BigInteger.valueOf(value(entry.getKey()))
                    .multiply(BigInteger.valueOf(entry.getValue())));
        }
        return total;
    }
}
