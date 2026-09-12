package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.QuickCraftCommon;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeResolver {
    private static final String[] IDENTITY_TAGS = {"GunId", "AmmoId", "AttachmentId"};

    private final RegistryAccess registryAccess;
    private final Map<Item, List<RecipeOption>> byResult = new HashMap<>();
    private final Map<Item, List<Ingredient>> cookedFrom = new HashMap<>();

    public RecipeResolver(RecipeManager recipeManager, RegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
        indexCrafting(recipeManager);
        indexSmithing(recipeManager);
        indexStonecutting(recipeManager);
        indexModded(recipeManager);
        indexTacz();
        indexCooking(recipeManager);
    }

    private void indexCrafting(RecipeManager recipeManager) {
        for (RecipeEntries.Entry<CraftingRecipe> entry : RecipeEntries.<CraftingRecipe>of(recipeManager, RecipeType.CRAFTING)) {
            CraftingRecipe recipe = entry.recipe();
            ItemStack result = recipe.getResultItem(registryAccess);
            if (result.isEmpty()) continue;
            CraftingRecipeOption option = new CraftingRecipeOption(entry.id(), recipe, registryAccess);
            if (option.inputs().isEmpty()) continue;
            add(result.getItem(), option);
        }
    }

    private void indexSmithing(RecipeManager recipeManager) {
        List<RecipeEntries.Entry<SmithingRecipe>> smithing = RecipeEntries.of(recipeManager, RecipeType.SMITHING);
        if (smithing.isEmpty()) return;

        List<Item> allItems = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) allItems.add(item);
        }

        for (RecipeEntries.Entry<SmithingRecipe> entry : smithing) {
            SmithingRecipe recipe = entry.recipe();
            if (!(recipe instanceof SmithingTransformRecipe)) continue;
            ItemStack result = recipe.getResultItem(registryAccess);
            if (result.isEmpty()) continue;

            List<ItemStack> templates = new ArrayList<>();
            List<ItemStack> bases = new ArrayList<>();
            List<ItemStack> additions = new ArrayList<>();
            for (Item item : allItems) {
                ItemStack stack = new ItemStack(item);
                if (recipe.isTemplateIngredient(stack)) templates.add(stack);
                if (recipe.isBaseIngredient(stack)) bases.add(stack);
                if (recipe.isAdditionIngredient(stack)) additions.add(stack);
            }

            List<Ingredient> inputs = new ArrayList<>();
            if (!templates.isEmpty()) inputs.add(Ingredient.of(templates.stream()));
            if (!bases.isEmpty()) inputs.add(Ingredient.of(bases.stream()));
            if (!additions.isEmpty()) inputs.add(Ingredient.of(additions.stream()));
            if (inputs.isEmpty()) continue;

            add(result.getItem(), new SmithingRecipeOption(entry.id(), result, inputs));
        }
    }

    private void indexStonecutting(RecipeManager recipeManager) {
        for (RecipeEntries.Entry<StonecutterRecipe> entry : RecipeEntries.<StonecutterRecipe>of(recipeManager, RecipeType.STONECUTTING)) {
            StonecutterRecipe recipe = entry.recipe();
            ItemStack result = recipe.getResultItem(registryAccess);
            if (result.isEmpty()) continue;
            List<Ingredient> inputs = new ArrayList<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient != null && !ingredient.isEmpty()) inputs.add(ingredient);
            }
            if (inputs.isEmpty()) continue;
            add(result.getItem(), new StonecutterRecipeOption(entry.id(), result.copy(), inputs));
        }
    }

    private void indexModded(RecipeManager recipeManager) {
        Map<String, StationRules.RecipeRule> types = StationRules.recipeTypes();
        if (types.isEmpty()) return;
        int count = 0;
        for (RecipeEntries.Entry<Recipe<?>> entry : RecipeEntries.all(recipeManager)) {
            Recipe<?> recipe = entry.recipe();
            ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            StationRules.RecipeRule rule = typeId == null ? null : types.get(typeId.toString());
            if (rule == null) continue;
            Station station = StationRules.stationFor(recipe, rule);
            ItemStack result;
            try {
                result = recipe.getResultItem(registryAccess);
            } catch (Throwable t) {
                continue;
            }
            if (result == null || result.isEmpty()) continue;
            List<Ingredient> inputs = new ArrayList<>();
            try {
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient != null && !ingredient.isEmpty()) inputs.add(ingredient);
                }
            } catch (Throwable t) {
                continue;
            }
            if (inputs.isEmpty()) continue;
            add(result.getItem(), new ModdedRecipeOption(entry.id(), result.copy(), inputs, station));
            count++;
        }
        if (count > 0) {
            QuickCraftCommon.LOGGER.info("Quick Craft indexed {} modded station recipe(s)", count);
        }
    }

    private void indexTacz() {
        for (ModdedRecipeOption option : TaczRecipes.collect()) {
            add(option.result().getItem(), option);
        }
    }

    private void indexCooking(RecipeManager recipeManager) {
        List<RecipeType<?>> types = List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING);
        for (RecipeType<?> type : types) {
            for (RecipeEntries.Entry<AbstractCookingRecipe> entry : RecipeEntries.<AbstractCookingRecipe>of(recipeManager, type)) {
                AbstractCookingRecipe recipe = entry.recipe();
                ItemStack result;
                try {
                    result = recipe.getResultItem(registryAccess);
                } catch (Throwable t) {
                    continue;
                }
                if (result == null || result.isEmpty()) continue;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient == null || ingredient.isEmpty()) continue;
                    cookedFrom.computeIfAbsent(result.getItem(), k -> new ArrayList<>()).add(ingredient);
                }
            }
        }
    }

    public List<Ingredient> cookingInputs(ItemStack output) {
        return cookedFrom.getOrDefault(output.getItem(), List.of());
    }

    private void add(Item result, RecipeOption option) {
        byResult.computeIfAbsent(result, k -> new ArrayList<>()).add(option);
    }

    public RegistryAccess registryAccess() {
        return registryAccess;
    }

    public boolean canCraft(ItemStack output) {
        return !recipesFor(output).isEmpty();
    }

    public List<RecipeOption> recipesFor(ItemStack output) {
        List<RecipeOption> all = byResult.getOrDefault(output.getItem(), List.of());
        //? if >=1.20.5 {
        /*return all;
        *///?} else {
        if (all.size() <= 1 || !anyTagged(all)) return all;
        List<RecipeOption> matched = new ArrayList<>();
        for (RecipeOption option : all) {
            if (resultMatches(option.result(), output)) matched.add(option);
        }
        return matched.isEmpty() ? all : matched;
        //?}
    }

    //? if <1.20.5 {
    private static boolean anyTagged(List<RecipeOption> options) {
        for (RecipeOption option : options) {
            if (option.result().getTag() != null) return true;
        }
        return false;
    }

    private static boolean resultMatches(ItemStack result, ItemStack requested) {
        CompoundTag resultTag = result.getTag();
        if (resultTag == null || resultTag.isEmpty()) return true;
        CompoundTag requestedTag = requested.getTag();
        if (requestedTag == null) return false;
        for (String key : IDENTITY_TAGS) {
            if (resultTag.contains(key)) {
                return requestedTag.contains(key) && resultTag.get(key).equals(requestedTag.get(key));
            }
        }
        return isSubset(resultTag, requestedTag);
    }

    private static boolean isSubset(CompoundTag inner, CompoundTag outer) {
        for (String key : inner.getAllKeys()) {
            Tag innerValue = inner.get(key);
            Tag outerValue = outer.get(key);
            if (outerValue == null) return false;
            if (innerValue instanceof CompoundTag innerCompound && outerValue instanceof CompoundTag outerCompound) {
                if (!isSubset(innerCompound, outerCompound)) return false;
            } else if (!innerValue.equals(outerValue)) {
                return false;
            }
        }
        return true;
    }
    //?}
}
