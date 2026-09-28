package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.EmcLookup;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.Set;

public interface EmcSource extends EmcLookup {
    BigInteger emc();

    long value(ItemStack stack);

    boolean learned(ItemStack stack);

    EmcBank bank(Set<ItemKey> keys, BigInteger budget);

    @Override
    default boolean obtainable(ItemKey key) {
        ItemStack stack = key.toStack(1);
        return learned(stack) && value(stack) > 0L;
    }
}
