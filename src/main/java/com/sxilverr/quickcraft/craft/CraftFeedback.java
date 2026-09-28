package com.sxilverr.quickcraft.craft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class CraftFeedback {
    private CraftFeedback() {
    }

    public static Component of(CraftSummary summary, ItemStack target) {
        Component name = target.getHoverName();
        if (summary.aborted()) {
            return Component.literal("Quick Craft: could not pull " + summary.blockedCount() + "x ")
                    .append(summary.blocked().getHoverName())
                    .append(Component.literal(" out of storage, nothing was crafted")).withStyle(ChatFormatting.RED);
        }
        if (summary.full() || (summary.partial() && summary.requested() >= CraftPlanner.MAX_QUANTITY)) {
            MutableComponent msg = Component.literal("Quick Craft: crafted " + summary.crafted() + "x ")
                    .append(name).withStyle(ChatFormatting.GREEN);
            return appendPlacements(msg, summary);
        }
        if (summary.partial()) {
            MutableComponent msg = Component.literal("Quick Craft: crafted " + summary.crafted() + "/" + requestedLabel(summary.requested()) + " ")
                    .append(name).append(Component.literal(" - ran out of materials" + limitHint(summary))).withStyle(ChatFormatting.YELLOW);
            return appendBlockers(appendPlacements(msg, summary), summary);
        }
        if (summary.missingStation() != null) {
            return Component.literal("Quick Craft: needs a " + summary.missingStation() + " nearby to craft ")
                    .append(name).withStyle(ChatFormatting.RED);
        }
        MutableComponent msg = Component.literal("Quick Craft: not enough materials to craft ")
                .append(name).append(Component.literal(limitHint(summary))).withStyle(ChatFormatting.RED);
        return appendBlockers(msg, summary);
    }

    private static MutableComponent appendBlockers(MutableComponent msg, CraftSummary summary) {
        List<CraftPlanner.Blocker> blockers = summary.blockers();
        if (blockers.isEmpty()) return msg;
        MutableComponent tail = Component.literal(" - missing: ");
        int shown = Math.min(3, blockers.size());
        for (int i = 0; i < shown; i++) {
            CraftPlanner.Blocker blocker = blockers.get(i);
            if (i > 0) tail.append(Component.literal(", "));
            tail.append(Component.literal(blocker.missing() + "x ")).append(blocker.key().toStack(1).getHoverName())
                    .append(Component.literal(blocker.reason().label));
        }
        if (blockers.size() > shown) tail.append(Component.literal(", +" + (blockers.size() - shown) + " more"));
        return msg.append(tail.withStyle(ChatFormatting.GRAY));
    }

    private static String limitHint(CraftSummary summary) {
        return summary.treeLimited() ? " (recipe tree hit the maxTreeNodes limit, raise it in the config)" : "";
    }

    private static MutableComponent appendPlacements(MutableComponent msg, CraftSummary summary) {
        List<CraftSummary.Placement> placements = summary.placements();
        if (placements.isEmpty() && summary.dropped() <= 0 && summary.byproducts() <= 0) return msg;
        StringBuilder sb = new StringBuilder(" → ");
        boolean first = true;
        for (CraftSummary.Placement placement : placements) {
            if (!first) sb.append(", ");
            sb.append(placement.count()).append(" to ").append(placement.where());
            first = false;
        }
        if (summary.dropped() > 0) {
            if (!first) sb.append(", ");
            sb.append(summary.dropped()).append(" dropped at your feet");
            first = false;
        }
        if (summary.byproducts() > 0) {
            if (!first) sb.append(", ");
            sb.append("+").append(summary.byproducts())
                    .append(summary.byproducts() == 1 ? " leftover item" : " leftover items");
        }
        return msg.append(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
    }

    private static String requestedLabel(int requested) {
        return requested >= CraftPlanner.MAX_QUANTITY ? "Max" : requested + "x";
    }
}
