package com.sxilverr.quickcraft.crafting;

import java.util.HashMap;
import java.util.Map;

public interface Availability {
    Availability NONE = key -> 0;

    int available(ItemKey key);

    static Availability of(Map<ItemKey, Integer> counts) {
        Map<ItemKey, Integer> byLoose = new HashMap<>();
        counts.forEach((key, count) -> byLoose.merge(key.loose(), count, Integer::sum));
        return key -> (key.isLoose() ? byLoose : counts).getOrDefault(key, 0);
    }

    static Availability exact(Map<ItemKey, Integer> counts) {
        return key -> counts.getOrDefault(key, 0);
    }
}
