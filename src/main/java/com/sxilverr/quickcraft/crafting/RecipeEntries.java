package com.sxilverr.quickcraft.crafting;

import net.minecraft.resources.ResourceLocation;
//? if >=1.20.2
/*import net.minecraft.world.item.crafting.RecipeHolder;*/
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

public final class RecipeEntries {
    private RecipeEntries() {
    }

    public record Entry<T>(ResourceLocation id, T recipe) {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> List<Entry<T>> of(RecipeManager manager, RecipeType<?> type) {
        List<Entry<T>> out = new ArrayList<>();
        for (Object entry : manager.getAllRecipesFor((RecipeType) type)) out.add(entry(entry));
        return out;
    }

    public static List<Entry<Recipe<?>>> all(RecipeManager manager) {
        List<Entry<Recipe<?>>> out = new ArrayList<>();
        for (Object entry : manager.getRecipes()) out.add(entry(entry));
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Entry<T> entry(Object entry) {
        //? if >=1.20.2 {
        /*RecipeHolder holder = (RecipeHolder) entry;
        return new Entry<>(holder.id(), (T) holder.value());
        *///?} else {
        Recipe recipe = (Recipe) entry;
        return new Entry<>(recipe.getId(), (T) recipe);
        //?}
    }
}
