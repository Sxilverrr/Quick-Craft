package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.StationProviders;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public final class StationIcons {
    private static final Map<Station, ItemStack> CACHE = new EnumMap<>(Station.class);

    private StationIcons() {
    }

    public static ItemStack icon(Station station) {
        return CACHE.computeIfAbsent(station, StationIcons::resolve);
    }

    public static String name(Station station) {
        ItemStack icon = icon(station);
        return icon.isEmpty() ? station.displayName() : icon.getHoverName().getString();
    }

    private static ItemStack resolve(Station station) {
        return StationProviders.iconFor(station.iconId());
    }
}
