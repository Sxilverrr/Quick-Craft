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
    private static final Map<ItemKey, List<MachineRecipeOption>> RECIPES = new HashMap<ItemKey, List<MachineRecipeOption>>();

    private static boolean boundViewer;

    private ItemOrigins() {
    }

    public static List<OriginHint> of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Collections.emptyList();
        boolean viewer = QuickCraftIntegrations.canFindOrigins();
        if (boundViewer != viewer) {
            invalidate();
            boundViewer = viewer;
        }
        ItemKey key = ItemKey.of(stack);
        List<OriginHint> cached = MEMO.get(key);
        if (cached != null) return cached;
        List<OriginHint> hints = trim(QuickCraftIntegrations.origins(key.toStack(1)));
        MEMO.put(key, hints);
        return hints;
    }

    public static List<MachineRecipeOption> recipes(ItemStack stack) {
        List<OriginHint> hints = of(stack);
        if (hints.isEmpty()) return Collections.emptyList();
        ItemKey key = ItemKey.of(stack);
        List<MachineRecipeOption> cached = RECIPES.get(key);
        if (cached != null) return cached;
        List<MachineRecipeOption> recipes = new ArrayList<MachineRecipeOption>();
        for (OriginHint hint : hints) recipes.addAll(hint.recipes());
        RECIPES.put(key, recipes);
        return recipes;
    }

    public static void invalidate() {
        MEMO.clear();
        RECIPES.clear();
    }

    private static List<OriginHint> trim(List<OriginHint> hints) {
        return Collections.unmodifiableList(new ArrayList<OriginHint>(hints.subList(0, Math.min(MAX_HINTS, hints.size()))));
    }
}
