package com.sxilverr.quickcraft.neoforge.storage;

import com.sxilverr.quickcraft.storage.CompositeItemSource;
import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.storage.LabeledSource;
import com.sxilverr.quickcraft.DepositBlacklist;
import com.sxilverr.quickcraft.config.QuickCraftConfig;
import com.sxilverr.quickcraft.neoforge.integration.projecte.AlchBagSource;
import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class ItemSourceFactory {
    private ItemSourceFactory() {
    }

    public static List<LabeledSource> scan(ServerPlayer player, int scanRange) {
        List<LabeledSource> out = new ArrayList<>();
        out.add(new LabeledSource(LabeledSource.SELF_ID, "My Inventory", new ItemStack(Items.PLAYER_HEAD),
                null, new HandlerItemSource(new PlayerMainInvWrapper(player.getInventory())), true));

        Set<ResourceLocation> extraSources = QuickCraftConfig.extraSources();
        DepositBlacklist blacklist = QuickCraftConfig.depositBlacklist();

        boolean ae2 = Services.PLATFORM.isModLoaded("ae2");
        boolean refinedStorage = Services.PLATFORM.isModLoaded("refinedstorage");
        boolean backpacks = Services.PLATFORM.isModLoaded("sophisticatedbackpacks");
        boolean projecte = Services.PLATFORM.isModLoaded("projecte");
        Set<Object> seenNetworks = (ae2 || refinedStorage)
                ? Collections.newSetFromMap(new IdentityHashMap<>()) : null;

        if (scanRange > 0) {
            addNearbyContainers(player, scanRange, out, blacklist, extraSources, ae2, refinedStorage,
                    seenNetworks, backpacks);
        }
        if (ae2) Ae2ItemSource.addWireless(player, seenNetworks, out);
        if (backpacks) SophisticatedBackpackSource.addBackpacks(player, out);
        if (projecte) AlchBagSource.addBags(player, out);
        addExtraItemSources(player, extraSources, blacklist, out);

        return out;
    }

    public static String posKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static void addNearbyContainers(ServerPlayer player, int range, List<LabeledSource> out,
                                            DepositBlacklist blacklist, Set<ResourceLocation> extraSources,
                                            boolean ae2, boolean refinedStorage, Set<Object> seenNetworks,
                                            boolean backpacks) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        Set<Long> mergedChests = new HashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (be == null) continue;
                    BlockPos pos = cursor.immutable();
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();

                    if (backpacks && isBackpackBlock(block)) {
                        SophisticatedBackpackSource.addBlock(be, pos, out);
                        continue;
                    }

                    if (be instanceof ChestBlockEntity && block instanceof ChestBlock
                            && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                        if (!mergedChests.add(pos.asLong())) continue;
                        addDoubleChest(level, be, pos, state, block, blacklist, mergedChests, out);
                        continue;
                    }

                    if (ae2) {
                        LabeledSource network = Ae2ItemSource.tryCreate(be, seenNetworks, player);
                        if (network != null) {
                            out.add(network);
                            continue;
                        }
                    }

                    if (refinedStorage) {
                        LabeledSource network = Rs2ItemSource.tryCreate(be, seenNetworks, player);
                        if (network != null) {
                            out.add(network);
                            continue;
                        }
                    }

                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                    if (handler == null) continue;
                    ItemStack blockItem = new ItemStack(block);
                    ItemStack icon = blockItem.isEmpty() ? new ItemStack(Items.CHEST) : blockItem;
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                    boolean canDeposit = depositable(be, block, blacklist)
                            || (extraSources.contains(blockId) && !blacklist.matches(block));
                    out.add(new LabeledSource("block:" + posKey(pos), icon.getHoverName().getString(),
                            icon, pos, new HandlerItemSource(handler, icon), canDeposit));
                }
            }
        }
    }

    private static void addExtraItemSources(ServerPlayer player, Set<ResourceLocation> extraSources,
                                            DepositBlacklist blacklist, List<LabeledSource> out) {
        if (extraSources.isEmpty()) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null || !extraSources.contains(id)) continue;
            IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
            if (handler == null) continue;
            ItemStack icon = stack.copy();
            out.add(new LabeledSource("extra:" + i, stack.getHoverName().getString(), icon, null,
                    new HandlerItemSource(handler, icon), !blacklist.matches(stack.getItem())));
        }
    }

    private static void addDoubleChest(Level level, BlockEntity be, BlockPos pos, BlockState state, Block block,
                                       DepositBlacklist blacklist, Set<Long> mergedChests, List<LabeledSource> out) {
        BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
        mergedChests.add(other.asLong());
        if (!(be instanceof Container thisHalf)) return;
        BlockEntity otherBe = level.getBlockEntity(other);
        Container container = otherBe instanceof Container otherHalf
                ? new CompoundContainer(thisHalf, otherHalf) : thisHalf;
        BlockPos primary = pos.asLong() <= other.asLong() ? pos : other;
        ItemStack blockItem = new ItemStack(block);
        ItemStack icon = blockItem.isEmpty() ? new ItemStack(Items.CHEST) : blockItem;
        IItemHandler handler = new InvWrapper(container);
        out.add(new LabeledSource("block:" + posKey(primary), "Double Chest", icon, primary,
                new HandlerItemSource(handler, icon), depositable(be, block, blacklist)));
    }

    private static boolean isBackpackBlock(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && "sophisticatedbackpacks".equals(id.getNamespace()) && id.getPath().endsWith("backpack");
    }

    private static boolean depositable(BlockEntity be, Block block, DepositBlacklist blacklist) {
        return !isMachine(be) && !blacklist.matches(block);
    }

    private static boolean isMachine(BlockEntity be) {
        return be instanceof AbstractFurnaceBlockEntity
                || be instanceof HopperBlockEntity
                || be instanceof DispenserBlockEntity
                || be instanceof BrewingStandBlockEntity;
    }
}
