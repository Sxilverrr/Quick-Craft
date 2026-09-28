package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientNetworkHandler;
import com.sxilverr.quickcraft.craft.CraftPlanner;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record CraftPreviewResponsePacket(int craftable, int requested, List<CraftPreview.Gain> gained,
                                         List<CraftPlanner.Blocker> blockers)
        implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 65536;

    public static final Type<CraftPreviewResponsePacket> TYPE = new Type<>(QuickCraftNetwork.id("craft_preview_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPreviewResponsePacket> STREAM_CODEC =
            StreamCodec.of(CraftPreviewResponsePacket::write, CraftPreviewResponsePacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, CraftPreviewResponsePacket msg) {
        buf.writeVarInt(msg.craftable());
        buf.writeVarInt(msg.requested());
        List<CraftPreview.Gain> gains = msg.gained().subList(0, Math.min(MAX_ENTRIES, msg.gained().size()));
        buf.writeVarInt(gains.size());
        for (CraftPreview.Gain gain : gains) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, gain.key().toStack(1));
            buf.writeVarInt(gain.count());
        }
        List<CraftPlanner.Blocker> blockers = msg.blockers().subList(0, Math.min(MAX_ENTRIES, msg.blockers().size()));
        buf.writeVarInt(blockers.size());
        for (CraftPlanner.Blocker blocker : blockers) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, blocker.key().toStack(1));
            buf.writeVarInt(blocker.missing());
            buf.writeVarInt(blocker.reason().ordinal());
        }
    }

    private static CraftPreviewResponsePacket read(RegistryFriendlyByteBuf buf) {
        int craftable = buf.readVarInt();
        int requested = buf.readVarInt();
        int count = Math.min(MAX_ENTRIES, buf.readVarInt());
        List<CraftPreview.Gain> gained = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int amount = buf.readVarInt();
            if (!stack.isEmpty()) gained.add(new CraftPreview.Gain(ItemKey.of(stack), amount));
        }
        int blockerCount = Math.min(MAX_ENTRIES, buf.readVarInt());
        List<CraftPlanner.Blocker> blockers = new ArrayList<>();
        CraftPlanner.Reason[] reasons = CraftPlanner.Reason.values();
        for (int i = 0; i < blockerCount; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int missing = buf.readVarInt();
            int reason = buf.readVarInt();
            if (stack.isEmpty() || reason < 0 || reason >= reasons.length) continue;
            blockers.add(new CraftPlanner.Blocker(ItemKey.of(stack), missing, reasons[reason]));
        }
        return new CraftPreviewResponsePacket(craftable, requested, gained, blockers);
    }

    public static void handle(CraftPreviewResponsePacket msg, IPayloadContext ctx) {
        ClientNetworkHandler.onCraftPreview(msg.craftable(), msg.requested(), msg.gained(), msg.blockers());
    }
}
