package com.sxilverr.quickcraft.integration;

import com.sxilverr.quickcraft.crafting.ForeignRecipe;
import net.minecraft.world.item.ItemStack;

public record OriginHint(ItemStack icon, String label, ForeignRecipe recipe) {
    public OriginHint(ItemStack icon, String label) {
        this(icon, label, null);
    }
}
