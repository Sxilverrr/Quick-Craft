package com.sxilverr.quickcraft.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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

    private final Map<Station, Item> sources = new EnumMap<>(Station.class);
    private final Map<Station, Integer> ranks = new EnumMap<>(Station.class);

    public void offer(Station station, Item source, int rank) {
        Integer current = ranks.get(station);
        if (current != null && current <= rank) return;
        ranks.put(station, rank);
        sources.put(station, source == Items.AIR ? null : source);
    }

    public void offer(Set<Station> stations, Item source, int rank) {
        for (Station station : stations) offer(station, source, rank);
    }

    public Stations build() {
        return new Stations(sources);
    }
}
