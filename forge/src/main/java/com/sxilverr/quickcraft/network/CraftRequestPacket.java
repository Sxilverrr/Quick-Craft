package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.forge.craft.CraftService;
import com.sxilverr.quickcraft.craft.CraftPlanner;
import com.sxilverr.quickcraft.craft.CraftSummary;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CraftRequestPacket {
    private static final int MAX_OVERRIDES = 8192;

    private final ItemStack target;
    private final int quantity;
    private final Map<ItemKey, ResourceLocation> overrides;
    private final Map<String, Item> ingredientChoices;
    private final String destinationId;

    public CraftRequestPacket(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                              Map<String, Item> ingredientChoices, String destinationId) {
        this.target = target;
        this.quantity = quantity;
        this.overrides = overrides;
        this.ingredientChoices = ingredientChoices;
        this.destinationId = destinationId == null ? "" : destinationId;
    }

    public static void encode(CraftRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.target);
        buf.writeVarInt(msg.quantity);
        buf.writeUtf(msg.destinationId);
        buf.writeVarInt(msg.overrides.size());
        for (Map.Entry<ItemKey, ResourceLocation> entry : msg.overrides.entrySet()) {
            buf.writeItem(entry.getKey().toStack(1));
            buf.writeResourceLocation(entry.getValue());
        }
        buf.writeVarInt(msg.ingredientChoices.size());
        for (Map.Entry<String, Item> entry : msg.ingredientChoices.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeResourceLocation(itemId(entry.getValue()));
        }
    }

    public static CraftRequestPacket decode(FriendlyByteBuf buf) {
        ItemStack target = buf.readItem();
        int quantity = buf.readVarInt();
        String destinationId = buf.readUtf();
        int count = Math.min(MAX_OVERRIDES, buf.readVarInt());
        Map<ItemKey, ResourceLocation> overrides = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ItemStack representative = buf.readItem();
            ResourceLocation recipe = buf.readResourceLocation();
            overrides.put(ItemKey.of(representative), recipe);
        }
        int choiceCount = Math.min(MAX_OVERRIDES, buf.readVarInt());
        Map<String, Item> ingredientChoices = new HashMap<>();
        for (int i = 0; i < choiceCount; i++) {
            String signature = buf.readUtf();
            Item item = ForgeRegistries.ITEMS.getValue(buf.readResourceLocation());
            if (item != null) ingredientChoices.put(signature, item);
        }
        return new CraftRequestPacket(target, quantity, overrides, ingredientChoices, destinationId);
    }

    private static ResourceLocation itemId(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? new ResourceLocation("minecraft", "air") : id;
    }

    public static void handle(CraftRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || msg.target.isEmpty()) return;
            CraftSummary summary;
            try {
                summary = CraftService.execute(player, msg.target, msg.quantity, msg.overrides,
                        msg.ingredientChoices, msg.destinationId);
            } catch (Throwable t) {
                QuickCraftCommon.LOGGER.error("Quick Craft craft failed for {}", msg.target, t);
                player.displayClientMessage(Component.literal("Quick Craft: crafting failed with an error, check the game log")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }
            player.displayClientMessage(feedback(summary, msg.target), false);
        });
        context.setPacketHandled(true);
    }

    private static Component feedback(CraftSummary summary, ItemStack target) {
        Component name = target.getHoverName();
        if (summary.aborted()) {
            return Component.literal("Quick Craft: could not pull " + summary.blockedCount() + "x ")
                    .append(summary.blocked().getHoverName())
                    .append(Component.literal(" out of storage, nothing was crafted")).withStyle(ChatFormatting.RED);
        }
        if (summary.full()) {
            MutableComponent msg = Component.literal("Quick Craft: crafted " + summary.crafted() + "x ")
                    .append(name).withStyle(ChatFormatting.GREEN);
            return appendPlacements(msg, summary);
        }
        if (summary.partial()) {
            MutableComponent msg = Component.literal("Quick Craft: crafted " + summary.crafted() + "/" + requestedLabel(summary.requested()) + " ")
                    .append(name).append(Component.literal(" - ran out of materials" + limitHint(summary))).withStyle(ChatFormatting.YELLOW);
            return appendBlockers(appendPlacements(msg, summary), summary);
        }
        if (summary.missingStation() != null) {
            return Component.literal("Quick Craft: needs a " + summary.missingStation() + " nearby to craft ")
                    .append(name).withStyle(ChatFormatting.RED);
        }
        MutableComponent msg = Component.literal("Quick Craft: not enough materials to craft ")
                .append(name).append(Component.literal(limitHint(summary))).withStyle(ChatFormatting.RED);
        return appendBlockers(msg, summary);
    }

    private static MutableComponent appendBlockers(MutableComponent msg, CraftSummary summary) {
        List<CraftPlanner.Blocker> blockers = summary.blockers();
        if (blockers.isEmpty()) return msg;
        MutableComponent tail = Component.literal(" - missing: ");
        int shown = Math.min(3, blockers.size());
        for (int i = 0; i < shown; i++) {
            CraftPlanner.Blocker blocker = blockers.get(i);
            if (i > 0) tail.append(Component.literal(", "));
            tail.append(Component.literal(blocker.missing() + "x ")).append(blocker.key().toStack(1).getHoverName())
                    .append(Component.literal(reasonLabel(blocker.reason())));
        }
        if (blockers.size() > shown) tail.append(Component.literal(", +" + (blockers.size() - shown) + " more"));
        return msg.append(tail.withStyle(ChatFormatting.GRAY));
    }

    private static String reasonLabel(CraftPlanner.Reason reason) {
        return switch (reason) {
            case NOT_LEARNED -> " (not learned)";
            case NOT_ENOUGH_EMC -> " (not enough EMC)";
            case CATALYST -> " (catalyst)";
            case STATION -> " (needs a station)";
            case TREE_LIMIT -> " (tree limit)";
            case LOOP -> " (loop)";
            case MANUAL -> " (supplied by you)";
            default -> "";
        };
    }

    private static String limitHint(CraftSummary summary) {
        return summary.treeLimited() ? " (recipe tree hit the maxTreeNodes limit, raise it in the config)" : "";
    }

    private static MutableComponent appendPlacements(MutableComponent msg, CraftSummary summary) {
        List<CraftSummary.Placement> placements = summary.placements();
        if (placements.isEmpty() && summary.dropped() <= 0 && summary.byproducts() <= 0) return msg;
        StringBuilder sb = new StringBuilder(" → ");
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
        return msg.append(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
    }

    private static String requestedLabel(int requested) {
        return requested >= 1_000_000 ? "Max" : requested + "x";
    }
}
