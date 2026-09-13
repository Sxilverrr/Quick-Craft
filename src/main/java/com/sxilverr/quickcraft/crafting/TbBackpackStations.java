package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.util.Reflect;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;

public final class TbBackpackStations {
    public static final String MODID = "travelersbackpack";
    private static final String BACKPACK_ITEM = "travelersbackpack:travelers_backpack";

    private static boolean resolved;
    private static Class<?> itemClass;
    private static Class<?> tileClass;
    private static Method getWearing;

    private TbBackpackStations() {
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        itemClass = Reflect.cls("com.tiviacz.travelersbackpack.items.ItemTravelersBackpack");
        tileClass = Reflect.cls("com.tiviacz.travelersbackpack.tileentity.TileEntityTravelersBackpack");
        getWearing = Reflect.method(Reflect.cls("com.tiviacz.travelersbackpack.capability.CapabilityUtils"),
                "getWearingBackpack", EntityPlayer.class);
    }

    public static void scan(World world, EntityPlayer player, StationSink sink) {
        resolve();
        if (itemClass == null) return;
        ItemStack found = worn(player);
        if (found.isEmpty()) found = inInventory(player);
        if (found.isEmpty() && nearby(world, player.getPosition())) found = Reg.stack(BACKPACK_ITEM);
        if (found.isEmpty()) return;
        sink.offer(Station.CRAFTING, new ItemStack(found.getItem(), 1, found.getMetadata()), StationSink.UPGRADE);
    }

    private static ItemStack worn(EntityPlayer player) {
        Object stack = Reflect.invoke(getWearing, null, player);
        return stack instanceof ItemStack && isBackpack((ItemStack) stack) ? (ItemStack) stack : ItemStack.EMPTY;
    }

    private static ItemStack inInventory(EntityPlayer player) {
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (isBackpack(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isBackpack(ItemStack stack) {
        return stack != null && !stack.isEmpty() && itemClass.isInstance(stack.getItem());
    }

    private static boolean nearby(World world, BlockPos center) {
        if (tileClass == null) return false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -Stations.RANGE; x <= Stations.RANGE; x++) {
            for (int y = -Stations.RANGE; y <= Stations.RANGE; y++) {
                for (int z = -Stations.RANGE; z <= Stations.RANGE; z++) {
                    pos.setPos(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!world.isBlockLoaded(pos)) continue;
                    TileEntity tile = world.getTileEntity(pos);
                    if (tile != null && tileClass.isInstance(tile)) return true;
                }
            }
        }
        return false;
    }
}
