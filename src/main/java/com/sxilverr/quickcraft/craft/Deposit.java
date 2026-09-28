package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Deposit {
    private final List<LabeledSource> sinks;
    private final ServerPlayer player;
    private final Map<String, Integer> tally = new LinkedHashMap<>();
    private int dropped;
    private int byproducts;
    private String emcLabel = "your EMC";

    private Deposit(List<LabeledSource> sinks, ServerPlayer player) {
        this.sinks = sinks;
        this.player = player;
    }

    public static Deposit to(List<LabeledSource> labeled, String destinationId, ServerPlayer player) {
        LabeledSource self = labeled.stream().filter(LabeledSource::isSelf).findFirst().orElse(labeled.get(0));
        LabeledSource chosen = labeled.stream()
                .filter(l -> l.depositable() && l.id().equals(destinationId))
                .findFirst().orElse(self);
        List<LabeledSource> sinks = new ArrayList<>();
        sinks.add(chosen);
        if (!chosen.isSelf()) sinks.add(self);
        return new Deposit(sinks, player);
    }

    public void put(ItemKey key, int amount, boolean target) {
        int max = Math.max(1, key.toStack(1).getMaxStackSize());
        int left = amount;
        while (left > 0) {
            int n = Math.min(left, max);
            ItemStack stack = key.toStack(n);
            for (LabeledSource sink : sinks) {
                if (stack.isEmpty()) break;
                int before = stack.getCount();
                stack = sink.source().insert(stack, false);
                int moved = before - stack.getCount();
                if (moved > 0) {
                    if (target) tally.merge(feedbackName(sink), moved, Integer::sum);
                    else byproducts += moved;
                }
            }
            if (!stack.isEmpty()) {
                dropped += stack.getCount();
                player.drop(stack, false);
            }
            left -= n;
        }
    }

    public void setEmcLabel(String label) {
        if (label != null && !label.isEmpty()) this.emcLabel = label;
    }

    public void toEmc(int amount, boolean target) {
        if (amount <= 0) return;
        if (target) tally.merge(emcLabel, amount, Integer::sum);
        else byproducts += amount;
    }

    private static String feedbackName(LabeledSource sink) {
        return sink.isSelf() ? "your inventory" : sink.name();
    }

    public List<CraftSummary.Placement> placements() {
        return tally.entrySet().stream().map(entry -> new CraftSummary.Placement(entry.getKey(), entry.getValue())).toList();
    }

    public int dropped() {
        return dropped;
    }

    public int byproducts() {
        return byproducts;
    }
}
