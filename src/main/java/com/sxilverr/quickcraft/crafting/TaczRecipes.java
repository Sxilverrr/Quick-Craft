package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.platform.Services;
import com.sxilverr.quickcraft.QuickCraftCommon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

public final class TaczRecipes {
    private TaczRecipes() {
    }

    public static List<StationRecipeOption> collect() {
        List<StationRecipeOption> out = new ArrayList<>();
        //? if >=1.20.5 {
        /*return out;
        *///?} else {
        if (!Services.PLATFORM.isModLoaded("tacz")) return out;
        try {
            Class<?> manager = Class.forName("com.tacz.guns.resource.CommonAssetsManager");
            Object instance = manager.getMethod("getInstance").invoke(null);
            if (instance == null) return out;
            Object recipeManager = manager.getField("recipeManager").get(instance);
            if (!(recipeManager instanceof RecipeManager rm)) return out;
            for (Recipe<?> recipe : rm.getRecipes()) {
                if (recipe.getClass().getName().endsWith("GunSmithTableRecipe")) {
                    addRecipe(recipe, out);
                }
            }
        } catch (Throwable t) {
            QuickCraftCommon.LOGGER.warn("Quick Craft: TACZ recipe indexing failed", t);
        }
        QuickCraftCommon.LOGGER.info("Quick Craft indexed {} TACZ gun smith table recipe(s)", out.size());
        return out;
        //?}
    }

    //? if <1.20.5 {
    private static void addRecipe(Object recipe, List<StationRecipeOption> out) {
        try {
            try {
                recipe.getClass().getMethod("init").invoke(recipe);
            } catch (Throwable ignored) {
            }
            ItemStack result = (ItemStack) recipe.getClass().getMethod("getOutput").invoke(recipe);
            if (result == null || result.isEmpty()) return;
            Object inputs = recipe.getClass().getMethod("getInputs").invoke(recipe);
            if (!(inputs instanceof List<?> list)) return;
            List<Ingredient> ingredients = new ArrayList<>();
            for (Object entry : list) {
                Ingredient ingredient = (Ingredient) entry.getClass().getMethod("getIngredient").invoke(entry);
                if (ingredient == null || ingredient.isEmpty()) continue;
                int count = (int) entry.getClass().getMethod("getCount").invoke(entry);
                int copies = Math.max(1, count);
                for (int i = 0; i < copies; i++) ingredients.add(ingredient);
            }
            if (ingredients.isEmpty()) return;
            ResourceLocation id = ((Recipe<?>) recipe).getId();
            out.add(new StationRecipeOption(id, result.copy(), ingredients, stationFor(result)));
        } catch (Throwable ignored) {
        }
    }

    private static Station stationFor(ItemStack result) {
        CompoundTag tag = result.getTag();
        if (tag != null) {
            if (tag.contains("AmmoId")) return Station.AMMO_ASSEMBLY_TABLE;
            if (tag.contains("AttachmentId")) return Station.ATTACHMENT_TABLE;
        }
        return Station.GUN_SMITH_TABLE;
    }
    //?}
}
