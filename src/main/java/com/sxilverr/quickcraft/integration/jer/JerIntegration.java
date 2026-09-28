package com.sxilverr.quickcraft.integration.jer;

import com.sxilverr.quickcraft.util.Reflect;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JerIntegration {
    public static final String MODID = "jeresources";

    private static Boolean loaded;
    private static Map<Item, List<MobItemSource>> cache;

    private JerIntegration() {
    }

    public static boolean available() {
        if (loaded == null) loaded = Reg.loaded(MODID) && Reflect.cls("jeresources.registry.MobRegistry") != null;
        return loaded;
    }

    public static List<MobItemSource> sourcesFor(Item item) {
        if (!available() || item == null) return Collections.emptyList();
        List<MobItemSource> list = index().get(item);
        return list == null ? Collections.<MobItemSource>emptyList() : list;
    }

    private static Map<Item, List<MobItemSource>> index() {
        Map<Item, List<MobItemSource>> local = cache;
        if (local == null) {
            try {
                local = build();
            } catch (Throwable t) {
                local = Collections.emptyMap();
            }
            if (!local.isEmpty()) cache = local;
        }
        return local;
    }

    private static Map<Item, List<MobItemSource>> build() {
        Map<Item, List<MobItemSource>> map = new HashMap<Item, List<MobItemSource>>();

        Class<?> registryClass = Reflect.cls("jeresources.registry.MobRegistry");
        Object registry = Reflect.invoke(Reflect.method(registryClass, "getInstance"), null);
        if (registry == null) return map;
        Object mobs = Reflect.invoke(Reflect.methodByName(registry.getClass(), "getMobs", 0), registry);
        if (!(mobs instanceof Iterable)) return map;

        Class<?> entryClass = Reflect.cls("jeresources.entry.MobEntry");
        Method getEntity = Reflect.methodByName(entryClass, "getEntity", 0);
        Method getMobName = Reflect.methodByName(entryClass, "getMobName", 0);
        Method getLightLevel = Reflect.methodByName(entryClass, "getLightLevel", 0);
        Method getExp = Reflect.methodByName(entryClass, "getExp", 0);
        Method getDrops = Reflect.methodByName(entryClass, "getDrops", 0);
        Method getBiomes = Reflect.methodByName(entryClass, "getBiomes", 0);

        Class<?> dropClass = Reflect.cls("jeresources.api.drop.LootDrop");
        Field dropItem = Reflect.field(dropClass, "item");
        Field dropMin = Reflect.field(dropClass, "minDrop");
        Field dropMax = Reflect.field(dropClass, "maxDrop");
        Method formatChance = Reflect.methodByName(dropClass, "formatChance", 0);
        Method isAffectedBy = Reflect.methodByName(dropClass, "isAffectedBy", 1);
        Object lootingConditional = Reflect.get(
                Reflect.field(Reflect.cls("jeresources.api.conditionals.Conditional"), "affectedByLooting"), null);

        if (getEntity == null || getDrops == null || dropItem == null) return map;

        for (Object entry : (Iterable<?>) mobs) {
            Object entityObj = Reflect.invoke(getEntity, entry);
            if (!(entityObj instanceof Entity)) continue;
            ResourceLocation id = EntityList.getKey((Entity) entityObj);

            List<DropLine> lines = new ArrayList<DropLine>();
            for (Object drop : toIterable(Reflect.invoke(getDrops, entry))) {
                Object itemObj = Reflect.get(dropItem, drop);
                if (!(itemObj instanceof ItemStack)) continue;
                ItemStack stack = (ItemStack) itemObj;
                if (stack.isEmpty()) continue;
                int min = Reflect.intValue(Reflect.get(dropMin, drop), 1);
                int max = Reflect.intValue(Reflect.get(dropMax, drop), min);
                boolean looting = lootingConditional != null
                        && Reflect.boolValue(Reflect.invoke(isAffectedBy, drop, lootingConditional), false);
                Object chance = Reflect.invoke(formatChance, drop);
                lines.add(new DropLine(stack, min, max, looting, chance instanceof String ? (String) chance : ""));
            }
            if (lines.isEmpty()) continue;

            String name = asString(Reflect.invoke(getMobName, entry), id == null ? "?" : id.toString());
            MobDropInfo info = new MobDropInfo(id, name, biomes(Reflect.invoke(getBiomes, entry)),
                    asString(Reflect.invoke(getLightLevel, entry), ""),
                    asString(Reflect.invoke(getExp, entry), ""), lines);

            for (DropLine line : lines) {
                Item key = line.item.getItem();
                List<MobItemSource> list = map.get(key);
                if (list == null) {
                    list = new ArrayList<MobItemSource>();
                    map.put(key, list);
                }
                list.add(new MobItemSource(info, line));
            }
        }
        return map;
    }

    private static Iterable<?> toIterable(Object value) {
        if (value instanceof Iterable) return (Iterable<?>) value;
        if (value instanceof Object[]) return java.util.Arrays.asList((Object[]) value);
        return Collections.emptyList();
    }

    private static List<String> biomes(Object value) {
        List<String> out = new ArrayList<String>();
        if (value instanceof Object[]) {
            for (Object o : (Object[]) value) {
                if (o != null) out.add(String.valueOf(o));
            }
        } else if (value instanceof Collection) {
            for (Object o : (Collection<?>) value) {
                if (o != null) out.add(String.valueOf(o));
            }
        }
        return out;
    }

    private static String asString(Object value, String fallback) {
        return value instanceof String ? (String) value : fallback;
    }
}
