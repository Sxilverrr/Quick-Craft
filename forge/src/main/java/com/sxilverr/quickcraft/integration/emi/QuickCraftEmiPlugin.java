package com.sxilverr.quickcraft.integration.emi;

import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.ItemOrigins;
import com.sxilverr.quickcraft.crafting.MachineRecipeOption;
import com.sxilverr.quickcraft.integration.OriginHint;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EmiEntrypoint
public class QuickCraftEmiPlugin implements EmiPlugin {
    private static final int MAX_RECIPES = 4;

    private static Set<EmiRecipeCategory> skipped() {
        return Set.of(
                VanillaEmiRecipeCategories.CRAFTING,
                VanillaEmiRecipeCategories.STONECUTTING,
                VanillaEmiRecipeCategories.SMITHING,
                VanillaEmiRecipeCategories.ANVIL_REPAIRING,
                VanillaEmiRecipeCategories.GRINDING,
                VanillaEmiRecipeCategories.FUEL,
                VanillaEmiRecipeCategories.COMPOSTING,
                VanillaEmiRecipeCategories.INFO);
    }

    @Override
    public void register(EmiRegistry registry) {
        ItemOrigins.invalidate();
        QuickCraftIntegrations.setHoveredItemProvider(QuickCraftEmiPlugin::hoveredItem);
        QuickCraftIntegrations.setRecipeViewer(QuickCraftEmiPlugin::showRecipe);
        QuickCraftIntegrations.setOriginProvider(QuickCraftEmiPlugin::findOrigins);
        QuickCraftIntegrations.setTextInputFocused(EmiApi::isSearchFocused);
    }

    private static ItemStack hoveredItem() {
        if (EmiApi.isSearchFocused()) return ItemStack.EMPTY;
        EmiStackInteraction hovered = EmiApi.getHoveredStack(true);
        if (hovered != null && !hovered.isEmpty()) return firstItem(hovered.getStack());
        try {
            return Minecraft.getInstance().screen instanceof RecipeScreen recipes ? firstItem(recipes.getHoveredStack()) : ItemStack.EMPTY;
        } catch (LinkageError e) {
            return ItemStack.EMPTY;
        }
    }

    private static List<OriginHint> findOrigins(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        try {
            EmiRecipeManager manager = EmiApi.getRecipeManager();
            if (manager == null) return List.of();
            Set<EmiRecipeCategory> skip = skipped();
            Map<EmiRecipeCategory, OriginHint> hints = new LinkedHashMap<>();
            for (EmiRecipe recipe : manager.getRecipesByOutput(EmiStack.of(stack))) {
                EmiRecipeCategory category = recipe.getCategory();
                if (category == null || skip.contains(category)) continue;
                OriginHint hint = hints.computeIfAbsent(category,
                        c -> new OriginHint(workstation(manager, c), c.getName().getString(), new ArrayList<>()));
                if (hint.recipes().size() >= MAX_RECIPES) continue;
                MachineRecipeOption option = recipeOf(recipe, stack, hint);
                if (option != null) hint.recipes().add(option);
            }
            return new ArrayList<>(hints.values());
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static ItemStack workstation(EmiRecipeManager manager, EmiRecipeCategory category) {
        for (EmiIngredient workstation : manager.getWorkstations(category)) {
            ItemStack icon = firstItem(workstation);
            if (!icon.isEmpty()) return icon;
        }
        return ItemStack.EMPTY;
    }

    private static MachineRecipeOption recipeOf(EmiRecipe recipe, ItemStack target, OriginHint hint) {
        try {
            List<MachineRecipeOption.Slot> slots = new ArrayList<>();
            for (EmiIngredient input : recipe.getInputs()) {
                List<ItemStack> options = new ArrayList<>();
                for (EmiStack option : input.getEmiStacks()) {
                    ItemStack item = option.getItemStack();
                    if (!item.isEmpty()) options.add(item.copyWithCount(1));
                }
                if (!options.isEmpty()) slots.add(new MachineRecipeOption.Slot(amount(input.getAmount()), options));
            }
            if (slots.isEmpty()) return null;
            int made = 1;
            for (EmiStack output : recipe.getOutputs()) {
                if (!ItemStack.isSameItem(output.getItemStack(), target)) continue;
                made = amount(output.getAmount());
                break;
            }
            ResourceLocation id = recipe.getId();
            if (id == null) {
                id = MachineRecipeOption.fallbackId(recipe.getCategory().getId(), ItemKey.of(target).toKey(), hint.recipes().size());
            }
            return new MachineRecipeOption(id, target.copyWithCount(1), made, slots, hint.icon(), hint.label());
        } catch (Throwable t) {
            return null;
        }
    }

    private static int amount(long amount) {
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, amount));
    }

    private static ItemStack firstItem(EmiIngredient ingredient) {
        if (ingredient == null) return ItemStack.EMPTY;
        List<EmiStack> stacks = ingredient.getEmiStacks();
        if (stacks == null) return ItemStack.EMPTY;
        for (EmiStack stack : stacks) {
            ItemStack item = stack.getItemStack();
            if (!item.isEmpty()) return item;
        }
        return ItemStack.EMPTY;
    }

    private static void showRecipe(ItemStack stack, boolean uses) {
        if (stack == null || stack.isEmpty()) return;
        EmiIngredient ingredient = EmiStack.of(stack);
        if (uses) {
            EmiApi.displayUses(ingredient);
            return;
        }
        EmiApi.displayRecipes(ingredient);
    }
}
