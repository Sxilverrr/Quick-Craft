package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.integration.OriginHint;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemOrigins {
    private static final int MAX_HINTS = 4;

    private record Builtin(Item icon, String label) {
    }

    private static final Map<String, Builtin> BUILTIN = Map.of(
            "minecraft:smelting", new Builtin(Items.FURNACE, "Smelting"),
            "minecraft:blasting", new Builtin(Items.BLAST_FURNACE, "Blasting"),
            "minecraft:smoking", new Builtin(Items.SMOKER, "Smoking"),
            "minecraft:campfire_cooking", new Builtin(Items.CAMPFIRE, "Campfire Cooking"));

    private static final Map<ItemKey, List<OriginHint>> MEMO = new HashMap<>();

    private static Map<Item, Set<String>> typeIndex;
    private static RecipeManager boundManager;
    private static boolean boundViewer;

    private ItemOrigins() {
    }

    public static List<OriginHint> of(RecipeManager manager, RegistryAccess registryAccess, ItemStack stack) {
        if (manager == null || stack == null || stack.isEmpty()) return List.of();
        boolean viewer = QuickCraftIntegrations.canFindOrigins();
        if (boundManager != manager) {
            MEMO.clear();
            typeIndex = null;
            boundManager = manager;
            boundViewer = viewer;
        } else if (boundViewer != viewer) {
            MEMO.clear();
            boundViewer = viewer;
        }
        return MEMO.computeIfAbsent(ItemKey.of(stack), key -> resolve(manager, registryAccess, key.toStack(1)));
    }

    public static void invalidate() {
        MEMO.clear();
        typeIndex = null;
        boundManager = null;
    }

    private static List<OriginHint> resolve(RecipeManager manager, RegistryAccess registryAccess, ItemStack stack) {
        List<OriginHint> viewer = QuickCraftIntegrations.origins(stack);
        if (!viewer.isEmpty()) return trim(viewer);
        if (QuickCraftIntegrations.canFindOrigins()) return List.of();
        Set<String> types = index(manager, registryAccess).get(stack.getItem());
        if (types == null || types.isEmpty()) return List.of();
        List<OriginHint> out = new ArrayList<>();
        for (String type : types) {
            OriginHint hint = hintFor(type);
            if (hint != null) out.add(hint);
        }
        return trim(out);
    }

    private static List<OriginHint> trim(List<OriginHint> hints) {
        return List.copyOf(hints.subList(0, Math.min(MAX_HINTS, hints.size())));
    }

    private static OriginHint hintFor(String typeId) {
        Builtin builtin = BUILTIN.get(typeId);
        if (builtin != null) {
            ItemStack icon = new ItemStack(builtin.icon());
            String name = icon.isEmpty() ? builtin.label() : icon.getHoverName().getString();
            return new OriginHint(icon, name);
        }
        Item item = StationRules.item(typeId);
        if (item == null) return null;
        ItemStack icon = new ItemStack(item);
        return new OriginHint(icon, icon.getHoverName().getString());
    }

    private static Map<Item, Set<String>> index(RecipeManager manager, RegistryAccess registryAccess) {
        if (typeIndex != null) return typeIndex;
        Map<Item, Set<String>> collected = new HashMap<>();
        for (RecipeEntries.Entry<Recipe<?>> entry : RecipeEntries.all(manager)) {
            Recipe<?> recipe = entry.recipe();
            ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            if (typeId == null || StationRules.isSupportedRecipeType(typeId)) continue;
            ItemStack result = RecipeResolver.safeResult(recipe, registryAccess);
            if (result.isEmpty()) continue;
            collected.computeIfAbsent(result.getItem(), k -> new LinkedHashSet<>()).add(typeId.toString());
        }
        typeIndex = collected;
        return typeIndex;
    }
}
