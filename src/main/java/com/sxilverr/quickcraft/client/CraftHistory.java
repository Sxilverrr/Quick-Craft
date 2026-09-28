package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.world.item.ItemStack;
import com.sxilverr.quickcraft.platform.Services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CraftHistory {
    public record Entry(ItemStack stack, int count) {
    }

    private record Stored(String key, int count) {
    }

    private static final int MAX = 40;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Stored> ENTRIES = new ArrayList<>();
    private static boolean loaded;

    private CraftHistory() {
    }

    private static Path file() {
        return Services.PLATFORM.getConfigDir().resolve("quickcraft-history.json");
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path path = file();
        if (!Files.exists(path)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("history") || !root.get("history").isJsonArray()) return;
            for (JsonElement el : root.getAsJsonArray("history")) {
                if (!el.isJsonObject() || ENTRIES.size() >= MAX) continue;
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("key")) continue;
                int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                ENTRIES.add(new Stored(obj.get("key").getAsString(), Math.max(1, count)));
            }
        } catch (Exception e) {
            QuickCraftCommon.LOGGER.warn("Quick Craft: failed to read craft history", e);
        }
    }

    private static synchronized void save() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (Stored e : ENTRIES) {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", e.key());
            obj.addProperty("count", e.count());
            arr.add(obj);
        }
        root.add("history", arr);
        try {
            Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            QuickCraftCommon.LOGGER.warn("Quick Craft: failed to save craft history", e);
        }
    }

    public static synchronized void record(ItemStack stack, int count) {
        if (stack == null || stack.isEmpty()) return;
        ensureLoaded();
        String key = ItemKey.of(stack).toKey();
        ENTRIES.removeIf(e -> e.key().equals(key));
        ENTRIES.add(0, new Stored(key, Math.max(1, count)));
        while (ENTRIES.size() > MAX) ENTRIES.remove(ENTRIES.size() - 1);
        save();
    }

    public static synchronized List<Entry> entries() {
        ensureLoaded();
        List<Entry> out = new ArrayList<>();
        for (Stored e : ENTRIES) {
            ItemKey parsed = ItemKey.parse(e.key());
            if (parsed == null) continue;
            ItemStack stack = parsed.toStack(1);
            if (!stack.isEmpty()) out.add(new Entry(stack, e.count()));
        }
        return out;
    }
}
