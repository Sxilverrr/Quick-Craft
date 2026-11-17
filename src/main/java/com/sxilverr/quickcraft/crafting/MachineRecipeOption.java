package com.sxilverr.quickcraft.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MachineRecipeOption implements RecipeOption {
    private static final int MAX_INPUTS = 256;

    public record Slot(int amount, List<ItemStack> options) {
    }

    private final ResourceLocation id;
    private final ItemStack result;
    private final int made;
    private final List<Slot> slots;
    private final ItemStack icon;
    private final String label;
    private final List<Ingredient> inputs = new ArrayList<>();

    public MachineRecipeOption(ResourceLocation id, ItemStack result, int made, List<Slot> slots, ItemStack icon, String label) {
        this.id = id;
        this.result = result;
        this.made = Math.max(1, made);
        this.slots = slots;
        this.icon = icon;
        this.label = label;
        for (Slot slot : slots) {
            Ingredient ingredient = Ingredient.of(slot.options().stream());
            for (int i = 0; i < Math.max(1, slot.amount()) && inputs.size() < MAX_INPUTS; i++) inputs.add(ingredient);
        }
    }

    public static ResourceLocation fallbackId(Object... parts) {
        String path = "machine/" + Integer.toHexString(Arrays.hashCode(parts));
        //? if >=1.20.5 {
        /*return ResourceLocation.fromNamespaceAndPath("quickcraft", path);
        *///?} else {
        return new ResourceLocation("quickcraft", path);
        //?}
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public ItemStack result() {
        return result;
    }

    @Override
    public int resultCount() {
        return made;
    }

    @Override
    public List<Ingredient> inputs() {
        return inputs;
    }

    @Override
    public Station station() {
        return null;
    }

    @Override
    public boolean fits(Stations stations) {
        return false;
    }

    public List<Slot> slots() {
        return slots;
    }

    public ItemStack icon() {
        return icon;
    }

    public String label() {
        return label;
    }
}
