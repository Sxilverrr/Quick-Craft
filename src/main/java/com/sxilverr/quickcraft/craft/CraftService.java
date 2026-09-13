package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.Availability;
import com.sxilverr.quickcraft.crafting.CraftTrees;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.RecipeResolver;
import com.sxilverr.quickcraft.crafting.ServerRecipeCache;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.StationScan;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.crafting.TreeBuilder;
import com.sxilverr.quickcraft.integration.projecte.EmcDeposit;
import com.sxilverr.quickcraft.integration.projecte.EmcSession;
import com.sxilverr.quickcraft.integration.projecte.ProjectESupport;
import com.sxilverr.quickcraft.storage.CompositeItemSource;
import com.sxilverr.quickcraft.storage.DamageMatch;
import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.storage.ItemSourceFactory;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final class Shortfall {
        private final ItemKey key;
        private final int wanted;
        private final int got;

        private Shortfall(ItemKey key, int wanted, int got) {
            this.key = key;
            this.wanted = wanted;
            this.got = got;
        }
    }

    private CraftService() {
    }

    public static CraftSummary execute(EntityPlayerMP player, ItemStack target, int quantity,
                                       Map<ItemKey, ResourceLocation> overrides, Map<String, Item> ingredientChoices,
                                       String destinationId) {
        if (target.isEmpty()) return CraftSummary.empty();
        int qty = Math.max(1, Math.min(MAX_QUANTITY, quantity));

        List<LabeledSource> labeled = ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange());
        Deposit deposit = Deposit.to(labeled, destinationId, player);
        if (EmcDeposit.isEmc(destinationId)) {
            deposit.setEmcLabel(EmcDeposit.label(destinationId, EmcDeposit.targets(player)));
        }

        if (QuickCraftConfig.creativeBypass() && player.capabilities.isCreativeMode) {
            int given = creativeQuantity(target, qty);
            deposit.put(ItemKey.of(target), given, true);
            playCraftSound(player);
            return new CraftSummary(given, given, null, deposit.placements(), deposit.dropped(), deposit.byproducts());
        }

        TreeBuilder builder = newBuilder();
        CompositeItemSource source = extractionSource(labeled);
        VirtualPool initial = poolFrom(source.snapshot());
        Stations stations = StationScan.detect(player.world, player);
        EmcSession emc = openEmcSession(player);
        ItemKey targetKey = ItemKey.of(target);

        Shortfall shortfall = null;
        for (int attempt = 0; attempt < MAX_COMMIT_ATTEMPTS; attempt++) {
            CraftPlanner.Plan plan = CraftPlanner.plan(builder, target, qty, overrides, ingredientChoices, initial,
                    Availability.Factory.of(new HashMap<ItemKey, Integer>(initial.counts())), stations,
                    QuickCraftConfig.collapseOwnedItems(), QuickCraftConfig.hideLoopingRecipes(), emc, null);

            shortfall = commit(source, deposit, initial, plan.working(), targetKey, emc, plan.bank(), player, destinationId);
            if (shortfall != null) {
                initial.limit(shortfall.key, shortfall.got);
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
        return CraftSummary.aborted(qty, shortfall.key.toStack(1), shortfall.wanted);
    }

    public static CraftPreview.Result preview(EntityPlayerMP player, ItemStack target, int quantity,
                                              Map<ItemKey, ResourceLocation> overrides,
                                              Map<String, Item> ingredientChoices) {
        int qty = Math.max(1, Math.min(MAX_QUANTITY, quantity));
        if (target.isEmpty()) {
            return new CraftPreview.Result(0, qty, Collections.<CraftPreview.Gain>emptyList());
        }

        if (QuickCraftConfig.creativeBypass() && player.capabilities.isCreativeMode) {
            int given = creativeQuantity(target, qty);
            List<CraftPreview.Gain> gains = new ArrayList<CraftPreview.Gain>();
            gains.add(new CraftPreview.Gain(ItemKey.of(target), given));
            return new CraftPreview.Result(given, given, gains);
        }

        List<LabeledSource> labeled = ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange());
        VirtualPool initial = poolFrom(extractionSource(labeled).snapshot());
        CraftPlanner.Plan plan = CraftPlanner.plan(newBuilder(), target, qty, overrides, ingredientChoices, initial,
                Availability.Factory.of(new HashMap<ItemKey, Integer>(initial.counts())),
                StationScan.detect(player.world, player), QuickCraftConfig.collapseOwnedItems(),
                QuickCraftConfig.hideLoopingRecipes(), openEmcSession(player), null);
        return plan.toResult();
    }

    private static TreeBuilder newBuilder() {
        RecipeResolver resolver = ServerRecipeCache.get();
        return new TreeBuilder(resolver, QuickCraftConfig.preferredItems(),
                QuickCraftConfig.maxTreeDepth(), QuickCraftConfig.maxTreeNodes());
    }

    private static int creativeQuantity(ItemStack target, int requested) {
        return Math.min(requested, Math.max(1, target.getMaxStackSize()) * INVENTORY_SLOTS);
    }

    private static EmcSession openEmcSession(EntityPlayerMP player) {
        if (!QuickCraftConfig.useProjectEEmc() || !ProjectESupport.available()) return null;
        return EmcSession.open(player, QuickCraftConfig.containerScanRange());
    }

    public static final class AvailabilitySnapshot {
        private final Map<ItemKey, Integer> counts;
        private final Map<ItemKey, ItemStack> sources;
        private final Map<ItemKey, ItemStack> samples;

        public AvailabilitySnapshot(Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources,
                                    Map<ItemKey, ItemStack> samples) {
            this.counts = counts;
            this.sources = sources;
            this.samples = samples;
        }

        public Map<ItemKey, Integer> counts() {
            return counts;
        }

        public Map<ItemKey, ItemStack> sources() {
            return sources;
        }

        public Map<ItemKey, ItemStack> samples() {
            return samples;
        }
    }

    public static AvailabilitySnapshot availability(EntityPlayerMP player, Set<ItemKey> keys) {
        Map<ItemKey, Integer> counts = new HashMap<ItemKey, Integer>();
        Map<ItemKey, ItemStack> sources = new HashMap<ItemKey, ItemStack>();
        Map<ItemKey, ItemStack> samples = new HashMap<ItemKey, ItemStack>();
        if (keys.isEmpty()) return new AvailabilitySnapshot(counts, sources, samples);

        Map<ItemKey, Integer> exact = new HashMap<ItemKey, Integer>();
        Map<ItemKey, Integer> loose = new HashMap<ItemKey, Integer>();
        Map<ItemKey, ItemStack> exactIcons = new HashMap<ItemKey, ItemStack>();
        Map<ItemKey, ItemStack> looseIcons = new HashMap<ItemKey, ItemStack>();
        List<ItemStack> all = new ArrayList<ItemStack>();
        for (LabeledSource labeled : ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange())) {
            ItemStack icon = labeled.source().sourceIcon();
            for (ItemStack stack : labeled.source().snapshot()) {
                if (stack == null || stack.isEmpty()) continue;
                all.add(stack);
                ItemKey key = ItemKey.of(stack);
                merge(exact, key, stack.getCount());
                merge(loose, key.loose(), stack.getCount());
                if (icon == null || icon.isEmpty()) continue;
                if (!exactIcons.containsKey(key)) exactIcons.put(key, icon);
                if (!looseIcons.containsKey(key.loose())) looseIcons.put(key.loose(), icon);
            }
        }
        for (ItemKey key : keys) {
            Integer found = key.isLoose() ? loose.get(key) : exact.get(key);
            int available = found == null ? 0 : found;
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

    private static void merge(Map<ItemKey, Integer> map, ItemKey key, int amount) {
        Integer existing = map.get(key);
        map.put(key, existing == null ? amount : existing + amount);
    }

    public static List<LabeledSource> depositTargets(EntityPlayerMP player) {
        List<LabeledSource> out = new ArrayList<LabeledSource>();
        for (LabeledSource labeled : ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange())) {
            if (labeled.depositable()) out.add(labeled);
        }
        out.addAll(EmcDeposit.targets(player));
        return out;
    }

    private static CompositeItemSource extractionSource(List<LabeledSource> labeled) {
        List<ItemSource> sources = new ArrayList<ItemSource>(labeled.size());
        for (LabeledSource l : labeled) sources.add(l.source());
        return new CompositeItemSource(sources);
    }

    private static VirtualPool poolFrom(List<ItemStack> snapshot) {
        VirtualPool pool = new VirtualPool(true);
        for (ItemStack stack : snapshot) pool.addStack(stack);
        return pool;
    }

    private static Shortfall commit(CompositeItemSource source, Deposit deposit, VirtualPool initial, VirtualPool working,
                                    ItemKey targetKey, EmcSession emc, EmcBank bank, EntityPlayerMP player,
                                    String destinationId) {
        boolean depositToEmc = emc != null && bank != null && EmcDeposit.isEmc(destinationId);

        Map<ItemKey, Integer> consumed = new LinkedHashMap<ItemKey, Integer>();
        Map<ItemKey, Integer> produced = new LinkedHashMap<ItemKey, Integer>();
        Set<ItemKey> keys = new HashSet<ItemKey>(initial.counts().keySet());
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

        Map<ItemSource, List<ItemStack>> taken = new LinkedHashMap<ItemSource, List<ItemStack>>();
        for (Map.Entry<ItemKey, Integer> entry : consumed.entrySet()) {
            ItemStack rep = entry.getKey().toStack(1);
            int remaining = entry.getValue();
            for (ItemSource part : source.sources()) {
                if (remaining <= 0) break;
                List<ItemStack> pulled = part.pull(rep, remaining);
                if (pulled.isEmpty()) continue;
                List<ItemStack> bucket = taken.get(part);
                if (bucket == null) {
                    bucket = new ArrayList<ItemStack>();
                    taken.put(part, bucket);
                }
                bucket.addAll(pulled);
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

    private static void refund(ItemSource fallback, Map<ItemSource, List<ItemStack>> taken, EntityPlayerMP player) {
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
                    if (!remainder.isEmpty()) player.dropItem(remainder, false);
                    left -= n;
                }
            }
        }
    }

    private static void playCraftSound(EntityPlayerMP player) {
        if (!QuickCraftConfig.craftSoundEnabled()) return;
        ResourceLocation id = QuickCraftConfig.craftSound();
        if (id == null) return;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (sound == null) return;
        player.world.playSound(null, player.posX, player.posY, player.posZ, sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }
}
