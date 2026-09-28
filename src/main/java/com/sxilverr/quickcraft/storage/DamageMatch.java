package com.sxilverr.quickcraft.storage;

import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DamageMatch {
    private DamageMatch() {
    }

    public static boolean tolerant(ItemStack representative) {
        return !representative.isEmpty() && representative.isDamageableItem();
    }

    public static List<ItemStack> variants(List<ItemStack> stacks, ItemStack representative) {
        ItemKey key = ItemKey.of(representative);
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || stack.getItem() != representative.getItem()) continue;
            if (!key.equals(ItemKey.of(stack)) || holds(out, stack)) continue;
            out.add(stack);
        }
        out.sort(Comparator.comparingInt(ItemStack::getDamageValue).reversed());
        return out;
    }

    public static ItemStack worst(List<ItemStack> stacks, ItemStack representative) {
        if (!tolerant(representative)) return ItemStack.EMPTY;
        List<ItemStack> found = variants(stacks, representative);
        if (found.isEmpty()) return ItemStack.EMPTY;
        ItemStack sample = found.get(0).copy();
        sample.setCount(1);
        return sample;
    }

    private static boolean holds(List<ItemStack> stacks, ItemStack candidate) {
        return stacks.stream().anyMatch(stack -> sameVariant(stack, candidate));
    }

    private static boolean sameVariant(ItemStack a, ItemStack b) {
        //? if >=1.20.5 {
        /*return ItemStack.isSameItemSameComponents(a, b);
        *///?} else {
        return ItemStack.isSameItemSameTags(a, b);
        //?}
    }
}
