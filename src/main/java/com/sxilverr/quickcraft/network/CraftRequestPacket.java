package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.craft.CraftPlanner;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.craft.CraftService;
import com.sxilverr.quickcraft.craft.CraftSummary;
import com.sxilverr.quickcraft.crafting.ItemKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraftRequestPacket implements IMessage {
    private static final int MAX_OVERRIDES = 8192;

    private ItemStack target = ItemStack.EMPTY;
    private int quantity;
    private Map<ItemKey, ResourceLocation> overrides = new HashMap<ItemKey, ResourceLocation>();
    private Map<String, Item> ingredientChoices = new HashMap<String, Item>();
    private String destinationId = "";
    private boolean preview;

    public CraftRequestPacket() {
    }

    public CraftRequestPacket(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                              Map<String, Item> ingredientChoices, String destinationId, boolean preview) {
        this.target = target;
        this.quantity = quantity;
        this.overrides = overrides;
        this.ingredientChoices = ingredientChoices;
        this.destinationId = destinationId == null ? "" : destinationId;
        this.preview = preview;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        Buf.writeStack(buf, target);
        buf.writeInt(quantity);
        Buf.writeString(buf, destinationId);
        buf.writeBoolean(preview);
        int overrideCount = Math.min(MAX_OVERRIDES, overrides.size());
        buf.writeInt(overrideCount);
        int written = 0;
        for (Map.Entry<ItemKey, ResourceLocation> entry : overrides.entrySet()) {
            if (written++ >= overrideCount) break;
            Buf.writeStack(buf, entry.getKey().toStack(1));
            Buf.writeId(buf, entry.getValue());
        }
        int choiceCount = Math.min(MAX_OVERRIDES, ingredientChoices.size());
        buf.writeInt(choiceCount);
        written = 0;
        for (Map.Entry<String, Item> entry : ingredientChoices.entrySet()) {
            if (written++ >= choiceCount) break;
            Buf.writeString(buf, entry.getKey());
            Buf.writeItem(buf, entry.getValue());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        target = Buf.readStack(buf);
        quantity = buf.readInt();
        destinationId = Buf.readString(buf);
        preview = buf.readBoolean();
        int count = Math.min(MAX_OVERRIDES, buf.readInt());
        overrides = new HashMap<ItemKey, ResourceLocation>();
        for (int i = 0; i < count; i++) {
            ItemStack representative = Buf.readStack(buf);
            ResourceLocation recipe = Buf.readId(buf);
            if (recipe != null) overrides.put(ItemKey.of(representative), recipe);
        }
        int choiceCount = Math.min(MAX_OVERRIDES, buf.readInt());
        ingredientChoices = new HashMap<String, Item>();
        for (int i = 0; i < choiceCount; i++) {
            String signature = Buf.readString(buf);
            Item item = Buf.readItem(buf);
            if (item != null) ingredientChoices.put(signature, item);
        }
    }

    public static class Handler implements IMessageHandler<CraftRequestPacket, IMessage> {
        @Override
        public IMessage onMessage(final CraftRequestPacket msg, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (msg.target.isEmpty()) return;
                    if (msg.preview) {
                        preview(player, msg);
                        return;
                    }
                    CraftSummary summary = CraftService.execute(player, msg.target, msg.quantity, msg.overrides,
                            msg.ingredientChoices, msg.destinationId);
                    player.sendStatusMessage(feedback(summary, msg.target), false);
                }
            });
            return null;
        }
    }

    private static void preview(EntityPlayerMP player, CraftRequestPacket msg) {
        CraftPreview.Result result;
        try {
            result = CraftService.preview(player, msg.target, msg.quantity, msg.overrides, msg.ingredientChoices);
        } catch (Throwable t) {
            QuickCraft.LOGGER.error("Quick Craft preview failed for " + msg.target, t);
            result = new CraftPreview.Result(0, Math.max(1, msg.quantity), Collections.<CraftPreview.Gain>emptyList());
        }
        QuickCraftNetwork.sendCraftPreview(player, result);
    }

    private static ITextComponent feedback(CraftSummary summary, ItemStack target) {
        String name = target.getDisplayName();
        if (summary.aborted()) {
            return colored(new TextComponentTranslation("quickcraft.chat.aborted", summary.blockedCount(),
                    summary.blocked().getDisplayName()), TextFormatting.RED);
        }
        if (summary.full() || (summary.partial() && summary.requested() >= CraftPlanner.MAX_QUANTITY)) {
            return withPlacements(colored(new TextComponentTranslation("quickcraft.chat.crafted", summary.crafted(), name),
                    TextFormatting.GREEN), summary);
        }
        if (summary.partial()) {
            ITextComponent msg = new TextComponentTranslation("quickcraft.chat.partial", summary.crafted(),
                    requestedLabel(summary.requested()), name);
            return withPlacements(colored(msg.appendSibling(limitHint(summary)).appendSibling(blockerText(summary)),
                    TextFormatting.YELLOW), summary);
        }
        if (summary.missingStation() != null) {
            return colored(new TextComponentTranslation("quickcraft.chat.needs_station", summary.missingStation(), name),
                    TextFormatting.RED);
        }
        ITextComponent msg = new TextComponentTranslation("quickcraft.chat.not_enough", name);
        return colored(msg.appendSibling(limitHint(summary)).appendSibling(blockerText(summary)), TextFormatting.RED);
    }

    private static ITextComponent colored(ITextComponent component, TextFormatting color) {
        component.getStyle().setColor(color);
        return component;
    }

    private static ITextComponent blockerText(CraftSummary summary) {
        List<CraftPlanner.Blocker> blockers = summary.blockers();
        if (blockers.isEmpty()) return new TextComponentString("");
        ITextComponent list = new TextComponentString("");
        int shown = Math.min(3, blockers.size());
        for (int i = 0; i < shown; i++) {
            CraftPlanner.Blocker blocker = blockers.get(i);
            if (i > 0) list.appendText(", ");
            list.appendText(blocker.missing() + "x " + blocker.key().toStack(1).getDisplayName())
                    .appendSibling(blocker.reason().suffix());
        }
        if (blockers.size() > shown) {
            list.appendText(", ").appendSibling(new TextComponentTranslation("quickcraft.more", blockers.size() - shown));
        }
        return new TextComponentString(" - ").appendSibling(new TextComponentTranslation("quickcraft.chat.missing", list));
    }

    private static ITextComponent limitHint(CraftSummary summary) {
        if (!summary.treeLimited()) return new TextComponentString("");
        return new TextComponentString(" ").appendSibling(new TextComponentTranslation("quickcraft.chat.tree_limit"));
    }

    private static ITextComponent withPlacements(ITextComponent message, CraftSummary summary) {
        List<CraftSummary.Placement> placements = summary.placements();
        if (placements.isEmpty() && summary.dropped() <= 0 && summary.byproducts() <= 0) return message;

        ITextComponent parts = new TextComponentString(" -> ");
        boolean first = true;
        for (CraftSummary.Placement placement : placements) {
            if (!first) parts.appendText(", ");
            parts.appendSibling(new TextComponentTranslation("quickcraft.chat.placed", placement.count(), placement.where()));
            first = false;
        }
        if (summary.dropped() > 0) {
            if (!first) parts.appendText(", ");
            parts.appendSibling(new TextComponentTranslation("quickcraft.chat.dropped", summary.dropped()));
            first = false;
        }
        if (summary.byproducts() > 0) {
            if (!first) parts.appendText(", ");
            String key = summary.byproducts() == 1 ? "quickcraft.chat.leftover" : "quickcraft.chat.leftovers";
            parts.appendSibling(new TextComponentTranslation(key, summary.byproducts()));
        }
        return message.appendSibling(colored(parts, TextFormatting.GRAY));
    }

    private static ITextComponent requestedLabel(int requested) {
        return requested >= CraftPlanner.MAX_QUANTITY
                ? new TextComponentTranslation("quickcraft.max") : new TextComponentString(requested + "x");
    }
}
