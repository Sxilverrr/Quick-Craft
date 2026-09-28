package com.sxilverr.quickcraft.craft;

import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

public final class CraftSummary {
    public static final class Placement {
        private final String where;
        private final int count;

        public Placement(String where, int count) {
            this.where = where;
            this.count = count;
        }

        public String where() {
            return where;
        }

        public int count() {
            return count;
        }
    }

    private final int crafted;
    private final int requested;
    private final String missingStation;
    private final List<Placement> placements;
    private final int dropped;
    private final int byproducts;
    private final ItemStack blocked;
    private final int blockedCount;
    private final boolean treeLimited;
    private final List<CraftPlanner.Blocker> blockers;

    public CraftSummary(int crafted, int requested, String missingStation,
                        List<Placement> placements, int dropped, int byproducts, ItemStack blocked, int blockedCount,
                        boolean treeLimited, List<CraftPlanner.Blocker> blockers) {
        this.crafted = crafted;
        this.requested = requested;
        this.missingStation = missingStation;
        this.placements = placements;
        this.dropped = dropped;
        this.byproducts = byproducts;
        this.blocked = blocked == null ? ItemStack.EMPTY : blocked;
        this.blockedCount = blockedCount;
        this.treeLimited = treeLimited;
        this.blockers = blockers == null ? Collections.<CraftPlanner.Blocker>emptyList() : blockers;
    }

    public CraftSummary(int crafted, int requested, String missingStation,
                        List<Placement> placements, int dropped, int byproducts) {
        this(crafted, requested, missingStation, placements, dropped, byproducts, ItemStack.EMPTY, 0, false,
                Collections.<CraftPlanner.Blocker>emptyList());
    }

    public static CraftSummary empty() {
        return new CraftSummary(0, 0, null, Collections.<Placement>emptyList(), 0, 0);
    }

    public static CraftSummary aborted(int requested, ItemStack blocked, int blockedCount) {
        return new CraftSummary(0, requested, null, Collections.<Placement>emptyList(), 0, 0, blocked, blockedCount, false,
                Collections.<CraftPlanner.Blocker>emptyList());
    }

    public int crafted() {
        return crafted;
    }

    public int requested() {
        return requested;
    }

    public String missingStation() {
        return missingStation;
    }

    public List<Placement> placements() {
        return placements;
    }

    public int dropped() {
        return dropped;
    }

    public int byproducts() {
        return byproducts;
    }

    public ItemStack blocked() {
        return blocked;
    }

    public int blockedCount() {
        return blockedCount;
    }

    public boolean treeLimited() {
        return treeLimited;
    }

    public List<CraftPlanner.Blocker> blockers() {
        return blockers;
    }

    public boolean aborted() {
        return !blocked.isEmpty();
    }

    public boolean full() {
        return requested > 0 && crafted >= requested;
    }

    public boolean partial() {
        return crafted > 0 && crafted < requested;
    }
}
