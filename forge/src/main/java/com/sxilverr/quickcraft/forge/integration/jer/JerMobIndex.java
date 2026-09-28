package com.sxilverr.quickcraft.forge.integration.jer;

import com.sxilverr.quickcraft.integration.jer.DropLine;
import com.sxilverr.quickcraft.integration.jer.MobDropInfo;
import com.sxilverr.quickcraft.integration.jer.MobItemSource;
import jeresources.api.conditionals.Conditional;
import jeresources.api.drop.LootDrop;
import jeresources.entry.MobEntry;
import jeresources.registry.MobRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class JerMobIndex {
    private JerMobIndex() {
    }

    static Map<Item, List<MobItemSource>> build() {
        Map<Item, List<MobItemSource>> map = new HashMap<>();
        for (MobEntry entry : MobRegistry.getInstance().getMobs()) {
            LivingEntity entity;
            try {
                entity = entry.getEntity();
            } catch (Throwable t) {
                continue;
            }
            if (entity == null) continue;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            List<String> biomes = safeBiomes(entry);
            String light = String.valueOf(entry.getLightLevel());
            String exp = safeExp(entry);
            List<DropLine> lines = new ArrayList<>();
            for (LootDrop drop : entry.getDrops()) {
                if (drop == null || drop.item == null || drop.item.isEmpty()) continue;
                lines.add(new DropLine(drop.item, drop.minDrop, drop.maxDrop, looting(drop), chance(drop)));
            }
            if (lines.isEmpty()) continue;
            MobDropInfo info = new MobDropInfo(id, entry.getMobName(), biomes, light, exp, lines);
            for (DropLine line : lines) {
                map.computeIfAbsent(line.item().getItem(), k -> new ArrayList<>()).add(new MobItemSource(info, line));
            }
        }
        return map;
    }

    private static List<String> safeBiomes(MobEntry entry) {
        try {
            return entry.getTranslatedBiomes().collect(Collectors.toList());
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static String safeExp(MobEntry entry) {
        try {
            return entry.getExp();
        } catch (Throwable t) {
            return "";
        }
    }

    private static boolean looting(LootDrop drop) {
        try {
            return drop.isAffectedBy(Conditional.affectedByLooting);
        } catch (Throwable t) {
            return false;
        }
    }

    private static String chance(LootDrop drop) {
        try {
            return drop.formatChance();
        } catch (Throwable t) {
            return "";
        }
    }
}
