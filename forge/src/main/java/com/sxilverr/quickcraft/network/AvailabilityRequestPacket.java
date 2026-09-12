package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.forge.craft.CraftService;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.StationScan;
import com.sxilverr.quickcraft.crafting.Stations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraftforge.network.NetworkEvent;

public class AvailabilityRequestPacket {
    private static final int MAX_KEYS = 4096;

    private final List<ItemStack> keys;

    public AvailabilityRequestPacket(List<ItemStack> keys) {
        this.keys = keys;
    }

    public static void encode(AvailabilityRequestPacket msg, FriendlyByteBuf buf) {
        int n = Math.min(MAX_KEYS, msg.keys.size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            buf.writeItem(msg.keys.get(i));
        }
    }

    public static AvailabilityRequestPacket decode(FriendlyByteBuf buf) {
        int n = Math.min(MAX_KEYS, buf.readVarInt());
        List<ItemStack> keys = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            keys.add(buf.readItem());
        }
        return new AvailabilityRequestPacket(keys);
    }

    public static void handle(AvailabilityRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            Set<ItemKey> wanted = new HashSet<>();
            for (ItemStack stack : msg.keys) {
                if (!stack.isEmpty()) wanted.add(ItemKey.of(stack));
            }
            CraftService.AvailabilitySnapshot snapshot = CraftService.availability(player, wanted);
            Stations stations = StationScan.detect(player.serverLevel(), player);
            QuickCraftNetwork.sendAvailability(player, snapshot.counts(), snapshot.sources(), snapshot.samples(), stations);
        });
        context.setPacketHandled(true);
    }
}
