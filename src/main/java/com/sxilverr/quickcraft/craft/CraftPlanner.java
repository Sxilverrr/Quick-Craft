package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.Availability;
import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.CraftTrees;
import com.sxilverr.quickcraft.crafting.EmcLookup;
import com.sxilverr.quickcraft.crafting.ForeignRecipe;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.crafting.TreeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

public final class CraftPlanner {
    public static final int MAX_QUANTITY = 1_000_000;
    private static final int INVENTORY_SLOTS = 36;

    private CraftPlanner() {
    }

    public enum Reason {
        NO_RECIPE(null),
        NOT_LEARNED("quickcraft.reason.not_learned"),
        NOT_ENOUGH_EMC("quickcraft.reason.not_enough_emc"),
        CATALYST("quickcraft.reason.catalyst"),
        STATION("quickcraft.reason.station"),
        TREE_LIMIT("quickcraft.reason.tree_limit"),
        LOOP("quickcraft.reason.loop"),
        MANUAL("quickcraft.reason.manual");

        private final String key;

        Reason(String key) {
            this.key = key;
        }

        public Component suffix() {
            return key == null ? Component.empty() : Component.literal(" ").append(Component.translatable(key));
        }
    }

    public record Blocker(ItemKey key, int missing, Reason reason) {
    }

    public record Plan(CraftNode root, VirtualPool initial, VirtualPool working, EmcBank bank, Set<ItemKey> keys,
                       int requested, int craftable, boolean truncated, List<Blocker> blockers,
                       Map<ItemKey, Integer> collected, boolean bought) {
        public boolean full() {
            return requested > 0 && craftable >= requested;
        }

        public Map<ItemKey, Integer> supplied() {
            return bank == null ? Map.of() : bank.purchased();
        }

        public BigInteger spentEmc() {
            return bank == null ? BigInteger.ZERO : bank.spentEmc();
        }
    }

    public static Plan plan(TreeBuilder builder, ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                            Map<String, Item> ingredientChoices, VirtualPool initial, Availability availability,
                            Stations stations, boolean collapseOwned, boolean hideLooping, EmcSource emc, BigInteger budget) {
        int qty = Math.max(1, quantity);
        EmcSource buyer = qty >= MAX_QUANTITY ? null : emc;
        builder.setEmcLookup(buyer == null ? EmcLookup.NONE : buyer);
        CraftNode root = builder.build(target, qty, overrides, ingredientChoices, availability, stations, collapseOwned, hideLooping);
        Set<ItemKey> keys = collectKeys(root, new HashSet<>());
        keys.addAll(builder.loopIngredientKeys());
        EmcBank bank = null;
        BigInteger start = emc == null ? null : budget == null ? emc.emc() : budget;
        if (emc != null) bank = emc.bank(buyer == null ? Set.of() : keys, start);
        VirtualPool working = initial.copy();
        working.setEmc(bank);
        CraftExecutor.simulate(root, working);
        ItemKey targetKey = ItemKey.of(target);
        buyTarget(bank, initial, working, targetKey, qty);
        int craftable = Math.max(0, Math.min(qty, working.count(targetKey) - initial.count(targetKey)));
        if (buyer != null && (craftable < qty || bank.spentEmc().signum() > 0)) {
            EmcBank direct = buyer.bank(keys, start);
            BigInteger cost = BigInteger.valueOf(direct.value(targetKey)).multiply(BigInteger.valueOf(qty));
            if ((craftable < qty || cost.compareTo(bank.spentEmc()) <= 0) && direct.buy(targetKey, qty)) {
                VirtualPool bought = initial.copy();
                bought.setEmc(direct);
                bought.produce(targetKey, qty);
                return new Plan(root, initial, bought, direct, keys, qty, qty, builder.truncated(), List.of(), Map.of(), true);
            }
        }
        List<Blocker> blockers = craftable >= qty ? List.of() : diagnose(root, craftable > 0 ? working : initial, bank, buyer,
                craftable > 0, qty, qty >= MAX_QUANTITY ? 1 : qty - craftable);
        return new Plan(root, initial, working, bank, keys, qty, craftable, builder.truncated(), blockers, Map.of(), false);
    }

    public static Plan collect(Plan plan, Map<ItemKey, ForeignRecipe> foreign) {
        if (plan.craftable() > 0 || plan.requested() >= MAX_QUANTITY) return plan;
        VirtualPool working = plan.initial().copy();
        working.setEmc(plan.bank());
        Map<ItemKey, Integer> collected = CraftExecutor.collect(plan.root(), working, foreign);
        if (collected.isEmpty()) return plan;
        return new Plan(plan.root(), plan.initial(), working, plan.bank(), plan.keys(), plan.requested(), 0,
                plan.truncated(), plan.blockers(), collected, false);
    }

    public static Plan fitInventory(Plan plan, List<ItemStack> inventory, IntFunction<Plan> replan) {
        for (int i = 0; i < 8; i++) {
            int over = overflow(plan, inventory);
            if (over <= 0 || plan.craftable() <= 1) return plan;
            plan = replan.apply(Math.max(1, plan.craftable() - over));
        }
        return plan;
    }

    private static int overflow(Plan plan, List<ItemStack> inventory) {
        List<ItemStack> slots = new ArrayList<>(inventory.size());
        for (ItemStack stack : inventory) slots.add(stack.copy());
        Set<ItemKey> keys = new HashSet<>(plan.initial().counts().keySet());
        keys.addAll(plan.working().counts().keySet());
        Map<ItemKey, Integer> produced = new HashMap<>();
        for (ItemKey key : keys) {
            int delta = plan.working().exact(key) - plan.initial().exact(key);
            if (delta < 0) take(slots, key, -delta);
            else if (delta > 0) produced.put(key, delta);
        }
        int over = 0;
        for (Map.Entry<ItemKey, Integer> entry : produced.entrySet()) over += put(slots, entry.getKey(), entry.getValue());
        return over;
    }

    private static void take(List<ItemStack> slots, ItemKey key, int amount) {
        for (ItemStack slot : slots) {
            if (amount <= 0) return;
            if (slot.isEmpty() || !ItemKey.of(slot).equals(key)) continue;
            int n = Math.min(amount, slot.getCount());
            slot.shrink(n);
            amount -= n;
        }
    }

    private static int put(List<ItemStack> slots, ItemKey key, int amount) {
        int max = Math.max(1, key.toStack(1).getMaxStackSize());
        for (ItemStack slot : slots) {
            if (amount <= 0) return 0;
            if (slot.isEmpty() || !ItemKey.of(slot).equals(key)) continue;
            int n = Math.min(amount, max - slot.getCount());
            if (n <= 0) continue;
            slot.grow(n);
            amount -= n;
        }
        for (int i = 0; i < slots.size() && amount > 0; i++) {
            if (!slots.get(i).isEmpty()) continue;
            int n = Math.min(amount, max);
            slots.set(i, key.toStack(n));
            amount -= n;
        }
        return amount;
    }

    public static int creativeQuantity(ItemStack target, int requested) {
        return Math.min(requested, Math.max(1, target.getMaxStackSize()) * INVENTORY_SLOTS);
    }

    private static Set<ItemKey> collectKeys(CraftNode node, Set<ItemKey> out) {
        out.add(ItemKey.of(node.output));
        for (CraftNode child : node.children) collectKeys(child, out);
        return out;
    }

    private static void buyTarget(EmcBank bank, VirtualPool initial, VirtualPool working, ItemKey targetKey, int qty) {
        if (bank == null || !bank.supplies(targetKey)) return;
        int made = Math.max(0, working.count(targetKey) - initial.count(targetKey));
        if (made >= qty) return;
        int buy = Math.min(qty - made, bank.affordable(targetKey));
        if (buy > 0 && bank.buy(targetKey, buy)) working.produce(targetKey, buy);
    }

    private static List<Blocker> diagnose(CraftNode root, VirtualPool have, EmcBank bank, EmcSource emc,
                                          boolean afterCrafting, int qty, int remaining) {
        Map<ItemKey, Integer> totals = CraftTrees.leafTotals(root);
        Map<ItemKey, CraftNode> samples = CraftTrees.leafSamples(root);
        List<Blocker> out = new ArrayList<>();
        List<Blocker> unbought = new ArrayList<>();
        BigInteger unboughtCost = BigInteger.ZERO;
        boolean stationMissing = CraftTrees.missingStation(root) != null;
        for (Map.Entry<ItemKey, Integer> entry : totals.entrySet()) {
            ItemKey key = entry.getKey();
            int bought = afterCrafting || bank == null ? 0 : bank.purchased().getOrDefault(key, 0);
            long need = ((long) entry.getValue() * remaining + qty - 1) / qty;
            int missing = (int) Math.min(Integer.MAX_VALUE, need - have.count(key) - bought);
            if (missing <= 0) continue;
            Reason reason = reasonFor(samples.get(key), key, emc, stationMissing);
            if (reason != Reason.NOT_ENOUGH_EMC) {
                out.add(new Blocker(key, missing, reason));
                continue;
            }
            long unit = bank == null ? 0L : bank.value(key);
            unboughtCost = unboughtCost.add(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(missing)));
            unbought.add(new Blocker(key, missing, reason));
        }
        if (bank == null || !bank.canAfford(unboughtCost)) out.addAll(unbought);
        return out;
    }

    private static Reason reasonFor(CraftNode node, ItemKey key, EmcSource emc, boolean stationMissing) {
        if (node != null) {
            if (node.truncated) return Reason.TREE_LIMIT;
            if (node.cyclic) return Reason.LOOP;
            if (node.isBlockedByStation()) return Reason.STATION;
            if (node.isCraftable() && node.selected() == null) return node.autoRecipe < 0 ? Reason.LOOP : Reason.MANUAL;
        }
        if (emc != null) {
            ItemStack stack = key.toStack(1);
            if (emc.value(stack) > 0L) {
                if (!emc.learned(stack)) return Reason.NOT_LEARNED;
                return stationMissing ? Reason.STATION : Reason.NOT_ENOUGH_EMC;
            }
        }
        if (node != null && node.catalyst) return Reason.CATALYST;
        return Reason.NO_RECIPE;
    }
}
