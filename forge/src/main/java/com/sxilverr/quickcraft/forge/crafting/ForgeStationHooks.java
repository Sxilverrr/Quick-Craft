package com.sxilverr.quickcraft.forge.crafting;

import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.StationRules;
import com.sxilverr.quickcraft.crafting.StationSink;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.forge.integration.curios.CuriosSlots;
import com.sxilverr.quickcraft.platform.IStationHooks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;

import java.util.function.Predicate;

public class ForgeStationHooks implements IStationHooks {

    @Override
    public boolean hasEnergy(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).map(energy -> energy.getEnergyStored() > 0).orElse(true);
    }

    @Override
    public ItemStack findCurio(Player player, Predicate<ItemStack> matcher) {
        if (!ModList.get().isLoaded("curios")) return ItemStack.EMPTY;
        return CuriosSlots.find(player, matcher);
    }

    @Override
    public void scan(Level level, Player player, StationSink sink) {
        if (ModList.get().isLoaded("ae2") && Ae2Stations.craftingTerminalNearby(level, player.blockPosition(), Stations.RANGE)) {
            sink.offer(Station.CRAFTING, StationRules.item("ae2:crafting_terminal"), StationSink.PART);
        }
        if (ModList.get().isLoaded("sophisticatedstorage")) SsStorageStations.scan(player, sink);
        if (ModList.get().isLoaded("sophisticatedbackpacks")) SbBackpackStations.scan(player, sink);
        if (ModList.get().isLoaded("travelersbackpack")) TbBackpackStations.scan(player, sink);
    }
}
