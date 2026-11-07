package com.sxilverr.quickcraft.forge.storage;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityController;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class StorageDrawersNetworks {
    private StorageDrawersNetworks() {
    }

    public static BlockEntity resolve(BlockEntity be, Set<Object> seen, List<BlockEntity> drawers) {
        if (be instanceof BlockEntityDrawers) {
            drawers.add(be);
            return null;
        }
        BlockEntity controller = be instanceof BlockEntityController ? be
                : be instanceof IControlGroup io && io.getBoundControlGroup() instanceof BlockEntityController bound
                ? bound : null;
        if (controller == null) return be;
        return seen.add(controller) ? controller : null;
    }

    public static List<BlockEntity> unlinked(List<BlockEntity> drawers, Set<Object> seen) {
        Set<IDrawer> linked = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object network : seen) {
            if (!(network instanceof BlockEntityController controller)) continue;
            for (int i = 0; i < controller.getDrawerCount(); i++) linked.add(controller.getDrawer(i));
        }
        List<BlockEntity> out = new ArrayList<>();
        for (BlockEntity be : drawers) {
            if (!isLinked((IDrawerGroup) be, linked)) out.add(be);
        }
        return out;
    }

    private static boolean isLinked(IDrawerGroup group, Set<IDrawer> linked) {
        for (int i = 0; i < group.getDrawerCount(); i++) {
            IDrawer drawer = group.getDrawer(i);
            if (drawer.isEnabled() && linked.contains(drawer)) return true;
        }
        return false;
    }
}
