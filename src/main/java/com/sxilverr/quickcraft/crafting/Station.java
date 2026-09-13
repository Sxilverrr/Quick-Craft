package com.sxilverr.quickcraft.crafting;

public enum Station {
    CRAFTING("Crafting Table", "minecraft:crafting_table"),
    EXTREME_CRAFTING("Extreme Crafting Table", "avaritia:extreme_crafting_table"),
    STONECUTTER("Stonecutter", "futuremc:stonecutter"),
    SMITHING("Smithing Table", "futuremc:smithing_table"),
    EXTENDED_BASIC("Basic Crafting Table", "extendedcrafting:table_basic"),
    EXTENDED_ADVANCED("Advanced Crafting Table", "extendedcrafting:table_advanced"),
    EXTENDED_ELITE("Elite Crafting Table", "extendedcrafting:table_elite"),
    EXTENDED_ULTIMATE("Ultimate Crafting Table", "extendedcrafting:table_ultimate");

    private final String displayName;
    private final String iconId;

    Station(String displayName, String iconId) {
        this.displayName = displayName;
        this.iconId = iconId;
    }

    public String displayName() {
        return displayName;
    }

    public String iconId() {
        return iconId;
    }
}
