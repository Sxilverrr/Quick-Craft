package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.platform.Services;
//? if >=1.20.5 {
/*import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
*///?} else {
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
//?}
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface StackRequirement {
    StackRequirement NONE = stack -> true;
    StackRequirement ENERGY = stack -> Services.STATIONS.hasEnergy(stack);

    boolean test(ItemStack stack);

    static StackRequirement flag(String nbtKey, String componentId) {
        //? if >=1.20.5 {
        /*return stack -> {
            ResourceLocation id = ResourceLocation.tryParse(componentId);
            if (id == null) return false;
            DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(id);
            return type != null && stack.has(type);
        };
        *///?} else {
        return stack -> {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains(nbtKey)) return false;
            return tag.getTagType(nbtKey) != Tag.TAG_BYTE || tag.getBoolean(nbtKey);
        };
        //?}
    }
}
