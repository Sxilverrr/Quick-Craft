package com.sxilverr.quickcraft.forge.crafting;

import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.StationSink;
import com.sxilverr.quickcraft.crafting.Stations;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

public final class SsStorageStations {
    private SsStorageStations() {
    }

    public static void scan(Player player, StationSink sink) {
        Level level = player.level();
        if (level.isClientSide) return;
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -Stations.RANGE; x <= Stations.RANGE; x++) {
            for (int y = -Stations.RANGE; y <= Stations.RANGE; y++) {
                for (int z = -Stations.RANGE; z <= Stations.RANGE; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
                        scan(storage.getStorageWrapper().getUpgradeHandler(), sink);
                    }
                }
            }
        }
    }

    private static void scan(UpgradeHandler upgrades, StationSink sink) {
        for (int u = 0; u < upgrades.getSlots(); u++) {
            Item item = upgrades.getStackInSlot(u).getItem();
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !"sophisticatedstorage".equals(id.getNamespace())) continue;
            Station station = switch (id.getPath()) {
                case "crafting_upgrade" -> Station.CRAFTING;
                case "stonecutter_upgrade" -> Station.STONECUTTER;
                default -> null;
            };
            if (station != null) sink.offer(station, item, StationSink.UPGRADE);
        }
    }
}
