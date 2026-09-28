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
            return colored("Quick Craft: could not pull " + summary.blockedCount() + "x "
                    + summary.blocked().getDisplayName() + " out of storage, nothing was crafted", TextFormatting.RED);
        }
        if (summary.full() || (summary.partial() && summary.requested() >= CraftPlanner.MAX_QUANTITY)) {
            return withPlacements(colored("Quick Craft: crafted " + summary.crafted() + "x " + name,
                    TextFormatting.GREEN), summary);
        }
        if (summary.partial()) {
            return withPlacements(colored("Quick Craft: crafted " + summary.crafted() + "/"
                    + requestedLabel(summary.requested()) + " " + name + " - ran out of materials" + limitHint(summary)
                    + blockerText(summary),
                    TextFormatting.YELLOW), summary);
        }
        if (summary.missingStation() != null) {
            return colored("Quick Craft: needs a " + summary.missingStation() + " nearby to craft " + name,
                    TextFormatting.RED);
        }
        return colored("Quick Craft: not enough materials to craft " + name + limitHint(summary) + blockerText(summary),
                TextFormatting.RED);
    }

    private static ITextComponent colored(String text, TextFormatting color) {
        TextComponentString component = new TextComponentString(text);
        component.getStyle().setColor(color);
        return component;
    }

    private static String blockerText(CraftSummary summary) {
        List<CraftPlanner.Blocker> blockers = summary.blockers();
        if (blockers.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" - missing: ");
        int shown = Math.min(3, blockers.size());
        for (int i = 0; i < shown; i++) {
            CraftPlanner.Blocker blocker = blockers.get(i);
            if (i > 0) sb.append(", ");
            sb.append(blocker.missing()).append("x ").append(blocker.key().toStack(1).getDisplayName())
                    .append(blocker.reason().label);
        }
        if (blockers.size() > shown) sb.append(", +").append(blockers.size() - shown).append(" more");
        return sb.toString();
    }

    private static String limitHint(CraftSummary summary) {
        return summary.treeLimited() ? " (recipe tree hit the maxTreeNodes limit, raise it in the config)" : "";
    }

    private static ITextComponent withPlacements(ITextComponent message, CraftSummary summary) {
        List<CraftSummary.Placement> placements = summary.placements();
        if (placements.isEmpty() && summary.dropped() <= 0 && summary.byproducts() <= 0) return message;

        StringBuilder sb = new StringBuilder(" -> ");
        boolean first = true;
        for (CraftSummary.Placement placement : placements) {
            if (!first) sb.append(", ");
            sb.append(placement.count()).append(" to ").append(placement.where());
            first = false;
        }
        if (summary.dropped() > 0) {
            if (!first) sb.append(", ");
            sb.append(summary.dropped()).append(" dropped at your feet");
            first = false;
        }
        if (summary.byproducts() > 0) {
            if (!first) sb.append(", ");
            sb.append("+").append(summary.byproducts())
                    .append(summary.byproducts() == 1 ? " leftover item" : " leftover items");
        }
        return message.appendSibling(colored(sb.toString(), TextFormatting.GRAY));
    }

    private static String requestedLabel(int requested) {
        return requested >= CraftPlanner.MAX_QUANTITY ? "Max" : requested + "x";
    }
}
