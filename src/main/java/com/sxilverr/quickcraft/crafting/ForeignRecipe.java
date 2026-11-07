package com.sxilverr.quickcraft.crafting;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ForeignRecipe(int made, List<List<ItemStack>> inputs) {
}
