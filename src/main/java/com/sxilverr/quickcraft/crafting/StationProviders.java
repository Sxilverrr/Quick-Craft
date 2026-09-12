package com.sxilverr.quickcraft.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class StationProviders {
    private static final Map<Station, List<String>> IDS = new EnumMap<>(Station.class);

    private StationProviders() {
    }

    public static List<ItemStack> icons(Station station) {
        List<ItemStack> out = new ArrayList<>();
        for (String id : IDS.computeIfAbsent(station, StationRules::providerIds)) {
            ItemStack stack = iconFor(id);
            if (!stack.isEmpty()) out.add(stack);
        }
        return out;
    }

    public static ItemStack iconFor(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        String blockIndex = switch (id) {
            case "tacz:workbench_a" -> "tacz:ammo_workbench";
            case "tacz:workbench_c" -> "tacz:attachment_workbench";
            default -> null;
        };
        if (blockIndex != null) {
            //? if <1.20.5 {
            stack.getOrCreateTag().putString("BlockId", blockIndex);
            //?}
        }
        return stack;
    }
}
