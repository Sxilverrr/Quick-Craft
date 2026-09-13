package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.craft.CraftService;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.StationScan;
import com.sxilverr.quickcraft.crafting.Stations;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AvailabilityRequestPacket implements IMessage {
    private static final int MAX_KEYS = 4096;

    private List<ItemStack> keys = new ArrayList<ItemStack>();

    public AvailabilityRequestPacket() {
    }

    public AvailabilityRequestPacket(List<ItemStack> keys) {
        this.keys = keys;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int n = Math.min(MAX_KEYS, keys.size());
        buf.writeInt(n);
        for (int i = 0; i < n; i++) {
            Buf.writeStack(buf, keys.get(i));
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int n = Math.min(MAX_KEYS, buf.readInt());
        keys = new ArrayList<ItemStack>(Math.max(0, n));
        for (int i = 0; i < n; i++) {
            keys.add(Buf.readStack(buf));
        }
    }

    public static class Handler implements IMessageHandler<AvailabilityRequestPacket, IMessage> {
        @Override
        public IMessage onMessage(final AvailabilityRequestPacket msg, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    Set<ItemKey> wanted = new HashSet<ItemKey>();
                    for (ItemStack stack : msg.keys) {
                        if (!stack.isEmpty()) wanted.add(ItemKey.of(stack));
                    }
                    CraftService.AvailabilitySnapshot snapshot = CraftService.availability(player, wanted);
                    Stations stations = StationScan.detect(player.world, player);
                    QuickCraftNetwork.sendAvailability(player, snapshot.counts(), snapshot.sources(),
                            snapshot.samples(), stations);
                }
            });
            return null;
        }
    }
}
