package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientHarvestSources;
import com.sxilverr.quickcraft.crafting.HarvestSources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Map;

public record HarvestSourcesPacket(Map<Item, List<Block>> sources) implements CustomPacketPayload {
    public static final Type<HarvestSourcesPacket> TYPE = new Type<>(QuickCraftNetwork.id("harvest_sources"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HarvestSourcesPacket> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> HarvestSources.write(buf, msg.sources()),
            buf -> new HarvestSourcesPacket(HarvestSources.read(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HarvestSourcesPacket msg, IPayloadContext ctx) {
        ClientHarvestSources.accept(msg.sources());
    }
}
