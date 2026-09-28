package com.sxilverr.quickcraft.integration;

import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public final class QuickCraftIntegrations {
    private static Supplier<ItemStack> hoveredItemProvider;
    private static BiConsumer<ItemStack, Boolean> recipeViewer;
    private static Function<ItemStack, List<OriginHint>> originProvider;
    private static BooleanSupplier textInputFocused;

    private QuickCraftIntegrations() {
    }

    public static void setTextInputFocused(BooleanSupplier supplier) {
        textInputFocused = supplier;
    }

    public static boolean isTextInputFocused() {
        return textInputFocused != null && textInputFocused.getAsBoolean();
    }

    public static void setHoveredItemProvider(Supplier<ItemStack> provider) {
        hoveredItemProvider = provider;
    }

    public static ItemStack hoveredItem() {
        if (hoveredItemProvider == null) return ItemStack.EMPTY;
        ItemStack stack = hoveredItemProvider.get();
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static void setRecipeViewer(BiConsumer<ItemStack, Boolean> viewer) {
        recipeViewer = viewer;
    }

    public static boolean canShowRecipes() {
        return recipeViewer != null;
    }

    public static void showRecipe(ItemStack stack) {
        if (recipeViewer != null && stack != null && !stack.isEmpty()) recipeViewer.accept(stack, false);
    }

    public static void showUses(ItemStack stack) {
        if (recipeViewer != null && stack != null && !stack.isEmpty()) recipeViewer.accept(stack, true);
    }

    public static void setOriginProvider(Function<ItemStack, List<OriginHint>> provider) {
        originProvider = provider;
    }

    public static boolean canFindOrigins() {
        return originProvider != null;
    }

    public static List<OriginHint> origins(ItemStack stack) {
        if (originProvider == null || stack == null || stack.isEmpty()) return Collections.emptyList();
        List<OriginHint> hints = originProvider.apply(stack);
        return hints == null ? Collections.<OriginHint>emptyList() : hints;
    }
}
