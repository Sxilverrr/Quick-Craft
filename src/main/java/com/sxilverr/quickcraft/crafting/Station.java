package com.sxilverr.quickcraft.crafting;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public enum Station {
    CRAFTING("tile.workbench.name", "minecraft:crafting_table"),
    EXTREME_CRAFTING("quickcraft.station.extreme_crafting_table", "avaritia:extreme_crafting_table"),
    STONECUTTER("quickcraft.station.stonecutter", "futuremc:stonecutter"),
    SMITHING("quickcraft.station.smithing_table", "futuremc:smithing_table"),
    EXTENDED_BASIC("quickcraft.station.basic_crafting_table", "extendedcrafting:table_basic"),
    EXTENDED_ADVANCED("quickcraft.station.advanced_crafting_table", "extendedcrafting:table_advanced"),
    EXTENDED_ELITE("quickcraft.station.elite_crafting_table", "extendedcrafting:table_elite"),
    EXTENDED_ULTIMATE("quickcraft.station.ultimate_crafting_table", "extendedcrafting:table_ultimate");

    private final String translationKey;
    private final String iconId;

    Station(String translationKey, String iconId) {
        this.translationKey = translationKey;
        this.iconId = iconId;
    }

    public ITextComponent displayName() {
        return new TextComponentTranslation(translationKey);
    }

    public String iconId() {
        return iconId;
    }
}
