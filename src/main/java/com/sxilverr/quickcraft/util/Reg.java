package com.sxilverr.quickcraft.util;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class Reg {
    private Reg() {
    }

    public static ResourceLocation rl(String id) {
        if (id == null) return null;
        String trimmed = id.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return new ResourceLocation(trimmed);
        } catch (Exception e) {
            return null;
        }
    }

    public static Item item(String id) {
        ResourceLocation loc = rl(baseId(id));
        if (loc == null) return null;
        Item item = ForgeRegistries.ITEMS.getValue(loc);
        return item == null || item == Items.AIR ? null : item;
    }

    public static Block block(String id) {
        ResourceLocation loc = rl(id);
        if (loc == null) return null;
        Block block = ForgeRegistries.BLOCKS.getValue(loc);
        return block == null || block == Blocks.AIR ? null : block;
    }

    public static ItemStack stack(String id) {
        Item item = item(id);
        if (item == null) return ItemStack.EMPTY;
        return new ItemStack(item, 1, Math.max(0, metaOf(id)));
    }

    public static boolean loaded(String modId) {
        return Loader.isModLoaded(modId);
    }

    public static ResourceLocation idOf(Item item) {
        return item == null ? null : item.getRegistryName();
    }

    public static ResourceLocation idOf(Block block) {
        return block == null ? null : block.getRegistryName();
    }

    public static String baseId(String id) {
        if (id == null) return null;
        int at = id.indexOf('@');
        return at < 0 ? id : id.substring(0, at);
    }

    public static int metaOf(String id) {
        if (id == null) return -1;
        int at = id.indexOf('@');
        if (at < 0 || at + 1 >= id.length()) return -1;
        try {
            return Integer.parseInt(id.substring(at + 1).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
