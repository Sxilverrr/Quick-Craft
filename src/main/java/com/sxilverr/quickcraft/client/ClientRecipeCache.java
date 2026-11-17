package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.crafting.ItemOrigins;
import com.sxilverr.quickcraft.crafting.RecipeResolver;
import net.minecraft.item.ItemStack;

public final class ClientRecipeCache {
    private static RecipeResolver resolver;

    private ClientRecipeCache() {
    }

    public static RecipeResolver get() {
        if (resolver == null) resolver = new RecipeResolver();
        return resolver;
    }

    public static boolean hasRecipe(ItemStack target) {
        return get().canCraft(target) || !ItemOrigins.recipes(target).isEmpty()
                || !ClientHarvestSources.of(target).isEmpty();
    }

    public static void clear() {
        resolver = null;
    }
}
