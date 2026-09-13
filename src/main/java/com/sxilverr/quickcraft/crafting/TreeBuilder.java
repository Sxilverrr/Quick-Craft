package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TreeBuilder {
    public static final ResourceLocation MANUAL = new ResourceLocation("quickcraft", "manual");

    private final RecipeResolver resolver;
    private final List<Item> preferred;
    private final int maxDepth;
    private final int maxNodes;
    private static final int REACH_DEPTH = 6;

    private Map<ItemKey, ResourceLocation> recipeOverrides = Collections.emptyMap();
    private Map<String, Item> ingredientChoices = Collections.emptyMap();
    private Availability availability = Availability.NONE;
    private Stations stations = Stations.of(Station.CRAFTING);
    private boolean collapseOwned = true;
    private boolean hideLooping = false;
    private final Map<ItemKey, Integer> claimedStock = new HashMap<ItemKey, Integer>();
    private final Set<ItemKey> loopIngredients = new HashSet<ItemKey>();
    private final Map<ItemKey, CraftNode> expanded = new HashMap<ItemKey, CraftNode>();
    private final Map<ItemKey, Map<ItemKey, Reach>> reachMemo = new HashMap<ItemKey, Map<ItemKey, Reach>>();
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
                           Map<String, Item> ingredientChoices, Availability availability, Stations stations,
                           boolean collapseOwned, boolean hideLooping) {
        this.recipeOverrides = overrides == null ? Collections.<ItemKey, ResourceLocation>emptyMap() : overrides;
        this.ingredientChoices = ingredientChoices == null ? Collections.<String, Item>emptyMap() : ingredientChoices;
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
        Integer claimedValue = claimedStock.get(outputKey);
        int claimed = catalyst || claimedValue == null ? 0 : claimedValue;
        int freeStock = availability.available(outputKey) - claimed;
        node.freeStock = Math.max(0, freeStock);

        boolean root = depth == 0;
        node.autoRecipe = alternatives.isEmpty() ? -1 : autoBestIndex(output, alternatives, requiredCount);
        node.selectedRecipe = resolveSelection(output, alternatives, node.autoRecipe);
        if (node.selectedRecipe < 0) {
            if (!catalyst) claimedStock.put(outputKey, claimed + Math.min(node.freeStock, requiredCount));
            if (!root && freeStock < requiredCount && emcLookup.obtainable(outputKey)) node.emcBuy = true;
            return node;
        }

        RecipeOption option = node.selected();
        if (!root && collapseOwned && freeStock >= requiredCount) {
            node.owned = true;
            if (!catalyst) claimedStock.put(outputKey, claimed + requiredCount);
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

        Map<ItemKey, Integer> needs = new LinkedHashMap<ItemKey, Integer>();
        Map<ItemKey, ItemStack[]> childOptions = new LinkedHashMap<ItemKey, ItemStack[]>();
        List<ItemKey> catalysts = new ArrayList<ItemKey>();
        for (Ingredient ingredient : option.inputs()) {
            if (Ingredients.isEmpty(ingredient)) continue;
            ItemStack[] items = Ingredients.matching(ingredient);
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            ItemKey key = ItemKey.of(choice);
            if (!childOptions.containsKey(key)) childOptions.put(key, items);
            if (isCatalystIngredient(items)) {
                catalysts.add(key);
            } else {
                Integer existing = needs.get(key);
                needs.put(key, existing == null ? crafts : clamp((long) existing + crafts));
            }
        }
        Set<ItemKey> kept = new HashSet<ItemKey>();
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
        if (!node.children.isEmpty() && !expanded.containsKey(outputKey)) expanded.put(outputKey, node);
        return node;
    }

    private boolean stockCraftable(RecipeOption option, int requiredCount) {
        int crafts = ceilDiv(Math.max(1, requiredCount), Math.max(1, option.resultCount()));
        Map<ItemKey, Integer> needs = new HashMap<ItemKey, Integer>();
        for (Ingredient ingredient : option.inputs()) {
            if (Ingredients.isEmpty(ingredient)) continue;
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            ItemKey key = ItemKey.of(choice);
            if (isCatalystIngredient(Ingredients.matching(ingredient))) {
                if (!needs.containsKey(key)) needs.put(key, 1);
            } else {
                Integer existing = needs.get(key);
                needs.put(key, existing == null ? crafts : clamp((long) existing + crafts));
            }
        }
        if (needs.isEmpty()) return false;
        for (Map.Entry<ItemKey, Integer> entry : needs.entrySet()) {
            Integer claimedValue = claimedStock.get(entry.getKey());
            int free = availability.available(entry.getKey()) - (claimedValue == null ? 0 : claimedValue);
            if (free < entry.getValue()) return false;
        }
        return true;
    }

    private static void applyTagOptions(CraftNode child, ItemStack[] options) {
        if (options == null || options.length <= 1) return;
        List<ItemStack> list = new ArrayList<ItemStack>();
        for (ItemStack stack : options) list.add(stack.copy());
        child.tagOptions = list;
        child.tagSignature = ingredientSignature(options);
    }

    public static String ingredientSignature(ItemStack[] items) {
        List<String> ids = new ArrayList<String>();
        for (ItemStack stack : items) {
            ResourceLocation id = Reg.idOf(stack.getItem());
            String base = id == null ? "?" : id.toString();
            int meta = stack.getItemDamage();
            ids.add(meta == 0 ? base : base + "@" + meta);
        }
        Collections.sort(ids);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(ids.get(i));
        }
        return sb.toString();
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

        Map<ItemKey, Integer> needs = new HashMap<ItemKey, Integer>();
        int preferredHits = 0;
        for (Ingredient ingredient : option.inputs()) {
            if (Ingredients.isEmpty(ingredient)) continue;
            if (ingredientAcceptsPreferred(ingredient)) preferredHits++;
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            ItemKey choiceKey = ItemKey.of(choice);
            if (isCatalystIngredient(Ingredients.matching(ingredient))) {
                if (!needs.containsKey(choiceKey)) needs.put(choiceKey, 1);
            } else {
                Integer existing = needs.get(choiceKey);
                needs.put(choiceKey, existing == null ? crafts : clamp((long) existing + crafts));
            }
        }

        int fullyAvailable = 0;
        int anyAvailable = 0;
        for (Map.Entry<ItemKey, Integer> entry : needs.entrySet()) {
            int have = availability.available(entry.getKey());
            if (have > 0) anyAvailable++;
            if (have >= entry.getValue()) fullyAvailable++;
        }

        return (fits ? 1000000 : 0)
                - (selfReferencing ? 500000 : 0)
                - (missingCatalyst ? 200000 : 0)
                + fullyAvailable * 1000
                + anyAvailable * 100
                + preferredHits * 10
                - needs.size();
    }

    private boolean referencesOutput(ItemStack output, RecipeOption option) {
        if (option == null) return false;
        ItemKey outputKey = ItemKey.of(output);
        for (Ingredient ingredient : option.inputs()) {
            if (Ingredients.isEmpty(ingredient)) continue;
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            if (loopsThrough(choice, outputKey)) return true;
        }
        return false;
    }

    private boolean ingredientAcceptsPreferred(Ingredient ingredient) {
        for (ItemStack stack : Ingredients.matching(ingredient)) {
            if (preferred.contains(stack.getItem())) return true;
        }
        return false;
    }

    private List<RecipeOption> visibleRecipes(ItemStack output, List<RecipeOption> alternatives) {
        if (!hideLooping || alternatives.isEmpty()) return alternatives;
        List<RecipeOption> visible = new ArrayList<RecipeOption>();
        for (RecipeOption option : alternatives) {
            if (!hidesAsLoop(output, option)) visible.add(option);
        }
        return visible;
    }

    private boolean missingCatalyst(RecipeOption option) {
        for (Ingredient ingredient : option.inputs()) {
            if (Ingredients.isEmpty(ingredient)) continue;
            ItemStack[] items = Ingredients.matching(ingredient);
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
        ItemStack remainder = ForgeHooks.getContainerItem(stack);
        return remainder != null && !remainder.isEmpty() && remainder.getItem() == stack.getItem();
    }

    private boolean hidesAsLoop(ItemStack output, RecipeOption option) {
        ItemKey outputKey = ItemKey.of(output);
        boolean hidden = false;
        for (Ingredient ingredient : option.inputs()) {
            if (Ingredients.isEmpty(ingredient)) continue;
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
        return reach(choiceKey, outputKey, new HashSet<ItemKey>(), 0, false).dead;
    }

    private static final class Reach {
        static final Reach OK = new Reach(false, Collections.<ItemKey>emptySet());
        static final Reach DEAD = new Reach(true, Collections.<ItemKey>emptySet());

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
        if (visited.contains(item)) return new Reach(false, Collections.singleton(item));
        Map<ItemKey, Reach> memo = reachMemo.get(output);
        if (memo == null) {
            memo = new HashMap<ItemKey, Reach>();
            reachMemo.put(output, memo);
        }
        Reach cached = checkStock ? memo.get(item) : null;
        if (cached != null) return cached;
        List<RecipeOption> recipes = resolver.recipesFor(item.toStack(1));
        if (recipes.isEmpty()) return Reach.OK;
        visited.add(item);
        boolean anyDead = false;
        Set<ItemKey> cycles = new HashSet<ItemKey>();
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
        for (ItemStack input : resolver.cookingInputs(item.toStack(1))) {
            if (input.isEmpty()) continue;
            if (reach(ItemKey.of(input), output, visited, depth + 1, true).ok()) return true;
        }
        return false;
    }

    private Reach reachRecipe(RecipeOption recipe, ItemKey output, Set<ItemKey> visited, int depth) {
        Set<ItemKey> cycles = new HashSet<ItemKey>();
        for (Ingredient ingredient : recipe.inputs()) {
            if (Ingredients.isEmpty(ingredient)) continue;
            ItemStack[] items = Ingredients.matching(ingredient);
            if (items.length == 0 || isCatalystIngredient(items)) continue;
            Reach best = Reach.DEAD;
            Set<ItemKey> ingredientCycles = new HashSet<ItemKey>();
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
        ItemStack[] items = Ingredients.matching(ingredient);
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
