package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientNetworkHandler;
import com.sxilverr.quickcraft.craft.CraftPlanner;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record CraftPreviewResponsePacket(int craftable, int requested, List<CraftPreview.Gain> gained,
                                         List<CraftPlanner.Blocker> blockers) {
    private static final int MAX_ENTRIES = 65536;

    public static void encode(CraftPreviewResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.craftable());
        buf.writeVarInt(msg.requested());
        buf.writeVarInt(msg.gained().size());
        for (CraftPreview.Gain gain : msg.gained()) {
            buf.writeItem(gain.key().toStack(1));
            buf.writeVarInt(gain.count());
        }
        buf.writeVarInt(msg.blockers().size());
        for (CraftPlanner.Blocker blocker : msg.blockers()) {
            buf.writeItem(blocker.key().toStack(1));
            buf.writeVarInt(blocker.missing());
            buf.writeVarInt(blocker.reason().ordinal());
        }
    }

    public static CraftPreviewResponsePacket decode(FriendlyByteBuf buf) {
        int craftable = buf.readVarInt();
        int requested = buf.readVarInt();
        int count = Math.min(MAX_ENTRIES, buf.readVarInt());
        List<CraftPreview.Gain> gained = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = buf.readItem();
            int amount = buf.readVarInt();
            if (!stack.isEmpty()) gained.add(new CraftPreview.Gain(ItemKey.of(stack), amount));
        }
        int blockerCount = Math.min(MAX_ENTRIES, buf.readVarInt());
        List<CraftPlanner.Blocker> blockers = new ArrayList<>();
        CraftPlanner.Reason[] reasons = CraftPlanner.Reason.values();
        for (int i = 0; i < blockerCount; i++) {
            ItemStack stack = buf.readItem();
            int missing = buf.readVarInt();
            int reason = buf.readVarInt();
            if (stack.isEmpty() || reason < 0 || reason >= reasons.length) continue;
            blockers.add(new CraftPlanner.Blocker(ItemKey.of(stack), missing, reasons[reason]));
        }
        return new CraftPreviewResponsePacket(craftable, requested, gained, blockers);
    }

    public static void handle(CraftPreviewResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientNetworkHandler.onCraftPreview(msg.craftable(), msg.requested(), msg.gained(), msg.blockers())));
        context.setPacketHandled(true);
    }
}
