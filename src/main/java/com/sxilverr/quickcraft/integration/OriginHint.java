package com.sxilverr.quickcraft.integration;

import com.sxilverr.quickcraft.crafting.ForeignRecipe;
import net.minecraft.item.ItemStack;

public final class OriginHint {
    private final ItemStack icon;
    private final String label;
    private final ForeignRecipe recipe;

    public OriginHint(ItemStack icon, String label) {
        this(icon, label, null);
    }

    public OriginHint(ItemStack icon, String label, ForeignRecipe recipe) {
        this.icon = icon;
        this.label = label;
        this.recipe = recipe;
    }

    public ItemStack icon() {
        return icon;
    }

    public String label() {
        return label;
    }

    public ForeignRecipe recipe() {
        return recipe;
    }
}
