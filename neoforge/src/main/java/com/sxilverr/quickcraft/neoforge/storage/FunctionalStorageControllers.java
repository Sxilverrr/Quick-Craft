package com.sxilverr.quickcraft.neoforge.storage;

import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;

public final class FunctionalStorageControllers {
    private FunctionalStorageControllers() {
    }

    public static BlockEntity resolve(Level level, BlockEntity be, Set<Object> seen) {
        BlockEntity controller = controllerOf(level, be);
        if (controller == null) return be;
        return seen.add(controller) ? controller : null;
    }

    private static BlockEntity controllerOf(Level level, BlockEntity be) {
        if (be instanceof StorageControllerTile<?>) return be;
        if (!(be instanceof ControllableDrawerTile<?> drawer)) return null;
        BlockPos pos = drawer.getControllerPos();
        if (pos == null || !level.isLoaded(pos)) return null;
        return level.getBlockEntity(pos) instanceof StorageControllerTile<?> controller ? controller : null;
    }
}
