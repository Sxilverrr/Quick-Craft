package com.sxilverr.quickcraft.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CraftHistory {
    public static final class Entry {
        private final ItemStack stack;
        private final int count;

        Entry(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }

        public ItemStack stack() {
            return stack;
        }

        public int count() {
            return count;
        }
    }

    private static final class Stored {
        final String key;
        final int count;

        Stored(String key, int count) {
            this.key = key;
            this.count = count;
        }
    }

    private static final int MAX = 40;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Stored> ENTRIES = new ArrayList<Stored>();
    private static boolean loaded;

    private CraftHistory() {
    }

    private static Path file() {
        return new File(Loader.instance().getConfigDir(), "quickcraft-history.json").toPath();
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path path = file();
        if (!Files.exists(path)) return;
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(text, JsonObject.class);
            if (root == null || !root.has("history") || !root.get("history").isJsonArray()) return;
            for (JsonElement el : root.getAsJsonArray("history")) {
                if (!el.isJsonObject() || ENTRIES.size() >= MAX) continue;
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("key")) continue;
                int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                ENTRIES.add(new Stored(obj.get("key").getAsString(), Math.max(1, count)));
            }
        } catch (Exception e) {
            QuickCraft.LOGGER.warn("Quick Craft: failed to read craft history", e);
        }
    }

    private static synchronized void save() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (Stored e : ENTRIES) {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", e.key);
            obj.addProperty("count", e.count);
            arr.add(obj);
        }
        root.add("history", arr);
        try {
            Files.write(file(), GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            QuickCraft.LOGGER.warn("Quick Craft: failed to save craft history", e);
        }
    }

    public static synchronized void record(ItemStack stack, int count) {
        if (stack == null || stack.isEmpty()) return;
        ensureLoaded();
        String key = ItemKey.of(stack).toKey();
        for (Iterator<Stored> it = ENTRIES.iterator(); it.hasNext(); ) {
            if (it.next().key.equals(key)) it.remove();
        }
        ENTRIES.add(0, new Stored(key, Math.max(1, count)));
        while (ENTRIES.size() > MAX) ENTRIES.remove(ENTRIES.size() - 1);
        save();
    }

    public static synchronized List<Entry> entries() {
        ensureLoaded();
        List<Entry> out = new ArrayList<Entry>();
        for (Stored e : ENTRIES) {
            ItemKey parsed = ItemKey.parse(e.key);
            if (parsed == null) continue;
            ItemStack stack = parsed.toStack(1);
            if (!stack.isEmpty()) out.add(new Entry(stack, e.count));
        }
        return out;
    }
}
