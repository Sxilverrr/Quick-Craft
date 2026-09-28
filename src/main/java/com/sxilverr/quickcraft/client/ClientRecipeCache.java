package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.crafting.RecipeResolver;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

public final class ClientRecipeCache {
    private static RecipeResolver resolver;
    private static RecipeManager boundManager;

    private ClientRecipeCache() {
    }

    public static RecipeResolver get(ClientLevel level) {
        RecipeManager manager = level.getRecipeManager();
        if (resolver == null || boundManager != manager) {
            resolver = new RecipeResolver(manager, level.registryAccess());
            boundManager = manager;
        }
        return resolver;
    }

    public static boolean hasRecipe(ClientLevel level, ItemStack target) {
        return get(level).canCraft(target);
    }
}
