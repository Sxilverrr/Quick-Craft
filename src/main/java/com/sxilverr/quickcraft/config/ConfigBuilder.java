package com.sxilverr.quickcraft.config;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface ConfigBuilder {

    ConfigBuilder comment(String... lines);

    ConfigBuilder push(String name);

    ConfigBuilder pop();

    Supplier<Boolean> define(String name, boolean def);

    Supplier<String> define(String name, String def);

    Supplier<String> define(String name, String def, Predicate<Object> validator);

    Supplier<Integer> defineInRange(String name, int def, int min, int max);

    Supplier<Double> defineInRange(String name, double def, double min, double max);

    Supplier<List<? extends String>> defineList(String name, List<String> def, Predicate<Object> validator);
}
