package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.neoforge.craft.CraftService;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.StationScan;
import com.sxilverr.quickcraft.crafting.Stations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AvailabilityRequestPacket implements CustomPacketPayload {
    private static final int MAX_KEYS = 4096;

    public static final Type<AvailabilityRequestPacket> TYPE = new Type<>(QuickCraftNetwork.id("availability_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AvailabilityRequestPacket> STREAM_CODEC =
            StreamCodec.of(AvailabilityRequestPacket::write, AvailabilityRequestPacket::read);

    private final List<ItemStack> keys;

    public AvailabilityRequestPacket(List<ItemStack> keys) {
        this.keys = keys;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, AvailabilityRequestPacket msg) {
        int n = Math.min(MAX_KEYS, msg.keys.size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, msg.keys.get(i));
        }
    }

    private static AvailabilityRequestPacket read(RegistryFriendlyByteBuf buf) {
        int n = Math.min(MAX_KEYS, buf.readVarInt());
        List<ItemStack> keys = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            keys.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return new AvailabilityRequestPacket(keys);
    }

    public static void handle(AvailabilityRequestPacket msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        Set<ItemKey> wanted = new HashSet<>();
        for (ItemStack stack : msg.keys) {
            if (!stack.isEmpty()) wanted.add(ItemKey.of(stack));
        }
        CraftService.AvailabilitySnapshot snapshot = CraftService.availability(player, wanted);
        Stations stations = StationScan.detect(player.serverLevel(), player);
        QuickCraftNetwork.sendAvailability(player, snapshot.counts(), snapshot.sources(), snapshot.samples(), stations);
    }
}
