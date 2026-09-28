package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class QuickCraftNetwork {
    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(QuickCraft.MODID);

    private QuickCraftNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(CraftRequestPacket.Handler.class, CraftRequestPacket.class, id++, Side.SERVER);
        CHANNEL.registerMessage(AvailabilityRequestPacket.Handler.class, AvailabilityRequestPacket.class, id++, Side.SERVER);
        CHANNEL.registerMessage(AvailabilityResponsePacket.Handler.class, AvailabilityResponsePacket.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(DepositTargetsRequestPacket.Handler.class, DepositTargetsRequestPacket.class, id++, Side.SERVER);
        CHANNEL.registerMessage(DepositTargetsResponsePacket.Handler.class, DepositTargetsResponsePacket.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(CraftPreviewResponsePacket.Handler.class, CraftPreviewResponsePacket.class, id++, Side.CLIENT);
    }

    public static void sendCraftRequest(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                        Map<String, Item> ingredientChoices, String destinationId) {
        CHANNEL.sendToServer(new CraftRequestPacket(target, quantity, overrides, ingredientChoices, destinationId, false));
    }

    public static void sendCraftPreviewRequest(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                               Map<String, Item> ingredientChoices) {
        CHANNEL.sendToServer(new CraftRequestPacket(target, quantity, overrides, ingredientChoices, null, true));
    }

    public static void sendCraftPreview(EntityPlayerMP player, CraftPreview.Result result) {
        CHANNEL.sendTo(new CraftPreviewResponsePacket(result.craftable(), result.requested(), result.gained(),
                result.blockers()), player);
    }

    public static void requestDepositTargets() {
        CHANNEL.sendToServer(new DepositTargetsRequestPacket());
    }

    public static void sendDepositTargets(EntityPlayerMP player, List<LabeledSource> targets) {
        List<DepositTargetsResponsePacket.Entry> entries =
                new ArrayList<DepositTargetsResponsePacket.Entry>(targets.size());
        for (LabeledSource target : targets) {
            entries.add(new DepositTargetsResponsePacket.Entry(target.id(), target.pickerLabel(), target.icon(),
                    target.source().freeSlots(), target.source().totalSlots()));
        }
        CHANNEL.sendTo(new DepositTargetsResponsePacket(entries), player);
    }

    public static void requestAvailability(Collection<ItemKey> keys) {
        if (keys.isEmpty()) return;
        List<ItemStack> stacks = new ArrayList<ItemStack>(keys.size());
        for (ItemKey key : keys) stacks.add(key.toStack(1));
        CHANNEL.sendToServer(new AvailabilityRequestPacket(stacks));
    }

    public static void sendAvailability(EntityPlayerMP player, Map<ItemKey, Integer> counts,
                                        Map<ItemKey, ItemStack> sources, Map<ItemKey, ItemStack> samples,
                                        Stations stations) {
        CHANNEL.sendTo(new AvailabilityResponsePacket(counts, sources, samples, stations), player);
    }
}
