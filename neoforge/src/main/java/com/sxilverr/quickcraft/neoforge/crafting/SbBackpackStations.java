package com.sxilverr.quickcraft.neoforge.crafting;

import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.StationSink;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.neoforge.storage.SbBackpacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;

public final class SbBackpackStations {
    private SbBackpackStations() {
    }

    public static void scan(Player player, StationSink sink) {
        if (player.level().isClientSide) return;
        SbBackpacks.forEach(player, (backpack, wrapper, inventoryName, slot) -> scan(wrapper.getUpgradeHandler(), sink));
        scanNearbyBlocks(player, sink);
    }

    private static void scanNearbyBlocks(Player player, StationSink sink) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -Stations.RANGE; x <= Stations.RANGE; x++) {
            for (int y = -Stations.RANGE; y <= Stations.RANGE; y++) {
                for (int z = -Stations.RANGE; z <= Stations.RANGE; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack) {
                        IBackpackWrapper wrapper = backpack.getBackpackWrapper();
                        if (wrapper != null) scan(wrapper.getUpgradeHandler(), sink);
                    }
                }
            }
        }
    }

    private static void scan(UpgradeHandler upgrades, StationSink sink) {
        for (int u = 0; u < upgrades.getSlots(); u++) {
            Item item = upgrades.getStackInSlot(u).getItem();
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || !"sophisticatedbackpacks".equals(id.getNamespace())) continue;
            Station station = switch (id.getPath()) {
                case "crafting_upgrade" -> Station.CRAFTING;
                case "smithing_upgrade" -> Station.SMITHING;
                case "stonecutter_upgrade" -> Station.STONECUTTER;
                default -> null;
            };
            if (station != null) sink.offer(station, item, StationSink.UPGRADE);
        }
    }
}
