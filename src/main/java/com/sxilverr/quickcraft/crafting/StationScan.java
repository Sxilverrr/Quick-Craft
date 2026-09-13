package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

public final class StationScan {
    private static final String AE2_MODID = "appliedenergistics2";
    private static final String AE2_CRAFTING_TERMINAL = "appliedenergistics2:part@340";

    private StationScan() {
    }

    public static Stations detect(World world, EntityPlayer player) {
        if (world == null || player == null) return Stations.inventoryOnly();
        StationSink sink = new StationSink();
        BlockPos center = player.getPosition();
        scanBlocks(world, center, sink);
        scanItems(player, sink);
        if (Reg.loaded(AE2_MODID) && Ae2Stations.craftingTerminalNearby(world, center, Stations.RANGE)) {
            sink.offer(Station.CRAFTING, Reg.stack(AE2_CRAFTING_TERMINAL), StationSink.PART);
        }
        if (Reg.loaded(TbBackpackStations.MODID)) TbBackpackStations.scan(world, player, sink);
        return sink.build();
    }

    private static void scanBlocks(World world, BlockPos center, StationSink sink) {
        Map<Block, List<StationRules.BlockEntry>> rules = StationRules.blocks();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -Stations.RANGE; x <= Stations.RANGE; x++) {
            for (int y = -Stations.RANGE; y <= Stations.RANGE; y++) {
                for (int z = -Stations.RANGE; z <= Stations.RANGE; z++) {
                    pos.setPos(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!world.isBlockLoaded(pos)) continue;
                    IBlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();
                    if (offerRule(rules.get(block), state, sink)) continue;
                    if (block instanceof BlockWorkbench) {
                        sink.offer(Station.CRAFTING, new ItemStack(block), StationSink.GENERIC);
                    }
                }
            }
        }
    }

    private static boolean offerRule(List<StationRules.BlockEntry> entries, IBlockState state, StationSink sink) {
        if (entries == null) return false;
        for (StationRules.BlockEntry entry : entries) {
            if (!entry.match.test(state)) continue;
            sink.offer(entry.rule.stations, entry.source, StationRules.rank(entry.rule));
            return true;
        }
        return false;
    }

    private static void scanItems(EntityPlayer player, StationSink sink) {
        for (StationRules.ItemRule entry : StationRules.items()) {
            ItemStack found = carried(player, entry);
            if (!found.isEmpty()) sink.offer(entry.rule.stations, found, StationSink.CARRIED);
        }
    }

    private static ItemStack carried(EntityPlayer player, StationRules.ItemRule entry) {
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (matches(stack, entry)) return icon(stack);
        }
        if (entry.rule.kind != StationRules.Kind.CARRIED || !Reg.loaded(BaubleSlots.MODID)) return ItemStack.EMPTY;
        ItemStack bauble = BaubleSlots.find(player, entry);
        return bauble.isEmpty() ? ItemStack.EMPTY : icon(bauble);
    }

    static boolean matches(ItemStack stack, StationRules.ItemRule entry) {
        if (stack == null || stack.isEmpty() || stack.getItem() != entry.item) return false;
        if (entry.meta >= 0 && stack.getMetadata() != entry.meta) return false;
        return entry.rule.requirement.test(stack);
    }

    private static ItemStack icon(ItemStack stack) {
        return new ItemStack(stack.getItem(), 1, stack.getMetadata());
    }
}
