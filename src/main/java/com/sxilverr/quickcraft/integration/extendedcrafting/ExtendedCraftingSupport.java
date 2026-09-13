package com.sxilverr.quickcraft.integration.extendedcrafting;

import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.crafting.Ingredients;
import com.sxilverr.quickcraft.crafting.ModdedRecipeOption;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.util.Reflect;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ExtendedCraftingSupport {
    public static final String MODID = "extendedcrafting";

    private static final String MANAGER_CLASS = "com.blakebr0.extendedcrafting.crafting.table.TableRecipeManager";

    private ExtendedCraftingSupport() {
    }

    public static boolean available() {
        return Reg.loaded(MODID);
    }

    public static List<ModdedRecipeOption> tableRecipes() {
        List<ModdedRecipeOption> out = new ArrayList<ModdedRecipeOption>();
        if (!available()) return out;
        try {
            Class<?> manager = Reflect.cls(MANAGER_CLASS);
            Method getInstance = Reflect.methodByName(manager, "getInstance", 0);
            Object instance = getInstance == null ? null : Reflect.invoke(getInstance, null);
            Method getRecipes = instance == null ? null : Reflect.methodByName(manager, "getRecipes", 0);
            Object recipes = getRecipes == null ? null : Reflect.invoke(getRecipes, instance);
            if (!(recipes instanceof Iterable)) return out;
            int index = 0;
            for (Object recipe : (Iterable<?>) recipes) {
                ModdedRecipeOption option = toOption(recipe, index++);
                if (option != null) out.add(option);
            }
        } catch (Throwable t) {
            QuickCraft.LOGGER.warn("Quick Craft: Extended Crafting table recipe indexing failed", t);
        }
        return out;
    }

    private static ModdedRecipeOption toOption(Object recipe, int index) {
        if (!(recipe instanceof IRecipe)) return null;
        IRecipe table = (IRecipe) recipe;
        ItemStack result = table.getRecipeOutput();
        if (result == null || result.isEmpty()) return null;
        List<Ingredient> inputs = new ArrayList<Ingredient>();
        for (Ingredient ingredient : table.getIngredients()) {
            if (!Ingredients.isEmpty(ingredient)) inputs.add(ingredient);
        }
        if (inputs.isEmpty()) return null;
        ResourceLocation id = table.getRegistryName();
        if (id == null) id = new ResourceLocation(MODID, "table/" + index);
        return new ModdedRecipeOption(id, result.copy(), inputs, stationFor(table, inputs.size()));
    }

    private static Station stationFor(IRecipe recipe, int inputCount) {
        int tier = intCall(recipe, "getTier", 0);
        if (tier <= 0) tier = tierForSize(recipe, inputCount);
        switch (tier) {
            case 2:
                return Station.EXTENDED_ADVANCED;
            case 3:
                return Station.EXTENDED_ELITE;
            case 4:
                return Station.EXTENDED_ULTIMATE;
            default:
                return Station.EXTENDED_BASIC;
        }
    }

    private static int tierForSize(IRecipe recipe, int inputCount) {
        int width = intCall(recipe, "getWidth", 0);
        int height = intCall(recipe, "getHeight", 0);
        int span = Math.max(width, height);
        if (span <= 0) span = (int) Math.ceil(Math.sqrt(inputCount));
        if (span <= 3) return 1;
        if (span <= 5) return 2;
        if (span <= 7) return 3;
        return 4;
    }

    private static int intCall(Object target, String name, int fallback) {
        Method method = Reflect.methodByName(target.getClass(), name, 0);
        return Reflect.intValue(Reflect.invoke(method, target), fallback);
    }
}
