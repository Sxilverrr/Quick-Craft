package com.sxilverr.quickcraft.integration;

import com.sxilverr.quickcraft.crafting.MachineRecipeOption;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record OriginHint(ItemStack icon, String label, List<MachineRecipeOption> recipes) {
    public OriginHint(ItemStack icon, String label) {
        this(icon, label, List.of());
    }
}
