package com.sxilverr.quickcraft.neoforge.storage;

import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.storage.LabeledSource;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.energy.EnergyNetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.NetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.api.support.network.AbstractNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Rs2ItemSource implements ItemSource {
    private static final int MAX_REPORT = 1_000_000;

    private final Network network;
    private final Actor actor;
    private final ItemStack icon;

    private Rs2ItemSource(Network network, Actor actor, ItemStack icon) {
        this.network = network;
        this.actor = actor;
        this.icon = icon;
    }

    @Override
    public ItemStack sourceIcon() {
        return icon;
    }

    public static LabeledSource tryCreate(BlockEntity be, Set<Object> seenNetworks, ServerPlayer player) {
        if (!(be instanceof AbstractNetworkNodeContainerBlockEntity<?> host)) return null;
        Network network = networkOf(host);
        if (network == null || !running(network)) return null;
        if (!seenNetworks.add(network)) return null;
        ItemStack icon = diskDriveIcon();
        BlockPos pos = be.getBlockPos();
        return new LabeledSource("rs:" + ItemSourceFactory.posKey(pos), "RS System", icon, pos,
                new Rs2ItemSource(network, new PlayerActor(player), icon), true);
    }

    private static Network networkOf(AbstractNetworkNodeContainerBlockEntity<?> host) {
        try {
            for (InWorldNetworkNodeContainer container : host.getContainerProvider().getContainers()) {
                NetworkNode node = container.getNode();
                if (node == null) continue;
                Network network = node.getNetwork();
                if (network != null) return network;
            }
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        return null;
    }

    private static boolean running(Network network) {
        try {
            EnergyNetworkComponent energy = network.getComponent(EnergyNetworkComponent.class);
            return energy != null && energy.getStored() > 0L;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private StorageNetworkComponent storage() {
        if (!running(network)) return null;
        try {
            return network.getComponent(StorageNetworkComponent.class);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static ItemStack diskDriveIcon() {
        Item item = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.fromNamespaceAndPath("refinedstorage", "disk_drive")).orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public List<ItemStack> snapshot() {
        List<ItemStack> out = new ArrayList<>();
        StorageNetworkComponent storage = storage();
        if (storage == null) return out;
        for (ResourceAmount entry : storage.getAll()) {
            if (!(entry.resource() instanceof ItemResource resource)) continue;
            long amount = entry.amount();
            if (amount <= 0) continue;
            out.add(resource.toItemStack(Math.min(amount, MAX_REPORT)));
        }
        return out;
    }

    @Override
    public int extract(ItemStack representative, int amount, boolean simulate) {
        StorageNetworkComponent storage = storage();
        if (storage == null || representative.isEmpty()) return 0;
        long extracted = storage.extract(ItemResource.ofItemStack(representative), amount,
                simulate ? Action.SIMULATE : Action.EXECUTE, actor);
        return (int) Math.min(extracted, Integer.MAX_VALUE);
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        StorageNetworkComponent storage = storage();
        if (storage == null) return stack.copy();
        long inserted = storage.insert(ItemResource.ofItemStack(stack), stack.getCount(),
                simulate ? Action.SIMULATE : Action.EXECUTE, actor);
        int remaining = stack.getCount() - (int) Math.min(inserted, stack.getCount());
        if (remaining <= 0) return ItemStack.EMPTY;
        ItemStack rem = stack.copyWithCount(remaining);
        return rem;
    }
}
