package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class StationProviders {
    private static final Map<Station, List<String>> IDS = new EnumMap<Station, List<String>>(Station.class);

    private StationProviders() {
    }

    public static List<ItemStack> icons(Station station) {
        List<String> ids = IDS.get(station);
        if (ids == null) {
            ids = StationRules.providerIds(station);
            IDS.put(station, ids);
        }
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (String id : ids) {
            ItemStack stack = iconFor(id);
            if (!stack.isEmpty()) out.add(stack);
        }
        return out;
    }

    public static ItemStack iconFor(String id) {
        return Reg.stack(id);
    }
}
