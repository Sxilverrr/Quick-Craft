package com.sxilverr.quickcraft;

import com.sxilverr.quickcraft.craft.CraftPlanner;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class QuickCraftConfig {
    private static final String GENERAL = "general";
    private static final String CLIENT = "client";
    private static final String MOB_PREVIEW = "client.mobPreview";
    private static final String PROJECTE = "client.projecte";

    private static final String[] DEFAULT_PREFERRED = {"minecraft:planks", "minecraft:log"};
    private static final String[] DEFAULT_BLACKLIST = {
            "appliedenergistics2:drive", "appliedenergistics2:controller",
            "appliedenergistics2:charger", "appliedenergistics2:wireless_access_point",
            "refinedstorage:disk_drive",
            "projecte:condenser_mk1", "projecte:condenser_mk2",
            "projecte:relay_mk1", "projecte:relay_mk2", "projecte:relay_mk3",
            "projecte:collector_mk1", "projecte:collector_mk2", "projecte:collector_mk3",
            "projecte:dm_pedestal", "projecte:dm_furnace", "projecte:rm_furnace",
            "@chargers"};
    private static final String[] DEFAULT_EXTRA_SOURCES = {};

    private static Configuration config;

    private static String[] preferredItemIds = DEFAULT_PREFERRED;
    private static String[] depositBlacklistEntries = DEFAULT_BLACKLIST;
    private static String[] extraSourceIds = DEFAULT_EXTRA_SOURCES;
    private static int containerScanRange = 8;
    private static boolean collapseOwnedItems = true;
    private static boolean craftSoundEnabled = true;
    private static String craftSound = "minecraft:entity.arrow.hit_player";
    private static int maxTreeDepth = 32;
    private static int maxTreeNodes = 512;
    private static boolean animationsEnabled = true;
    private static boolean creativeBypass = false;
    private static boolean pinnedListAnimation = true;
    private static boolean hideLoopingRecipes = false;
    private static boolean openOnlyWithRecipe = true;
    private static boolean hoverBulge = true;
    private static boolean sizeTabToFit = true;
    private static String shiftCraftAmount = "64";
    private static boolean useProjectEEmc = true;
    private static boolean useKleinStarEmc = true;
    private static boolean backpackSources = true;

    private static int colorAvailable = 0xFF55FF55;
    private static int colorCrafted = 0xFFFFC64B;
    private static int colorMissing = 0xFFFF5555;
    private static int colorNoStation = 0xFF5A5A5A;
    private static int colorTarget = 0xFF4AA3FF;
    private static int colorNodeBackground = 0xF01A1A1A;
    private static int colorLines = 0xFF7A7A7A;

    private static boolean entitySpin = true;
    private static boolean entityIdle = true;
    private static boolean entityWalk = false;
    private static double entitySpinSpeed = 1.0;
    private static boolean showEmc = true;

    private QuickCraftConfig() {
    }

    public static void load(File file) {
        config = new Configuration(file);
        sync();
    }

    public static void reload() {
        sync();
    }

    private static void sync() {
        if (config == null) return;
        config.load();

        preferredItemIds = config.getStringList("preferredItems", GENERAL, DEFAULT_PREFERRED,
                "Priority list of item ids to prefer when a recipe ingredient or recipe is ambiguous. "
                        + "Earlier entries win, and recipes that use these items are favored. "
                        + "Preference is per item, so minecraft:planks covers every plank variant.");

        containerScanRange = config.getInt("containerScanRange", GENERAL, 8, 0, 64,
                "Block radius around the player to pull items from and deposit results into. 0 disables nearby containers.");

        depositBlacklistEntries = config.getStringList("depositBlacklist", GENERAL, DEFAULT_BLACKLIST,
                "Blocks that will not be shown as options to deposit results into. "
                        + "Entries can be a block id (appliedenergistics2:charger), a whole mod id (appliedenergistics2), "
                        + "or an OreDictionary name prefixed with @ or # (@chargers).");

        extraSourceIds = config.getStringList("extraSources", GENERAL, DEFAULT_EXTRA_SOURCES,
                "Extra block or item ids to pull from and deposit into. They must expose an item inventory.");

        collapseOwnedItems = config.getBoolean("collapseOwnedItems", GENERAL, true,
                "Items you already have enough of are not expanded into their own recipe tree.");

        craftSoundEnabled = config.getBoolean("craftSoundEnabled", GENERAL, true,
                "Play a sound when a Quick Craft finishes crafting something.");

        craftSound = config.getString("craftSound", GENERAL, "minecraft:entity.arrow.hit_player",
                "The sound id played when a Quick Craft finishes.");

        maxTreeDepth = config.getInt("maxTreeDepth", GENERAL, 32, 1, 128,
                "Maximum depth the crafting tree will expand to.");

        maxTreeNodes = config.getInt("maxTreeNodes", GENERAL, 512, 16, 8192,
                "Safety cap on the total number of nodes in a crafting tree.");

        animationsEnabled = config.getBoolean("animationsEnabled", GENERAL, true, "Enable Animations");

        creativeBypass = config.getBoolean("creativeBypass", GENERAL, false,
                "Players in creative mode craft the requested item instantly without needing or consuming any ingredients.");

        pinnedListAnimation = config.getBoolean("pinnedListAnimation", GENERAL, true,
                "The pinned (bookmarked) ingredients list eases its rows in when it appears.");

        hideLoopingRecipes = config.getBoolean("hideLoopingRecipes", GENERAL, false,
                "Hide recipes that loop back on themselves as options unless you already have the looping item on hand.");

        openOnlyWithRecipe = config.getBoolean("openOnlyWithRecipe", GENERAL, true,
                "Pressing the Quick Craft key on an item that has no supported recipe does nothing instead of opening an empty menu.");

        hoverBulge = config.getBoolean("hoverBulge", GENERAL, true, "Tab bulges when hovering over it");

        sizeTabToFit = config.getBoolean("sizeTabToFit", GENERAL, true, "Size a tab to fit a item name");

        shiftCraftAmount = config.getString("shiftCraftAmount", GENERAL, "64",
                "How many items should be crafted when holding SHIFT. Accepts Max");

        useProjectEEmc = config.getBoolean("useProjectEEmc", GENERAL, true,
                "When ProjectE is installed and a transmutation table is nearby or a transmutation tablet is in your "
                        + "inventory, use your EMC to supply missing learned materials and learn the items you craft.");

        useKleinStarEmc = config.getBoolean("useKleinStarEmc", GENERAL, true,
                "Klein Stars carried in your inventory add their stored EMC to what Quick Craft can spend. "
                        + "Your own EMC is spent first, then the stars are drained.");

        backpackSources = config.getBoolean("backpackSources", GENERAL, true,
                "Pull from, and deposit into, any item in your inventory that holds its own inventory. "
                        + "This is what makes 1.12.2 backpack mods such as Iron Backpacks usable as a source.");

        colorAvailable = color("colorAvailable", "FF55FF55", "Color for items you already have. Hex like FF55FF55 or 55FF55.", 0xFF55FF55);
        colorCrafted = color("colorCrafted", "FFFFC64B", "Color for items that will be crafted.", 0xFFFFC64B);
        colorMissing = color("colorMissing", "FFFF5555", "Color for missing materials.", 0xFFFF5555);
        colorNoStation = color("colorNoStation", "FF5A5A5A", "Color for steps you have no station to craft.", 0xFF5A5A5A);
        colorTarget = color("colorTarget", "FF4AA3FF", "Color for the item you are crafting.", 0xFF4AA3FF);
        colorNodeBackground = color("colorNodeBackground", "F01A1A1A", "Fill color behind each step.", 0xF01A1A1A);
        colorLines = color("colorLines", "FF7A7A7A", "Color of the lines linking steps.", 0xFF7A7A7A);

        entitySpin = config.getBoolean("entitySpin", MOB_PREVIEW, true, "Spin the mob preview.");
        entityIdle = config.getBoolean("entityIdleAnimation", MOB_PREVIEW, true, "Play the mob idle animation.");
        entityWalk = config.getBoolean("entityWalkAnimation", MOB_PREVIEW, false, "Play the mob walk animation.");
        entitySpinSpeed = config.get(MOB_PREVIEW, "entitySpinSpeed", 1.0,
                "Mob preview spin speed multiplier.", 0.0, 20.0).getDouble();

        showEmc = config.getBoolean("showEmc", PROJECTE, true,
                "Show your total EMC in the Quick Craft header when a transmutation table or tablet is available.");

        if (config.hasChanged()) config.save();
    }

    private static int color(String name, String def, String comment, int fallback) {
        return parseColor(config.getString(name, CLIENT, def, comment), fallback);
    }

    private static int parseColor(String value, int fallback) {
        if (value == null) return fallback;
        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() > 1 && (hex.startsWith("0x") || hex.startsWith("0X"))) hex = hex.substring(2);
        if (hex.length() == 6) hex = "FF" + hex;
        try {
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static List<Item> preferredItems() {
        String[] ids = preferredItemIds.length == 0 ? DEFAULT_PREFERRED : preferredItemIds;
        List<Item> out = new ArrayList<Item>();
        for (String id : ids) {
            Item item = Reg.item(id);
            if (item != null) out.add(item);
        }
        return out;
    }

    public static int containerScanRange() {
        return containerScanRange;
    }

    public static DepositBlacklist depositBlacklist() {
        return DepositBlacklist.parse(Arrays.asList(depositBlacklistEntries));
    }

    public static Set<ResourceLocation> extraSources() {
        Set<ResourceLocation> out = new HashSet<ResourceLocation>();
        for (String id : extraSourceIds) {
            ResourceLocation loc = Reg.rl(id);
            if (loc != null) out.add(loc);
        }
        return out;
    }

    public static boolean collapseOwnedItems() {
        return collapseOwnedItems;
    }

    public static boolean craftSoundEnabled() {
        return craftSoundEnabled;
    }

    public static ResourceLocation craftSound() {
        return Reg.rl(craftSound);
    }

    public static int maxTreeDepth() {
        return maxTreeDepth;
    }

    public static int maxTreeNodes() {
        return maxTreeNodes;
    }

    public static boolean animationsEnabled() {
        return animationsEnabled;
    }

    public static boolean creativeBypass() {
        return creativeBypass;
    }

    public static boolean pinnedListAnimation() {
        return pinnedListAnimation;
    }

    public static boolean hideLoopingRecipes() {
        return hideLoopingRecipes;
    }

    public static boolean openOnlyWithRecipe() {
        return openOnlyWithRecipe;
    }

    public static boolean hoverBulge() {
        return hoverBulge;
    }

    public static boolean sizeTabToFit() {
        return sizeTabToFit;
    }

    public static boolean useProjectEEmc() {
        return useProjectEEmc;
    }

    public static boolean useKleinStarEmc() {
        return useKleinStarEmc;
    }

    public static boolean backpackSources() {
        return backpackSources;
    }

    public static boolean shiftCraftIsMax() {
        return shiftCraftAmount.trim().equalsIgnoreCase("max");
    }

    public static int shiftCraftAmount() {
        try {
            return Math.max(1, Math.min(CraftPlanner.MAX_QUANTITY, Integer.parseInt(shiftCraftAmount.trim())));
        } catch (NumberFormatException e) {
            return 64;
        }
    }

    public static int colorAvailable() {
        return colorAvailable;
    }

    public static int colorCrafted() {
        return colorCrafted;
    }

    public static int colorMissing() {
        return colorMissing;
    }

    public static int colorNoStation() {
        return colorNoStation;
    }

    public static int colorTarget() {
        return colorTarget;
    }

    public static int colorNodeBackground() {
        return colorNodeBackground;
    }

    public static int colorLines() {
        return colorLines;
    }

    public static boolean entitySpin() {
        return entitySpin;
    }

    public static boolean entityIdle() {
        return entityIdle;
    }

    public static boolean entityWalk() {
        return entityWalk;
    }

    public static double entitySpinSpeed() {
        return entitySpinSpeed;
    }

    public static boolean showEmc() {
        return showEmc;
    }
}
