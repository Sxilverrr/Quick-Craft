package com.sxilverr.quickcraft.integration.projecte;

import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class ProjectEIntegration {
    public static final String MODID = "projecte";
    private static final List<String> TABLET_IDS = List.of("projecte:transmutation_tablet", "projectexpansion:arcane_transmutation_tablet");
    private static final TagKey<Item> TABLET_TAG = TagKey.create(Registries.ITEM, id("projecte", "transmutation_tablets"));

    private ProjectEIntegration() {
    }

    private static ResourceLocation id(String namespace, String path) {
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
        *///?} else {
        return new ResourceLocation(namespace, path);
        //?}
    }

    public static boolean available() {
        return Services.PLATFORM.isModLoaded(MODID);
    }

    public static Block tableBlock() {
        return BuiltInRegistries.BLOCK.getOptional(id(MODID, "transmutation_table")).orElse(null);
    }

    public static ItemStack findTablet(Player player) {
        if (player == null) return ItemStack.EMPTY;
        List<Item> tablets = TABLET_IDS.stream()
                .map(id -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(id)).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        Predicate<ItemStack> isTablet = stack -> !stack.isEmpty()
                && (stack.is(TABLET_TAG) || tablets.stream().anyMatch(stack::is));
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isTablet.test(stack)) return stack;
        }
        return Services.STATIONS.findCurio(player, isTablet);
    }

    public static BlockPos findTable(Level level, BlockPos center, int range) {
        if (level == null || center == null || range <= 0) return null;
        Block table = tableBlock();
        if (table == null) return null;
        return BlockPos.betweenClosedStream(center.offset(-range, -range, -range), center.offset(range, range, range))
                .filter(pos -> level.getBlockState(pos).is(table))
                .findFirst()
                .map(BlockPos::immutable)
                .orElse(null);
    }

    public static boolean hasAccess(Player player, Level level, BlockPos center, int range) {
        return !findTablet(player).isEmpty() || findTable(level, center, range) != null;
    }
}
