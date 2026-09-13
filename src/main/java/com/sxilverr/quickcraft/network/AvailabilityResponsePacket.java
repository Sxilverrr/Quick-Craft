package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.Stations;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

public class AvailabilityResponsePacket implements IMessage {
    private static final int MAX_ENTRIES = 65536;

    Map<ItemKey, Integer> counts = new HashMap<ItemKey, Integer>();
    Map<ItemKey, ItemStack> sources = new HashMap<ItemKey, ItemStack>();
    Map<ItemKey, ItemStack> samples = new HashMap<ItemKey, ItemStack>();
    Stations stations = Stations.inventoryOnly();

    public AvailabilityResponsePacket() {
    }

    public AvailabilityResponsePacket(Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources,
                                      Map<ItemKey, ItemStack> samples, Stations stations) {
        this.counts = counts;
        this.sources = sources;
        this.samples = samples;
        this.stations = stations;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(counts.size());
        for (Map.Entry<ItemKey, Integer> entry : counts.entrySet()) {
            ItemStack sample = samples.get(entry.getKey());
            Buf.writeStack(buf, sample == null ? entry.getKey().toStack(1) : sample);
            buf.writeInt(entry.getValue());
            ItemStack source = sources.get(entry.getKey());
            Buf.writeStack(buf, source == null ? ItemStack.EMPTY : source);
        }
        stations.write(buf);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = Math.min(MAX_ENTRIES, buf.readInt());
        counts = new HashMap<ItemKey, Integer>();
        sources = new HashMap<ItemKey, ItemStack>();
        samples = new HashMap<ItemKey, ItemStack>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = Buf.readStack(buf);
            int amount = buf.readInt();
            ItemStack source = Buf.readStack(buf);
            if (stack.isEmpty()) continue;
            ItemKey key = ItemKey.of(stack);
            counts.put(key, amount);
            if (!source.isEmpty()) sources.put(key, source);
            if (stack.isItemDamaged()) samples.put(key, stack);
        }
        stations = Stations.read(buf);
    }

    public static class Handler implements IMessageHandler<AvailabilityResponsePacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(final AvailabilityResponsePacket msg, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.sxilverr.quickcraft.client.ClientNetworkHandler.onAvailability(
                            msg.counts, msg.sources, msg.samples, msg.stations);
                }
            });
            return null;
        }
    }
}
