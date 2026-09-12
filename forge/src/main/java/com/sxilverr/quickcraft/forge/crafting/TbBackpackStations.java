package com.sxilverr.quickcraft.forge.crafting;

import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.StationRules;
import com.sxilverr.quickcraft.crafting.StationSink;
import com.sxilverr.quickcraft.crafting.Stations;
import com.tiviacz.travelersbackpack.blockentity.BackpackBlockEntity;
import com.tiviacz.travelersbackpack.capability.CapabilityUtils;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.upgrades.crafting.CraftingUpgrade;
import com.tiviacz.travelersbackpack.items.TravelersBackpackItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TbBackpackStations {
    private TbBackpackStations() {
    }

    public static void scan(Player player, StationSink sink) {
        if (!hasCraftingUpgrade(player)) return;
        sink.offer(Station.CRAFTING, StationRules.item("travelersbackpack:crafting_upgrade"), StationSink.UPGRADE);
    }

    private static boolean hasCraftingUpgrade(Player player) {
        try {
            if (hasCrafting(CapabilityUtils.getWearingBackpack(player))) return true;
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (hasCrafting(inv.getItem(i))) return true;
            }
            return nearbyBlockHasCrafting(player);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private static boolean hasCrafting(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TravelersBackpackItem)) return false;
        return hasCrafting(BackpackWrapper.fromStack(stack));
    }

    private static boolean hasCrafting(BackpackWrapper wrapper) {
        if (wrapper == null) return false;
        UpgradeManager upgrades = wrapper.getUpgradeManager();
        return upgrades != null && upgrades.getUpgrade(CraftingUpgrade.class).isPresent();
    }

    private static boolean nearbyBlockHasCrafting(Player player) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -Stations.RANGE; x <= Stations.RANGE; x++) {
            for (int y = -Stations.RANGE; y <= Stations.RANGE; y++) {
                for (int z = -Stations.RANGE; z <= Stations.RANGE; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack && hasCrafting(backpack.getWrapper())) return true;
                }
            }
        }
        return false;
    }
}
