package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientHarvestSources;
import com.sxilverr.quickcraft.crafting.HarvestSources;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record HarvestSourcesPacket(Map<Item, List<Block>> sources) {

    public static void encode(HarvestSourcesPacket msg, FriendlyByteBuf buf) {
        HarvestSources.write(buf, msg.sources());
    }

    public static HarvestSourcesPacket decode(FriendlyByteBuf buf) {
        return new HarvestSourcesPacket(HarvestSources.read(buf));
    }

    public static void handle(HarvestSourcesPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHarvestSources.accept(msg.sources())));
        context.setPacketHandled(true);
    }
}
