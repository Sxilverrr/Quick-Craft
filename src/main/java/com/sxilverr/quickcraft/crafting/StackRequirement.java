package com.sxilverr.quickcraft.crafting;

import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public interface StackRequirement {
    StackRequirement NONE = new StackRequirement() {
        @Override
        public boolean test(ItemStack stack) {
            return true;
        }
    };

    StackRequirement ENERGY = new StackRequirement() {
        @Override
        public boolean test(ItemStack stack) {
            if (!stack.hasCapability(CapabilityEnergy.ENERGY, null)) return true;
            IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            return energy == null || energy.getEnergyStored() > 0;
        }
    };

    boolean test(ItemStack stack);
}
