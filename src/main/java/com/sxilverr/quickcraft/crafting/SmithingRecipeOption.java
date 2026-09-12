package com.sxilverr.quickcraft.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class SmithingRecipeOption implements RecipeOption {
    private final ResourceLocation id;
    private final ItemStack result;
    private final List<Ingredient> inputs;

    public SmithingRecipeOption(ResourceLocation id, ItemStack result, List<Ingredient> inputs) {
        this.id = id;
        this.result = result;
        this.inputs = inputs;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public ItemStack result() {
        return result;
    }

    @Override
    public int resultCount() {
        return Math.max(1, result.getCount());
    }

    @Override
    public List<Ingredient> inputs() {
        return inputs;
    }

    @Override
    public Station station() {
        return Station.SMITHING;
    }

    @Override
    public boolean fits(Stations stations) {
        return stations.has(Station.SMITHING);
    }
}
