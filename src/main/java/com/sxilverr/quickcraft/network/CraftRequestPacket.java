package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.craft.CraftPlanner;
import com.sxilverr.quickcraft.craft.CraftService;
import com.sxilverr.quickcraft.craft.CraftSummary;
import com.sxilverr.quickcraft.crafting.ForeignRecipe;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraftRequestPacket implements IMessage {
    private static final int MAX_OVERRIDES = 8192;
    private static final int MAX_FOREIGN = 512;
    private static final int MAX_SLOTS = 32;
    private static final int MAX_OPTIONS = 32;

    private ItemStack target = ItemStack.EMPTY;
    private int quantity;
    private Map<ItemKey, ResourceLocation> overrides = new HashMap<ItemKey, ResourceLocation>();
    private Map<String, Item> ingredientChoices = new HashMap<String, Item>();
    private String destinationId = "";
    private Map<ItemKey, ForeignRecipe> foreign = new HashMap<ItemKey, ForeignRecipe>();

    public CraftRequestPacket() {
    }

    public CraftRequestPacket(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                              Map<String, Item> ingredientChoices, String destinationId,
                              Map<ItemKey, ForeignRecipe> foreign) {
        this.target = target;
        this.quantity = quantity;
        this.overrides = overrides;
        this.ingredientChoices = ingredientChoices;
        this.destinationId = destinationId == null ? "" : destinationId;
        this.foreign = foreign == null ? new HashMap<ItemKey, ForeignRecipe>() : foreign;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        Buf.writeStack(buf, target);
        buf.writeInt(quantity);
        Buf.writeString(buf, destinationId);
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
        int foreignCount = Math.min(MAX_FOREIGN, foreign.size());
        buf.writeInt(foreignCount);
        written = 0;
        for (Map.Entry<ItemKey, ForeignRecipe> entry : foreign.entrySet()) {
            if (written++ >= foreignCount) break;
            Buf.writeStack(buf, entry.getKey().toStack(1));
            buf.writeInt(entry.getValue().made());
            List<List<ItemStack>> slots = entry.getValue().inputs();
            int slotCount = Math.min(MAX_SLOTS, slots.size());
            buf.writeInt(slotCount);
            for (int s = 0; s < slotCount; s++) {
                List<ItemStack> options = slots.get(s);
                int optionCount = Math.min(MAX_OPTIONS, options.size());
                buf.writeInt(optionCount);
                for (int o = 0; o < optionCount; o++) Buf.writeStack(buf, options.get(o));
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        target = Buf.readStack(buf);
        quantity = buf.readInt();
        destinationId = Buf.readString(buf);
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
        int foreignCount = Math.min(MAX_FOREIGN, buf.readInt());
        foreign = new HashMap<ItemKey, ForeignRecipe>();
        for (int i = 0; i < foreignCount; i++) {
            ItemStack output = Buf.readStack(buf);
            int made = buf.readInt();
            int slotCount = Math.min(MAX_SLOTS, buf.readInt());
            List<List<ItemStack>> inputs = new ArrayList<List<ItemStack>>();
            for (int s = 0; s < slotCount; s++) {
                int optionCount = Math.min(MAX_OPTIONS, buf.readInt());
                List<ItemStack> options = new ArrayList<ItemStack>();
                for (int o = 0; o < optionCount; o++) {
                    ItemStack option = Buf.readStack(buf);
                    if (!option.isEmpty()) options.add(option);
                }
                if (!options.isEmpty()) inputs.add(options);
            }
            if (!output.isEmpty()) foreign.put(ItemKey.of(output), new ForeignRecipe(made, inputs));
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
                    CraftSummary summary = CraftService.execute(player, msg.target, msg.quantity, msg.overrides,
                            msg.ingredientChoices, msg.destinationId, msg.foreign);
                    player.sendStatusMessage(feedback(summary, msg.target), false);
                }
            });
            return null;
        }
    }

    private static ITextComponent feedback(CraftSummary summary, ItemStack target) {
        String name = target.getDisplayName();
        if (summary.aborted()) {
            return colored(new TextComponentTranslation("quickcraft.chat.aborted", summary.blockedCount(),
                    summary.blocked().getDisplayName()), TextFormatting.RED);
        }
        if (summary.bought()) {
            return withPlacements(colored(new TextComponentTranslation("quickcraft.chat.bought", summary.crafted(), name),
                    TextFormatting.GREEN), summary);
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
        if (summary.collected()) {
            ITextComponent msg = withPlacements(colored(new TextComponentTranslation("quickcraft.chat.collected", name),
                    TextFormatting.YELLOW), summary);
            return msg.appendSibling(colored(blockerText(summary), TextFormatting.GRAY));
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
