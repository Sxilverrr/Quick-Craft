package com.sxilverr.quickcraft.storage;

import com.sxilverr.quickcraft.DepositBlacklist;
import com.sxilverr.quickcraft.QuickCraftConfig;
import com.sxilverr.quickcraft.integration.ae2.Ae2ItemSource;
import com.sxilverr.quickcraft.integration.projecte.AlchBagSource;
import com.sxilverr.quickcraft.integration.projecte.ProjectESupport;
import com.sxilverr.quickcraft.integration.ae2.Ae2Support;
import com.sxilverr.quickcraft.integration.rs.RsItemSource;
import com.sxilverr.quickcraft.integration.rs.RsSupport;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class ItemSourceFactory {
    private ItemSourceFactory() {
    }

    public static List<LabeledSource> scan(EntityPlayerMP player, int scanRange) {
        List<LabeledSource> out = new ArrayList<LabeledSource>();
        out.add(new LabeledSource(LabeledSource.SELF_ID, "My Inventory", new ItemStack(Items.SKULL, 1, 3),
                null, new HandlerItemSource(new PlayerMainInvWrapper(player.inventory)), true));

        Set<Object> seenNetworks = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        Set<ResourceLocation> extraSources = QuickCraftConfig.extraSources();
        DepositBlacklist blacklist = QuickCraftConfig.depositBlacklist();

        if (scanRange > 0) {
            addNearbyContainers(player, scanRange, out, seenNetworks, blacklist, extraSources);
        }
        if (ProjectESupport.available()) AlchBagSource.addBags(player, out);
        addInventoryItemSources(player, extraSources, blacklist, out);

        return out;
    }

    public static String posKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static void addNearbyContainers(EntityPlayerMP player, int range, List<LabeledSource> out,
                                            Set<Object> seenNetworks, DepositBlacklist blacklist,
                                            Set<ResourceLocation> extraSources) {
        World world = player.world;
        BlockPos center = player.getPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    cursor.setPos(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!world.isBlockLoaded(cursor)) continue;
                    TileEntity tile = world.getTileEntity(cursor);
                    if (tile == null) continue;
                    BlockPos pos = cursor.toImmutable();
                    Block block = world.getBlockState(pos).getBlock();

                    if (tile instanceof TileEntityChest && skipSecondChestHalf((TileEntityChest) tile, pos)) {
                        continue;
                    }

                    IItemHandler handler = handlerOf(tile);
                    if (handler != null) {
                        ItemStack icon = iconFor(block);
                        ResourceLocation blockId = Reg.idOf(block);
                        boolean canDeposit = depositable(tile, block, blacklist)
                                || (extraSources.contains(blockId) && !blacklist.matches(block));
                        String name = handler.getSlots() > 27 && tile instanceof TileEntityChest
                                ? "Double Chest" : icon.getDisplayName();
                        out.add(new LabeledSource("block:" + posKey(pos), name, icon, pos,
                                new HandlerItemSource(handler, icon), canDeposit));
                    }

                    if (Ae2Support.available()) {
                        LabeledSource network = Ae2ItemSource.tryCreate(tile, seenNetworks, player);
                        if (network != null) out.add(network);
                    }
                    if (RsSupport.available()) {
                        LabeledSource network = RsItemSource.tryCreate(tile, seenNetworks);
                        if (network != null) out.add(network);
                    }
                }
            }
        }
    }

    private static boolean skipSecondChestHalf(TileEntityChest chest, BlockPos pos) {
        chest.checkForAdjacentChests();
        TileEntityChest partner = chest.adjacentChestXNeg != null ? chest.adjacentChestXNeg
                : chest.adjacentChestZNeg != null ? chest.adjacentChestZNeg
                : chest.adjacentChestXPos != null ? chest.adjacentChestXPos
                : chest.adjacentChestZPos;
        if (partner == null) return false;
        return pos.toLong() > partner.getPos().toLong();
    }

    private static IItemHandler handlerOf(TileEntity tile) {
        if (tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler != null) return handler;
        }
        return tile instanceof IInventory ? new InvWrapper((IInventory) tile) : null;
    }

    private static ItemStack iconFor(Block block) {
        ItemStack stack = new ItemStack(block);
        return stack.isEmpty() ? new ItemStack(net.minecraft.init.Blocks.CHEST) : stack;
    }

    private static void addInventoryItemSources(EntityPlayerMP player, Set<ResourceLocation> extraSources,
                                                DepositBlacklist blacklist, List<LabeledSource> out) {
        boolean pullFromItems = QuickCraftConfig.backpackSources();
        if (!pullFromItems && extraSources.isEmpty()) return;

        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ResourceLocation id = Reg.idOf(stack.getItem());
            boolean listed = id != null && extraSources.contains(id);
            if (!pullFromItems && !listed) continue;
            if (!stack.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) continue;
            IItemHandler handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler == null || handler.getSlots() == 0) continue;
            ItemStack icon = stack.copy();
            icon.setCount(1);
            out.add(new LabeledSource("item:" + i, stack.getDisplayName(), icon, null,
                    new HandlerItemSource(handler, icon), !blacklist.matches(stack.getItem())));
        }
    }

    private static boolean depositable(TileEntity tile, Block block, DepositBlacklist blacklist) {
        return !isMachine(tile) && !blacklist.matches(block);
    }

    private static boolean isMachine(TileEntity tile) {
        return tile instanceof TileEntityFurnace
                || tile instanceof TileEntityHopper
                || tile instanceof TileEntityDispenser
                || tile instanceof TileEntityBrewingStand;
    }
}
