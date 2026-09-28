package com.sxilverr.quickcraft.config;

import com.sxilverr.quickcraft.DepositBlacklist;
import com.sxilverr.quickcraft.craft.CraftPlanner;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class QuickCraftConfig {
    private static final List<String> DEFAULT_PREFERRED_IDS = List.of("minecraft:oak_planks", "minecraft:oak_log");

    private static final List<String> DEFAULT_DEPOSIT_BLACKLIST = List.of(
            "ae2:drive", "ae2:controller", "ae2:charger", "ae2:wireless_access_point",
            //? if >=1.20.5 {
            /*"refinedstorage:disk_drive", "toms_storage:inventory_connector",
            *///?} else {
            "refinedstorage:disk_drive", "toms_storage:ts.inventory_connector",
            //?}
            "projecte:condenser_mk1", "projecte:condenser_mk2",
            "projecte:relay_mk1", "projecte:relay_mk2", "projecte:relay_mk3",
            "projecte:collector_mk1", "projecte:collector_mk2", "projecte:collector_mk3",
            "projecte:dm_pedestal", "projecte:dm_furnace", "projecte:rm_furnace",
            "@chargers");

    private static Supplier<List<? extends String>> preferredItems;
    private static Supplier<Integer> containerScanRange;
    private static Supplier<List<? extends String>> depositBlacklist;
    private static Supplier<List<? extends String>> extraSources;
    private static Supplier<Boolean> collapseOwnedItems;
    private static Supplier<Boolean> craftSoundEnabled;
    private static Supplier<String> craftSound;
    private static Supplier<Integer> maxTreeDepth;
    private static Supplier<Integer> maxTreeNodes;
    private static Supplier<Boolean> animationsEnabled;
    private static Supplier<Boolean> creativeBypass;
    private static Supplier<Boolean> pinnedListAnimation;
    private static Supplier<Boolean> hideLoopingRecipes;
    private static Supplier<Boolean> openOnlyWithRecipe;
    private static Supplier<Boolean> hoverBulge;
    private static Supplier<Boolean> sizeTabToFit;
    private static Supplier<String> shiftCraftAmount;
    private static Supplier<Boolean> useProjectEEmc;
    private static Supplier<Boolean> useKleinStarEmc;

    private QuickCraftConfig() {
    }

    public static void define(ConfigBuilder b) {
        preferredItems = b.comment("Priority list of item ids to prefer when a recipe ingredient or recipe is ambiguous (such as tags).",
                        "Earlier entries win, and recipes that use these items are favored.",
                        "Leave empty to use the built-in defaults.")
                .defineList("preferredItems", DEFAULT_PREFERRED_IDS, QuickCraftConfig::validateItemId);
        containerScanRange = b.comment("Block radius around the player to pull items from and deposit results into. 0 disables nearby containers.")
                .defineInRange("containerScanRange", 8, 0, 64);
        depositBlacklist = b.comment("Blocks that will not be shown as options to deposit results into.",
                        "Entries can be a block id (ae2:charger), a whole mod id (ae2),",
                        "or a block tag prefixed with @ or # (@chargers or #forge:chargers).")
                .defineList("depositBlacklist", DEFAULT_DEPOSIT_BLACKLIST, QuickCraftConfig::validateBlacklistEntry);
        extraSources = b.comment("Extra block or item ids to pull from and deposit into. They must hold an item inventory.")
                .defineList("extraSources", List.of(), QuickCraftConfig::validateResourceId);
        collapseOwnedItems = b.comment("Items you already have enough of are not expanded into their own recipe tree.")
                .define("collapseOwnedItems", true);
        craftSoundEnabled = b.comment("Play a sound when a Quick Craft finishes crafting something.")
                .define("craftSoundEnabled", true);
        craftSound = b.comment("The sound id played when a Quick Craft finishes.")
                .define("craftSound", "minecraft:entity.arrow.hit_player");
        maxTreeDepth = b.comment("Maximum depth the crafting tree will expand to.")
                .defineInRange("maxTreeDepth", 32, 1, 128);
        maxTreeNodes = b.comment("Safety cap on the total number of nodes in a crafting tree.")
                .defineInRange("maxTreeNodes", 512, 16, 8192);
        animationsEnabled = b.comment("Enable Animations")
                .define("animationsEnabled", true);
        creativeBypass = b.comment("Players in creative mode craft the requested item instantly without needing or consuming any ingredients.")
                .define("creativeBypass", false);
        pinnedListAnimation = b.comment("The pinned (bookmarked) ingredients list eases its rows in when it appears.")
                .define("pinnedListAnimation", true);
        hideLoopingRecipes = b.comment("Hide recipes that loop back on themselves as options unless you already have the looping item on hand.")
                .define("hideLoopingRecipes", false);
        openOnlyWithRecipe = b.comment("Pressing the Quick Craft key on an item that has no supported recipe does nothing instead of opening an empty menu.")
                .define("openOnlyWithRecipe", true);
        hoverBulge = b.comment("Tab bulges when hovering over it")
                .define("hoverBulge", true);
        sizeTabToFit = b.comment("Size a tab to fit a item name")
                .define("sizeTabToFit", true);
        shiftCraftAmount = b.comment("How many items should be crafted when holding SHIFT. Accepts Max")
                .define("shiftCraftAmount", "64", QuickCraftConfig::validateShiftAmount);
        useProjectEEmc = b.comment("When ProjectE is installed and a transmutation table is nearby or a transmutation tablet is in your inventory,",
                        "use your EMC to supply missing learned materials and learn the items you craft. Requires ProjectE.")
                .define("useProjectEEmc", true);
        useKleinStarEmc = b.comment("Klein Stars carried in your inventory add their stored EMC to what Quick Craft can spend.",
                        "Your own EMC is spent first, then the stars are drained. Requires ProjectE.")
                .define("useKleinStarEmc", true);
    }

    public static List<Item> preferredItems() {
        List<? extends String> configured = preferredItems.get();
        List<? extends String> ids = configured.isEmpty() ? DEFAULT_PREFERRED_IDS : configured;
        List<Item> out = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) continue;
            Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (item != null && item != Items.AIR) out.add(item);
        }
        return out;
    }

    public static int containerScanRange() {
        return containerScanRange.get();
    }

    public static DepositBlacklist depositBlacklist() {
        return DepositBlacklist.parse(depositBlacklist.get());
    }

    public static Set<ResourceLocation> extraSources() {
        Set<ResourceLocation> out = new HashSet<>();
        for (String id : extraSources.get()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) out.add(rl);
        }
        return out;
    }

    public static boolean collapseOwnedItems() {
        return collapseOwnedItems.get();
    }

    public static boolean craftSoundEnabled() {
        return craftSoundEnabled.get();
    }

    public static ResourceLocation craftSound() {
        return ResourceLocation.tryParse(craftSound.get());
    }

    public static int maxTreeDepth() {
        return maxTreeDepth.get();
    }

    public static int maxTreeNodes() {
        return maxTreeNodes.get();
    }

    public static boolean animationsEnabled() {
        return animationsEnabled.get();
    }

    public static boolean creativeBypass() {
        return creativeBypass.get();
    }

    public static boolean pinnedListAnimation() {
        return pinnedListAnimation.get();
    }

    public static boolean hideLoopingRecipes() {
        return hideLoopingRecipes.get();
    }

    public static boolean openOnlyWithRecipe() {
        return openOnlyWithRecipe.get();
    }

    public static boolean hoverBulge() {
        return hoverBulge.get();
    }

    public static boolean sizeTabToFit() {
        return sizeTabToFit.get();
    }

    public static boolean shiftCraftIsMax() {
        return shiftCraftAmount.get().trim().equalsIgnoreCase("max");
    }

    public static boolean useProjectEEmc() {
        return useProjectEEmc.get();
    }

    public static boolean useKleinStarEmc() {
        return useKleinStarEmc.get();
    }

    public static int shiftCraftAmount() {
        try {
            return Math.max(1, Math.min(CraftPlanner.MAX_QUANTITY, Integer.parseInt(shiftCraftAmount.get().trim())));
        } catch (NumberFormatException e) {
            return 64;
        }
    }

    private static boolean validateShiftAmount(final Object obj) {
        if (!(obj instanceof String s)) return false;
        if (s.trim().equalsIgnoreCase("max")) return true;
        try {
            int value = Integer.parseInt(s.trim());
            return value >= 1 && value <= CraftPlanner.MAX_QUANTITY;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean validateItemId(final Object obj) {
        if (!(obj instanceof String s)) return false;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return rl != null && BuiltInRegistries.ITEM.containsKey(rl);
    }

    private static boolean validateResourceId(final Object obj) {
        return obj instanceof String s && ResourceLocation.tryParse(s) != null;
    }

    private static boolean validateBlacklistEntry(final Object obj) {
        if (!(obj instanceof String raw)) return false;
        String entry = raw.trim();
        if (entry.isEmpty()) return false;
        if (entry.startsWith("@") || entry.startsWith("#")) {
            String tag = entry.substring(1).trim();
            if (tag.isEmpty()) return false;
            return tag.contains(":") ? ResourceLocation.tryParse(tag) != null : isValidResourcePart(tag, true);
        }
        if (entry.contains(":")) {
            String[] parts = entry.split(":", 2);
            if (parts[1].isEmpty() || parts[1].equals("*")) return isValidResourcePart(parts[0], false);
            return ResourceLocation.tryParse(entry) != null;
        }
        return isValidResourcePart(entry, false);
    }

    private static boolean isValidResourcePart(String s, boolean allowSlash) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = c == '_' || c == '-' || c == '.'
                    || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || (allowSlash && c == '/');
            if (!ok) return false;
        }
        return true;
    }
}
