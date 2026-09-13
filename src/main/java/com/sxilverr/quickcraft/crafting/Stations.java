package com.sxilverr.quickcraft.crafting;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class Stations {
    public static final int RANGE = 6;

    private final Map<Station, ItemStack> sources;

    public Stations(Map<Station, ItemStack> sources) {
        Map<Station, ItemStack> copy = new EnumMap<Station, ItemStack>(Station.class);
        for (Map.Entry<Station, ItemStack> entry : sources.entrySet()) {
            ItemStack stack = entry.getValue();
            copy.put(entry.getKey(), stack == null ? ItemStack.EMPTY : stack);
        }
        this.sources = Collections.unmodifiableMap(copy);
    }

    public static Stations inventoryOnly() {
        return new Stations(Collections.<Station, ItemStack>emptyMap());
    }

    public static Stations of(Station... stations) {
        Map<Station, ItemStack> map = new EnumMap<Station, ItemStack>(Station.class);
        for (Station station : stations) map.put(station, ItemStack.EMPTY);
        return new Stations(map);
    }

    public int gridSize() {
        return has(Station.CRAFTING) ? 3 : 2;
    }

    public boolean has(Station station) {
        return sources.containsKey(station);
    }

    public ItemStack sourceFor(Station station) {
        ItemStack stack = sources.get(station);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public void write(ByteBuf buf) {
        buf.writeInt(sources.size());
        for (Map.Entry<Station, ItemStack> entry : sources.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey().name());
            ByteBufUtils.writeItemStack(buf, entry.getValue());
        }
    }

    public static Stations read(ByteBuf buf) {
        int count = Math.min(buf.readInt(), Station.values().length);
        Map<Station, ItemStack> map = new EnumMap<Station, ItemStack>(Station.class);
        for (int i = 0; i < count; i++) {
            String name = ByteBufUtils.readUTF8String(buf);
            ItemStack stack = ByteBufUtils.readItemStack(buf);
            Station station = byName(name);
            if (station != null) map.put(station, stack == null ? ItemStack.EMPTY : stack);
        }
        return new Stations(map);
    }

    private static Station byName(String name) {
        for (Station station : Station.values()) {
            if (station.name().equals(name)) return station;
        }
        return null;
    }
}
