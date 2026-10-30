package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.network.DepositTargetsResponsePacket;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientDepositTargets {
    public static final class Target {
        private final String id;
        private final ITextComponent label;
        private final ItemStack icon;
        private final int freeSlots;
        private final int totalSlots;

        public Target(String id, ITextComponent label, ItemStack icon, int freeSlots, int totalSlots) {
            this.id = id;
            this.label = label;
            this.icon = icon;
            this.freeSlots = freeSlots;
            this.totalSlots = totalSlots;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label.getUnformattedText();
        }

        public ItemStack icon() {
            return icon;
        }

        public int freeSlots() {
            return freeSlots;
        }

        public int totalSlots() {
            return totalSlots;
        }
    }

    private static final Target DEFAULT =
            new Target("self", new TextComponentTranslation("quickcraft.source.my_inventory"),
                    new ItemStack(Items.SKULL, 1, 3), -1, -1);

    private static List<Target> targets = Collections.singletonList(DEFAULT);
    private static String selectedId = "self";

    private ClientDepositTargets() {
    }

    public static void accept(List<DepositTargetsResponsePacket.Entry> entries) {
        List<Target> list = new ArrayList<Target>(entries.size());
        for (DepositTargetsResponsePacket.Entry entry : entries) {
            list.add(new Target(entry.id(), entry.label(), entry.icon(), entry.freeSlots(), entry.totalSlots()));
        }
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
