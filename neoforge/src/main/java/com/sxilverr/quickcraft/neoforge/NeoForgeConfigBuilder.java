package com.sxilverr.quickcraft.neoforge;

import com.sxilverr.quickcraft.config.ConfigBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class NeoForgeConfigBuilder implements ConfigBuilder {
    private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

    public ModConfigSpec build() {
        return builder.build();
    }

    @Override
    public ConfigBuilder comment(String... lines) {
        builder.comment(lines);
        return this;
    }

    @Override
    public ConfigBuilder push(String name) {
        builder.push(name);
        return this;
    }

    @Override
    public ConfigBuilder pop() {
        builder.pop();
        return this;
    }

    @Override
    public Supplier<Boolean> define(String name, boolean def) {
        return builder.define(name, def);
    }

    @Override
    public Supplier<String> define(String name, String def) {
        return builder.define(name, def);
    }

    @Override
    public Supplier<String> define(String name, String def, Predicate<Object> validator) {
        return builder.define(name, def, validator);
    }

    @Override
    public Supplier<Integer> defineInRange(String name, int def, int min, int max) {
        return builder.defineInRange(name, def, min, max);
    }

    @Override
    public Supplier<Double> defineInRange(String name, double def, double min, double max) {
        return builder.defineInRange(name, def, min, max);
    }

    @Override
    public Supplier<List<? extends String>> defineList(String name, List<String> def, Predicate<Object> validator) {
        return builder.defineListAllowEmpty(name, def, validator);
    }
}
