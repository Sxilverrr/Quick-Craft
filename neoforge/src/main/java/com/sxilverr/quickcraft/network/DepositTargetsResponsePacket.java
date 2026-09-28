package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientDepositTargets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record DepositTargetsResponsePacket(List<ClientDepositTargets.Target> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 4096;

    public static final Type<DepositTargetsResponsePacket> TYPE = new Type<>(QuickCraftNetwork.id("deposit_targets_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositTargetsResponsePacket> STREAM_CODEC =
            StreamCodec.of(DepositTargetsResponsePacket::write, DepositTargetsResponsePacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, DepositTargetsResponsePacket msg) {
        int n = Math.min(MAX_ENTRIES, msg.entries().size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            ClientDepositTargets.Target e = msg.entries().get(i);
            buf.writeUtf(e.id());
            buf.writeUtf(e.label());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.icon());
            buf.writeInt(e.freeSlots());
            buf.writeInt(e.totalSlots());
        }
    }

    private static DepositTargetsResponsePacket read(RegistryFriendlyByteBuf buf) {
        int n = Math.min(MAX_ENTRIES, buf.readVarInt());
        List<ClientDepositTargets.Target> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf();
            String label = buf.readUtf();
            ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int freeSlots = buf.readInt();
            int totalSlots = buf.readInt();
            entries.add(new ClientDepositTargets.Target(id, label, icon, freeSlots, totalSlots));
        }
        return new DepositTargetsResponsePacket(entries);
    }

    public static void handle(DepositTargetsResponsePacket msg, IPayloadContext ctx) {
        ClientDepositTargets.accept(msg.entries());
    }
}
