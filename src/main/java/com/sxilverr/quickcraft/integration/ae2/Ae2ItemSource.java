package com.sxilverr.quickcraft.integration.ae2;

import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.storage.LabeledSource;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Ae2ItemSource implements ItemSource {
    private static final int MAX_REPORT = 1000000;

    private final Object monitor;
    private final Object actionSource;
    private final ItemStack icon;

    private Ae2ItemSource(Object monitor, Object actionSource, ItemStack icon) {
        this.monitor = monitor;
        this.actionSource = actionSource;
        this.icon = icon;
    }

    @Override
    public ItemStack sourceIcon() {
        return icon;
    }

    public static LabeledSource tryCreate(TileEntity tile, Set<Object> seenNetworks, EntityPlayerMP player) {
        if (!Ae2Support.available() || !Ae2Support.isGridHost(tile)) return null;
        Object grid = Ae2Support.gridOf(tile);
        ItemStack icon = driveIcon();
        ItemSource source = fromGrid(grid, seenNetworks, player, icon);
        if (source == null) return null;
        BlockPos pos = tile.getPos();
        return new LabeledSource("ae2:" + pos.getX() + "," + pos.getY() + "," + pos.getZ(),
                new TextComponentTranslation("quickcraft.source.me_system"), icon, pos, source, true);
    }

    private static ItemSource fromGrid(Object grid, Set<Object> seenNetworks, EntityPlayerMP player, ItemStack icon) {
        if (grid == null || !seenNetworks.add(grid)) return null;
        if (!Ae2Support.powered(Ae2Support.energyGrid(grid))) return null;
        Object monitor = Ae2Support.itemInventory(Ae2Support.storageGrid(grid));
        if (monitor == null) return null;
        Object source = Ae2Support.actionSource(player);
        if (source == null) return null;
        return new Ae2ItemSource(monitor, source, icon);
    }

    private static ItemStack driveIcon() {
        ItemStack drive = Reg.stack("appliedenergistics2:drive");
        return drive.isEmpty() ? ItemStack.EMPTY : drive;
    }

    @Override
    public List<ItemStack> snapshot() {
        List<ItemStack> out = new ArrayList<ItemStack>();
        Iterable<?> list = Ae2Support.storageList(monitor);
        if (list == null) return out;
        for (Object entry : list) {
            long amount = Ae2Support.stackSize(entry);
            if (amount <= 0) continue;
            ItemStack stack = Ae2Support.toStack(entry);
            if (stack.isEmpty()) continue;
            stack.setCount((int) Math.min(amount, MAX_REPORT));
            out.add(stack);
        }
        return out;
    }

    @Override
    public int extract(ItemStack representative, int amount, boolean simulate) {
        Object request = requestFor(representative, amount);
        if (request == null) return 0;
        Object extracted = Ae2Support.extract(monitor, request, simulate, actionSource);
        if (extracted == null) return 0;
        return (int) Math.min(Ae2Support.stackSize(extracted), Integer.MAX_VALUE);
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        Object request = requestFor(stack, stack.getCount());
        if (request == null) return stack.copy();
        Object leftover = Ae2Support.inject(monitor, request, simulate, actionSource);
        long remaining = leftover == null ? 0L : Ae2Support.stackSize(leftover);
        if (remaining <= 0L) return ItemStack.EMPTY;
        ItemStack rest = stack.copy();
        rest.setCount((int) Math.min(remaining, stack.getCount()));
        return rest;
    }

    private static Object requestFor(ItemStack representative, int amount) {
        ItemStack single = representative.copy();
        single.setCount(1);
        Object request = Ae2Support.aeStack(single);
        if (request == null) return null;
        Object setter = com.sxilverr.quickcraft.util.Reflect.invoke(
                com.sxilverr.quickcraft.util.Reflect.methodByName(request.getClass(), "setStackSize", 1),
                request, (long) Math.max(1, amount));
        return setter == null ? request : setter;
    }
}
