package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

public final class ItemKey {
    private final Item item;
    private final int meta;
    private final NBTTagCompound data;

    private ItemKey(Item item, int meta, NBTTagCompound data) {
        this.item = item;
        this.meta = meta;
        this.data = data;
    }

    public static ItemKey of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new ItemKey(net.minecraft.init.Items.AIR, 0, null);
        NBTTagCompound tag = stack.getTagCompound();
        NBTTagCompound copy = tag == null || tag.isEmpty() ? null : tag.copy();
        return new ItemKey(stack.getItem(), normalizeMeta(stack.getItem(), stack.getMetadata()), copy);
    }

    private static int normalizeMeta(Item item, int meta) {
        if (meta == OreDictionary.WILDCARD_VALUE) return 0;
        if (item != null && item.isDamageable()) return 0;
        return meta;
    }

    public String toKey() {
        ResourceLocation id = Reg.idOf(item);
        StringBuilder sb = new StringBuilder(id == null ? "minecraft:air" : id.toString());
        if (meta != 0) sb.append('@').append(meta);
        if (data != null) sb.append(data.toString());
        return sb.toString();
    }

    public static ItemKey parse(String key) {
        if (key == null || key.isEmpty()) return null;
        int brace = key.indexOf('{');
        String head = brace < 0 ? key : key.substring(0, brace);

        int meta = 0;
        int at = head.indexOf('@');
        if (at >= 0) {
            try {
                meta = Integer.parseInt(head.substring(at + 1).trim());
            } catch (NumberFormatException e) {
                return null;
            }
            head = head.substring(0, at);
        }

        Item item = Reg.item(head);
        if (item == null) return null;

        NBTTagCompound data = null;
        if (brace >= 0) {
            try {
                NBTTagCompound parsed = JsonToNBT.getTagFromJson(key.substring(brace));
                data = parsed == null || parsed.isEmpty() ? null : parsed;
            } catch (Exception e) {
                return null;
            }
        }
        return new ItemKey(item, normalizeMeta(item, meta), data);
    }

    public Item item() {
        return item;
    }

    public int meta() {
        return meta;
    }

    public boolean isLoose() {
        return data == null;
    }

    public ItemKey loose() {
        return data == null ? this : new ItemKey(item, meta, null);
    }

    public boolean sameItem(ItemKey other) {
        return other != null && item == other.item && meta == other.meta;
    }

    public ItemStack toStack(int count) {
        ItemStack stack = new ItemStack(item, count, meta);
        if (data != null) stack.setTagCompound(data.copy());
        return stack;
    }

    public boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == item && equals(of(stack));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey)) return false;
        ItemKey other = (ItemKey) o;
        if (item != other.item || meta != other.meta) return false;
        return data == null ? other.data == null : data.equals(other.data);
    }

    @Override
    public int hashCode() {
        int result = item == null ? 0 : item.hashCode();
        result = 31 * result + meta;
        result = 31 * result + (data == null ? 0 : data.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return toKey();
    }
}
