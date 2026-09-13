package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.util.Reflect;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import java.util.Locale;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;

public final class Ae2Stations {
    private static boolean resolved;
    private static Class<?> partHostClass;
    private static Method getPart;
    private static Method getItemStack;
    private static Object[] partLocations;
    private static Object partItemStackPick;

    private Ae2Stations() {
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        partHostClass = Reflect.cls("appeng.api.parts.IPartHost");
        if (partHostClass == null) return;
        getPart = Reflect.methodByName(partHostClass, "getPart", 1);

        Class<?> partClass = Reflect.cls("appeng.api.parts.IPart");
        getItemStack = Reflect.methodByName(partClass, "getItemStack", 1);

        Class<?> locationClass = Reflect.cls("appeng.api.util.AEPartLocation");
        if (locationClass != null && locationClass.isEnum()) partLocations = locationClass.getEnumConstants();

        partItemStackPick = Reflect.enumValue(Reflect.cls("appeng.api.parts.PartItemStack"), "PICK");
    }

    public static boolean craftingTerminalNearby(World world, BlockPos center, int range) {
        resolve();
        if (partHostClass == null || getPart == null || getItemStack == null || partLocations == null) return false;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    cursor.setPos(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!world.isBlockLoaded(cursor)) continue;
                    TileEntity tile = world.getTileEntity(cursor);
                    if (tile == null || !partHostClass.isInstance(tile)) continue;
                    if (hasCraftingTerminal(tile)) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCraftingTerminal(Object host) {
        for (Object location : partLocations) {
            Object part = Reflect.invoke(getPart, host, location);
            if (part == null) continue;
            Object stack = Reflect.invoke(getItemStack, part, partItemStackPick);
            if (!(stack instanceof ItemStack)) continue;
            ItemStack itemStack = (ItemStack) stack;
            if (itemStack.isEmpty()) continue;
            String unlocalized = itemStack.getItem().getTranslationKey(itemStack);
            if (unlocalized != null && unlocalized.toLowerCase(Locale.ROOT).contains("crafting_terminal")) return true;
        }
        return false;
    }
}
