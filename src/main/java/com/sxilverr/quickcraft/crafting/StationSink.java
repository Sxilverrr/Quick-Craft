package com.sxilverr.quickcraft.crafting;

import net.minecraft.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class StationSink {
    public static final int VANILLA = 0;
    public static final int BLOCK = 1;
    public static final int GENERIC = 2;
    public static final int PART = 3;
    public static final int CARRIED = 4;
    public static final int UPGRADE = 5;

    private final Map<Station, ItemStack> sources = new EnumMap<Station, ItemStack>(Station.class);
    private final Map<Station, Integer> ranks = new EnumMap<Station, Integer>(Station.class);

    public void offer(Station station, ItemStack source, int rank) {
        Integer current = ranks.get(station);
        if (current != null && current <= rank) return;
        ranks.put(station, rank);
        sources.put(station, source == null ? ItemStack.EMPTY : source);
    }

    public void offer(Set<Station> stations, ItemStack source, int rank) {
        for (Station station : stations) offer(station, source, rank);
    }

    public Stations build() {
        return new Stations(sources);
    }
}
