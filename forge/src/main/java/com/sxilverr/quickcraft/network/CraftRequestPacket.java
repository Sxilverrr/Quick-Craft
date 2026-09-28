package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.forge.craft.CraftService;
import com.sxilverr.quickcraft.craft.CraftFeedback;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.craft.CraftSummary;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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

public record CraftRequestPacket(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                 Map<String, Item> ingredientChoices, String destinationId, boolean preview) {
    private static final int MAX_OVERRIDES = 8192;

    public CraftRequestPacket {
        if (destinationId == null) destinationId = "";
    }

    public static void encode(CraftRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.target());
        buf.writeVarInt(msg.quantity());
        buf.writeUtf(msg.destinationId());
        buf.writeBoolean(msg.preview());
        List<Map.Entry<ItemKey, ResourceLocation>> overrides = msg.overrides().entrySet().stream().limit(MAX_OVERRIDES).toList();
        buf.writeVarInt(overrides.size());
        for (Map.Entry<ItemKey, ResourceLocation> entry : overrides) {
            buf.writeItem(entry.getKey().toStack(1));
            buf.writeResourceLocation(entry.getValue());
        }
        List<Map.Entry<String, Item>> choices = msg.ingredientChoices().entrySet().stream().limit(MAX_OVERRIDES).toList();
        buf.writeVarInt(choices.size());
        for (Map.Entry<String, Item> entry : choices) {
            buf.writeUtf(entry.getKey());
            buf.writeResourceLocation(itemId(entry.getValue()));
        }
    }

    public static CraftRequestPacket decode(FriendlyByteBuf buf) {
        ItemStack target = buf.readItem();
        int quantity = buf.readVarInt();
        String destinationId = buf.readUtf();
        boolean preview = buf.readBoolean();
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
        return new CraftRequestPacket(target, quantity, overrides, ingredientChoices, destinationId, preview);
    }

    private static ResourceLocation itemId(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? new ResourceLocation("minecraft", "air") : id;
    }

    public static void handle(CraftRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || msg.target().isEmpty()) return;
            if (msg.preview()) {
                preview(player, msg);
                return;
            }
            CraftSummary summary;
            try {
                summary = CraftService.execute(player, msg.target(), msg.quantity(), msg.overrides(),
                        msg.ingredientChoices(), msg.destinationId());
            } catch (Throwable t) {
                QuickCraftCommon.LOGGER.error("Quick Craft craft failed for {}", msg.target(), t);
                player.displayClientMessage(Component.literal("Quick Craft: crafting failed with an error, check the game log")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }
            player.displayClientMessage(CraftFeedback.of(summary, msg.target()), false);
        });
        context.setPacketHandled(true);
    }

    private static void preview(ServerPlayer player, CraftRequestPacket msg) {
        CraftPreview.Result result;
        try {
            result = CraftService.preview(player, msg.target(), msg.quantity(), msg.overrides(), msg.ingredientChoices());
        } catch (Throwable t) {
            QuickCraftCommon.LOGGER.error("Quick Craft preview failed for {}", msg.target(), t);
            result = new CraftPreview.Result(0, Math.max(1, msg.quantity()), List.of());
        }
        QuickCraftNetwork.sendCraftPreview(player, result);
    }
}
