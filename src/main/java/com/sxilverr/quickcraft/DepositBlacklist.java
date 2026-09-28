package com.sxilverr.quickcraft;

import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DepositBlacklist {
    private final Set<ResourceLocation> ids = new HashSet<ResourceLocation>();
    private final Set<String> namespaces = new HashSet<String>();
    private final Set<String> oreNames = new HashSet<String>();

    private DepositBlacklist() {
    }

    public static DepositBlacklist parse(List<String> entries) {
        DepositBlacklist bl = new DepositBlacklist();
        if (entries == null) return bl;
        for (String raw : entries) {
            if (raw == null) continue;
            String entry = raw.trim();
            if (entry.isEmpty()) continue;
            if (entry.startsWith("@") || entry.startsWith("#")) {
                String tag = entry.substring(1).trim();
                if (tag.isEmpty()) continue;
                int colon = tag.indexOf(':');
                String name = colon < 0 ? tag : tag.substring(colon + 1);
                int slash = name.lastIndexOf('/');
                if (slash >= 0) name = name.substring(slash + 1);
                bl.oreNames.add(name.toLowerCase(Locale.ROOT));
                if (colon < 0) bl.namespaces.add(tag);
            } else if (entry.contains(":")) {
                String[] parts = entry.split(":", 2);
                if (parts[1].isEmpty() || parts[1].equals("*")) {
                    bl.namespaces.add(parts[0]);
                } else {
                    ResourceLocation loc = Reg.rl(entry);
                    if (loc != null) bl.ids.add(loc);
                }
            } else {
                bl.namespaces.add(entry);
            }
        }
        return bl;
    }

    public boolean matches(Block block) {
        return block != null && matches(Reg.idOf(block), Item.getItemFromBlock(block));
    }

    public boolean matches(Item item) {
        return item != null && matches(Reg.idOf(item), item);
    }

    private boolean matches(ResourceLocation id, Item item) {
        if (matchesId(id)) return true;
        return !oreNames.isEmpty() && item != null && matchesOre(new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
    }

    private boolean matchesId(ResourceLocation id) {
        return id != null && (ids.contains(id) || namespaces.contains(id.getNamespace()));
    }

    private boolean matchesOre(ItemStack stack) {
        if (stack.isEmpty()) return false;
        int[] oreIds;
        try {
            oreIds = OreDictionary.getOreIDs(stack);
        } catch (RuntimeException e) {
            return false;
        }
        for (int oreId : oreIds) {
            if (oreNames.contains(OreDictionary.getOreName(oreId).toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
