package com.sxilverr.quickcraft.neoforge.storage;

import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.storage.LabeledSource;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import appeng.api.util.DimensionalBlockPos;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.items.tools.powered.WirelessTerminalItem;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Ae2ItemSource implements ItemSource {
    private static final int MAX_REPORT = 1_000_000;

    private final IStorageService storageService;
    private final IEnergyService energyService;
    private final IActionSource actionSource;
    private final ItemStack icon;

    private Ae2ItemSource(IStorageService storageService, IEnergyService energyService, IActionSource actionSource, ItemStack icon) {
        this.storageService = storageService;
        this.energyService = energyService;
        this.actionSource = actionSource;
        this.icon = icon;
    }

    @Override
    public ItemStack sourceIcon() {
        return icon;
    }

    public static LabeledSource tryCreate(BlockEntity be, Set<Object> seenNetworks, ServerPlayer player) {
        if (!(be instanceof IInWorldGridNodeHost host)) return null;
        ItemStack icon = driveIcon();
        ItemSource source = fromGrid(gridOf(host), seenNetworks, player, icon);
        if (source == null) return null;
        BlockPos pos = be.getBlockPos();
        return new LabeledSource("ae2:" + ItemSourceFactory.posKey(pos), "ME System", icon, pos, source, true);
    }

    public static void addWireless(ServerPlayer player, Set<Object> seenNetworks, List<LabeledSource> out) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof WirelessTerminalItem terminal)) continue;
            IGrid grid;
            try {
                grid = terminal.getLinkedGrid(stack, player.level(), message -> {
                });
            } catch (RuntimeException e) {
                continue;
            }
            if (!withinWirelessRange(grid, player)) continue;
            ItemStack icon = stack.copy();
            ItemSource source = fromGrid(grid, seenNetworks, player, icon);
            if (source != null) out.add(new LabeledSource("ae2w:" + i, "ME System (Wireless)", icon, null, source, true));
        }
    }

    private static ItemSource fromGrid(IGrid grid, Set<Object> seenNetworks, ServerPlayer player, ItemStack icon) {
        if (grid == null || !seenNetworks.add(grid)) return null;
        IEnergyService energy = grid.getEnergyService();
        IStorageService storage = grid.getStorageService();
        if (energy == null || storage == null || storage.getInventory() == null) return null;
        if (!energy.isNetworkPowered()) return null;
        return new Ae2ItemSource(storage, energy, IActionSource.ofPlayer(player), icon);
    }

    private static boolean withinWirelessRange(IGrid grid, ServerPlayer player) {
        if (grid == null) return false;
        try {
            for (WirelessAccessPointBlockEntity wap : grid.getMachines(WirelessAccessPointBlockEntity.class)) {
                if (!wap.isActive()) continue;
                DimensionalBlockPos loc = wap.getLocation();
                if (loc.getLevel() != player.level()) continue;
                double range = wap.getRange();
                double dx = loc.getPos().getX() - player.getX();
                double dy = loc.getPos().getY() - player.getY();
                double dz = loc.getPos().getZ() - player.getZ();
                if (dx * dx + dy * dy + dz * dz <= range * range) return true;
            }
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
        return false;
    }

    private static ItemStack driveIcon() {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("ae2", "drive")).orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static IGrid gridOf(IInWorldGridNodeHost host) {
        IGridNode node = safeNode(host, null);
        if (node == null) {
            for (Direction dir : Direction.values()) {
                node = safeNode(host, dir);
                if (node != null) break;
            }
        }
        return node == null ? null : node.getGrid();
    }

    private static IGridNode safeNode(IInWorldGridNodeHost host, Direction dir) {
        try {
            return host.getGridNode(dir);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public List<ItemStack> snapshot() {
        List<ItemStack> out = new ArrayList<>();
        KeyCounter counter = storageService.getCachedInventory();
        if (counter == null) return out;
        for (Object2LongMap.Entry<AEKey> entry : counter) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            if (amount <= 0 || !(key instanceof AEItemKey itemKey)) continue;
            out.add(itemKey.toStack((int) Math.min(amount, MAX_REPORT)));
        }
        return out;
    }

    @Override
    public int extract(ItemStack representative, int amount, boolean simulate) {
        AEItemKey key = AEItemKey.of(representative);
        if (key == null) return 0;
        long extracted = StorageHelper.poweredExtraction(energyService, storageService.getInventory(), key, amount,
                actionSource, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
        return (int) Math.min(extracted, Integer.MAX_VALUE);
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) return stack;
        long inserted = StorageHelper.poweredInsert(energyService, storageService.getInventory(), key, stack.getCount(),
                actionSource, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
        int remaining = stack.getCount() - (int) Math.min(inserted, stack.getCount());
        if (remaining <= 0) return ItemStack.EMPTY;
        ItemStack rem = stack.copyWithCount(remaining);
        return rem;
    }
}
