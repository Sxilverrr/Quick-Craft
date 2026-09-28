package com.sxilverr.quickcraft.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class CraftingRecipeOption implements RecipeOption {
    private final ResourceLocation id;
    private final CraftingRecipe recipe;
    private final ItemStack result;
    private final List<Ingredient> inputs;

    public CraftingRecipeOption(ResourceLocation id, CraftingRecipe recipe, RegistryAccess access) {
        this.id = id;
        this.recipe = recipe;
        this.result = recipe.getResultItem(access);
        this.inputs = RecipeResolver.nonEmptyInputs(recipe);
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
        return Station.CRAFTING;
    }

    @Override
    public boolean fits(Stations stations) {
        return recipe.canCraftInDimensions(stations.gridSize(), stations.gridSize());
    }

    @Override
    public boolean fitsInventory() {
        return recipe.canCraftInDimensions(2, 2);
    }
}
