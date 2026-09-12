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
    public static final int MAX_HINTS = 4;

    private record Builtin(Item icon, String label) {
    }

    private static final Map<String, Builtin> BUILTIN = Map.of(
            "minecraft:smelting", new Builtin(Items.FURNACE, "Smelting"),
            "minecraft:blasting", new Builtin(Items.BLAST_FURNACE, "Blasting"),
            "minecraft:smoking", new Builtin(Items.SMOKER, "Smoking"),
            "minecraft:campfire_cooking", new Builtin(Items.CAMPFIRE, "Campfire Cooking"));

    private static final Map<ItemKey, List<OriginHint>> MEMO = new HashMap<>();

    private static Map<Item, List<String>> typeIndex;
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
        ItemKey key = ItemKey.of(stack);
        List<OriginHint> cached = MEMO.get(key);
        if (cached != null) return cached;
        List<OriginHint> hints = resolve(manager, registryAccess, key.toStack(1));
        MEMO.put(key, hints);
        return hints;
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
        List<String> types = index(manager, registryAccess).get(stack.getItem());
        if (types == null || types.isEmpty()) return List.of();
        List<OriginHint> out = new ArrayList<>();
        for (String type : types) {
            OriginHint hint = hintFor(type);
            if (hint != null) out.add(hint);
        }
        return trim(out);
    }

    private static List<OriginHint> trim(List<OriginHint> hints) {
        if (hints.size() <= MAX_HINTS) return List.copyOf(hints);
        return List.copyOf(hints.subList(0, MAX_HINTS));
    }

    private static OriginHint hintFor(String typeId) {
        Builtin builtin = BUILTIN.get(typeId);
        if (builtin != null) {
            ItemStack icon = new ItemStack(builtin.icon());
            String name = icon.isEmpty() ? builtin.label() : icon.getHoverName().getString();
            return new OriginHint(icon, name);
        }
        ResourceLocation rl = ResourceLocation.tryParse(typeId);
        if (rl == null) return null;
        Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (item == null || item == Items.AIR) return null;
        ItemStack icon = new ItemStack(item);
        return new OriginHint(icon, icon.getHoverName().getString());
    }

    private static Map<Item, List<String>> index(RecipeManager manager, RegistryAccess registryAccess) {
        if (typeIndex != null) return typeIndex;
        Map<Item, Set<String>> collected = new HashMap<>();
        for (RecipeEntries.Entry<Recipe<?>> entry : RecipeEntries.all(manager)) {
            Recipe<?> recipe = entry.recipe();
            ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            if (typeId == null || StationRules.isSupportedRecipeType(typeId)) continue;
            ItemStack result;
            try {
                result = recipe.getResultItem(registryAccess);
            } catch (Throwable t) {
                continue;
            }
            if (result == null || result.isEmpty()) continue;
            collected.computeIfAbsent(result.getItem(), k -> new LinkedHashSet<>()).add(typeId.toString());
        }
        Map<Item, List<String>> out = new HashMap<>();
        for (Map.Entry<Item, Set<String>> entry : collected.entrySet()) {
            out.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        typeIndex = out;
        return typeIndex;
    }
}
