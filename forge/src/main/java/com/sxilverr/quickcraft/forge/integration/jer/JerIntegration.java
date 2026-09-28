package com.sxilverr.quickcraft.forge.integration.jer;

import com.sxilverr.quickcraft.integration.jer.MobItemSource;
import net.minecraft.world.item.Item;
import com.sxilverr.quickcraft.platform.Services;

import java.util.List;
import java.util.Map;

public final class JerIntegration {
    private static Map<Item, List<MobItemSource>> cache;

    private JerIntegration() {
    }

    public static boolean available() {
        return Services.PLATFORM.isModLoaded("jeresources");
    }

    public static List<MobItemSource> sourcesFor(Item item) {
        if (!available() || item == null) return List.of();
        List<MobItemSource> list = index().get(item);
        return list == null ? List.of() : list;
    }

    private static Map<Item, List<MobItemSource>> index() {
        Map<Item, List<MobItemSource>> local = cache;
        if (local == null) {
            try {
                local = JerMobIndex.build();
            } catch (Throwable t) {
                local = Map.of();
            }
            if (!local.isEmpty()) cache = local;
        }
        return local;
    }
}
