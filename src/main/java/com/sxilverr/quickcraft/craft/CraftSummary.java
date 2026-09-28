package com.sxilverr.quickcraft.craft;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record CraftSummary(int crafted, int requested, String missingStation,
                           List<Placement> placements, int dropped, int byproducts,
                           ItemStack blocked, int blockedCount, boolean treeLimited,
                           List<CraftPlanner.Blocker> blockers) {
    public record Placement(String where, int count) {
    }

    public CraftSummary(int crafted, int requested, String missingStation,
                        List<Placement> placements, int dropped, int byproducts) {
        this(crafted, requested, missingStation, placements, dropped, byproducts, ItemStack.EMPTY, 0, false, List.of());
    }

    public static CraftSummary empty() {
        return new CraftSummary(0, 0, null, List.of(), 0, 0);
    }

    public static CraftSummary aborted(int requested, ItemStack blocked, int blockedCount) {
        return new CraftSummary(0, requested, null, List.of(), 0, 0, blocked, blockedCount, false, List.of());
    }

    public boolean aborted() {
        return blocked != null && !blocked.isEmpty();
    }

    public boolean full() {
        return requested > 0 && crafted >= requested;
    }

    public boolean partial() {
        return crafted > 0 && crafted < requested;
    }
}
