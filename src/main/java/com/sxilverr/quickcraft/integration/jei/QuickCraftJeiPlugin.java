package com.sxilverr.quickcraft.integration.jei;

import com.sxilverr.quickcraft.crafting.ForeignRecipe;
import com.sxilverr.quickcraft.integration.OriginHint;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import com.sxilverr.quickcraft.util.Reflect;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IRecipeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

@JEIPlugin
public class QuickCraftJeiPlugin implements IModPlugin {
    private static final Set<String> SKIP_CATEGORIES = new HashSet<String>(Arrays.asList(
            VanillaRecipeCategoryUid.CRAFTING,
            VanillaRecipeCategoryUid.FUEL,
            VanillaRecipeCategoryUid.ANVIL,
            VanillaRecipeCategoryUid.INFORMATION));

    private IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.runtime = jeiRuntime;
        QuickCraftIntegrations.setHoveredItemProvider(new Supplier<ItemStack>() {
            @Override
            public ItemStack get() {
                return hoveredItem();
            }
        });
        QuickCraftIntegrations.setRecipeViewer(new BiConsumer<ItemStack, Boolean>() {
            @Override
            public void accept(ItemStack stack, Boolean uses) {
                showRecipe(stack, uses);
            }
        });
        QuickCraftIntegrations.setOriginProvider(new Function<ItemStack, List<OriginHint>>() {
            @Override
            public List<OriginHint> apply(ItemStack output) {
                return findOrigins(output);
            }
        });
        QuickCraftIntegrations.setTextInputFocused(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return searchFocused();
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private List<OriginHint> findOrigins(ItemStack stack) {
        if (runtime == null || stack == null || stack.isEmpty()) return Collections.emptyList();
        try {
            IRecipeRegistry registry = runtime.getRecipeRegistry();
            IFocus<ItemStack> focus = registry.createFocus(IFocus.Mode.OUTPUT, stack);
            List<OriginHint> hints = new ArrayList<OriginHint>();
            for (IRecipeCategory category : registry.getRecipeCategories(focus)) {
                if (category == null || SKIP_CATEGORIES.contains(category.getUid())) continue;
                ItemStack icon = ItemStack.EMPTY;
                for (Object catalyst : registry.getRecipeCatalysts(category)) {
                    ItemStack candidate = asStack(catalyst);
                    if (!candidate.isEmpty()) {
                        icon = candidate;
                        break;
                    }
                }
                hints.add(new OriginHint(icon, category.getTitle(), recipeOf(registry, category, focus, stack)));
            }
            return hints;
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ForeignRecipe recipeOf(IRecipeRegistry registry, IRecipeCategory category, IFocus<ItemStack> focus,
                                          ItemStack target) {
        try {
            List wrappers = registry.getRecipeWrappers(category, focus);
            Class<?> type = Reflect.cls("mezz.jei.ingredients.Ingredients");
            if (wrappers.isEmpty() || type == null) return null;
            IIngredients ingredients = (IIngredients) type.getConstructor().newInstance();
            ((IRecipeWrapper) wrappers.get(0)).getIngredients(ingredients);
            List<List<ItemStack>> inputs = new ArrayList<List<ItemStack>>();
            for (List<ItemStack> slot : ingredients.getInputs(VanillaTypes.ITEM)) {
                if (slot == null) continue;
                List<ItemStack> options = new ArrayList<ItemStack>();
                for (ItemStack option : slot) {
                    if (option != null && !option.isEmpty()) options.add(option);
                }
                if (!options.isEmpty()) inputs.add(options);
            }
            int made = 0;
            for (List<ItemStack> slot : ingredients.getOutputs(VanillaTypes.ITEM)) {
                if (slot == null || made > 0) continue;
                for (ItemStack output : slot) {
                    if (output == null || output.getItem() != target.getItem()) continue;
                    made = Math.max(1, output.getCount());
                    break;
                }
            }
            return inputs.isEmpty() ? null : new ForeignRecipe(Math.max(1, made), inputs);
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean searchFocused() {
        try {
            return runtime != null && runtime.getIngredientListOverlay().hasKeyboardFocus();
        } catch (Throwable t) {
            return false;
        }
    }

    private void showRecipe(ItemStack stack, boolean uses) {
        if (runtime == null || stack == null || stack.isEmpty()) return;
        try {
            IFocus.Mode mode = uses ? IFocus.Mode.INPUT : IFocus.Mode.OUTPUT;
            runtime.getRecipesGui().show(runtime.getRecipeRegistry().createFocus(mode, stack));
        } catch (Throwable t) {
        }
    }

    private ItemStack hoveredItem() {
        if (runtime == null) return ItemStack.EMPTY;
        try {
            if (runtime.getIngredientListOverlay().hasKeyboardFocus()) return ItemStack.EMPTY;
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
        ItemStack fromList = ItemStack.EMPTY;
        try {
            fromList = asStack(runtime.getIngredientListOverlay().getIngredientUnderMouse());
        } catch (Throwable ignored) {
        }
        if (!fromList.isEmpty()) return fromList;
        ItemStack fromBookmark = ItemStack.EMPTY;
        try {
            fromBookmark = asStack(runtime.getBookmarkOverlay().getIngredientUnderMouse());
        } catch (Throwable ignored) {
        }
        if (!fromBookmark.isEmpty()) return fromBookmark;
        ItemStack fromRecipes = ItemStack.EMPTY;
        try {
            fromRecipes = asStack(runtime.getRecipesGui().getIngredientUnderMouse());
        } catch (Throwable ignored) {
        }
        return fromRecipes;
    }

    private static ItemStack asStack(Object ingredient) {
        Object value = ingredient;
        for (int depth = 0; depth < 2 && value != null && !(value instanceof ItemStack); depth++) {
            Method getter = Reflect.methodByName(value.getClass(), "getIngredient", 0);
            if (getter == null) break;
            value = Reflect.invoke(getter, value);
        }
        return value instanceof ItemStack ? ((ItemStack) value).copy() : ItemStack.EMPTY;
    }
}
