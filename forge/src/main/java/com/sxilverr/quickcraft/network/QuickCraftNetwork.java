package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.client.ClientDepositTargets;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class QuickCraftNetwork {
    private static final String PROTOCOL = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(QuickCraftCommon.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private QuickCraftNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CraftRequestPacket.class,
                CraftRequestPacket::encode, CraftRequestPacket::decode, CraftRequestPacket::handle);
        CHANNEL.registerMessage(id++, AvailabilityRequestPacket.class,
                AvailabilityRequestPacket::encode, AvailabilityRequestPacket::decode, AvailabilityRequestPacket::handle);
        CHANNEL.registerMessage(id++, AvailabilityResponsePacket.class,
                AvailabilityResponsePacket::encode, AvailabilityResponsePacket::decode, AvailabilityResponsePacket::handle);
        CHANNEL.registerMessage(id++, DepositTargetsRequestPacket.class,
                DepositTargetsRequestPacket::encode, DepositTargetsRequestPacket::decode, DepositTargetsRequestPacket::handle);
        CHANNEL.registerMessage(id++, DepositTargetsResponsePacket.class,
                DepositTargetsResponsePacket::encode, DepositTargetsResponsePacket::decode, DepositTargetsResponsePacket::handle);
        CHANNEL.registerMessage(id++, CraftPreviewResponsePacket.class,
                CraftPreviewResponsePacket::encode, CraftPreviewResponsePacket::decode, CraftPreviewResponsePacket::handle);
    }

    public static void sendCraftRequest(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                        Map<String, Item> ingredientChoices, String destinationId) {
        CHANNEL.sendToServer(new CraftRequestPacket(target, quantity, overrides, ingredientChoices, destinationId, false));
    }

    public static void sendCraftPreviewRequest(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                               Map<String, Item> ingredientChoices) {
        CHANNEL.sendToServer(new CraftRequestPacket(target, quantity, overrides, ingredientChoices, null, true));
    }

    public static void sendCraftPreview(ServerPlayer player, CraftPreview.Result result) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new CraftPreviewResponsePacket(result.craftable(), result.requested(), result.gained(), result.blockers()));
    }

    public static void requestDepositTargets() {
        CHANNEL.sendToServer(new DepositTargetsRequestPacket());
    }

    public static void sendDepositTargets(ServerPlayer player, List<LabeledSource> targets) {
        List<ClientDepositTargets.Target> entries = new ArrayList<>(targets.size());
        for (LabeledSource target : targets) {
            entries.add(new ClientDepositTargets.Target(target.id(), target.pickerLabel(), target.icon(),
                    target.source().freeSlots(), target.source().totalSlots()));
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DepositTargetsResponsePacket(entries));
    }

    public static void requestAvailability(Collection<ItemKey> keys) {
        if (keys.isEmpty()) return;
        List<ItemStack> stacks = new ArrayList<>(keys.size());
        for (ItemKey key : keys) stacks.add(key.toStack(1));
        CHANNEL.sendToServer(new AvailabilityRequestPacket(stacks));
    }

    public static void sendAvailability(ServerPlayer player, Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources,
                                        Map<ItemKey, ItemStack> samples, Stations stations) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AvailabilityResponsePacket(counts, sources, samples, stations));
    }
}
