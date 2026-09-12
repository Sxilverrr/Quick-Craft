package com.sxilverr.quickcraft.forge.craft;

import com.sxilverr.quickcraft.craft.CraftPlanner;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.craft.CraftSummary;
import com.sxilverr.quickcraft.craft.Deposit;
import com.sxilverr.quickcraft.craft.EmcBank;
import com.sxilverr.quickcraft.craft.VirtualPool;
import com.sxilverr.quickcraft.forge.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.Availability;
import com.sxilverr.quickcraft.crafting.CraftTrees;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.RecipeResolver;
import com.sxilverr.quickcraft.crafting.ServerRecipeCache;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.StationScan;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.crafting.TreeBuilder;
import com.sxilverr.quickcraft.forge.integration.projecte.EmcDeposit;
import com.sxilverr.quickcraft.forge.integration.projecte.EmcSession;
import com.sxilverr.quickcraft.forge.integration.projecte.ProjectEIntegration;
import com.sxilverr.quickcraft.storage.CompositeItemSource;
import com.sxilverr.quickcraft.storage.DamageMatch;
import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.forge.storage.ItemSourceFactory;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CraftService {
    private static final int MAX_QUANTITY = 1000000;
    private static final int INVENTORY_SLOTS = 36;
    private static final int MAX_COMMIT_ATTEMPTS = 8;

    private record Shortfall(ItemKey key, int wanted, int got) {
    }

    private CraftService() {
    }

    public static CraftSummary execute(ServerPlayer player, ItemStack target, int quantity,
                                       Map<ItemKey, ResourceLocation> overrides, Map<String, Item> ingredientChoices,
                                       String destinationId) {
        if (target.isEmpty()) return CraftSummary.empty();
        int qty = Math.max(1, Math.min(MAX_QUANTITY, quantity));

        List<LabeledSource> labeled = ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange());
        Deposit deposit = Deposit.to(labeled, destinationId, player);
        if (EmcDeposit.isEmc(destinationId)) {
            deposit.setEmcLabel(EmcDeposit.label(destinationId, EmcDeposit.targets(player)));
        }

        if (QuickCraftConfig.creativeBypass() && player.getAbilities().instabuild) {
            int given = creativeQuantity(target, qty);
            deposit.put(ItemKey.of(target), given, true);
            playCraftSound(player);
            return new CraftSummary(given, given, null, deposit.placements(), deposit.dropped(), deposit.byproducts());
        }

        ServerLevel level = player.serverLevel();
        TreeBuilder builder = newBuilder(level);
        CompositeItemSource source = extractionSource(labeled);
        VirtualPool initial = poolFrom(source.snapshot());
        Stations stations = StationScan.detect(level, player);
        EmcSession emc = openEmcSession(player);
        ItemKey targetKey = ItemKey.of(target);

        Shortfall shortfall = null;
        for (int attempt = 0; attempt < MAX_COMMIT_ATTEMPTS; attempt++) {
            CraftPlanner.Plan plan = CraftPlanner.plan(builder, target, qty, overrides, ingredientChoices, initial,
                    Availability.of(new HashMap<>(initial.counts())), stations, QuickCraftConfig.collapseOwnedItems(),
                    QuickCraftConfig.hideLoopingRecipes(), emc, null);

            shortfall = commit(source, deposit, initial, plan.working(), targetKey, emc, plan.bank(), player, destinationId);
            if (shortfall != null) {
                initial.limit(shortfall.key(), shortfall.got());
                continue;
            }
            if (emc != null) emc.apply(plan.bank(), plan.working().producedKeys());

            int crafted = plan.craftable();
            if (crafted > 0) playCraftSound(player);
            Station missing = CraftTrees.missingStation(plan.root());
            return new CraftSummary(crafted, qty, missing == null ? null : missing.displayName(),
                    deposit.placements(), deposit.dropped(), deposit.byproducts(), ItemStack.EMPTY, 0,
                    plan.truncated(), plan.blockers());
        }
        return CraftSummary.aborted(qty, shortfall.key().toStack(1), shortfall.wanted());
    }

    public static CraftPreview.Result preview(ServerPlayer player, ItemStack target, int quantity,
                                              Map<ItemKey, ResourceLocation> overrides, Map<String, Item> ingredientChoices) {
        int qty = Math.max(1, Math.min(MAX_QUANTITY, quantity));
        if (target.isEmpty()) return new CraftPreview.Result(0, qty, List.of());

        if (QuickCraftConfig.creativeBypass() && player.getAbilities().instabuild) {
            int given = creativeQuantity(target, qty);
            return new CraftPreview.Result(given, given, List.of(new CraftPreview.Gain(ItemKey.of(target), given)));
        }

        List<LabeledSource> labeled = ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange());
        ServerLevel level = player.serverLevel();
        VirtualPool initial = poolFrom(extractionSource(labeled).snapshot());
        CraftPlanner.Plan plan = CraftPlanner.plan(newBuilder(level), target, qty, overrides, ingredientChoices, initial,
                Availability.of(new HashMap<>(initial.counts())), StationScan.detect(level, player),
                QuickCraftConfig.collapseOwnedItems(), QuickCraftConfig.hideLoopingRecipes(), openEmcSession(player), null);
        return plan.toResult();
    }

    private static TreeBuilder newBuilder(ServerLevel level) {
        RecipeResolver resolver = ServerRecipeCache.get(level.getRecipeManager(), level.registryAccess());
        return new TreeBuilder(resolver, QuickCraftConfig.preferredItems(),
                QuickCraftConfig.maxTreeDepth(), QuickCraftConfig.maxTreeNodes());
    }

    private static int creativeQuantity(ItemStack target, int requested) {
        return Math.min(requested, Math.max(1, target.getMaxStackSize()) * INVENTORY_SLOTS);
    }

    private static EmcSession openEmcSession(ServerPlayer player) {
        if (!QuickCraftConfig.useProjectEEmc() || !ProjectEIntegration.available()) return null;
        return EmcSession.open(player, QuickCraftConfig.containerScanRange());
    }

    public record AvailabilitySnapshot(Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources,
                                      Map<ItemKey, ItemStack> samples) {
    }

    public static AvailabilitySnapshot availability(ServerPlayer player, Set<ItemKey> keys) {
        Map<ItemKey, Integer> counts = new HashMap<>();
        Map<ItemKey, ItemStack> sources = new HashMap<>();
        Map<ItemKey, ItemStack> samples = new HashMap<>();
        if (keys.isEmpty()) return new AvailabilitySnapshot(counts, sources, samples);
        Map<ItemKey, Integer> exact = new HashMap<>();
        Map<ItemKey, Integer> loose = new HashMap<>();
        Map<ItemKey, ItemStack> exactIcons = new HashMap<>();
        Map<ItemKey, ItemStack> looseIcons = new HashMap<>();
        List<ItemStack> all = new ArrayList<>();
        for (LabeledSource labeled : ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange())) {
            ItemStack icon = labeled.source().sourceIcon();
            for (ItemStack stack : labeled.source().snapshot()) {
                if (stack.isEmpty()) continue;
                all.add(stack);
                ItemKey key = ItemKey.of(stack);
                exact.merge(key, stack.getCount(), Integer::sum);
                loose.merge(key.loose(), stack.getCount(), Integer::sum);
                if (icon.isEmpty()) continue;
                exactIcons.putIfAbsent(key, icon);
                looseIcons.putIfAbsent(key.loose(), icon);
            }
        }
        for (ItemKey key : keys) {
            int available = key.isLoose() ? loose.getOrDefault(key, 0) : exact.getOrDefault(key, 0);
            counts.put(key, available);
            if (available <= 0) continue;
            ItemStack icon = key.isLoose() ? looseIcons.get(key) : exactIcons.get(key);
            if (icon != null && !icon.isEmpty()) sources.put(key, icon);
            ItemStack rep = key.toStack(1);
            if (!DamageMatch.tolerant(rep)) continue;
            ItemStack sample = DamageMatch.worst(all, rep);
            if (!sample.isEmpty()) samples.put(key, sample);
        }
        return new AvailabilitySnapshot(counts, sources, samples);
    }

    public static List<LabeledSource> depositTargets(ServerPlayer player) {
        List<LabeledSource> out = new ArrayList<>();
        for (LabeledSource labeled : ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange())) {
            if (labeled.depositable()) out.add(labeled);
        }
        out.addAll(EmcDeposit.targets(player));
        return out;
    }

    private static CompositeItemSource extractionSource(List<LabeledSource> labeled) {
        List<ItemSource> sources = new ArrayList<>(labeled.size());
        for (LabeledSource l : labeled) sources.add(l.source());
        return new CompositeItemSource(sources);
    }

    private static VirtualPool poolFrom(List<ItemStack> snapshot) {
        VirtualPool pool = new VirtualPool(true);
        for (ItemStack stack : snapshot) pool.addStack(stack);
        return pool;
    }

    private static Shortfall commit(CompositeItemSource source, Deposit deposit, VirtualPool initial, VirtualPool working,
                                    ItemKey targetKey, EmcSession emc, EmcBank bank, ServerPlayer player, String destinationId) {
        boolean depositToEmc = emc != null && bank != null && EmcDeposit.isEmc(destinationId);

        Map<ItemKey, Integer> consumed = new LinkedHashMap<>();
        Map<ItemKey, Integer> produced = new LinkedHashMap<>();
        Set<ItemKey> keys = new HashSet<>(initial.counts().keySet());
        keys.addAll(working.counts().keySet());
        for (ItemKey key : keys) {
            int delta = working.exact(key) - initial.exact(key);
            if (delta < 0) consumed.put(key, -delta);
            else if (delta > 0) produced.put(key, delta);
        }

        for (Map.Entry<ItemKey, Integer> entry : consumed.entrySet()) {
            int can = source.extractMatching(entry.getKey().toStack(1), entry.getValue(), true);
            if (can < entry.getValue()) return new Shortfall(entry.getKey(), entry.getValue(), can);
        }

        Map<ItemSource, List<ItemStack>> taken = new LinkedHashMap<>();
        for (Map.Entry<ItemKey, Integer> entry : consumed.entrySet()) {
            ItemStack rep = entry.getKey().toStack(1);
            int remaining = entry.getValue();
            for (ItemSource part : source.sources()) {
                if (remaining <= 0) break;
                List<ItemStack> pulled = part.pull(rep, remaining);
                if (pulled.isEmpty()) continue;
                taken.computeIfAbsent(part, p -> new ArrayList<>()).addAll(pulled);
                for (ItemStack stack : pulled) remaining -= stack.getCount();
            }
            if (remaining > 0) {
                refund(source, taken, player);
                return new Shortfall(entry.getKey(), entry.getValue(), Math.max(0, entry.getValue() - remaining));
            }
        }

        for (Map.Entry<ItemKey, Integer> entry : produced.entrySet()) {
            ItemKey key = entry.getKey();
            int amount = entry.getValue();
            if (depositToEmc) {
                long value = emc.sellValue(key.toStack(1));
                if (value > 0L) {
                    bank.gain(BigInteger.valueOf(value).multiply(BigInteger.valueOf(amount)));
                    deposit.toEmc(amount, key.equals(targetKey));
                    continue;
                }
            }
            deposit.put(key, amount, key.equals(targetKey));
        }
        return null;
    }

    private static void refund(ItemSource fallback, Map<ItemSource, List<ItemStack>> taken, ServerPlayer player) {
        for (Map.Entry<ItemSource, List<ItemStack>> entry : taken.entrySet()) {
            for (ItemStack stack : entry.getValue()) {
                int max = Math.max(1, stack.getMaxStackSize());
                int left = stack.getCount();
                while (left > 0) {
                    int n = Math.min(left, max);
                    ItemStack chunk = stack.copy();
                    chunk.setCount(n);
                    ItemStack remainder = entry.getKey().insert(chunk, false);
                    if (!remainder.isEmpty()) remainder = fallback.insert(remainder, false);
                    if (!remainder.isEmpty()) player.drop(remainder, false);
                    left -= n;
                }
            }
        }
    }

    private static void playCraftSound(ServerPlayer player) {
        if (!QuickCraftConfig.craftSoundEnabled()) return;
        ResourceLocation id = QuickCraftConfig.craftSound();
        if (id == null) return;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (sound == null) return;
        player.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
