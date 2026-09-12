package com.sxilverr.quickcraft.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record Stations(Map<Station, Item> sources) {
    public static final int RANGE = 6;

    public Stations {
        Map<Station, Item> copy = new EnumMap<>(Station.class);
        copy.putAll(sources);
        sources = Collections.unmodifiableMap(copy);
    }

    public static Stations inventoryOnly() {
        return new Stations(Map.of());
    }

    public static Stations of(Station... stations) {
        Map<Station, Item> map = new EnumMap<>(Station.class);
        for (Station station : stations) map.put(station, null);
        return new Stations(map);
    }

    public int gridSize() {
        return has(Station.CRAFTING) ? 3 : 2;
    }

    public boolean has(Station station) {
        return sources.containsKey(station);
    }

    public Item sourceFor(Station station) {
        return sources.get(station);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(sources.size());
        for (Map.Entry<Station, Item> entry : sources.entrySet()) {
            buf.writeEnum(entry.getKey());
            Item item = entry.getValue() == null ? Items.AIR : entry.getValue();
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
        }
    }

    public static Stations read(FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), Station.values().length);
        Map<Station, Item> map = new EnumMap<>(Station.class);
        for (int i = 0; i < count; i++) {
            Station station = buf.readEnum(Station.class);
            Item item = BuiltInRegistries.ITEM.getOptional(buf.readResourceLocation()).orElse(Items.AIR);
            map.put(station, item == Items.AIR ? null : item);
        }
        return new Stations(map);
    }
}
