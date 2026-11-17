package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public class CraftingRecipeOption implements RecipeOption {
    private final ResourceLocation id;
    private final IRecipe recipe;
    private final ItemStack result;
    private final List<Ingredient> inputs;
    private final boolean carpentry;

    public CraftingRecipeOption(ResourceLocation id, IRecipe recipe) {
        this.id = id;
        this.recipe = recipe;
        this.result = recipe.getRecipeOutput();
        this.carpentry = recipe instanceof IShapedRecipe && !recipe.canFit(3, 3) && recipe.canFit(4, 4)
                && Reg.loaded("variedcommodities");
        this.inputs = new ArrayList<Ingredient>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!Ingredients.isEmpty(ingredient)) inputs.add(ingredient);
        }
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
        return carpentry ? Station.CARPENTRY_BENCH : Station.CRAFTING;
    }

    @Override
    public boolean fits(Stations stations) {
        if (carpentry) return stations.has(Station.CARPENTRY_BENCH);
        return recipe.canFit(stations.gridSize(), stations.gridSize());
    }

    @Override
    public boolean fitsInventory() {
        return recipe.canFit(2, 2);
    }
}
