package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ClientHarvestSources {
    private static Map<ItemKey, List<IBlockState>> sources = Collections.emptyMap();

    private ClientHarvestSources() {
    }

    public static void accept(Map<ItemKey, List<IBlockState>> next) {
        sources = next;
    }

    public static List<IBlockState> of(ItemStack stack) {
        List<IBlockState> plants = sources.get(ItemKey.of(stack));
        return plants == null ? Collections.<IBlockState>emptyList() : plants;
    }
}
