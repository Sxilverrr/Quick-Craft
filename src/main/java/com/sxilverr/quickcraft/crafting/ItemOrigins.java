package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.integration.OriginHint;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ItemOrigins {
    private static final int MAX_HINTS = 4;

    private static final Map<ItemKey, List<OriginHint>> MEMO = new HashMap<ItemKey, List<OriginHint>>();

    private static boolean boundViewer;

    private ItemOrigins() {
    }

    public static List<OriginHint> of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Collections.emptyList();
        boolean viewer = QuickCraftIntegrations.canFindOrigins();
        if (boundViewer != viewer) {
            MEMO.clear();
            boundViewer = viewer;
        }
        ItemKey key = ItemKey.of(stack);
        List<OriginHint> cached = MEMO.get(key);
        if (cached != null) return cached;
        List<OriginHint> hints = trim(QuickCraftIntegrations.origins(key.toStack(1)));
        MEMO.put(key, hints);
        return hints;
    }

    public static void invalidate() {
        MEMO.clear();
    }

    private static List<OriginHint> trim(List<OriginHint> hints) {
        return Collections.unmodifiableList(new ArrayList<OriginHint>(hints.subList(0, Math.min(MAX_HINTS, hints.size()))));
    }
}
