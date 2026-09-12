package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientNetworkHandler;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.Stations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AvailabilityResponsePacket {
    private static final int MAX_ENTRIES = 65536;

    private final Map<ItemKey, Integer> counts;
    private final Map<ItemKey, ItemStack> sources;
    private final Map<ItemKey, ItemStack> samples;
    private final Stations stations;

    public AvailabilityResponsePacket(Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources,
                                      Map<ItemKey, ItemStack> samples, Stations stations) {
        this.counts = counts;
        this.sources = sources;
        this.samples = samples;
        this.stations = stations;
    }

    public static void encode(AvailabilityResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.counts.size());
        for (Map.Entry<ItemKey, Integer> entry : msg.counts.entrySet()) {
            buf.writeItem(msg.samples.getOrDefault(entry.getKey(), entry.getKey().toStack(1)));
            buf.writeVarInt(entry.getValue());
            buf.writeItem(msg.sources.getOrDefault(entry.getKey(), ItemStack.EMPTY));
        }
        msg.stations.write(buf);
    }

    public static AvailabilityResponsePacket decode(FriendlyByteBuf buf) {
        int count = Math.min(MAX_ENTRIES, buf.readVarInt());
        Map<ItemKey, Integer> counts = new HashMap<>();
        Map<ItemKey, ItemStack> sources = new HashMap<>();
        Map<ItemKey, ItemStack> samples = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = buf.readItem();
            int amount = buf.readVarInt();
            ItemStack source = buf.readItem();
            if (stack.isEmpty()) continue;
            ItemKey key = ItemKey.of(stack);
            counts.put(key, amount);
            if (!source.isEmpty()) sources.put(key, source);
            if (stack.isDamaged()) samples.put(key, stack);
        }
        Stations stations = Stations.read(buf);
        return new AvailabilityResponsePacket(counts, sources, samples, stations);
    }

    public static void handle(AvailabilityResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientNetworkHandler.onAvailability(msg.counts, msg.sources, msg.samples, msg.stations)));
        context.setPacketHandled(true);
    }
}
