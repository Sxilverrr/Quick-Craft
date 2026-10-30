package com.sxilverr.quickcraft.integration.projecte;

import com.sxilverr.quickcraft.QuickCraftConfig;
import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EmcDeposit {
    public static final String TABLET_ID = "emc:tablet";
    public static final String TABLE_ID = "emc:table";

    private static final ItemSource SINK = new ItemSource() {
        @Override
        public List<ItemStack> snapshot() {
            return Collections.emptyList();
        }

        @Override
        public int extract(ItemStack representative, int amount, boolean simulate) {
            return 0;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return stack;
        }
    };

    private EmcDeposit() {
    }

    public static boolean isEmc(String id) {
        return TABLET_ID.equals(id) || TABLE_ID.equals(id);
    }

    public static List<LabeledSource> targets(EntityPlayerMP player) {
        List<LabeledSource> out = new ArrayList<LabeledSource>();
        if (!QuickCraftConfig.useProjectEEmc() || !ProjectESupport.available() || player == null) return out;

        ItemStack tablet = ProjectESupport.findTablet(player);
        if (!tablet.isEmpty()) {
            ItemStack icon = tablet.copy();
            icon.setCount(1);
            out.add(new LabeledSource(TABLET_ID, new TextComponentString(icon.getDisplayName()), icon, null, SINK, true));
        }

        BlockPos table = ProjectESupport.findTable(player.world, player.getPosition(),
                QuickCraftConfig.containerScanRange());
        if (table != null) {
            Block block = ProjectESupport.tableBlock();
            ItemStack icon = block == null ? ItemStack.EMPTY : new ItemStack(block);
            ITextComponent name = icon.isEmpty() ? new TextComponentTranslation("tile.pe_transmutation_stone.name")
                    : new TextComponentString(icon.getDisplayName());
            out.add(new LabeledSource(TABLE_ID, name, icon, table, SINK, true));
        }
        return out;
    }

    public static ITextComponent label(String id, List<LabeledSource> targets) {
        for (LabeledSource target : targets) {
            if (target.id().equals(id)) return target.name();
        }
        return new TextComponentTranslation("quickcraft.deposit.your_emc");
    }
}
