package com.sxilverr.quickcraft.platform;

import com.sxilverr.quickcraft.crafting.StationSink;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

public interface IStationHooks {

    boolean hasEnergy(ItemStack stack);

    ItemStack findCurio(Player player, Predicate<ItemStack> matcher);

    void scan(Level level, Player player, StationSink sink);
}
