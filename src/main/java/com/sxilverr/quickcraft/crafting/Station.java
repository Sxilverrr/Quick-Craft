package com.sxilverr.quickcraft.crafting;

public enum Station {
    CRAFTING("Crafting Table", "minecraft:crafting_table"),
    SMITHING("Smithing Table", "minecraft:smithing_table"),
    STONECUTTER("Stonecutter", "minecraft:stonecutter"),
    GUN_SMITH_TABLE("Gun Smith Table", "tacz:gun_smith_table"),
    AMMO_ASSEMBLY_TABLE("Ammo Assembly Table", "tacz:workbench_a"),
    ATTACHMENT_TABLE("Attachment Table", "tacz:workbench_c"),
    EXTREME_CRAFTING("Extreme Crafting Table", "avaritia:extreme_crafting_table"),
    EXTENDED_BASIC("Basic Crafting Table", "extendedcrafting:basic_table"),
    EXTENDED_ADVANCED("Advanced Crafting Table", "extendedcrafting:advanced_table"),
    EXTENDED_ELITE("Elite Crafting Table", "extendedcrafting:elite_table"),
    EXTENDED_ULTIMATE("Ultimate Crafting Table", "extendedcrafting:ultimate_table");

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
