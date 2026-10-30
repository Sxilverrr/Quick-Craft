package com.sxilverr.quickcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class DepositTargetsResponsePacket implements IMessage {
    private static final int MAX_ENTRIES = 4096;

    public static final class Entry {
        private final String id;
        private final ITextComponent label;
        private final ItemStack icon;
        private final int freeSlots;
        private final int totalSlots;

        public Entry(String id, ITextComponent label, ItemStack icon, int freeSlots, int totalSlots) {
            this.id = id;
            this.label = label;
            this.icon = icon;
            this.freeSlots = freeSlots;
            this.totalSlots = totalSlots;
        }

        public String id() {
            return id;
        }

        public ITextComponent label() {
            return label;
        }

        public ItemStack icon() {
            return icon;
        }

        public int freeSlots() {
            return freeSlots;
        }

        public int totalSlots() {
            return totalSlots;
        }
    }

    List<Entry> entries = new ArrayList<Entry>();

    public DepositTargetsResponsePacket() {
    }

    public DepositTargetsResponsePacket(List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int n = Math.min(MAX_ENTRIES, entries.size());
        buf.writeInt(n);
        for (int i = 0; i < n; i++) {
            Entry e = entries.get(i);
            Buf.writeString(buf, e.id());
            Buf.writeString(buf, ITextComponent.Serializer.componentToJson(e.label()));
            Buf.writeStack(buf, e.icon());
            buf.writeInt(e.freeSlots());
            buf.writeInt(e.totalSlots());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int n = Math.min(MAX_ENTRIES, buf.readInt());
        entries = new ArrayList<Entry>(Math.max(0, n));
        for (int i = 0; i < n; i++) {
            String id = Buf.readString(buf);
            ITextComponent label = ITextComponent.Serializer.jsonToComponent(Buf.readString(buf));
            ItemStack icon = Buf.readStack(buf);
            int freeSlots = buf.readInt();
            int totalSlots = buf.readInt();
            entries.add(new Entry(id, label, icon, freeSlots, totalSlots));
        }
    }

    public static class Handler implements IMessageHandler<DepositTargetsResponsePacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(final DepositTargetsResponsePacket msg, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.sxilverr.quickcraft.client.ClientDepositTargets.accept(msg.entries);
                }
            });
            return null;
        }
    }
}
