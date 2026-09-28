package com.sxilverr.quickcraft;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DepositBlacklist {
    private final Set<ResourceLocation> ids = new HashSet<>();
    private final Set<String> namespaces = new HashSet<>();
    private final Set<ResourceLocation> tagIds = new HashSet<>();
    private final Set<String> tagPaths = new HashSet<>();

    private DepositBlacklist() {
    }

    public static DepositBlacklist parse(List<? extends String> entries) {
        DepositBlacklist bl = new DepositBlacklist();
        for (String raw : entries) {
            if (raw == null) continue;
            String entry = raw.trim();
            if (entry.isEmpty()) continue;
            if (entry.startsWith("@") || entry.startsWith("#")) {
                String tag = entry.substring(1).trim();
                if (tag.isEmpty()) continue;
                if (tag.contains(":")) {
                    ResourceLocation rl = ResourceLocation.tryParse(tag);
                    if (rl != null) bl.tagIds.add(rl);
                } else {
                    bl.tagPaths.add(tag);
                    bl.namespaces.add(tag);
                }
            } else if (entry.contains(":")) {
                String[] parts = entry.split(":", 2);
                if (parts[1].isEmpty() || parts[1].equals("*")) {
                    bl.namespaces.add(parts[0]);
                } else {
                    ResourceLocation rl = ResourceLocation.tryParse(entry);
                    if (rl != null) bl.ids.add(rl);
                }
            } else {
                bl.namespaces.add(entry);
            }
        }
        return bl;
    }

    public boolean matches(Block block) {
        return block != null && matches(BuiltInRegistries.BLOCK.getKey(block), block.builtInRegistryHolder());
    }

    public boolean matches(Item item) {
        return item != null && matches(BuiltInRegistries.ITEM.getKey(item), item.builtInRegistryHolder());
    }

    private boolean matches(ResourceLocation id, Holder<?> holder) {
        return matchesId(id) || (hasTags() && holder.tags().anyMatch(this::matchesTag));
    }

    private boolean hasTags() {
        return !tagIds.isEmpty() || !tagPaths.isEmpty();
    }

    private boolean matchesId(ResourceLocation id) {
        return id != null && (ids.contains(id) || namespaces.contains(id.getNamespace()));
    }

    private boolean matchesTag(TagKey<?> tag) {
        ResourceLocation loc = tag.location();
        return tagIds.contains(loc) || tagPaths.contains(loc.getPath());
    }
}
