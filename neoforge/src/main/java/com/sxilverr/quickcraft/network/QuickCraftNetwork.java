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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class QuickCraftNetwork {
    private static final String PROTOCOL = "2";

    private QuickCraftNetwork() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuickCraftCommon.MODID, path);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToServer(CraftRequestPacket.TYPE, CraftRequestPacket.STREAM_CODEC, CraftRequestPacket::handle);
        registrar.playToServer(AvailabilityRequestPacket.TYPE, AvailabilityRequestPacket.STREAM_CODEC, AvailabilityRequestPacket::handle);
        registrar.playToServer(DepositTargetsRequestPacket.TYPE, DepositTargetsRequestPacket.STREAM_CODEC, DepositTargetsRequestPacket::handle);
        registrar.playToClient(AvailabilityResponsePacket.TYPE, AvailabilityResponsePacket.STREAM_CODEC, AvailabilityResponsePacket::handle);
        registrar.playToClient(DepositTargetsResponsePacket.TYPE, DepositTargetsResponsePacket.STREAM_CODEC, DepositTargetsResponsePacket::handle);
        registrar.playToClient(CraftPreviewResponsePacket.TYPE, CraftPreviewResponsePacket.STREAM_CODEC, CraftPreviewResponsePacket::handle);
    }

    public static void sendCraftRequest(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                        Map<String, Item> ingredientChoices, String destinationId) {
        PacketDistributor.sendToServer(new CraftRequestPacket(target, quantity, overrides, ingredientChoices, destinationId, false));
    }

    public static void sendCraftPreviewRequest(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                               Map<String, Item> ingredientChoices) {
        PacketDistributor.sendToServer(new CraftRequestPacket(target, quantity, overrides, ingredientChoices, null, true));
    }

    public static void sendCraftPreview(ServerPlayer player, CraftPreview.Result result) {
        PacketDistributor.sendToPlayer(player,
                new CraftPreviewResponsePacket(result.craftable(), result.requested(), result.gained(), result.blockers()));
    }

    public static void requestDepositTargets() {
        PacketDistributor.sendToServer(DepositTargetsRequestPacket.INSTANCE);
    }

    public static void sendDepositTargets(ServerPlayer player, List<LabeledSource> targets) {
        List<ClientDepositTargets.Target> entries = new ArrayList<>(targets.size());
        for (LabeledSource target : targets) {
            entries.add(new ClientDepositTargets.Target(target.id(), target.pickerLabel(), target.icon(),
                    target.source().freeSlots(), target.source().totalSlots()));
        }
        PacketDistributor.sendToPlayer(player, new DepositTargetsResponsePacket(entries));
    }

    public static void requestAvailability(Collection<ItemKey> keys) {
        if (keys.isEmpty()) return;
        List<ItemStack> stacks = new ArrayList<>(keys.size());
        for (ItemKey key : keys) stacks.add(key.toStack(1));
        PacketDistributor.sendToServer(new AvailabilityRequestPacket(stacks));
    }

    public static void sendAvailability(ServerPlayer player, Map<ItemKey, Integer> counts,
                                        Map<ItemKey, ItemStack> sources, Map<ItemKey, ItemStack> samples, Stations stations) {
        PacketDistributor.sendToPlayer(player, new AvailabilityResponsePacket(counts, sources, samples, stations));
    }
}
