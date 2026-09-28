package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.integration.jer.MobItemSource;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CraftNode {
    public final ItemStack output;
    public int requiredCount;
    public final List<RecipeOption> alternatives;
    public int selectedRecipe;
    public int autoRecipe = -1;
    public int resultPerCraft = 1;
    public int craftsNeeded;
    public final List<CraftNode> children = new ArrayList<CraftNode>();
    public boolean cyclic;
    public boolean owned;
    public boolean catalyst;
    public boolean truncated;
    public CraftNode reference;
    public boolean emcBuy;
    public int freeStock;
    public boolean fitsStation = true;
    public boolean craftReachable = true;
    public List<ItemStack> tagOptions = Collections.emptyList();
    public String tagSignature = "";
    public final int depth;
    public List<MobItemSource> mobSources;
    public int mobIndex;

    public CraftNode(ItemStack output, int requiredCount, List<RecipeOption> alternatives, int depth) {
        this.output = output;
        this.requiredCount = requiredCount;
        this.alternatives = alternatives;
        this.depth = depth;
        this.selectedRecipe = alternatives.isEmpty() ? -1 : 0;
    }

    public boolean isCraftable() {
        return !alternatives.isEmpty();
    }

    public boolean isTagChoice() {
        return tagOptions.size() > 1;
    }

    public boolean isMobSource() {
        return mobSources != null && !mobSources.isEmpty();
    }

    public MobItemSource currentMob() {
        return mobSources.get(Math.floorMod(mobIndex, mobSources.size()));
    }

    public boolean isBlockedByStation() {
        return isCraftable() && !fitsStation && selected() != null && !owned;
    }

    public RecipeOption selected() {
        if (selectedRecipe < 0 || selectedRecipe >= alternatives.size()) return null;
        return alternatives.get(selectedRecipe);
    }

    public Station requiredStation() {
        RecipeOption option = selected();
        return option == null ? null : option.station();
    }
}
