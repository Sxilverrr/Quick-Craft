package com.sxilverr.quickcraft.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class ClientDepositTargets {
    public record Target(String id, String label, ItemStack icon, int freeSlots, int totalSlots) {
    }

    private static final Target DEFAULT = new Target("self", "My Inventory", new ItemStack(Items.PLAYER_HEAD), -1, -1);

    private static List<Target> targets = List.of(DEFAULT);
    private static String selectedId = "self";

    private ClientDepositTargets() {
    }

    public static void accept(List<Target> entries) {
        List<Target> list = new ArrayList<>(entries);
        if (list.isEmpty()) list.add(DEFAULT);
        targets = list;
        if (find(selectedId) == null) selectedId = "self";
    }

    public static List<Target> targets() {
        return targets;
    }

    public static String selectedId() {
        return selectedId;
    }

    public static void select(String id) {
        if (find(id) != null) selectedId = id;
    }

    public static Target selected() {
        Target target = find(selectedId);
        return target != null ? target : DEFAULT;
    }

    private static Target find(String id) {
        for (Target target : targets) {
            if (target.id().equals(id)) return target;
        }
        return null;
    }
}
