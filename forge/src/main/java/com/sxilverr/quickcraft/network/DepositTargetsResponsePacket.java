package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientDepositTargets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record DepositTargetsResponsePacket(List<ClientDepositTargets.Target> entries) {
    private static final int MAX_ENTRIES = 4096;

    public static void encode(DepositTargetsResponsePacket msg, FriendlyByteBuf buf) {
        int n = Math.min(MAX_ENTRIES, msg.entries().size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            ClientDepositTargets.Target e = msg.entries().get(i);
            buf.writeUtf(e.id());
            buf.writeUtf(e.label());
            buf.writeItem(e.icon());
            buf.writeInt(e.freeSlots());
            buf.writeInt(e.totalSlots());
        }
    }

    public static DepositTargetsResponsePacket decode(FriendlyByteBuf buf) {
        int n = Math.min(MAX_ENTRIES, buf.readVarInt());
        List<ClientDepositTargets.Target> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf();
            String label = buf.readUtf();
            ItemStack icon = buf.readItem();
            int freeSlots = buf.readInt();
            int totalSlots = buf.readInt();
            entries.add(new ClientDepositTargets.Target(id, label, icon, freeSlots, totalSlots));
        }
        return new DepositTargetsResponsePacket(entries);
    }

    public static void handle(DepositTargetsResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientDepositTargets.accept(msg.entries())));
        context.setPacketHandled(true);
    }
}
