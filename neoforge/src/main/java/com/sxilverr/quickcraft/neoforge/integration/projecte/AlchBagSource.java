package com.sxilverr.quickcraft.neoforge.integration.projecte;

import com.sxilverr.quickcraft.integration.projecte.ProjectEIntegration;
import com.sxilverr.quickcraft.neoforge.storage.HandlerItemSource;
import com.sxilverr.quickcraft.storage.LabeledSource;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AlchBagSource {
    private static final String BAG_SUFFIX = "_alchemical_bag";

    private AlchBagSource() {
    }

    public static void addBags(ServerPlayer player, List<LabeledSource> out) {
        IAlchBagProvider provider = providerFor(player);
        if (provider == null) return;
        Set<DyeColor> seen = EnumSet.noneOf(DyeColor.class);
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            DyeColor color = bagColor(stack);
            if (color == null || !seen.add(color)) continue;
            IItemHandler handler = bag(provider, color);
            if (handler == null) continue;
            ItemStack icon = stack.copyWithCount(1);
            out.add(new LabeledSource("pebag:" + color.getName(), icon.getHoverName().getString(), icon, null,
                    new BagSource(handler, icon, provider, color, player), true));
        }
    }

    private static DyeColor bagColor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !ProjectEIntegration.MODID.equals(id.getNamespace())) return null;
        String path = id.getPath();
        if (!path.endsWith(BAG_SUFFIX)) return null;
        return DyeColor.byName(path.substring(0, path.length() - BAG_SUFFIX.length()), null);
    }

    private static IAlchBagProvider providerFor(ServerPlayer player) {
        try {
            return player.getCapability(PECapabilities.ALCH_BAG_CAPABILITY);
        } catch (Throwable t) {
            return null;
        }
    }

    private static IItemHandler bag(IAlchBagProvider provider, DyeColor color) {
        try {
            return provider.getBag(color);
        } catch (Throwable t) {
            return null;
        }
    }

    private static final class BagSource extends HandlerItemSource {
        private final IAlchBagProvider provider;
        private final DyeColor color;
        private final ServerPlayer player;

        private BagSource(IItemHandler handler, ItemStack icon, IAlchBagProvider provider, DyeColor color,
                          ServerPlayer player) {
            super(handler, icon);
            this.provider = provider;
            this.color = color;
            this.player = player;
        }

        @Override
        public int extract(ItemStack representative, int amount, boolean simulate) {
            int got = super.extract(representative, amount, simulate);
            if (!simulate && got > 0) sync();
            return got;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            ItemStack remainder = super.insert(stack, simulate);
            if (!simulate && remainder.getCount() < stack.getCount()) sync();
            return remainder;
        }

        private void sync() {
            try {
                provider.sync(player, EnumSet.of(color));
            } catch (Throwable ignored) {
            }
        }
    }
}
