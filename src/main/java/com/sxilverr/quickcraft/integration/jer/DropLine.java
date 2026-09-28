package com.sxilverr.quickcraft.integration.jer;

import net.minecraft.world.item.ItemStack;

public record DropLine(ItemStack item, int min, int max, boolean looting, String chanceLabel) {
    public DropLine {
        if (chanceLabel == null) chanceLabel = "";
    }

    public String rangeLabel() {
        return min == max ? Integer.toString(min) : min + "-" + max;
    }
}
