package com.sxilverr.quickcraft.crafting;

//? if >=1.20.5 {
/*import com.mojang.serialization.Dynamic;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
*///?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

public final class ItemKey {
    private final Item item;
    private final Object data;

    private ItemKey(Item item, Object data) {
        this.item = item;
        this.data = data;
    }

    public static ItemKey of(ItemStack stack) {
        //? if >=1.20.5 {
        /*DataComponentPatch patch = withoutDamage(stack, stack.getComponentsPatch());
        return new ItemKey(stack.getItem(), patch.isEmpty() ? null : patch);
        *///?} else {
        CompoundTag t = withoutDamage(stack, stack.getTag());
        return new ItemKey(stack.getItem(), t);
        //?}
    }

    //? if >=1.20.5 {
    /*private static DataComponentPatch withoutDamage(ItemStack stack, DataComponentPatch patch) {
        if (patch.isEmpty() || !stack.isDamageableItem()) return patch;
        return patch.forget(type -> type == DataComponents.DAMAGE);
    }
    *///?} else {
    private static CompoundTag withoutDamage(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return null;
        if (!tag.contains("Damage") || !stack.isDamageableItem()) return tag.copy();
        CompoundTag copy = tag.copy();
        copy.remove("Damage");
        return copy.isEmpty() ? null : copy;
    }
    //?}

    public String toKey() {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String base = id == null ? "minecraft:air" : id.toString();
        return data == null ? base : base + encode();
    }

    private String encode() {
        //? if >=1.20.5 {
        /*return DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, (DataComponentPatch) data)
                .result().map(Tag::toString).orElse("");
        *///?} else {
        return data.toString();
        //?}
    }

    public static ItemKey parse(String key) {
        if (key == null || key.isEmpty()) return null;
        int brace = key.indexOf('{');
        String id = brace < 0 ? key : key.substring(0, brace);
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (item == null || item == Items.AIR) return null;
        Object data = null;
        if (brace >= 0) {
            try {
                data = decode(key.substring(brace));
            } catch (Exception e) {
                return null;
            }
        }
        if (data == null) return new ItemKey(item, null);
        return of(new ItemKey(item, data).toStack(1));
    }

    private static Object decode(String text) throws Exception {
        CompoundTag tag = TagParser.parseTag(text);
        //? if >=1.20.5 {
        /*DataComponentPatch patch = DataComponentPatch.CODEC
                .parse(new Dynamic<>(NbtOps.INSTANCE, tag)).result().orElse(null);
        return patch == null || patch.isEmpty() ? null : patch;
        *///?} else {
        return tag;
        //?}
    }

    public Item item() {
        return item;
    }

    public boolean isLoose() {
        return data == null;
    }

    public ItemKey loose() {
        return data == null ? this : new ItemKey(item, null);
    }

    public boolean sameItem(ItemKey other) {
        return other != null && item == other.item;
    }

    public ItemStack toStack(int count) {
        ItemStack stack = new ItemStack(item, count);
        if (data == null) return stack;
        //? if >=1.20.5 {
        /*stack.applyComponents((DataComponentPatch) data);
        *///?} else {
        stack.setTag(((CompoundTag) data).copy());
        //?}
        return stack;
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == item && equals(of(stack));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey k)) return false;
        return item == k.item && Objects.equals(data, k.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, data);
    }
}
