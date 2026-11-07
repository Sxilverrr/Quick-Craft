package com.sxilverr.quickcraft.integration.storagedrawers;

import com.sxilverr.quickcraft.util.Reflect;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.CapabilityItemHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class StorageDrawersSupport {
    private static boolean resolved;
    private static boolean available;

    private static Class<?> controllerClass;
    private static Class<?> slaveClass;
    private static Class<?> drawersClass;
    private static Method getController;
    private static Method getDrawerCount;
    private static Method getDrawer;
    private static Method isEnabled;

    private StorageDrawersSupport() {
    }

    public static synchronized boolean available() {
        if (!resolved) {
            resolved = true;
            available = resolve();
        }
        return available;
    }

    private static boolean resolve() {
        String tile = "com.jaquadro.minecraft.storagedrawers.block.tile.";
        controllerClass = Reflect.cls(tile + "TileEntityController");
        slaveClass = Reflect.cls(tile + "TileEntitySlave");
        drawersClass = Reflect.cls(tile + "TileEntityDrawers");
        getController = Reflect.methodByName(slaveClass, "getController", 0);

        Class<?> group = Reflect.cls("com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup");
        getDrawerCount = Reflect.method(group, "getDrawerCount");
        getDrawer = Reflect.method(group, "getDrawer", int.class);
        isEnabled = Reflect.method(Reflect.cls("com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer"), "isEnabled");

        return controllerClass != null && drawersClass != null && getController != null
                && getDrawerCount != null && getDrawer != null && isEnabled != null;
    }

    public static TileEntity resolve(TileEntity tile, Set<Object> seen, List<TileEntity> drawers) {
        if (drawersClass.isInstance(tile)) {
            drawers.add(tile);
            return null;
        }
        Object controller = controllerClass.isInstance(tile) ? tile
                : slaveClass.isInstance(tile) ? Reflect.invoke(getController, tile) : null;
        if (!(controller instanceof TileEntity)) return tile;
        return seen.add(controller) ? (TileEntity) controller : null;
    }

    public static List<TileEntity> unlinked(List<TileEntity> drawers, Set<Object> seen) {
        Set<Object> linked = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        for (Object network : seen) {
            if (!controllerClass.isInstance(network)) continue;
            if (!((TileEntity) network).hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) continue;
            int count = Reflect.intValue(Reflect.invoke(getDrawerCount, network), 0);
            for (int i = 0; i < count; i++) {
                Object drawer = Reflect.invoke(getDrawer, network, i);
                if (drawer != null) linked.add(drawer);
            }
        }
        List<TileEntity> out = new ArrayList<TileEntity>();
        for (TileEntity tile : drawers) {
            if (!isLinked(tile, linked)) out.add(tile);
        }
        return out;
    }

    private static boolean isLinked(TileEntity tile, Set<Object> linked) {
        int count = Reflect.intValue(Reflect.invoke(getDrawerCount, tile), 0);
        for (int i = 0; i < count; i++) {
            Object drawer = Reflect.invoke(getDrawer, tile, i);
            if (drawer != null && Reflect.boolValue(Reflect.invoke(isEnabled, drawer), false)
                    && linked.contains(drawer)) return true;
        }
        return false;
    }
}
