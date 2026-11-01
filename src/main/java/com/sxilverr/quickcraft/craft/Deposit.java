package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;

public final class Deposit {
    private final List<LabeledSource> sinks;
    private final EntityPlayerMP player;
    private final List<CraftSummary.Placement> tally = new ArrayList<CraftSummary.Placement>();
    private int dropped;
    private int byproducts;
    private ITextComponent emcLabel = new TextComponentTranslation("quickcraft.deposit.your_emc");

    private Deposit(List<LabeledSource> sinks, EntityPlayerMP player) {
        this.sinks = sinks;
        this.player = player;
    }

    public static Deposit to(List<LabeledSource> labeled, String destinationId, EntityPlayerMP player) {
        LabeledSource self = null;
        for (LabeledSource source : labeled) {
            if (source.isSelf()) {
                self = source;
                break;
            }
        }
        if (self == null) self = labeled.get(0);

        LabeledSource chosen = null;
        for (LabeledSource source : labeled) {
            if (source.depositable() && source.id().equals(destinationId)) {
                chosen = source;
                break;
            }
        }
        if (chosen == null) chosen = self;

        List<LabeledSource> sinks = new ArrayList<LabeledSource>();
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
                    if (target) merge(feedbackName(sink), moved);
                    else byproducts += moved;
                }
            }
            if (!stack.isEmpty()) {
                dropped += stack.getCount();
                player.dropItem(stack, false);
            }
            left -= n;
        }
    }

    public void setEmcLabel(ITextComponent label) {
        if (label != null) this.emcLabel = label;
    }

    public void toEmc(int amount, boolean target) {
        if (amount <= 0) return;
        if (target) merge(emcLabel, amount);
        else byproducts += amount;
    }

    private void merge(ITextComponent where, int amount) {
        for (int i = 0; i < tally.size(); i++) {
            CraftSummary.Placement placement = tally.get(i);
            if (!placement.where().equals(where)) continue;
            tally.set(i, new CraftSummary.Placement(where, placement.count() + amount));
            return;
        }
        tally.add(new CraftSummary.Placement(where, amount));
    }

    private static ITextComponent feedbackName(LabeledSource sink) {
        return sink.isSelf() ? new TextComponentTranslation("quickcraft.deposit.your_inventory") : sink.name();
    }

    public List<CraftSummary.Placement> placements() {
        return tally;
    }

    public int dropped() {
        return dropped;
    }

    public int byproducts() {
        return byproducts;
    }
}
