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
            return Component.translatable("quickcraft.chat.aborted", summary.blockedCount(), summary.blocked().getHoverName())
                    .withStyle(ChatFormatting.RED);
        }
        if (summary.bought()) {
            MutableComponent msg = Component.translatable("quickcraft.chat.bought", summary.crafted(), name)
                    .withStyle(ChatFormatting.GREEN);
            return appendPlacements(msg, summary);
        }
        if (summary.full() || (summary.partial() && summary.requested() >= CraftPlanner.MAX_QUANTITY)) {
            MutableComponent msg = Component.translatable("quickcraft.chat.crafted", summary.crafted(), name)
                    .withStyle(ChatFormatting.GREEN);
            return appendPlacements(msg, summary);
        }
        if (summary.partial()) {
            MutableComponent msg = Component.translatable("quickcraft.chat.partial", summary.crafted(),
                    requestedLabel(summary.requested()), name).append(limitHint(summary)).withStyle(ChatFormatting.YELLOW);
            return appendBlockers(appendPlacements(msg, summary), summary);
        }
        if (summary.collected()) {
            MutableComponent msg = Component.translatable("quickcraft.chat.collected", name).withStyle(ChatFormatting.YELLOW);
            return appendBlockers(appendPlacements(msg, summary), summary);
        }
        if (summary.missingStation() != null) {
            return Component.translatable("quickcraft.chat.needs_station", summary.missingStation(), name)
                    .withStyle(ChatFormatting.RED);
        }
        MutableComponent msg = Component.translatable("quickcraft.chat.not_enough", name)
                .append(limitHint(summary)).withStyle(ChatFormatting.RED);
        return appendBlockers(msg, summary);
    }

    private static MutableComponent appendBlockers(MutableComponent msg, CraftSummary summary) {
        List<CraftPlanner.Blocker> blockers = summary.blockers();
        if (blockers.isEmpty()) return msg;
        MutableComponent list = Component.empty();
        int shown = Math.min(3, blockers.size());
        for (int i = 0; i < shown; i++) {
            CraftPlanner.Blocker blocker = blockers.get(i);
            if (i > 0) list.append(", ");
            list.append(blocker.missing() + "x ").append(blocker.key().toStack(1).getHoverName())
                    .append(blocker.reason().suffix());
        }
        if (blockers.size() > shown) list.append(", ").append(Component.translatable("quickcraft.more", blockers.size() - shown));
        return msg.append(Component.literal(" - ").append(Component.translatable("quickcraft.chat.missing", list))
                .withStyle(ChatFormatting.GRAY));
    }

    private static Component limitHint(CraftSummary summary) {
        if (!summary.treeLimited()) return Component.empty();
        return Component.literal(" ").append(Component.translatable("quickcraft.chat.tree_limit"));
    }

    private static MutableComponent appendPlacements(MutableComponent msg, CraftSummary summary) {
        List<CraftSummary.Placement> placements = summary.placements();
        if (placements.isEmpty() && summary.dropped() <= 0 && summary.byproducts() <= 0) return msg;
        MutableComponent parts = Component.literal(" → ");
        boolean first = true;
        for (CraftSummary.Placement placement : placements) {
            if (!first) parts.append(", ");
            parts.append(Component.translatable("quickcraft.chat.placed", placement.count(), placement.where()));
            first = false;
        }
        if (summary.dropped() > 0) {
            if (!first) parts.append(", ");
            parts.append(Component.translatable("quickcraft.chat.dropped", summary.dropped()));
            first = false;
        }
        if (summary.byproducts() > 0) {
            if (!first) parts.append(", ");
            String key = summary.byproducts() == 1 ? "quickcraft.chat.leftover" : "quickcraft.chat.leftovers";
            parts.append(Component.translatable(key, summary.byproducts()));
        }
        return msg.append(parts.withStyle(ChatFormatting.GRAY));
    }

    private static Component requestedLabel(int requested) {
        return requested >= CraftPlanner.MAX_QUANTITY ? Component.translatable("quickcraft.max") : Component.literal(requested + "x");
    }
}
