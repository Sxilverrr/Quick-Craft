package com.sxilverr.quickcraft.crafting;

import java.util.HashMap;
import java.util.Map;

public interface Availability {
    Availability NONE = new Availability() {
        @Override
        public int available(ItemKey key) {
            return 0;
        }
    };

    int available(ItemKey key);

    final class Factory {
        private Factory() {
        }

        public static Availability of(final Map<ItemKey, Integer> counts) {
            final Map<ItemKey, Integer> byLoose = new HashMap<ItemKey, Integer>();
            for (Map.Entry<ItemKey, Integer> entry : counts.entrySet()) {
                ItemKey loose = entry.getKey().loose();
                byLoose.put(loose, byLoose.getOrDefault(loose, 0) + entry.getValue());
            }
            return new Availability() {
                @Override
                public int available(ItemKey key) {
                    return (key.isLoose() ? byLoose : counts).getOrDefault(key, 0);
                }
            };
        }

        public static Availability exact(final Map<ItemKey, Integer> counts) {
            return new Availability() {
                @Override
                public int available(ItemKey key) {
                    return counts.getOrDefault(key, 0);
                }
            };
        }
    }
}
