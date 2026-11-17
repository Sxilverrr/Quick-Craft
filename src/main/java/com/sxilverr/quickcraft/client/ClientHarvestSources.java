package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.crafting.HarvestSources;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientHarvestSources {
    private static Map<Item, List<BlockState>> sources = Map.of();

    private ClientHarvestSources() {
    }

    public static void accept(Map<Item, List<Block>> blocks) {
        Map<Item, List<BlockState>> next = new HashMap<>();
        for (Map.Entry<Item, List<Block>> entry : blocks.entrySet()) {
            List<BlockState> states = new ArrayList<>();
            for (Block block : entry.getValue()) {
                BlockState state = HarvestSources.mature(block);
                if (state != null) states.add(state);
            }
            if (!states.isEmpty()) next.put(entry.getKey(), states);
        }
        sources = next;
    }

    public static List<BlockState> of(Item item) {
        return sources.getOrDefault(item, List.of());
    }
}
