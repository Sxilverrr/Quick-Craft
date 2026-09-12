package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.FletchingTableBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;

import java.util.Map;

public final class StationScan {
    private StationScan() {
    }

    public static Stations detect(Level level, Player player) {
        StationSink sink = new StationSink();
        scanBlocks(level, player.blockPosition(), sink);
        scanItems(player, sink);
        Services.STATIONS.scan(level, player, sink);
        return sink.build();
    }

    private static void scanBlocks(Level level, BlockPos center, StationSink sink) {
        Map<Block, StationRules.Rule> rules = StationRules.blocks();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -Stations.RANGE; x <= Stations.RANGE; x++) {
            for (int y = -Stations.RANGE; y <= Stations.RANGE; y++) {
                for (int z = -Stations.RANGE; z <= Stations.RANGE; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    Block block = level.getBlockState(pos).getBlock();
                    StationRules.Rule rule = rules.get(block);
                    if (rule != null) {
                        sink.offer(rule.stations(), block.asItem(), StationRules.rank(rule));
                        continue;
                    }
                    Station generic = genericStation(block);
                    if (generic != null) sink.offer(generic, block.asItem(), StationSink.GENERIC);
                }
            }
        }
    }

    private static void scanItems(Player player, StationSink sink) {
        for (StationRules.ItemRule entry : StationRules.items()) {
            if (carries(player, entry)) sink.offer(entry.rule().stations(), entry.item(), StationSink.CARRIED);
        }
    }

    private static boolean carries(Player player, StationRules.ItemRule entry) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (matches(inv.getItem(i), entry)) return true;
        }
        if (entry.rule().kind() != StationRules.Kind.CARRIED) return false;
        return !Services.STATIONS.findCurio(player, stack -> matches(stack, entry)).isEmpty();
    }

    private static boolean matches(ItemStack stack, StationRules.ItemRule entry) {
        return stack.getItem() == entry.item() && entry.rule().requirement().test(stack);
    }

    private static Station genericStation(Block block) {
        if (block instanceof SmithingTableBlock) return Station.SMITHING;
        if (block instanceof FletchingTableBlock) return null;
        if (block instanceof CraftingTableBlock) return Station.CRAFTING;
        if (block instanceof StonecutterBlock) return Station.STONECUTTER;
        return null;
    }
}
