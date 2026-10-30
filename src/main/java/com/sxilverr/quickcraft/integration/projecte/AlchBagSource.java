package com.sxilverr.quickcraft.integration.projecte;

import com.sxilverr.quickcraft.storage.HandlerItemSource;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.IItemHandler;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AlchBagSource {
    private AlchBagSource() {
    }

    public static void addBags(EntityPlayerMP player, List<LabeledSource> out) {
        Object provider = ProjectESupport.bagProvider(player);
        if (provider == null) return;
        Set<EnumDyeColor> seen = EnumSet.noneOf(EnumDyeColor.class);
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!ProjectESupport.isBag(stack)) continue;
            EnumDyeColor color = EnumDyeColor.byMetadata(stack.getItemDamage());
            if (!seen.add(color)) continue;
            IItemHandler handler = ProjectESupport.bag(provider, color);
            if (handler == null) continue;
            ItemStack icon = stack.copy();
            icon.setCount(1);
            out.add(new LabeledSource("pebag:" + color.getName(), new TextComponentString(stack.getDisplayName()), icon, null,
                    new BagSource(handler, icon, provider, color, player), true));
        }
    }

    private static final class BagSource extends HandlerItemSource {
        private final Object provider;
        private final EnumDyeColor color;
        private final EntityPlayerMP player;

        private BagSource(IItemHandler handler, ItemStack icon, Object provider, EnumDyeColor color,
                          EntityPlayerMP player) {
            super(handler, icon);
            this.provider = provider;
            this.color = color;
            this.player = player;
        }

        @Override
        public int extract(ItemStack representative, int amount, boolean simulate) {
            int got = super.extract(representative, amount, simulate);
            if (!simulate && got > 0) ProjectESupport.syncBag(provider, color, player);
            return got;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            ItemStack remainder = super.insert(stack, simulate);
            if (!simulate && remainder.getCount() < stack.getCount()) ProjectESupport.syncBag(provider, color, player);
            return remainder;
        }
    }
}
