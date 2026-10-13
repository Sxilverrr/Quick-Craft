package com.sxilverr.quickcraft.crafting;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

public final class Ingredients {
    private static final ItemStack[] NONE = new ItemStack[0];

    private Ingredients() {
    }

    public static boolean isEmpty(Ingredient ingredient) {
        return ingredient == null || ingredient == Ingredient.EMPTY || matching(ingredient).length == 0;
    }

    public static ItemStack[] matching(Ingredient ingredient) {
        if (ingredient == null) return NONE;
        ItemStack[] raw;
        try {
            raw = ingredient.getMatchingStacks();
        } catch (RuntimeException e) {
            return NONE;
        }
        if (raw == null || raw.length == 0) return NONE;

        List<ItemStack> out = new ArrayList<ItemStack>(raw.length);
        for (ItemStack stack : raw) {
            if (stack == null || stack.isEmpty()) continue;
            out.add(normalize(stack));
        }
        return out.toArray(new ItemStack[0]);
    }

    private static ItemStack normalize(ItemStack stack) {
        int meta = stack.getMetadata();
        if (stack.copy().isItemStackDamageable()) {
            return new ItemStack(stack.getItem(), stack.getCount(), meta == OreDictionary.WILDCARD_VALUE ? 0 : meta);
        }
        if (meta != OreDictionary.WILDCARD_VALUE) return stack;
        ItemStack concrete = stack.copy();
        concrete.setItemDamage(0);
        return concrete;
    }
}
