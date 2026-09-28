package com.sxilverr.quickcraft.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public record StationRecipeOption(ResourceLocation id, ItemStack result, List<Ingredient> inputs, Station station)
        implements RecipeOption {

    @Override
    public int resultCount() {
        return Math.max(1, result.getCount());
    }

    @Override
    public boolean fits(Stations stations) {
        return stations.has(station);
    }
}
