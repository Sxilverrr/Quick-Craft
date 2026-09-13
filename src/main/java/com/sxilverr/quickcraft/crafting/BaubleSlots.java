package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.util.Reflect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Method;

public final class BaubleSlots {
    public static final String MODID = "baubles";

    private static boolean resolved;
    private static Method getHandler;

    private BaubleSlots() {
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        getHandler = Reflect.method(Reflect.cls("baubles.api.BaublesApi"), "getBaublesHandler", EntityPlayer.class);
    }

    public static ItemStack find(EntityPlayer player, StationRules.ItemRule entry) {
        resolve();
        if (getHandler == null) return ItemStack.EMPTY;
        Object handler = Reflect.invoke(getHandler, null, player);
        if (!(handler instanceof IItemHandler)) return ItemStack.EMPTY;
        IItemHandler slots = (IItemHandler) handler;
        for (int i = 0; i < slots.getSlots(); i++) {
            ItemStack stack = slots.getStackInSlot(i);
            if (StationScan.matches(stack, entry)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
