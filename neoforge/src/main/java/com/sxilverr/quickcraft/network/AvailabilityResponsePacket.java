package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientNetworkHandler;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.Stations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AvailabilityResponsePacket(Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources,
                                         Map<ItemKey, ItemStack> samples, Stations stations)
        implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 65536;

    public static final Type<AvailabilityResponsePacket> TYPE = new Type<>(QuickCraftNetwork.id("availability_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AvailabilityResponsePacket> STREAM_CODEC =
            StreamCodec.of(AvailabilityResponsePacket::write, AvailabilityResponsePacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, AvailabilityResponsePacket msg) {
        List<Map.Entry<ItemKey, Integer>> counts = msg.counts().entrySet().stream().limit(MAX_ENTRIES).toList();
        buf.writeVarInt(counts.size());
        for (Map.Entry<ItemKey, Integer> entry : counts) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, msg.samples().getOrDefault(entry.getKey(), entry.getKey().toStack(1)));
            buf.writeVarInt(entry.getValue());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, msg.sources().getOrDefault(entry.getKey(), ItemStack.EMPTY));
        }
        msg.stations().write(buf);
    }

    private static AvailabilityResponsePacket read(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX_ENTRIES, buf.readVarInt());
        Map<ItemKey, Integer> counts = new HashMap<>();
        Map<ItemKey, ItemStack> sources = new HashMap<>();
        Map<ItemKey, ItemStack> samples = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int amount = buf.readVarInt();
            ItemStack source = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            if (stack.isEmpty()) continue;
            ItemKey key = ItemKey.of(stack);
            counts.put(key, amount);
            if (!source.isEmpty()) sources.put(key, source);
            if (stack.isDamaged()) samples.put(key, stack);
        }
        Stations stations = Stations.read(buf);
        return new AvailabilityResponsePacket(counts, sources, samples, stations);
    }

    public static void handle(AvailabilityResponsePacket msg, IPayloadContext ctx) {
        ClientNetworkHandler.onAvailability(msg.counts(), msg.sources(), msg.samples(), msg.stations());
    }
}
