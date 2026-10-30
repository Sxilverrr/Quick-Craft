package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.StationProviders;
import net.minecraft.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public final class StationIcons {
    private static final Map<Station, ItemStack> CACHE = new EnumMap<Station, ItemStack>(Station.class);

    private StationIcons() {
    }

    public static ItemStack icon(Station station) {
        ItemStack cached = CACHE.get(station);
        if (cached == null) {
            cached = StationProviders.iconFor(station.iconId());
            CACHE.put(station, cached);
        }
        return cached;
    }

    public static String name(Station station) {
        ItemStack icon = icon(station);
        return icon.isEmpty() ? station.displayName().getUnformattedText() : icon.getDisplayName();
    }
}
