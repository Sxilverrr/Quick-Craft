package com.sxilverr.quickcraft.crafting;

import net.minecraft.item.ItemStack;

import java.util.List;

public final class ForeignRecipe {
    private final int made;
    private final List<List<ItemStack>> inputs;

    public ForeignRecipe(int made, List<List<ItemStack>> inputs) {
        this.made = made;
        this.inputs = inputs;
    }

    public int made() {
        return made;
    }

    public List<List<ItemStack>> inputs() {
        return inputs;
    }
}
