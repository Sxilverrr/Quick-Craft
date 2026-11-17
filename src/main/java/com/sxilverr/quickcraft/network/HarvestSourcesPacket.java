package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.crafting.HarvestSources;
import com.sxilverr.quickcraft.crafting.ItemKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HarvestSourcesPacket implements IMessage {
    Map<ItemKey, List<IBlockState>> sources = new HashMap<ItemKey, List<IBlockState>>();

    public HarvestSourcesPacket() {
    }

    public HarvestSourcesPacket(Map<ItemKey, List<IBlockState>> sources) {
        this.sources = sources;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        HarvestSources.write(buf, sources);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        sources = HarvestSources.read(buf);
    }

    public static class Handler implements IMessageHandler<HarvestSourcesPacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(final HarvestSourcesPacket msg, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.sxilverr.quickcraft.client.ClientHarvestSources.accept(msg.sources);
                }
            });
            return null;
        }
    }
}
