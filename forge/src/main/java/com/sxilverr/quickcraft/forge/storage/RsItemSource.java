package com.sxilverr.quickcraft.forge.storage;

import com.sxilverr.quickcraft.platform.Services;
import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.storage.LabeledSource;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.INetworkNodeGraphEntry;
import com.refinedmods.refinedstorage.api.network.IWirelessTransmitter;
import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.api.network.node.INetworkNodeProxy;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.refinedmods.refinedstorage.capability.NetworkNodeProxyCapability;
import com.refinedmods.refinedstorage.item.NetworkItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RsItemSource implements ItemSource {
    private static final int MAX_REPORT = 1_000_000;

    private final INetwork network;
    private final ItemStack icon;

    private RsItemSource(INetwork network, ItemStack icon) {
        this.network = network;
        this.icon = icon;
    }

    @Override
    public ItemStack sourceIcon() {
        return icon;
    }

    @SuppressWarnings("rawtypes")
    public static LabeledSource tryCreate(BlockEntity be, Set<Object> seenNetworks, ServerPlayer player) {
        INetworkNodeProxy proxy = be.getCapability(NetworkNodeProxyCapability.NETWORK_NODE_PROXY_CAPABILITY).orElse(null);
        if (proxy == null) return null;
        INetworkNode node;
        try {
            node = proxy.getNode();
        } catch (RuntimeException e) {
            return null;
        }
        if (node == null) return null;
        ItemStack icon = diskDriveIcon();
        ItemSource source = fromNetwork(node.getNetwork(), seenNetworks, icon);
        if (source == null) return null;
        BlockPos pos = be.getBlockPos();
        return new LabeledSource("rs:" + ItemSourceFactory.posKey(pos), "RS System", icon, pos, source, true);
    }

    public static void addWireless(ServerPlayer player, Set<Object> seenNetworks, List<LabeledSource> out) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof NetworkItem netItem)) continue;
            if (!Services.STATIONS.hasEnergy(stack)) continue;
            ItemStack wirelessIcon = stack.copy();
            int slot = i;
            try {
                netItem.applyNetwork(player.getServer(), stack, network -> {
                    if (!withinWirelessRange(network, player)) return;
                    ItemSource source = fromNetwork(network, seenNetworks, wirelessIcon);
                    if (source != null) out.add(new LabeledSource("rsw:" + slot, "RS System (Wireless)",
                            wirelessIcon, null, source, true));
                }, error -> {
                });
            } catch (RuntimeException e) {
                continue;
            }
        }
    }

    private static ItemSource fromNetwork(INetwork network, Set<Object> seenNetworks, ItemStack icon) {
        if (network == null || !running(network) || !seenNetworks.add(network)) return null;
        return new RsItemSource(network, icon);
    }

    private static boolean running(INetwork network) {
        try {
            return network.canRun();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private static boolean withinWirelessRange(INetwork network, ServerPlayer player) {
        if (network == null) return false;
        try {
            if (!network.canRun()) return false;
            for (INetworkNodeGraphEntry entry : network.getNodeGraph().all()) {
                INetworkNode node = entry.getNode();
                if (!(node instanceof IWirelessTransmitter transmitter) || !node.isActive()) continue;
                if (!player.level().dimension().equals(transmitter.getDimension())) continue;
                double dx = transmitter.getOrigin().getX() - player.getX();
                double dy = transmitter.getOrigin().getY() - player.getY();
                double dz = transmitter.getOrigin().getZ() - player.getZ();
                if (Math.sqrt(dx * dx + dy * dy + dz * dz) <= transmitter.getRange()) return true;
            }
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
        return false;
    }

    private static ItemStack diskDriveIcon() {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("refinedstorage", "disk_drive"));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public List<ItemStack> snapshot() {
        List<ItemStack> out = new ArrayList<>();
        if (!running(network)) return out;
        for (StackListEntry<ItemStack> entry : network.getItemStorageCache().getList().getStacks()) {
            ItemStack stack = entry.getStack();
            if (stack.isEmpty()) continue;
            ItemStack copy = stack.copy();
            if (copy.getCount() > MAX_REPORT) copy.setCount(MAX_REPORT);
            out.add(copy);
        }
        return out;
    }

    @Override
    public int extract(ItemStack representative, int amount, boolean simulate) {
        if (!running(network)) return 0;
        ItemStack extracted = network.extractItem(representative, amount, simulate ? Action.SIMULATE : Action.PERFORM);
        return extracted.isEmpty() ? 0 : extracted.getCount();
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!running(network)) return stack.copy();
        ItemStack remainder = network.insertItem(stack, stack.getCount(), simulate ? Action.SIMULATE : Action.PERFORM);
        return remainder == null ? ItemStack.EMPTY : remainder;
    }
}
