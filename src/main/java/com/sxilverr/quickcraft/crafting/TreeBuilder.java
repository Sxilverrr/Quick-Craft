package com.sxilverr.quickcraft.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TreeBuilder {
    //? if >=1.20.5 {
    /*public static final ResourceLocation MANUAL = ResourceLocation.fromNamespaceAndPath("quickcraft", "manual");
    *///?} else {
    public static final ResourceLocation MANUAL = new ResourceLocation("quickcraft", "manual");
    //?}

    private final RecipeResolver resolver;
    private final List<Item> preferred;
    private final int maxDepth;
    private final int maxNodes;
    private static final int REACH_DEPTH = 6;

    private Map<ItemKey, ResourceLocation> recipeOverrides = Map.of();
    private Map<String, Item> ingredientChoices = Map.of();
    private Availability availability = Availability.NONE;
    private Stations stations = Stations.of(Station.CRAFTING, Station.SMITHING);
    private boolean collapseOwned = true;
    private boolean hideLooping = false;
    private final Map<ItemKey, Integer> claimedStock = new HashMap<>();
    private final Set<ItemKey> loopIngredients = new HashSet<>();
    private final Map<ItemKey, CraftNode> expanded = new HashMap<>();
    private final Map<ItemKey, Map<ItemKey, Reach>> reachMemo = new HashMap<>();
    private int nodeCount;
    private boolean truncated;
    private EmcLookup emcLookup = EmcLookup.NONE;

    public void setEmcLookup(EmcLookup lookup) {
        this.emcLookup = lookup == null ? EmcLookup.NONE : lookup;
    }

    public TreeBuilder(RecipeResolver resolver, List<Item> preferred, int maxDepth, int maxNodes) {
        this.resolver = resolver;
        this.preferred = preferred;
        this.maxDepth = maxDepth;
        this.maxNodes = maxNodes;
    }

    public CraftNode build(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                           Map<String, Item> ingredientChoices,
                           Availability availability, Stations stations, boolean collapseOwned, boolean hideLooping) {
        this.recipeOverrides = overrides == null ? Map.of() : overrides;
        this.ingredientChoices = ingredientChoices == null ? Map.of() : ingredientChoices;
        this.availability = availability == null ? Availability.NONE : availability;
        this.stations = stations;
        this.collapseOwned = collapseOwned;
        this.hideLooping = hideLooping;
        this.claimedStock.clear();
        this.loopIngredients.clear();
        this.expanded.clear();
        this.reachMemo.clear();
        this.nodeCount = 0;
        this.truncated = false;
        return buildNode(target, Math.max(1, quantity), 0, new HashSet<ItemKey>(), true, false);
    }

    private CraftNode buildNode(ItemStack output, int requiredCount, int depth, Set<ItemKey> path, boolean parentReachable,
                                boolean catalyst) {
        nodeCount++;
        List<RecipeOption> alternatives = visibleRecipes(output, resolver.recipesFor(output));
        CraftNode node = new CraftNode(output.copy(), requiredCount, alternatives, depth);
        node.catalyst = catalyst;

        ItemKey outputKey = ItemKey.of(output);
        int claimed = catalyst ? 0 : claimedStock.getOrDefault(outputKey, 0);
        int freeStock = availability.available(outputKey) - claimed;
        node.freeStock = Math.max(0, freeStock);

        boolean root = depth == 0;
        node.autoRecipe = alternatives.isEmpty() ? -1 : autoBestIndex(output, alternatives, requiredCount);
        node.selectedRecipe = resolveSelection(output, alternatives, node.autoRecipe);
        if (node.selectedRecipe < 0) {
            if (!catalyst) claimedStock.merge(outputKey, Math.min(node.freeStock, requiredCount), Integer::sum);
            if (!root && freeStock < requiredCount && emcLookup.obtainable(outputKey)) node.emcBuy = true;
            return node;
        }

        RecipeOption option = node.selected();
        if (!root && collapseOwned && freeStock >= requiredCount) {
            node.owned = true;
            if (!catalyst) claimedStock.merge(outputKey, requiredCount, Integer::sum);
            return node;
        }
        if (!root && collapseOwned && emcLookup.obtainable(outputKey) && !stockCraftable(option, requiredCount)) {
            node.emcBuy = true;
            return node;
        }

        node.fitsStation = option.fits(stations);
        node.craftReachable = parentReachable && node.fitsStation;

        CraftNode original = expanded.get(outputKey);
        if (original != null && original.selected() != null && original.selected().id().equals(option.id())) {
            node.reference = original;
            node.resultPerCraft = original.resultPerCraft;
            node.craftsNeeded = ceilDiv(requiredCount, node.resultPerCraft);
            return node;
        }

        if (depth >= maxDepth || nodeCount >= maxNodes) {
            node.truncated = true;
            truncated = true;
            return node;
        }
        if (path.contains(outputKey)) {
            node.cyclic = true;
            node.selectedRecipe = -1;
            return node;
        }

        int resultPer = Math.max(1, option.resultCount());
        node.resultPerCraft = resultPer;
        int crafts = ceilDiv(requiredCount, resultPer);
        node.craftsNeeded = crafts;

        Map<ItemKey, Integer> needs = new LinkedHashMap<>();
        Map<ItemKey, ItemStack[]> childOptions = new LinkedHashMap<>();
        List<ItemKey> catalysts = new ArrayList<>();
        for (Ingredient ingredient : option.inputs()) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] items = ingredient.getItems();
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            ItemKey key = ItemKey.of(choice);
            childOptions.putIfAbsent(key, items);
            if (isCatalystIngredient(items)) catalysts.add(key);
            else needs.merge(key, crafts, (a, b) -> clamp((long) a + b));
        }
        Set<ItemKey> kept = new HashSet<>();
        for (ItemKey key : catalysts) {
            if (needs.containsKey(key)) continue;
            needs.put(key, 1);
            kept.add(key);
        }

        path.add(outputKey);
        for (Map.Entry<ItemKey, Integer> entry : needs.entrySet()) {
            CraftNode child = buildNode(entry.getKey().toStack(1), entry.getValue(), depth + 1, path, node.craftReachable,
                    kept.contains(entry.getKey()));
            applyTagOptions(child, childOptions.get(entry.getKey()));
            node.children.add(child);
        }
        path.remove(outputKey);
        if (!node.children.isEmpty()) expanded.putIfAbsent(outputKey, node);
        return node;
    }

    private boolean stockCraftable(RecipeOption option, int requiredCount) {
        int crafts = ceilDiv(Math.max(1, requiredCount), Math.max(1, option.resultCount()));
        Map<ItemKey, Integer> needs = new HashMap<>();
        for (Ingredient ingredient : option.inputs()) {
            if (ingredient.isEmpty()) continue;
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            ItemKey key = ItemKey.of(choice);
            if (isCatalystIngredient(ingredient.getItems())) needs.putIfAbsent(key, 1);
            else needs.merge(key, crafts, (a, b) -> clamp((long) a + b));
        }
        if (needs.isEmpty()) return false;
        for (Map.Entry<ItemKey, Integer> entry : needs.entrySet()) {
            int free = availability.available(entry.getKey()) - claimedStock.getOrDefault(entry.getKey(), 0);
            if (free < entry.getValue()) return false;
        }
        return true;
    }

    private static void applyTagOptions(CraftNode child, ItemStack[] options) {
        if (options == null || options.length <= 1) return;
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : options) list.add(stack.copy());
        child.tagOptions = list;
        child.tagSignature = ingredientSignature(options);
    }

    public static String ingredientSignature(ItemStack[] items) {
        List<String> ids = new ArrayList<>();
        for (ItemStack stack : items) {
            ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
            ids.add(rl == null ? "?" : rl.toString());
        }
        Collections.sort(ids);
        return String.join(",", ids);
    }

    private int resolveSelection(ItemStack output, List<RecipeOption> alternatives, int auto) {
        if (alternatives.isEmpty()) return -1;

        ResourceLocation override = recipeOverrides.get(ItemKey.of(output));
        if (MANUAL.equals(override)) return -1;
        if (override != null) {
            for (int i = 0; i < alternatives.size(); i++) {
                if (alternatives.get(i).id().equals(override)) return i;
            }
        }
        return auto;
    }

    private int autoBestIndex(ItemStack output, List<RecipeOption> alternatives, int requiredCount) {
        int best = 0;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < alternatives.size(); i++) {
            int score = recipeScore(output, alternatives.get(i), requiredCount);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private int recipeScore(ItemStack output, RecipeOption option, int requiredCount) {
        boolean fits = option.fits(stations);
        boolean selfReferencing = referencesOutput(output, option);
        boolean missingCatalyst = missingCatalyst(option);
        int resultPer = Math.max(1, option.resultCount());
        int crafts = ceilDiv(Math.max(1, requiredCount), resultPer);

        Map<ItemKey, Integer> needs = new HashMap<>();
        int preferredHits = 0;
        for (Ingredient ingredient : option.inputs()) {
            if (ingredient.isEmpty()) continue;
            if (ingredientAcceptsPreferred(ingredient)) preferredHits++;
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            if (isCatalystIngredient(ingredient.getItems())) needs.putIfAbsent(ItemKey.of(choice), 1);
            else needs.merge(ItemKey.of(choice), crafts, (a, b) -> clamp((long) a + b));
        }

        int fullyAvailable = 0;
        int anyAvailable = 0;
        for (Map.Entry<ItemKey, Integer> entry : needs.entrySet()) {
            int have = availability.available(entry.getKey());
            if (have > 0) anyAvailable++;
            if (have >= entry.getValue()) fullyAvailable++;
        }

        return (fits ? 1_000_000 : 0)
                - (selfReferencing ? 500_000 : 0)
                - (missingCatalyst ? 200_000 : 0)
                + fullyAvailable * 1000
                + anyAvailable * 100
                + preferredHits * 10
                - needs.size();
    }

    private boolean referencesOutput(ItemStack output, RecipeOption option) {
        if (option == null) return false;
        ItemKey outputKey = ItemKey.of(output);
        for (Ingredient ingredient : option.inputs()) {
            if (ingredient.isEmpty()) continue;
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            if (loopsThrough(choice, outputKey)) return true;
        }
        return false;
    }

    private boolean ingredientAcceptsPreferred(Ingredient ingredient) {
        for (ItemStack stack : ingredient.getItems()) {
            if (preferred.contains(stack.getItem())) return true;
        }
        return false;
    }

    private List<RecipeOption> visibleRecipes(ItemStack output, List<RecipeOption> alternatives) {
        if (!hideLooping || alternatives.isEmpty()) return alternatives;
        List<RecipeOption> visible = new ArrayList<>();
        for (RecipeOption option : alternatives) {
            if (!hidesAsLoop(output, option)) visible.add(option);
        }
        return visible;
    }

    private boolean missingCatalyst(RecipeOption option) {
        for (Ingredient ingredient : option.inputs()) {
            if (ingredient == null || ingredient.isEmpty()) continue;
            ItemStack[] items = ingredient.getItems();
            if (!isCatalystIngredient(items)) continue;
            boolean owned = false;
            for (ItemStack item : items) {
                if (availability.available(ItemKey.of(item)) > 0) {
                    owned = true;
                    break;
                }
            }
            if (!owned) return true;
        }
        return false;
    }

    private static boolean isCatalystIngredient(ItemStack[] items) {
        if (items.length == 0) return false;
        for (ItemStack item : items) {
            if (!isCatalyst(item)) return false;
        }
        return true;
    }

    private static boolean isCatalyst(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ItemStack remainder = Services.PLATFORM.getCraftingRemainder(stack);
        return remainder != null && !remainder.isEmpty() && remainder.getItem() == stack.getItem();
    }

    private boolean hidesAsLoop(ItemStack output, RecipeOption option) {
        ItemKey outputKey = ItemKey.of(output);
        boolean hidden = false;
        for (Ingredient ingredient : option.inputs()) {
            if (ingredient.isEmpty()) continue;
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            ItemKey choiceKey = ItemKey.of(choice);
            boolean loops = loopsThrough(choice, outputKey);
            if (!loops) continue;
            loopIngredients.add(choiceKey);
            if (availability.available(choiceKey) <= 0 && !emcLookup.obtainable(choiceKey)) hidden = true;
        }
        return hidden;
    }

    public Set<ItemKey> loopIngredientKeys() {
        return loopIngredients;
    }

    public boolean truncated() {
        return truncated;
    }

    private boolean loopsThrough(ItemStack choice, ItemKey outputKey) {
        ItemKey choiceKey = ItemKey.of(choice);
        if (choiceKey.equals(outputKey)) return true;
        return reach(choiceKey, outputKey, new HashSet<>(), 0, false).dead;
    }

    private static final class Reach {
        static final Reach OK = new Reach(false, Set.of());
        static final Reach DEAD = new Reach(true, Set.of());

        final boolean dead;
        final Set<ItemKey> cycles;

        Reach(boolean dead, Set<ItemKey> cycles) {
            this.dead = dead;
            this.cycles = cycles;
        }

        boolean ok() {
            return !dead && cycles.isEmpty();
        }
    }

    private Reach reach(ItemKey item, ItemKey output, Set<ItemKey> visited, int depth, boolean checkStock) {
        if (item.equals(output)) return Reach.DEAD;
        if (checkStock) {
            loopIngredients.add(item);
            if (availability.available(item) > 0) return Reach.OK;
        }
        if (depth >= REACH_DEPTH) return Reach.OK;
        if (visited.contains(item)) return new Reach(false, Set.of(item));
        Map<ItemKey, Reach> memo = reachMemo.computeIfAbsent(output, k -> new HashMap<>());
        Reach cached = checkStock ? memo.get(item) : null;
        if (cached != null) return cached;
        List<RecipeOption> recipes = resolver.recipesFor(item.toStack(1));
        if (recipes.isEmpty()) return Reach.OK;
        visited.add(item);
        boolean anyDead = false;
        Set<ItemKey> cycles = new HashSet<>();
        Reach result = cookedFromRaw(item, output, visited, depth) ? Reach.OK : null;
        if (result == null) {
            for (RecipeOption recipe : recipes) {
                Reach r = reachRecipe(recipe, output, visited, depth + 1);
                if (r.ok()) {
                    result = Reach.OK;
                    break;
                }
                if (r.dead) anyDead = true;
                cycles.addAll(r.cycles);
            }
        }
        visited.remove(item);
        if (result == null) {
            cycles.remove(item);
            if (!cycles.isEmpty()) result = new Reach(false, cycles);
            else result = anyDead ? Reach.DEAD : Reach.OK;
        }
        if (checkStock && result.cycles.isEmpty()) memo.put(item, result);
        return result;
    }

    private boolean cookedFromRaw(ItemKey item, ItemKey output, Set<ItemKey> visited, int depth) {
        for (Ingredient ingredient : resolver.cookingInputs(item.toStack(1))) {
            for (ItemStack stack : ingredient.getItems()) {
                if (stack.isEmpty()) continue;
                if (reach(ItemKey.of(stack), output, visited, depth + 1, true).ok()) return true;
            }
        }
        return false;
    }

    private Reach reachRecipe(RecipeOption recipe, ItemKey output, Set<ItemKey> visited, int depth) {
        Set<ItemKey> cycles = new HashSet<>();
        for (Ingredient ingredient : recipe.inputs()) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] items = ingredient.getItems();
            if (items.length == 0 || isCatalystIngredient(items)) continue;
            Reach best = Reach.DEAD;
            Set<ItemKey> ingredientCycles = new HashSet<>();
            for (ItemStack stack : items) {
                Reach r = reach(ItemKey.of(stack), output, visited, depth, true);
                if (r.ok()) {
                    best = Reach.OK;
                    break;
                }
                if (!r.dead) {
                    best = r;
                    ingredientCycles.addAll(r.cycles);
                }
            }
            if (best.dead) return Reach.DEAD;
            if (!best.ok()) cycles.addAll(ingredientCycles);
        }
        return cycles.isEmpty() ? Reach.OK : new Reach(false, cycles);
    }

    private ItemStack chooseIngredient(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        if (items.length > 1) {
            Item chosen = ingredientChoices.get(ingredientSignature(items));
            if (chosen != null) {
                for (ItemStack stack : items) {
                    if (stack.getItem() == chosen) return stack.copy();
                }
            }
        }
        for (Item pref : preferred) {
            for (ItemStack stack : items) {
                if (stack.getItem() == pref && availability.available(ItemKey.of(stack)) > 0) return stack.copy();
            }
        }
        for (ItemStack stack : items) {
            if (availability.available(ItemKey.of(stack)) > 0) return stack.copy();
        }
        for (Item pref : preferred) {
            for (ItemStack stack : items) {
                if (stack.getItem() == pref && emcLookup.obtainable(ItemKey.of(stack))) return stack.copy();
            }
        }
        for (ItemStack stack : items) {
            if (emcLookup.obtainable(ItemKey.of(stack))) return stack.copy();
        }
        for (Item pref : preferred) {
            for (ItemStack stack : items) {
                if (stack.getItem() == pref && resolver.canCraft(stack)) return stack.copy();
            }
        }
        for (ItemStack stack : items) {
            if (resolver.canCraft(stack)) return stack.copy();
        }
        for (Item pref : preferred) {
            for (ItemStack stack : items) {
                if (stack.getItem() == pref) return stack.copy();
            }
        }
        return items[0].copy();
    }

    private static int ceilDiv(int a, int b) {
        return clamp(((long) a + b - 1) / b);
    }

    private static int clamp(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }
}
