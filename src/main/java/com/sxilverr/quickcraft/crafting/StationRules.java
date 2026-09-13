package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class StationRules {
    public enum Kind {
        BLOCK, INVENTORY, CARRIED, UPGRADE
    }

    public interface StateMatch {
        boolean test(IBlockState state);
    }

    public static final StateMatch ANY_STATE = new StateMatch() {
        @Override
        public boolean test(IBlockState state) {
            return true;
        }
    };

    public static final class Rule {
        public final String modId;
        public final Kind kind;
        public final Set<Station> stations;
        public final List<String> ids;
        public final StackRequirement requirement;
        public final StateMatch state;

        Rule(String modId, Kind kind, Set<Station> stations, List<String> ids, StackRequirement requirement, StateMatch state) {
            this.modId = modId;
            this.kind = kind;
            this.stations = Collections.unmodifiableSet(stations);
            this.ids = Collections.unmodifiableList(ids);
            this.requirement = requirement;
            this.state = state;
        }
    }

    public static final class BlockEntry {
        public final Rule rule;
        public final StateMatch match;
        public final ItemStack source;

        BlockEntry(Rule rule, StateMatch match, ItemStack source) {
            this.rule = rule;
            this.match = match;
            this.source = source;
        }
    }

    public static final class ItemRule {
        public final Item item;
        public final int meta;
        public final Rule rule;

        ItemRule(Item item, int meta, Rule rule) {
            this.item = item;
            this.meta = meta;
            this.rule = rule;
        }
    }

    private static final List<Rule> RULES = Arrays.asList(
            block("minecraft", Station.CRAFTING, "minecraft:crafting_table"),

            block("refinedstorage", Station.CRAFTING, property("type", "crafting", "pattern"), "refinedstorage:grid@1"),
            inventory("refinedstorageaddons", Station.CRAFTING, StackRequirement.ENERGY,
                    "refinedstorageaddons:wireless_crafting_grid"),
            upgrade("appliedenergistics2", Station.CRAFTING, "appliedenergistics2:part@340"),
            carried("wct", Station.CRAFTING, "wct:wct", "wct:wct_creative"),
            block("storagenetwork", Station.CRAFTING, "storagenetwork:request"),
            inventory("storagenetwork", Station.CRAFTING,
                    "storagenetwork:remote@0", "storagenetwork:remote@1", "storagenetwork:remote@2"),

            upgrade("travelersbackpack", Station.CRAFTING, "travelersbackpack:travelers_backpack"),
            inventory("craftingcraft", Station.CRAFTING, "craftingcraft:portable_crafting_table"),

            block("tconstruct", Station.CRAFTING, "tconstruct:tooltables@0"),
            block("twilightforest", Station.CRAFTING, "twilightforest:uncrafting_table"),
            block("forestry", Station.CRAFTING, "forestry:worktable"),
            block("extendedcrafting", Station.CRAFTING, "extendedcrafting:crafting_table"),
            block("extendedcrafting", EnumSet.of(Station.CRAFTING, Station.EXTENDED_BASIC), "extendedcrafting:table_basic"),
            block("extendedcrafting", extendedTiers(2), "extendedcrafting:table_advanced"),
            block("extendedcrafting", extendedTiers(3), "extendedcrafting:table_elite"),
            block("extendedcrafting", extendedTiers(4), "extendedcrafting:table_ultimate"),
            block("avaritia", Station.EXTREME_CRAFTING, property("type", "extreme"),
                    "avaritia:extreme_crafting_table", "avaritia:extreme_crafting"),

            block("futuremc", Station.STONECUTTER, "futuremc:stonecutter"),
            block("futuremc", Station.SMITHING, "futuremc:smithing_table"),
            block("ubm", Station.SMITHING, "ubm:smithing_table")
    );

    private static Map<Block, List<BlockEntry>> blockCache;
    private static List<ItemRule> itemCache;

    private StationRules() {
    }

    private static Rule block(String modId, Station station, String... ids) {
        return new Rule(modId, Kind.BLOCK, EnumSet.of(station), Arrays.asList(ids), StackRequirement.NONE, ANY_STATE);
    }

    private static Rule block(String modId, Set<Station> stations, String... ids) {
        return new Rule(modId, Kind.BLOCK, stations, Arrays.asList(ids), StackRequirement.NONE, ANY_STATE);
    }

    private static Rule block(String modId, Station station, StateMatch state, String... ids) {
        return new Rule(modId, Kind.BLOCK, EnumSet.of(station), Arrays.asList(ids), StackRequirement.NONE, state);
    }

    private static Rule inventory(String modId, Station station, String... ids) {
        return inventory(modId, station, StackRequirement.NONE, ids);
    }

    private static Rule inventory(String modId, Station station, StackRequirement requirement, String... ids) {
        return new Rule(modId, Kind.INVENTORY, EnumSet.of(station), Arrays.asList(ids), requirement, ANY_STATE);
    }

    private static Rule carried(String modId, Station station, String... ids) {
        return new Rule(modId, Kind.CARRIED, EnumSet.of(station), Arrays.asList(ids), StackRequirement.NONE, ANY_STATE);
    }

    private static Rule upgrade(String modId, Station station, String... ids) {
        return new Rule(modId, Kind.UPGRADE, EnumSet.of(station), Arrays.asList(ids), StackRequirement.NONE, ANY_STATE);
    }

    private static Set<Station> extendedTiers(int tier) {
        List<Station> tiers = Arrays.asList(Station.EXTENDED_BASIC, Station.EXTENDED_ADVANCED,
                Station.EXTENDED_ELITE, Station.EXTENDED_ULTIMATE);
        return EnumSet.copyOf(tiers.subList(0, tier));
    }

    private static StateMatch property(final String name, final String... contains) {
        return new StateMatch() {
            @Override
            public boolean test(IBlockState state) {
                for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
                    if (!name.equals(entry.getKey().getName())) continue;
                    String value = String.valueOf(entry.getValue()).toLowerCase(Locale.ROOT);
                    for (String part : contains) {
                        if (value.contains(part)) return true;
                    }
                    return false;
                }
                return true;
            }
        };
    }

    private static StateMatch meta(final Block block, final int meta) {
        return new StateMatch() {
            @Override
            public boolean test(IBlockState state) {
                return block.getMetaFromState(state) == meta;
            }
        };
    }

    public static int rank(Rule rule) {
        if (rule.kind != Kind.BLOCK) return StationSink.CARRIED;
        return "minecraft".equals(rule.modId) ? StationSink.VANILLA : StationSink.BLOCK;
    }

    public static List<String> providerIds(Station station) {
        List<String> ids = new ArrayList<String>();
        for (Rule rule : RULES) {
            if (rule.stations.contains(station)) ids.addAll(rule.ids);
        }
        return Collections.unmodifiableList(ids);
    }

    public static Map<Block, List<BlockEntry>> blocks() {
        if (blockCache != null) return blockCache;
        Map<Block, List<BlockEntry>> map = new HashMap<Block, List<BlockEntry>>();
        for (Rule rule : RULES) {
            if (rule.kind != Kind.BLOCK || !Reg.loaded(rule.modId)) continue;
            for (String id : rule.ids) {
                Block block = Reg.block(Reg.baseId(id));
                if (block == null) continue;
                int meta = Reg.metaOf(id);
                StateMatch match = rule.state != ANY_STATE ? rule.state : (meta >= 0 ? meta(block, meta) : ANY_STATE);
                List<BlockEntry> entries = map.get(block);
                if (entries == null) {
                    entries = new ArrayList<BlockEntry>();
                    map.put(block, entries);
                }
                entries.add(new BlockEntry(rule, match, Reg.stack(id)));
            }
        }
        blockCache = map;
        return map;
    }

    public static List<ItemRule> items() {
        if (itemCache != null) return itemCache;
        List<ItemRule> list = new ArrayList<ItemRule>();
        for (Rule rule : RULES) {
            if (rule.kind == Kind.BLOCK || rule.kind == Kind.UPGRADE || !Reg.loaded(rule.modId)) continue;
            for (String id : rule.ids) {
                Item item = Reg.item(id);
                if (item != null) list.add(new ItemRule(item, Reg.metaOf(id), rule));
            }
        }
        itemCache = Collections.unmodifiableList(list);
        return itemCache;
    }
}
