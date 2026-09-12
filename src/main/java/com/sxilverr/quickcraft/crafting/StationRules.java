package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class StationRules {
    public enum Kind {
        BLOCK, INVENTORY, CARRIED, UPGRADE
    }

    public record Rule(String modId, Kind kind, Set<Station> stations, List<String> ids, StackRequirement requirement) {
    }

    public record RecipeRule(String modId, Station station, List<String> typeIds, Function<Recipe<?>, Station> refine) {
    }

    public record ItemRule(Item item, Rule rule) {
    }

    private static final Set<String> VANILLA_RECIPE_TYPES =
            Set.of("minecraft:crafting", "minecraft:smithing", "minecraft:stonecutting");

    private static final List<Rule> RULES = List.of(
            block("minecraft", Station.CRAFTING, "minecraft:crafting_table"),
            block("minecraft", Station.SMITHING, "minecraft:smithing_table"),
            block("minecraft", Station.STONECUTTER, "minecraft:stonecutter"),

            block("refinedstorage", Station.CRAFTING, "refinedstorage:crafting_grid"),
            inventory("ae2", Station.CRAFTING, "ae2:wireless_crafting_terminal"),
            inventory("ae2wtlib", Station.CRAFTING, StackRequirement.flag("crafting", "ae2wtlib:has_crafting_terminal"),
                    "ae2wtlib:wireless_universal_terminal"),
            inventory("refinedstorageaddons", Station.CRAFTING, StackRequirement.ENERGY,
                    "refinedstorageaddons:wireless_crafting_grid"),
            inventory("refinedstorageaddons", Station.CRAFTING, "refinedstorageaddons:creative_wireless_crafting_grid"),
            block("toms_storage", Station.CRAFTING, "toms_storage:ts.storage_terminal", "toms_storage:ts.crafting_terminal",
                    "toms_storage:storage_terminal", "toms_storage:crafting_terminal"),
            inventory("toms_storage", Station.CRAFTING, "toms_storage:ts.adv_wireless_terminal", "toms_storage:adv_wireless_terminal"),
            block("storagenetwork", Station.CRAFTING, "storagenetwork:request"),
            inventory("storagenetwork", Station.CRAFTING, "storagenetwork:crafting_remote"),
            block("occultism", Station.CRAFTING, "occultism:storage_controller", "occultism:stable_wormhole"),
            inventory("occultism", Station.CRAFTING, StackRequirement.flag("linkedStorageController", "occultism:linked_storage_controller"),
                    "occultism:storage_remote"),

            upgrade("sophisticatedbackpacks", Station.CRAFTING, "sophisticatedbackpacks:crafting_upgrade"),
            upgrade("sophisticatedbackpacks", Station.SMITHING, "sophisticatedbackpacks:smithing_upgrade"),
            upgrade("sophisticatedbackpacks", Station.STONECUTTER, "sophisticatedbackpacks:stonecutter_upgrade"),
            upgrade("sophisticatedstorage", Station.CRAFTING, "sophisticatedstorage:crafting_upgrade"),
            upgrade("sophisticatedstorage", Station.STONECUTTER, "sophisticatedstorage:stonecutter_upgrade"),
            upgrade("travelersbackpack", Station.CRAFTING, "travelersbackpack:crafting_upgrade"),

            carried("crafting_on_a_stick", Station.CRAFTING, "crafting_on_a_stick:crafting_table"),
            carried("crafting_on_a_stick", Station.SMITHING, "crafting_on_a_stick:smithing_table"),
            carried("crafting_on_a_stick", Station.STONECUTTER, "crafting_on_a_stick:stonecutter"),
            inventory("craftingcraft", Station.CRAFTING, "craftingcraft:portable_crafting", "craftingcraft:inventory_crafting"),
            inventory("craftingslots", Station.CRAFTING, "craftingslots:portable_crafting", "craftingslots:inventory_crafting"),

            block("tconstruct", Station.CRAFTING, "tconstruct:crafting_station"),
            block("twilightforest", Station.CRAFTING, "twilightforest:uncrafting_table"),
            block("tacz", Station.GUN_SMITH_TABLE, "tacz:gun_smith_table"),
            block("tacz", Station.AMMO_ASSEMBLY_TABLE, "tacz:workbench_a"),
            block("tacz", Station.ATTACHMENT_TABLE, "tacz:workbench_c"),
            block("avaritia", Station.EXTREME_CRAFTING, "avaritia:extreme_crafting_table"),
            block("extendedcrafting", EnumSet.of(Station.CRAFTING, Station.EXTENDED_BASIC),
                    "extendedcrafting:basic_table", "extendedcrafting:basic_auto_table"),
            block("extendedcrafting", extendedTiers(2), "extendedcrafting:advanced_table", "extendedcrafting:advanced_auto_table"),
            block("extendedcrafting", extendedTiers(3), "extendedcrafting:elite_table", "extendedcrafting:elite_auto_table"),
            block("extendedcrafting", extendedTiers(4), "extendedcrafting:ultimate_table", "extendedcrafting:ultimate_auto_table"),

            block("colouredstuff", Station.CRAFTING, colouredStuffTables()),
            block("colors", Station.CRAFTING, bblColorsTables())
    );

    private static final List<RecipeRule> RECIPE_RULES = List.of(
            new RecipeRule("tacz", Station.GUN_SMITH_TABLE, List.of("tacz:gun_smith_table"), null),
            new RecipeRule("avaritia", Station.EXTREME_CRAFTING,
                    List.of("avaritia:extreme_shaped", "avaritia:extreme_shapeless", "avaritia:extreme_crafting"), null),
            new RecipeRule("extendedcrafting", Station.EXTENDED_BASIC, List.of("extendedcrafting:table"), StationRules::extendedTier)
    );

    private static Map<Block, Rule> blockCache;
    private static List<ItemRule> itemCache;
    private static Map<String, RecipeRule> recipeCache;

    private StationRules() {
    }

    private static Rule block(String modId, Station station, String... ids) {
        return new Rule(modId, Kind.BLOCK, EnumSet.of(station), List.of(ids), StackRequirement.NONE);
    }

    private static Rule block(String modId, Station station, List<String> ids) {
        return new Rule(modId, Kind.BLOCK, EnumSet.of(station), ids, StackRequirement.NONE);
    }

    private static Rule block(String modId, Set<Station> stations, String... ids) {
        return new Rule(modId, Kind.BLOCK, stations, List.of(ids), StackRequirement.NONE);
    }

    private static Rule inventory(String modId, Station station, String... ids) {
        return inventory(modId, station, StackRequirement.NONE, ids);
    }

    private static Rule inventory(String modId, Station station, StackRequirement requirement, String... ids) {
        return new Rule(modId, Kind.INVENTORY, EnumSet.of(station), List.of(ids), requirement);
    }

    private static Rule carried(String modId, Station station, String... ids) {
        return new Rule(modId, Kind.CARRIED, EnumSet.of(station), List.of(ids), StackRequirement.NONE);
    }

    private static Rule upgrade(String modId, Station station, String... ids) {
        return new Rule(modId, Kind.UPGRADE, EnumSet.of(station), List.of(ids), StackRequirement.NONE);
    }

    private static Set<Station> extendedTiers(int tier) {
        List<Station> tiers = List.of(Station.EXTENDED_BASIC, Station.EXTENDED_ADVANCED, Station.EXTENDED_ELITE, Station.EXTENDED_ULTIMATE);
        return EnumSet.copyOf(tiers.subList(0, tier));
    }

    private static Station extendedTier(Recipe<?> recipe) {
        try {
            int tier = (int) recipe.getClass().getMethod("getTier").invoke(recipe);
            return switch (tier) {
                case 2 -> Station.EXTENDED_ADVANCED;
                case 3 -> Station.EXTENDED_ELITE;
                case 4 -> Station.EXTENDED_ULTIMATE;
                default -> Station.EXTENDED_BASIC;
            };
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Station.EXTENDED_BASIC;
        }
    }

    private static List<String> colouredStuffTables() {
        List<String> ids = new ArrayList<>();
        ids.add("colouredstuff:crafting_table_none");
        for (DyeColor color : DyeColor.values()) ids.add("colouredstuff:crafting_table_" + color.getName());
        ids.add("colouredstuff:crafting_table_rainbow");
        return List.copyOf(ids);
    }

    private static List<String> bblColorsTables() {
        List<String> ids = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) ids.add("colors:" + color.getName() + "_crafting_table");
        return List.copyOf(ids);
    }

    public static int rank(Rule rule) {
        if (rule.kind() != Kind.BLOCK) return StationSink.CARRIED;
        return "minecraft".equals(rule.modId()) ? StationSink.VANILLA : StationSink.BLOCK;
    }

    public static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        return item == null || item == Items.AIR ? null : item;
    }

    public static List<String> providerIds(Station station) {
        List<String> ids = new ArrayList<>();
        for (Rule rule : RULES) {
            if (rule.stations().contains(station)) ids.addAll(rule.ids());
        }
        return List.copyOf(ids);
    }

    public static Map<Block, Rule> blocks() {
        if (blockCache != null) return blockCache;
        Map<Block, Rule> map = new HashMap<>();
        for (Rule rule : RULES) {
            if (rule.kind() != Kind.BLOCK || !Services.PLATFORM.isModLoaded(rule.modId())) continue;
            for (String id : rule.ids()) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl == null) continue;
                Block block = BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
                if (block != null && block != Blocks.AIR) map.putIfAbsent(block, rule);
            }
        }
        blockCache = map;
        return map;
    }

    public static List<ItemRule> items() {
        if (itemCache != null) return itemCache;
        List<ItemRule> list = new ArrayList<>();
        for (Rule rule : RULES) {
            if (rule.kind() == Kind.BLOCK || rule.kind() == Kind.UPGRADE || !Services.PLATFORM.isModLoaded(rule.modId())) continue;
            for (String id : rule.ids()) {
                Item item = item(id);
                if (item != null) list.add(new ItemRule(item, rule));
            }
        }
        itemCache = List.copyOf(list);
        return itemCache;
    }

    public static Map<String, RecipeRule> recipeTypes() {
        if (recipeCache != null) return recipeCache;
        Map<String, RecipeRule> map = new HashMap<>();
        for (RecipeRule rule : RECIPE_RULES) {
            if (!Services.PLATFORM.isModLoaded(rule.modId())) continue;
            for (String id : rule.typeIds()) map.put(id, rule);
        }
        recipeCache = map;
        return map;
    }

    public static boolean isSupportedRecipeType(ResourceLocation typeId) {
        if (typeId == null) return false;
        String key = typeId.toString();
        return VANILLA_RECIPE_TYPES.contains(key) || recipeTypes().containsKey(key);
    }

    public static Station stationFor(Recipe<?> recipe, RecipeRule rule) {
        return rule.refine() == null ? rule.station() : rule.refine().apply(recipe);
    }
}
