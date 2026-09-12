package com.sxilverr.quickcraft.forge.integration.projecte;

import com.sxilverr.quickcraft.forge.integration.curios.CuriosSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class ProjectEIntegration {
    public static final String MODID = "projecte";
    private static final String CURIOS_MODID = "curios";
    private static final ResourceLocation TABLE_ID = new ResourceLocation(MODID, "transmutation_table");
    private static final List<ResourceLocation> TABLET_IDS = List.of(
            new ResourceLocation(MODID, "transmutation_tablet"),
            new ResourceLocation("projectexpansion", "arcane_transmutation_tablet"));
    private static final TagKey<Item> TABLET_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("projecte", "transmutation_tablets"));

    private static Boolean loaded;
    private static Boolean curios;

    private ProjectEIntegration() {
    }

    public static boolean available() {
        if (loaded == null) loaded = ModList.get().isLoaded(MODID);
        return loaded;
    }

    private static boolean curiosLoaded() {
        if (curios == null) curios = ModList.get().isLoaded(CURIOS_MODID);
        return curios;
    }

    public static Block tableBlock() {
        return ForgeRegistries.BLOCKS.getValue(TABLE_ID);
    }

    private static List<Item> tablets() {
        List<Item> out = new ArrayList<>();
        for (ResourceLocation id : TABLET_IDS) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null && item != Items.AIR) out.add(item);
        }
        return out;
    }

    private static boolean isTablet(ItemStack stack, List<Item> tablets) {
        if (stack.isEmpty()) return false;
        if (stack.is(TABLET_TAG)) return true;
        for (Item tablet : tablets) {
            if (stack.is(tablet)) return true;
        }
        return false;
    }

    public static ItemStack findTablet(Player player) {
        if (player == null) return ItemStack.EMPTY;
        List<Item> tablets = tablets();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isTablet(stack, tablets)) return stack;
        }
        if (curiosLoaded()) return CuriosSlots.find(player, stack -> isTablet(stack, tablets));
        return ItemStack.EMPTY;
    }

    public static BlockPos findTable(Level level, BlockPos center, int range) {
        if (level == null || center == null || range <= 0) return null;
        Block table = tableBlock();
        if (table == null) return null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockState(cursor).is(table)) return cursor.immutable();
                }
            }
        }
        return null;
    }

    public static boolean hasAccess(Player player, Level level, BlockPos center, int range) {
        return !findTablet(player).isEmpty() || findTable(level, center, range) != null;
    }
}
