package com.sxilverr.quickcraft.integration.rs;

import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.storage.LabeledSource;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RsItemSource implements ItemSource {
    private static final int MAX_REPORT = 1000000;

    private final Object network;
    private final ItemStack icon;

    private RsItemSource(Object network, ItemStack icon) {
        this.network = network;
        this.icon = icon;
    }

    @Override
    public ItemStack sourceIcon() {
        return icon;
    }

    public static LabeledSource tryCreate(TileEntity tile, Set<Object> seenNetworks) {
        if (!RsSupport.available()) return null;
        Object network = RsSupport.networkOf(tile);
        if (network == null || !RsSupport.running(network) || !seenNetworks.add(network)) return null;
        ItemStack icon = Reg.stack("refinedstorage:disk_drive");
        BlockPos pos = tile.getPos();
        return new LabeledSource("rs:" + pos.getX() + "," + pos.getY() + "," + pos.getZ(),
                new TextComponentTranslation("quickcraft.source.rs_system"), icon, pos, new RsItemSource(network, icon), true);
    }

    @Override
    public List<ItemStack> snapshot() {
        List<ItemStack> out = new ArrayList<ItemStack>();
        if (!RsSupport.running(network)) return out;
        for (Object entry : RsSupport.stacks(network)) {
            if (!(entry instanceof ItemStack)) continue;
            ItemStack stack = (ItemStack) entry;
            if (stack.isEmpty()) continue;
            ItemStack copy = stack.copy();
            if (copy.getCount() > MAX_REPORT) copy.setCount(MAX_REPORT);
            out.add(copy);
        }
        return out;
    }

    @Override
    public int extract(ItemStack representative, int amount, boolean simulate) {
        if (!RsSupport.running(network)) return 0;
        ItemStack extracted = RsSupport.extract(network, representative, amount, simulate);
        return extracted.isEmpty() ? 0 : extracted.getCount();
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!RsSupport.running(network)) return stack.copy();
        return RsSupport.insert(network, stack, stack.getCount(), simulate);
    }
}
