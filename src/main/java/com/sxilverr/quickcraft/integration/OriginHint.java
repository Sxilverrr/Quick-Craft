package com.sxilverr.quickcraft.integration;

import com.sxilverr.quickcraft.crafting.MachineRecipeOption;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

public final class OriginHint {
    private final ItemStack icon;
    private final String label;
    private final List<MachineRecipeOption> recipes;

    public OriginHint(ItemStack icon, String label) {
        this(icon, label, Collections.<MachineRecipeOption>emptyList());
    }

    public OriginHint(ItemStack icon, String label, List<MachineRecipeOption> recipes) {
        this.icon = icon;
        this.label = label;
        this.recipes = recipes;
    }

    public ItemStack icon() {
        return icon;
    }

    public String label() {
        return label;
    }

    public List<MachineRecipeOption> recipes() {
        return recipes;
    }
}
