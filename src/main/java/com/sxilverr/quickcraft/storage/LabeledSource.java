package com.sxilverr.quickcraft.storage;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;

public final class LabeledSource {
    public static final String SELF_ID = "self";

    private final String id;
    private final ITextComponent name;
    private final ItemStack icon;
    private final BlockPos pos;
    private final ItemSource source;
    private final boolean depositable;

    public LabeledSource(String id, ITextComponent name, ItemStack icon, BlockPos pos, ItemSource source, boolean depositable) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.pos = pos;
        this.source = source;
        this.depositable = depositable;
    }

    public String id() {
        return id;
    }

    public ITextComponent name() {
        return name;
    }

    public ItemStack icon() {
        return icon;
    }

    public BlockPos pos() {
        return pos;
    }

    public ItemSource source() {
        return source;
    }

    public boolean depositable() {
        return depositable;
    }

    public boolean isSelf() {
        return SELF_ID.equals(id);
    }

    public ITextComponent pickerLabel() {
        if (pos == null) return name;
        return name.createCopy().appendText(" (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
    }
}
