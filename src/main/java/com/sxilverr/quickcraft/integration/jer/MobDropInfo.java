package com.sxilverr.quickcraft.integration.jer;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record MobDropInfo(ResourceLocation entityId, String mobName, List<String> biomes,
                          String lightLevel, String exp, List<DropLine> drops) {
}
