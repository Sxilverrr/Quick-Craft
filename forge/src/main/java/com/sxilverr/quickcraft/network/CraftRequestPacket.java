package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.forge.craft.CraftService;
import com.sxilverr.quickcraft.craft.CraftFeedback;
import com.sxilverr.quickcraft.craft.CraftSummary;
import com.sxilverr.quickcraft.crafting.ForeignRecipe;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record CraftRequestPacket(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                 Map<String, Item> ingredientChoices, String destinationId,
                                 Map<ItemKey, ForeignRecipe> foreign) {
    private static final int MAX_OVERRIDES = 8192;
    private static final int MAX_FOREIGN = 512;
    private static final int MAX_SLOTS = 32;
    private static final int MAX_OPTIONS = 32;

    public CraftRequestPacket {
        if (destinationId == null) destinationId = "";
        if (foreign == null) foreign = Map.of();
    }

    public static void encode(CraftRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.target());
        buf.writeVarInt(msg.quantity());
        buf.writeUtf(msg.destinationId());
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
        List<Map.Entry<ItemKey, ForeignRecipe>> foreign = msg.foreign().entrySet().stream().limit(MAX_FOREIGN).toList();
        buf.writeVarInt(foreign.size());
        for (Map.Entry<ItemKey, ForeignRecipe> entry : foreign) {
            buf.writeItem(entry.getKey().toStack(1));
            buf.writeVarInt(entry.getValue().made());
            List<List<ItemStack>> slots = entry.getValue().inputs().stream().limit(MAX_SLOTS).toList();
            buf.writeVarInt(slots.size());
            for (List<ItemStack> slot : slots) {
                List<ItemStack> options = slot.stream().limit(MAX_OPTIONS).toList();
                buf.writeVarInt(options.size());
                for (ItemStack option : options) buf.writeItem(option);
            }
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
        int foreignCount = Math.min(MAX_FOREIGN, buf.readVarInt());
        Map<ItemKey, ForeignRecipe> foreign = new HashMap<>();
        for (int i = 0; i < foreignCount; i++) {
            ItemStack output = buf.readItem();
            int made = buf.readVarInt();
            int slotCount = Math.min(MAX_SLOTS, buf.readVarInt());
            List<List<ItemStack>> inputs = new ArrayList<>();
            for (int s = 0; s < slotCount; s++) {
                int optionCount = Math.min(MAX_OPTIONS, buf.readVarInt());
                List<ItemStack> options = new ArrayList<>();
                for (int o = 0; o < optionCount; o++) {
                    ItemStack option = buf.readItem();
                    if (!option.isEmpty()) options.add(option);
                }
                if (!options.isEmpty()) inputs.add(options);
            }
            if (!output.isEmpty()) foreign.put(ItemKey.of(output), new ForeignRecipe(made, inputs));
        }
        return new CraftRequestPacket(target, quantity, overrides, ingredientChoices, destinationId, foreign);
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
            CraftSummary summary;
            try {
                summary = CraftService.execute(player, msg.target(), msg.quantity(), msg.overrides(),
                        msg.ingredientChoices(), msg.destinationId(), msg.foreign());
            } catch (Throwable t) {
                QuickCraftCommon.LOGGER.error("Quick Craft craft failed for {}", msg.target(), t);
                player.displayClientMessage(Component.translatable("quickcraft.chat.error")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }
            player.displayClientMessage(CraftFeedback.of(summary, msg.target()), false);
        });
        context.setPacketHandled(true);
    }
}
