package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecipePreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> recipes = new LinkedHashMap<>();
    private static final Map<String, String> ingredients = new LinkedHashMap<>();
    private static boolean loaded;

    private RecipePreferences() {
    }

    private static Path file() {
        return Services.PLATFORM.getConfigDir().resolve("quickcraft-preferences.json");
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path path = file();
        if (!Files.exists(path)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;
            readSection(root, "recipes", recipes);
            readSection(root, "ingredients", ingredients);
        } catch (Exception e) {
            QuickCraftCommon.LOGGER.warn("Quick Craft: failed to read recipe preferences", e);
        }
    }

    private static void readSection(JsonObject root, String name, Map<String, String> into) {
        if (!root.has(name) || !root.get(name).isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(name).entrySet()) {
            if (entry.getValue().isJsonPrimitive()) into.put(entry.getKey(), entry.getValue().getAsString());
        }
    }

    private static synchronized void save() {
        JsonObject root = new JsonObject();
        root.add("recipes", toJson(recipes));
        root.add("ingredients", toJson(ingredients));
        try {
            Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            QuickCraftCommon.LOGGER.warn("Quick Craft: failed to save recipe preferences", e);
        }
    }

    private static JsonObject toJson(Map<String, String> map) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String> entry : map.entrySet()) obj.addProperty(entry.getKey(), entry.getValue());
        return obj;
    }

    public static Map<ItemKey, ResourceLocation> recipeOverrides() {
        ensureLoaded();
        Map<ItemKey, ResourceLocation> out = new HashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            ItemKey key = ItemKey.parse(entry.getKey());
            ResourceLocation id = ResourceLocation.tryParse(entry.getValue());
            if (key != null && id != null) out.put(key, id);
        }
        return out;
    }

    public static Map<String, Item> ingredientChoices() {
        ensureLoaded();
        Map<String, Item> out = new HashMap<>();
        for (Map.Entry<String, String> entry : ingredients.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getValue());
            Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item != null) out.put(entry.getKey(), item);
        }
        return out;
    }

    public static void setRecipe(ItemKey key, ResourceLocation recipeId) {
        ensureLoaded();
        if (key == null || recipeId == null) return;
        recipes.put(key.toKey(), recipeId.toString());
        save();
    }

    public static void clearRecipe(ItemKey key) {
        ensureLoaded();
        if (key == null) return;
        if (recipes.remove(key.toKey()) != null) save();
    }

    public static void setIngredient(String signature, Item item) {
        ensureLoaded();
        if (signature == null || signature.isEmpty() || item == null) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return;
        ingredients.put(signature, id.toString());
        save();
    }

    public static void clearIngredient(String signature) {
        ensureLoaded();
        if (signature == null || signature.isEmpty()) return;
        if (ingredients.remove(signature) != null) save();
    }
}
