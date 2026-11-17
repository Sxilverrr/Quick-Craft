package com.sxilverr.quickcraft.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HarvestSources {
    private static final int ROLLS = 16;
    private static final int MAX_ITEMS = 8192;
    private static final int MAX_BLOCKS = 16;

    private HarvestSources() {
    }

    public static BlockState mature(Block block) {
        BlockState state = block.defaultBlockState();
        boolean grows = false;
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && "age".equals(age.getName())) {
                state = state.setValue(age, Collections.max(age.getPossibleValues()));
                grows = true;
            } else if (property instanceof BooleanProperty berries && "berries".equals(berries.getName())) {
                state = state.setValue(berries, true);
                grows = true;
            }
        }
        return grows ? state : null;
    }

    public static Map<Item, List<Block>> scan(ServerLevel level, BlockPos pos) {
        Map<Item, List<Block>> out = new HashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = mature(block);
            if (state == null) continue;
            Set<Item> drops = new LinkedHashSet<>();
            try {
                for (int i = 0; i < ROLLS; i++) {
                    for (ItemStack stack : Block.getDrops(state, level, pos, null)) {
                        if (!stack.isEmpty()) drops.add(stack.getItem());
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            for (Item item : drops) out.computeIfAbsent(item, k -> new ArrayList<>()).add(block);
        }
        return out;
    }

    public static void write(FriendlyByteBuf buf, Map<Item, List<Block>> sources) {
        List<Map.Entry<Item, List<Block>>> entries = sources.entrySet().stream().limit(MAX_ITEMS).toList();
        buf.writeVarInt(entries.size());
        for (Map.Entry<Item, List<Block>> entry : entries) {
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
            List<Block> blocks = entry.getValue().stream().limit(MAX_BLOCKS).toList();
            buf.writeVarInt(blocks.size());
            for (Block block : blocks) buf.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(block));
        }
    }

    public static Map<Item, List<Block>> read(FriendlyByteBuf buf) {
        int count = Math.min(MAX_ITEMS, buf.readVarInt());
        Map<Item, List<Block>> out = new HashMap<>();
        for (int i = 0; i < count; i++) {
            Item item = BuiltInRegistries.ITEM.get(buf.readResourceLocation());
            int blockCount = Math.min(MAX_BLOCKS, buf.readVarInt());
            List<Block> blocks = new ArrayList<>();
            for (int b = 0; b < blockCount; b++) {
                Block block = BuiltInRegistries.BLOCK.get(buf.readResourceLocation());
                if (block != Blocks.AIR) blocks.add(block);
            }
            if (item != Items.AIR && !blocks.isEmpty()) out.put(item, blocks);
        }
        return out;
    }
}
