package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.craft.Deposit;
import com.sxilverr.quickcraft.forge.QuickCraftClientConfig;
import com.sxilverr.quickcraft.forge.QuickCraftConfig;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.Availability;
import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.integration.jer.DropLine;
import com.sxilverr.quickcraft.forge.integration.jer.JerIntegration;
import com.sxilverr.quickcraft.integration.jer.MobDropInfo;
import com.sxilverr.quickcraft.integration.jer.MobItemSource;
import com.sxilverr.quickcraft.craft.CraftPlanner;
import com.sxilverr.quickcraft.craft.EmcSource;
import com.sxilverr.quickcraft.craft.VirtualPool;
import com.sxilverr.quickcraft.forge.integration.projecte.ProjectEClient;
import com.sxilverr.quickcraft.forge.integration.projecte.ProjectEIntegration;
import com.sxilverr.quickcraft.crafting.CraftTrees;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.ItemOrigins;
import com.sxilverr.quickcraft.crafting.RecipeOption;
import com.sxilverr.quickcraft.crafting.RecipeResolver;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.StationProviders;
import com.sxilverr.quickcraft.crafting.StationScan;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.crafting.TreeBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import com.sxilverr.quickcraft.integration.OriginHint;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuickCraftScreen extends Screen {
    private int COLOR_HAVE = 0xFF55FF55;
    private int COLOR_CRAFT = 0xFFFFC64B;
    private int COLOR_MISSING = 0xFFFF5555;
    private int COLOR_DISABLED = 0xFF5A5A5A;
    private int COLOR_ROOT = 0xFF4AA3FF;
    private int COLOR_NODE_BG = 0xF01A1A1A;
    private int COLOR_EDGE = 0xFF7A7A7A;
    private static final int COLOR_BAR = 0xF0080808;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 2.5;
    private static final int PANEL_W = 174;
    private static final int PANEL_ROW_H = 20;
    private static final int Z_OVERLAY = 300;
    private static final long TREE_DEPTH_DELAY = 55;
    private static final long TREE_NODE_SLIDE = 210;
    private static final long SUMMARY_SLIDE = 150;
    private static final long SUMMARY_SWAP = 130;
    private static final long ROW_STAGGER = 20;
    private static final long ROW_SLIDE = 120;
    private static final int CRAFT_MAX = 1000000;
    private static final float HOVER_BULGE = 1.12F;
    private static final int MAX_NODE_WIDTH = 300;
    private static final int COLOR_MOB = 0xFFB07CE8;
    private static final int MAX_MOB_SOURCES = 10;
    private static final long ORIGIN_CYCLE = 1500;
    private static final int MAX_ORIGIN_LABELS = 4;
    private static final int COLOR_EMC = 0xFF6FC3DF;
    private static final int DEP_X = 6;
    private static final int DEP_Y = 4;
    private static final int DEP_W = 26;
    private static final int DEP_H = 16;
    private static final int DEP_MENU_W = 200;
    private static final int DEP_ROW_H = 18;
    private static final int DEP_HEADER_H = 14;
    private static final int DEP_SEARCH_H = 14;
    private static final int DEP_MAX_VISIBLE = 8;
    private static final int CT_X = 8;
    private static final int CT_W = 60;
    private static final int CT_H = 20;
    private static final int HIST_X = 72;
    private static final int HIST_W = 60;
    private static final int HIST_H = 20;
    private static final int HIST_MENU_W = 200;
    private static final int HIST_ROW_H = 18;
    private static final int HIST_HEADER_H = 14;
    private static final int HIST_MAX_VISIBLE = 8;
    private static final int CTRL_W = 216;
    private static final long CONTROLS_SLIDE = 160;
    private static final int TAB_BTN_W = 28;
    private static final int COPY_DROP_W = 152;
    private static final int COPY_ROW_H = 14;
    private static final String[] COPY_OPTIONS = {"Copy full list", "Copy missing materials", "Copy material shortages"};
    private static final String[] CONTROL_LINES = {
            "Drag/Scroll: pan/zoom",
            "Left-click: swap recipe",
            "Middle-click: view item's tree",
            "W/S or Up/Down: switch tag item",
            "Right-click: show/hide recipe",
            "Ctrl-click: reset preferences",
    };

    private ItemStack target;
    private int quantity;
    private final Map<ItemKey, Integer> mobSelection = new HashMap<>();
    private final Map<ItemKey, net.minecraft.resources.ResourceLocation> overrides = new HashMap<>();
    private final Map<String, Item> ingredientChoices = new HashMap<>();
    private final Map<ItemKey, Integer> haveCounts = new HashMap<>();
    private final Map<ItemKey, ItemStack> sourceIcons = new HashMap<>();
    private final Map<ItemKey, ItemStack> damageSamples = new HashMap<>();

    private RecipeResolver resolver;
    private TreeBuilder builder;
    private CraftNode root;
    private TreeLayout layout;

    private double panX;
    private double panY;
    private double zoom = 1.0;
    private boolean panning;
    private boolean stationProblem;
    private String stationWarning = "";
    private boolean treeLimited;
    private Stations detectedStations;
    private Stations serverStations;
    private Station missingStation;
    private boolean hoveringStationName;
    private int stationSelectedIndex;
    private int baseQuantity;
    private boolean shiftActive;
    private boolean maxMode;
    private NodeView hoveredView;
    private boolean suppressResponder;
    private boolean applyingAvailability;
    private boolean showSummary;
    private boolean copyMenuOpen;
    private int copySelected;
    private boolean depositMenuOpen;
    private int depositScroll;
    private String depositSearch = "";
    private boolean controlsHovered;
    private long controlsAnimStart;
    private int summaryScroll;
    private List<Map.Entry<ItemKey, Integer>> summaryItems = List.of();
    private List<Map.Entry<ItemKey, Integer>> outgoingItems = List.of();
    private int outgoingScroll;
    private long summarySwapStart;

    private boolean animate;
    private long treeAnimStart;
    private long summaryAnimStart;
    private int summaryRowCount;
    private final Map<CraftNode, double[]> animData = new IdentityHashMap<>();
    private final Map<CraftNode, Boolean> achievableCache = new IdentityHashMap<>();
    private int maxCraftableCache = -1;

    private boolean bookmarkDragging;
    private double bmGrabX;
    private double bmGrabY;

    private EditBox quantityBox;

    private final java.util.Deque<ItemStack> history = new java.util.ArrayDeque<>();

    private boolean showMobs;
    private Button showMobsButton;
    private Button minusButton;
    private Button plusButton;
    private String emcTotalText;
    private String emcCostText;
    private boolean emcAffordable = true;
    private BigInteger lastEmc;
    private BigInteger emcRequired = BigInteger.ZERO;
    private boolean planFull;
    private int emcPollTicks;
    private static final int EMC_POLL_TICKS = 20;
    private final Map<ItemKey, Integer> emcSupplied = new HashMap<>();
    private EmcSource emcSource;
    private List<CraftPlanner.Blocker> blockers = List.of();
    private final Map<ItemKey, String> summaryEmcText = new HashMap<>();
    private final Map<ItemKey, Integer> summaryEmcColor = new HashMap<>();
    private static final BigInteger EMC_UNLIMITED = BigInteger.ONE.shiftLeft(96);
    private boolean historyOpen;
    private int historyScroll;
    private List<CraftHistory.Entry> historyEntries = List.of();

    public QuickCraftScreen(ItemStack target, int quantity) {
        super(Component.literal("Quick Craft"));
        this.target = target.copy();
        this.quantity = Math.max(1, quantity);
    }

    @Override
    public void onClose() {
        if (!history.isEmpty()) {
            retarget(history.pop(), false);
        } else {
            this.minecraft.setScreen(null);
        }
    }

    private void closeAfterCraft() {
        history.clear();
        this.minecraft.setScreen(null);
    }

    private void retarget(ItemStack newTarget, boolean pushHistory) {
        if (pushHistory) history.push(target.copy());
        if (animate && showSummary) {
            outgoingItems = summaryItems;
            outgoingScroll = summaryScroll;
            summarySwapStart = Util.getMillis();
        }
        target = newTarget.copy();
        quantity = 1;
        baseQuantity = 1;
        maxMode = false;
        summaryScroll = 0;
        hoveredView = null;
        panning = false;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        resolver = ClientRecipeCache.get(level);
        builder = new TreeBuilder(resolver, QuickCraftConfig.preferredItems(),
                QuickCraftConfig.maxTreeDepth(), QuickCraftConfig.maxTreeNodes());

        overrides.clear();
        overrides.putAll(RecipePreferences.recipeOverrides());
        ingredientChoices.clear();
        ingredientChoices.putAll(RecipePreferences.ingredientChoices());

        quantityBox = new EditBox(this.font, this.width - 146, 4, 46, 16, Component.literal("Quantity"));
        quantityBox.setValue(String.valueOf(quantity));
        quantityBox.setFilter(s -> s.matches("\\d{0,6}") || s.equalsIgnoreCase("max") || s.matches("(?i)max \\(\\d+\\)"));
        quantityBox.setResponder(this::onQuantityChanged);
        addRenderableWidget(quantityBox);

        minusButton = Button.builder(Component.literal("-"), b -> stepQuantity(-1))
                .bounds(this.width - 164, 4, 16, 16).build();
        addRenderableWidget(minusButton);
        plusButton = Button.builder(Component.literal("+"), b -> stepQuantity(1))
                .bounds(this.width - 98, 4, 16, 16).build();
        addRenderableWidget(plusButton);

        addRenderableWidget(Button.builder(Component.literal("Craft Max"), b -> onCraftMax())
                .bounds(this.width - 78, 4, 72, 16).build());

        addRenderableWidget(Button.builder(Component.literal("Confirm Craft"), b -> onConfirm())
                .bounds(this.width - 124, this.height - 28, 116, 22).build());
        addRenderableWidget(Button.builder(Component.literal(history.isEmpty() ? "Close" : "Back"), b -> onClose())
                .bounds(8, this.height - 28, 60, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Ingredients"), b -> toggleSummary())
                .bounds(this.width / 2 - 172, this.height - 52, 112, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> onRefresh())
                .bounds(this.width / 2 - 56, this.height - 52, 112, 20)
                .tooltip(Tooltip.create(Component.literal("Scan for items available"))).build());
        if (JerIntegration.available()) {
            showMobsButton = Button.builder(Component.literal(showMobs ? "Hide Mobs" : "Show Mobs"), b -> toggleShowMobs())
                    .bounds(this.width / 2 + 62, this.height - 52, 96, 20).build();
            addRenderableWidget(showMobsButton);
        }

        loadColors();
        computeHaveCounts();
        baseQuantity = quantity;
        applyShiftState(hasShiftDown());
        autoFit();
        animate = QuickCraftConfig.animationsEnabled();
        treeAnimStart = Util.getMillis();
        com.sxilverr.quickcraft.network.QuickCraftNetwork.requestDepositTargets();
    }

    private CraftPlanner.Plan planFor(int qty, Stations stations, boolean collapse, boolean hideLoop,
                                      BigInteger budget, boolean withEmc) {
        VirtualPool pool = new VirtualPool();
        for (Map.Entry<ItemKey, Integer> entry : haveCounts.entrySet()) pool.add(entry.getKey(), entry.getValue());
        return CraftPlanner.plan(builder, target, qty, overrides, ingredientChoices, pool, Availability.exact(haveCounts),
                stations, collapse, hideLoop, withEmc ? emcSource : null, budget);
    }

    private void applyEmcPlan(CraftPlanner.Plan plan, int qty, Stations stations, boolean collapse, boolean hideLoop) {
        emcSupplied.clear();
        emcTotalText = null;
        emcCostText = null;
        emcAffordable = true;
        lastEmc = null;
        if (emcSource == null) return;
        emcSupplied.putAll(plan.supplied());
        BigInteger owned = emcSource.emc();
        lastEmc = owned;
        planFull = plan.full();
        BigInteger required = plan.spentEmc();
        if (!planFull) required = planFor(qty, stations, collapse, hideLoop, EMC_UNLIMITED, true).spentEmc();
        emcRequired = required;
        emcAffordable = required.compareTo(owned) <= 0;
        if (!QuickCraftClientConfig.showEmc()) return;
        emcTotalText = ProjectEClient.format(owned);
        emcCostText = required.signum() > 0 ? ProjectEClient.format(required) : null;
    }

    private int nodeHave(CraftNode node) {
        return node.freeStock;
    }

    private int effectiveHave(ItemKey key) {
        return haveCounts.getOrDefault(key, 0) + emcSupplied.getOrDefault(key, 0);
    }

    private void renderEmc(GuiGraphics g) {
        if (emcTotalText == null && emcCostText == null) return;
        String totalPart = emcTotalText == null ? "" : "EMC: " + emcTotalText;
        String costPart = emcCostText == null ? "" : "Uses " + emcCostText;
        String gap = (!totalPart.isEmpty() && !costPart.isEmpty()) ? "   " : "";

        int w = this.font.width(totalPart + gap + costPart);
        int right = this.width - 8;
        if (summaryVisible()) right = Math.min(right, summaryPanelX() - 4);
        int x = Math.max(8, right - w);
        boolean stationBar = (stationProblem && missingStation != null) || treeLimited;
        if (!stationBar) g.fill(x - 6, 24, right + 8, 38, COLOR_BAR);

        if (!totalPart.isEmpty()) g.drawString(this.font, totalPart, x, 27, COLOR_EMC, false);
        if (!costPart.isEmpty()) {
            int costX = x + this.font.width(totalPart + gap);
            g.drawString(this.font, costPart, costX, 27, emcAffordable ? COLOR_EMC : COLOR_MISSING, false);
        }
    }

    private void toggleSummary() {
        showSummary = !showSummary;
        if (!showSummary) copyMenuOpen = false;
        summaryAnimStart = Util.getMillis();
    }

    private void onRefresh() {
        playClick();
        computeHaveCounts();
        rebuild();
    }

    private void loadColors() {
        COLOR_HAVE = QuickCraftClientConfig.colorAvailable();
        COLOR_CRAFT = QuickCraftClientConfig.colorCrafted();
        COLOR_MISSING = QuickCraftClientConfig.colorMissing();
        COLOR_DISABLED = QuickCraftClientConfig.colorNoStation();
        COLOR_ROOT = QuickCraftClientConfig.colorTarget();
        COLOR_NODE_BG = QuickCraftClientConfig.colorNodeBackground();
        COLOR_EDGE = QuickCraftClientConfig.colorLines();
    }

    private void toggleShowMobs() {
        showMobs = !showMobs;
        if (showMobsButton != null) {
            showMobsButton.setMessage(Component.literal(showMobs ? "Hide Mobs" : "Show Mobs"));
        }
        hoveredView = null;
        playClick();
        if (root != null) {
            if (showMobs && JerIntegration.available()) attachMobSources(root);
            else detachMobSources(root);
            achievableCache.clear();
            layout = new TreeLayout(root, this::nodeWidth);
        }
    }

    private boolean overCraftMax(double mouseX, double mouseY) {
        return mouseX >= this.width - 78 && mouseX <= this.width - 6 && mouseY >= 4 && mouseY <= 20;
    }

    private int maxCraftable() {
        if (maxCraftableCache >= 0) return maxCraftableCache;
        Minecraft mc = Minecraft.getInstance();
        Stations stations = detectedStations != null ? detectedStations : StationScan.detect(mc.level, mc.player);
        maxCraftableCache = Math.max(0, planFor(CRAFT_MAX, stations, QuickCraftConfig.collapseOwnedItems(),
                QuickCraftConfig.hideLoopingRecipes(), null, false).craftable());
        return maxCraftableCache;
    }

    @Override
    public void tick() {
        super.tick();
        boolean shift = hasShiftDown();
        if (shift != shiftActive) applyShiftState(shift);
        pollEmc();
    }

    private void pollEmc() {
        if (emcSource == null || lastEmc == null) return;
        if (++emcPollTicks < EMC_POLL_TICKS) return;
        emcPollTicks = 0;
        BigInteger owned = emcSource.emc();
        if (owned.equals(lastEmc)) return;
        boolean shrunk = owned.compareTo(lastEmc) < 0;
        lastEmc = owned;
        if (!planFull || shrunk) {
            applyingAvailability = true;
            rebuild();
            applyingAvailability = false;
            return;
        }
        emcAffordable = emcRequired.compareTo(owned) <= 0;
        if (QuickCraftClientConfig.showEmc()) emcTotalText = ProjectEClient.format(owned);
        computeSummaryEmc(summaryItems);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (depositMenuOpen && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            depositMenuOpen = false;
            return true;
        }
        if (depositMenuOpen && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!depositSearch.isEmpty()) depositSearch = depositSearch.substring(0, depositSearch.length() - 1);
            depositScroll = 0;
            return true;
        }
        if (copyMenuOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                copyMenuOpen = false;
                return true;
            }
            int dir = tagDirection(keyCode);
            if (dir != 0) {
                copySelected = Math.floorMod(copySelected + dir, COPY_OPTIONS.length);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                doCopy(copySelected);
                copyMenuOpen = false;
                return true;
            }
        }
        if (isShiftKey(keyCode) && !shiftActive) applyShiftState(true);
        boolean typing = depositMenuOpen || quantityBox.isFocused();
        if (!typing && hoveringStationName && missingStation != null) {
            int dir = tagDirection(keyCode);
            if (dir != 0) {
                int count = StationProviders.icons(missingStation).size();
                if (count > 0) stationSelectedIndex = Math.floorMod(stationSelectedIndex + dir, count);
                return true;
            }
        }
        if (!typing && hoveredView != null && hoveredView.node.isTagChoice()) {
            int dir = tagDirection(keyCode);
            if (dir != 0) {
                cycleTagOption(hoveredView.node, dir);
                return true;
            }
        }
        if (!typing && hoveredView != null && QuickCraftIntegrations.canShowRecipes()) {
            if (QuickCraftClient.SHOW_RECIPE_KEY.matches(keyCode, scanCode)) {
                QuickCraftIntegrations.showRecipe(hoveredView.node.output);
                return true;
            }
            if (QuickCraftClient.SHOW_USES_KEY.matches(keyCode, scanCode)) {
                QuickCraftIntegrations.showUses(hoveredView.node.output);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (depositMenuOpen && codePoint >= ' ' && codePoint != 167) {
            depositSearch += codePoint;
            depositScroll = 0;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private static int tagDirection(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_W || keyCode == GLFW.GLFW_KEY_UP) return -1;
        if (keyCode == GLFW.GLFW_KEY_S || keyCode == GLFW.GLFW_KEY_DOWN) return 1;
        return 0;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (isShiftKey(keyCode) && shiftActive) applyShiftState(false);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private static boolean isShiftKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT;
    }

    private void applyShiftState(boolean shift) {
        shiftActive = shift;
        maxMode = shift && QuickCraftConfig.shiftCraftIsMax();
        quantity = shift ? (maxMode ? CRAFT_MAX : QuickCraftConfig.shiftCraftAmount()) : baseQuantity;
        suppressResponder = true;
        quantityBox.setValue(maxMode ? "Max" : String.valueOf(quantity));
        quantityBox.setEditable(!shift);
        if (minusButton != null) minusButton.active = !shift;
        if (plusButton != null) plusButton.active = !shift;
        suppressResponder = false;
        rebuild();
    }

    private void onQuantityChanged(String value) {
        if (suppressResponder) return;
        maxMode = false;
        baseQuantity = value.isEmpty() ? 1 : Math.max(1, safeParse(value));
        quantity = baseQuantity;
        rebuild();
    }

    private void stepQuantity(int delta) {
        if (shiftActive) return;
        int next = Math.max(1, Math.min(CRAFT_MAX, baseQuantity + delta));
        if (next == baseQuantity) return;
        playClick();
        quantityBox.setValue(String.valueOf(next));
    }

    private static int safeParse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void rebuild() {
        Minecraft mc = Minecraft.getInstance();
        Stations stations = serverStations != null ? serverStations : StationScan.detect(mc.level, mc.player);
        detectedStations = stations;
        boolean collapse = QuickCraftConfig.collapseOwnedItems();
        boolean hideLoop = QuickCraftConfig.hideLoopingRecipes();
        emcSource = ProjectEClient.session(mc.player, QuickCraftConfig.containerScanRange());
        int qty = quantity;
        if (maxMode) {
            qty = Math.max(1, planFor(CRAFT_MAX, stations, collapse, hideLoop, null, false).craftable());
            if (quantityBox != null) {
                suppressResponder = true;
                quantityBox.setValue("Max (" + qty + ")");
                suppressResponder = false;
            }
        }
        CraftPlanner.Plan plan = planFor(qty, stations, collapse, hideLoop, null, true);
        root = plan.root();
        blockers = plan.blockers();
        java.util.Set<ItemKey> requestKeys = relevantKeys();
        applyEmcPlan(plan, qty, stations, collapse, hideLoop);

        if (showMobs && JerIntegration.available()) attachMobSources(root);
        primeOrigins(root);
        layout = new TreeLayout(root, this::nodeWidth);
        Station missing = CraftTrees.missingStation(root);
        if (missing != missingStation) stationSelectedIndex = 0;
        stationProblem = missing != null;
        missingStation = missing;
        stationWarning = missing == null ? "" : StationIcons.name(missing);
        treeLimited = CraftTrees.truncated(root);
        achievableCache.clear();
        maxCraftableCache = -1;
        computeSummary();
        if (!applyingAvailability) {
            com.sxilverr.quickcraft.network.QuickCraftNetwork.requestAvailability(requestKeys);
        }
    }

    private void attachMobSources(CraftNode node) {
        List<CraftNode> original = new ArrayList<>(node.children);
        for (CraftNode child : original) attachMobSources(child);
        if (node.isMobSource()) return;
        List<MobItemSource> sources = JerIntegration.sourcesFor(node.output.getItem());
        if (sources.isEmpty()) return;
        if (sources.size() > MAX_MOB_SOURCES) sources = new ArrayList<>(sources.subList(0, MAX_MOB_SOURCES));
        CraftNode mobNode = new CraftNode(node.output.copy(), 0, List.of(), node.depth + 1);
        mobNode.mobSources = sources;
        mobNode.mobIndex = mobSelection.getOrDefault(ItemKey.of(node.output), 0);
        node.children.add(mobNode);
    }

    private void detachMobSources(CraftNode node) {
        node.children.removeIf(CraftNode::isMobSource);
        for (CraftNode child : node.children) detachMobSources(child);
    }

    private java.util.Set<ItemKey> relevantKeys() {
        java.util.Set<ItemKey> keys = new java.util.LinkedHashSet<>();
        keys.add(ItemKey.of(target));
        collectTreeKeys(root, keys);
        keys.addAll(builder.loopIngredientKeys());
        return keys;
    }

    private void collectTreeKeys(CraftNode node, java.util.Set<ItemKey> keys) {
        if (node == null) return;
        keys.add(ItemKey.of(node.output));
        for (ItemStack option : node.tagOptions) keys.add(ItemKey.of(option));
        for (CraftNode child : node.children) collectTreeKeys(child, keys);
    }

    private void computeSummary() {
        Map<ItemKey, Integer> totals = CraftTrees.leafTotals(root);
        List<Map.Entry<ItemKey, Integer>> list = new ArrayList<>(totals.entrySet());
        list.sort(Comparator
                .comparingInt((Map.Entry<ItemKey, Integer> e) -> summaryTier(e.getKey(), e.getValue()))
                .thenComparing(e -> -e.getValue()));
        summaryItems = list;
        computeSummaryEmc(list);
    }

    private int summaryTier(ItemKey key, int need) {
        int have = effectiveHave(key);
        if (have >= need) return 0;
        if (have > 0) return 1;
        return emcSource != null && emcSource.obtainable(key) ? 2 : 3;
    }

    private void computeSummaryEmc(List<Map.Entry<ItemKey, Integer>> list) {
        summaryEmcText.clear();
        summaryEmcColor.clear();
        if (emcSource == null) return;
        BigInteger owned = emcSource.emc();
        BigInteger running = BigInteger.ZERO;
        for (Map.Entry<ItemKey, Integer> entry : list) {
            int missing = entry.getValue() - haveCounts.getOrDefault(entry.getKey(), 0);
            if (missing <= 0) continue;
            ItemStack stack = entry.getKey().toStack(1);
            long unit = emcSource.value(stack);
            if (unit <= 0L) continue;
            if (!emcSource.learned(stack)) {
                summaryEmcText.put(entry.getKey(), "not learned");
                summaryEmcColor.put(entry.getKey(), COLOR_MISSING);
                continue;
            }
            BigInteger cost = BigInteger.valueOf(unit).multiply(BigInteger.valueOf(missing));
            running = running.add(cost);
            summaryEmcText.put(entry.getKey(), "EMC " + ProjectEClient.format(cost));
            summaryEmcColor.put(entry.getKey(), running.compareTo(owned) <= 0 ? COLOR_EMC : COLOR_MISSING);
        }
    }

    public void setAvailability(Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources,
                                Map<ItemKey, ItemStack> samples, Stations stations) {
        serverStations = stations;
        haveCounts.putAll(counts);
        for (ItemKey key : counts.keySet()) {
            ItemStack src = sources.get(key);
            if (src == null) sourceIcons.remove(key);
            else sourceIcons.put(key, src);
            ItemStack sample = samples.get(key);
            if (sample == null) damageSamples.remove(key);
            else damageSamples.put(key, sample);
        }
        applyingAvailability = true;
        rebuild();
        applyingAvailability = false;
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void computeHaveCounts() {
        haveCounts.clear();
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) haveCounts.merge(ItemKey.of(stack), stack.getCount(), Integer::sum);
        }
    }

    private void autoFit() {
        if (root == null || layout.ordered.isEmpty()) {
            panX = 40;
            panY = this.height / 2.0;
            return;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (NodeView v : layout.ordered) {
            minX = Math.min(minX, v.x);
            minY = Math.min(minY, v.y);
            maxX = Math.max(maxX, v.x + v.width);
            maxY = Math.max(maxY, v.y + NodeView.HEIGHT);
        }
        double treeW = Math.max(1, maxX - minX);
        double treeH = Math.max(1, maxY - minY);
        double availTop = 28;
        double availBottom = this.height - 60;
        double availW = this.width - 20;
        double availH = Math.max(1, availBottom - availTop);
        double fit = Math.min(availW / treeW, availH / treeH);
        zoom = Math.max(MIN_ZOOM, Math.min(1.0, fit));
        panX = this.width / 2.0 - (minX + maxX) / 2.0 * zoom;
        panY = (availTop + availBottom) / 2.0 - (minY + maxY) / 2.0 * zoom;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        updateControlsHover(mouseX, mouseY);
        boolean overChrome = overDepositBox(mouseX, mouseY) || (depositMenuOpen && overDepositMenu(mouseX, mouseY))
                || overControlsArea(mouseX, mouseY) || overHistoryTab(mouseX, mouseY) || overHistoryPanel(mouseX, mouseY);
        hoveredView = (overChrome || (showSummary && overSummaryPanel(mouseX, mouseY))) ? null : nodeAt(mouseX, mouseY);
        boolean bulge = QuickCraftConfig.hoverBulge() && hoveredView != null;

        g.pose().pushPose();
        g.pose().translate(panX, panY, 0);
        g.pose().scale((float) zoom, (float) zoom, 1.0F);
        if (animate && root != null) {
            drawTreeAnimated(g);
        } else {
            for (NodeView view : layout.ordered) {
                for (CraftNode child : view.node.children) {
                    drawEdge(g, view, layout.views.get(child), 1.0);
                }
            }
            for (NodeView view : layout.ordered) {
                if (bulge && view == hoveredView) drawNodeScaled(g, view, HOVER_BULGE);
                else drawNode(g, view);
            }
        }
        g.pose().popPose();

        g.pose().pushPose();
        g.pose().translate(0, 0, Z_OVERLAY);

        g.fill(0, 0, this.width, 24, COLOR_BAR);
        g.fill(0, this.height - 56, this.width, this.height, COLOR_BAR);

        String title = this.font.plainSubstrByWidth("Quick Craft - " + target.getHoverName().getString(), this.width - 232);
        g.drawString(this.font, title, DEP_X + DEP_W + 6, 8, 0xFFFFFF, false);
        g.drawString(this.font, "Qty:", this.width - 186, 8, 0xFFFFFF, false);
        renderDepositControl(g, mouseX, mouseY);
        renderControlsTab(g, mouseX, mouseY);
        renderHistoryTab(g, mouseX, mouseY);

        int stationNameX = -1;
        if (stationProblem && missingStation != null) {
            g.fill(0, 24, this.width, 38, COLOR_BAR);
            List<ItemStack> options = StationProviders.icons(missingStation);
            String name = options.isEmpty()
                    ? stationWarning
                    : options.get(Math.floorMod(stationSelectedIndex, options.size())).getHoverName().getString();
            String prefix = "Missing: ";
            g.drawString(this.font, prefix, 8, 27, COLOR_MISSING, false);
            stationNameX = 8 + this.font.width(prefix);
            int nameW = this.font.width(name);
            hoveringStationName = mouseX >= stationNameX && mouseX <= stationNameX + nameW && mouseY >= 25 && mouseY <= 37;
            g.drawString(this.font, name, stationNameX, 27, hoveringStationName ? 0xFFFFFF55 : COLOR_MISSING, false);
        } else if (treeLimited) {
            hoveringStationName = false;
            g.fill(0, 24, this.width, 38, COLOR_BAR);
            g.drawString(this.font, "Tree limit reached: raise maxTreeNodes or maxTreeDepth in the config", 8, 27, COLOR_MISSING, false);
        } else {
            hoveringStationName = false;
        }

        renderEmc(g);

        super.render(g, mouseX, mouseY, partialTick);

        if (summaryVisible()) renderSummary(g, mouseX, mouseY);

        if (BookmarkOverlay.isActive()) {
            BookmarkOverlay.render(g, this.width, this.height, true, mouseX, mouseY);
        }

        if (depositMenuOpen) renderDepositMenu(g, mouseX, mouseY);
        renderControlsPanel(g);
        if (historyOpen) renderHistoryPanel(g, mouseX, mouseY);

        if (hoveringStationName && stationNameX >= 0) {
            renderStationDropdown(g, stationNameX);
        } else if (hoveredView != null) {
            drawTooltip(g, hoveredView, mouseX, mouseY);
        } else if (!depositMenuOpen && overDepositBox(mouseX, mouseY)) {
            drawDepositTooltip(g, mouseX, mouseY);
        } else if (depositMenuOpen && overDepositMenu(mouseX, mouseY)) {
            drawDepositRowTooltip(g, mouseX, mouseY);
        } else if (overCraftMax(mouseX, mouseY)) {
            g.renderTooltip(this.font, Component.literal("can craft: " + maxCraftable()), mouseX, mouseY);
        } else if (!copyMenuOpen && overCopyButton(mouseX, mouseY)) {
            g.renderTooltip(this.font, Component.literal("Copy material list to clipboard."), mouseX, mouseY);
        }

        g.pose().popPose();
    }

    private void renderStationDropdown(GuiGraphics g, int anchorX) {
        List<ItemStack> options = StationProviders.icons(missingStation);
        if (options.isEmpty()) return;
        int total = options.size();
        int selected = Math.floorMod(stationSelectedIndex, total);
        int rowH = 18;
        int maxVisible = 6;
        int visible = Math.min(maxVisible, total);
        int scroll = selected >= maxVisible ? selected - maxVisible + 1 : 0;
        int headerH = 14;
        int w = 172;
        int x = Math.min(anchorX, this.width - w - 4);
        int top = 40;
        int h = headerH + visible * rowH + 4;
        g.fill(x, top, x + w, top + h, 0xF0080808);
        g.fill(x, top, x + w, top + headerH, 0xF0202020);
        g.drawString(this.font, "Can craft this at (W/S):", x + 5, top + 3, 0xFFFFFF, false);
        int listTop = top + headerH + 2;
        for (int r = 0; r < visible; r++) {
            int idx = r + scroll;
            if (idx >= total) break;
            ItemStack icon = options.get(idx);
            int ry = listTop + r * rowH;
            if (idx == selected) g.fill(x + 2, ry - 1, x + w - 2, ry + rowH - 3, 0x50FFFFFF);
            g.renderItem(icon, x + 5, ry);
            g.drawString(this.font, trim(icon.getHoverName().getString(), 24), x + 26, ry + 4,
                    idx == selected ? 0xFFFF55 : 0xFFFFFF, false);
        }
        if (total > maxVisible) {
            if (scroll > 0) g.drawString(this.font, "▲", x + w - 11, listTop, 0xFFFFFF, false);
            if (scroll + maxVisible < total) g.drawString(this.font, "▼", x + w - 11, top + h - 10, 0xFFFFFF, false);
        }
    }

    private void renderDepositControl(GuiGraphics g, int mouseX, int mouseY) {
        boolean active = depositMenuOpen || overDepositBox(mouseX, mouseY);
        g.fill(DEP_X, DEP_Y, DEP_X + DEP_W, DEP_Y + DEP_H, active ? 0xFF3A3A3A : 0x90000000);
        g.renderOutline(DEP_X, DEP_Y, DEP_W, DEP_H, active ? 0xFFAAAAAA : 0xFF555555);
        ItemStack icon = depositIcon(ClientDepositTargets.selected());
        if (!icon.isEmpty()) g.renderItem(icon, DEP_X + 1, DEP_Y);
        g.drawString(this.font, "▾", DEP_X + DEP_W - 8, DEP_Y + 4, 0xFFDDDDDD, false);
    }

    private List<ClientDepositTargets.Target> depositFiltered() {
        List<ClientDepositTargets.Target> all = ClientDepositTargets.targets();
        if (depositSearch.isEmpty()) return all;
        String q = depositSearch.toLowerCase(java.util.Locale.ROOT);
        List<ClientDepositTargets.Target> out = new ArrayList<>();
        for (ClientDepositTargets.Target t : all) {
            if (t.label().toLowerCase(java.util.Locale.ROOT).contains(q)) out.add(t);
        }
        return out;
    }

    private int depositListTop() {
        return DEP_Y + DEP_H + 2 + DEP_HEADER_H + DEP_SEARCH_H + 2;
    }

    private void renderDepositMenu(GuiGraphics g, int mouseX, int mouseY) {
        List<ClientDepositTargets.Target> list = depositFiltered();
        int total = list.size();
        int visible = Math.min(DEP_MAX_VISIBLE, total);
        int maxScroll = Math.max(0, total - DEP_MAX_VISIBLE);
        depositScroll = Math.max(0, Math.min(depositScroll, maxScroll));
        int x = DEP_X;
        int top = DEP_Y + DEP_H + 2;
        int searchTop = top + DEP_HEADER_H;
        int listTop = depositListTop();
        int h = DEP_HEADER_H + DEP_SEARCH_H + Math.max(1, visible) * DEP_ROW_H + 6;
        g.fill(x, top, x + DEP_MENU_W, top + h, 0xF0080808);
        g.fill(x, top, x + DEP_MENU_W, top + DEP_HEADER_H, 0xF0202020);
        g.drawString(this.font, "Deposit results into:", x + 5, top + 3, 0xFFFFFF, false);

        g.fill(x + 4, searchTop + 1, x + DEP_MENU_W - 4, searchTop + DEP_SEARCH_H - 1, 0xFF101010);
        g.renderOutline(x + 4, searchTop + 1, DEP_MENU_W - 8, DEP_SEARCH_H - 2, 0xFF555555);
        boolean empty = depositSearch.isEmpty();
        g.drawString(this.font, trim(empty ? "Search..." : depositSearch, 34), x + 8, searchTop + 4,
                empty ? 0xFF777777 : 0xFFFFFFFF, false);

        String selectedId = ClientDepositTargets.selectedId();
        for (int r = 0; r < visible; r++) {
            int idx = r + depositScroll;
            if (idx >= total) break;
            ClientDepositTargets.Target target = list.get(idx);
            int ry = listTop + r * DEP_ROW_H;
            boolean rowHover = mouseX >= x + 2 && mouseX <= x + DEP_MENU_W - 2 && mouseY >= ry - 1 && mouseY <= ry + DEP_ROW_H - 3;
            boolean isSelected = target.id().equals(selectedId);
            if (isSelected) g.fill(x + 2, ry - 1, x + DEP_MENU_W - 2, ry + DEP_ROW_H - 3, 0x5000FF00);
            else if (rowHover) g.fill(x + 2, ry - 1, x + DEP_MENU_W - 2, ry + DEP_ROW_H - 3, 0x40FFFFFF);
            ItemStack rowIcon = depositIcon(target);
            if (!rowIcon.isEmpty()) g.renderItem(rowIcon, x + 4, ry);
            g.drawString(this.font, trim(target.label(), 30), x + 25, ry + 4, isSelected ? 0x55FF55 : 0xFFFFFF, false);
        }
        if (total == 0) {
            g.drawString(this.font, "No matches", x + 8, listTop + 4, 0xFF888888, false);
        }
        if (total > DEP_MAX_VISIBLE) {
            if (depositScroll > 0) g.drawString(this.font, "▲", x + DEP_MENU_W - 11, listTop, 0xFFFFFF, false);
            if (depositScroll + DEP_MAX_VISIBLE < total) g.drawString(this.font, "▼", x + DEP_MENU_W - 11, top + h - 10, 0xFFFFFF, false);
        }
    }

    private void drawDepositRowTooltip(GuiGraphics g, int mouseX, int mouseY) {
        List<ClientDepositTargets.Target> list = depositFiltered();
        int total = list.size();
        int visible = Math.min(DEP_MAX_VISIBLE, total);
        int listTop = depositListTop();
        for (int r = 0; r < visible; r++) {
            int idx = r + depositScroll;
            if (idx >= total) break;
            int ry = listTop + r * DEP_ROW_H;
            if (mouseY >= ry - 1 && mouseY <= ry + DEP_ROW_H - 3) {
                ClientDepositTargets.Target target = list.get(idx);
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(target.label()).withStyle(ChatFormatting.WHITE));
                if (target.totalSlots() >= 0) {
                    lines.add(Component.literal(target.freeSlots() + "/" + target.totalSlots() + " slots available")
                            .withStyle(ChatFormatting.GRAY));
                }
                g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }
    }

    private void drawDepositTooltip(GuiGraphics g, int mouseX, int mouseY) {
        ClientDepositTargets.Target selected = ClientDepositTargets.selected();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Deposit results").withStyle(ChatFormatting.WHITE));
        lines.add(Component.literal("Into: " + selected.label()).withStyle(ChatFormatting.GREEN));
        lines.add(Component.literal("Click to choose a container").withStyle(ChatFormatting.AQUA));
        g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private ItemStack depositIcon(ClientDepositTargets.Target target) {
        return target.id().equals("self") ? PlayerHeadIcon.get() : target.icon();
    }

    private boolean overDepositBox(double mouseX, double mouseY) {
        return mouseX >= DEP_X && mouseX <= DEP_X + DEP_W && mouseY >= DEP_Y && mouseY <= DEP_Y + DEP_H;
    }

    private boolean overDepositMenu(double mouseX, double mouseY) {
        if (!depositMenuOpen) return false;
        int visible = Math.min(DEP_MAX_VISIBLE, depositFiltered().size());
        int top = DEP_Y + DEP_H + 2;
        int h = DEP_HEADER_H + DEP_SEARCH_H + Math.max(1, visible) * DEP_ROW_H + 6;
        return mouseX >= DEP_X && mouseX <= DEP_X + DEP_MENU_W && mouseY >= top && mouseY <= top + h;
    }

    private void toggleDepositMenu() {
        depositMenuOpen = !depositMenuOpen;
        if (depositMenuOpen) {
            depositScroll = 0;
            depositSearch = "";
            this.setFocused(null);
            com.sxilverr.quickcraft.network.QuickCraftNetwork.requestDepositTargets();
        }
    }

    private void handleDepositRowClick(double mouseY) {
        List<ClientDepositTargets.Target> list = depositFiltered();
        int total = list.size();
        int visible = Math.min(DEP_MAX_VISIBLE, total);
        int listTop = depositListTop();
        for (int r = 0; r < visible; r++) {
            int idx = r + depositScroll;
            if (idx >= total) break;
            int ry = listTop + r * DEP_ROW_H;
            if (mouseY >= ry - 1 && mouseY <= ry + DEP_ROW_H - 3) {
                ClientDepositTargets.select(list.get(idx).id());
                playClick();
                depositMenuOpen = false;
                return;
            }
        }
    }

    private void updateControlsHover(int mouseX, int mouseY) {
        boolean over = overControlsArea(mouseX, mouseY);
        if (over != controlsHovered) {
            controlsHovered = over;
            controlsAnimStart = Util.getMillis();
        }
        if (over && historyOpen) historyOpen = false;
    }

    private boolean overControlsTab(double mouseX, double mouseY) {
        int y = this.height - 52;
        return mouseX >= CT_X && mouseX <= CT_X + CT_W && mouseY >= y && mouseY <= y + CT_H;
    }

    private boolean overControlsPanelRect(double mouseX, double mouseY) {
        int bottom = this.height - 54;
        int top = bottom - controlsPanelHeight();
        return mouseX >= CT_X && mouseX <= CT_X + CTRL_W && mouseY >= top && mouseY <= bottom;
    }

    private boolean overControlsArea(double mouseX, double mouseY) {
        return overControlsTab(mouseX, mouseY) || (controlsHovered && overControlsPanelRect(mouseX, mouseY));
    }

    private int controlsPanelHeight() {
        return 5 + 5 * 15 + 12 + 5 + CONTROL_LINES.length * 11 + 6;
    }

    private void renderControlsTab(GuiGraphics g, int mouseX, int mouseY) {
        int y = this.height - 52;
        boolean hover = overControlsArea(mouseX, mouseY);
        g.fill(CT_X, y, CT_X + CT_W, y + CT_H, hover ? 0xFF3A3A3A : 0xF0202020);
        g.renderOutline(CT_X, y, CT_W, CT_H, hover ? 0xFFAAAAAA : 0xFF555555);
        int tw = this.font.width("Controls");
        g.drawString(this.font, "Controls", CT_X + (CT_W - tw) / 2, y + 6, hover ? 0xFFFFFF : 0xFFCFCFCF, false);
    }

    private void renderControlsPanel(GuiGraphics g) {
        long elapsed = Util.getMillis() - controlsAnimStart;
        double p = clamp(elapsed / (double) CONTROLS_SLIDE);
        double slide = controlsHovered ? easeOut(p) : 1 - easeOut(p);
        if (slide <= 0.001) return;

        int h = controlsPanelHeight();
        int x = CT_X;
        int restBottom = this.height - 54;
        int top = restBottom - h + (int) ((1 - slide) * (h + 40));

        g.enableScissor(x, 0, x + CTRL_W, restBottom);
        g.fill(x, top, x + CTRL_W, top + h, 0xF0080808);
        g.renderOutline(x, top, CTRL_W, h, 0xFF555555);

        int pad = 5;
        int[] colors = {COLOR_ROOT, COLOR_HAVE, COLOR_CRAFT, COLOR_MISSING, COLOR_DISABLED};
        String[] labels = {"End result item", "Items you have", "Items being crafted", "Missing materials",
                "Can't craft (no station)"};
        int y = top + pad;
        for (int i = 0; i < colors.length; i++) {
            g.fill(x + pad, y, x + CTRL_W - pad, y + 12, colors[i]);
            g.drawString(this.font, labels[i], x + pad + 5, y + 2, 0xFFFFFFFF, true);
            y += 15;
        }
        g.drawString(this.font, "Dimmed tab = branch can't be finished", x + pad, y, 0xFF808080, false);
        y += 12;
        g.fill(x + pad, y, x + CTRL_W - pad, y + 1, 0x40FFFFFF);
        y += 4;
        for (String line : CONTROL_LINES) {
            g.drawString(this.font, line, x + pad, y, 0xFFC8C8C8, false);
            y += 11;
        }
        g.disableScissor();
    }

    private boolean overHistoryTab(double mouseX, double mouseY) {
        int y = this.height - 52;
        return mouseX >= HIST_X && mouseX <= HIST_X + HIST_W && mouseY >= y && mouseY <= y + HIST_H;
    }

    private int historyPanelTop() {
        int visible = Math.min(HIST_MAX_VISIBLE, historyEntries.size());
        int h = HIST_HEADER_H + Math.max(1, visible) * HIST_ROW_H + 6;
        return this.height - 54 - h;
    }

    private boolean overHistoryPanel(double mouseX, double mouseY) {
        if (!historyOpen) return false;
        return mouseX >= HIST_X && mouseX <= HIST_X + HIST_MENU_W
                && mouseY >= historyPanelTop() && mouseY <= this.height - 54;
    }

    private int historyRowAt(double mouseX, double mouseY) {
        int listTop = historyPanelTop() + HIST_HEADER_H + 3;
        int visible = Math.min(HIST_MAX_VISIBLE, historyEntries.size());
        for (int r = 0; r < visible; r++) {
            int idx = r + historyScroll;
            if (idx >= historyEntries.size()) break;
            int ry = listTop + r * HIST_ROW_H;
            if (mouseX >= HIST_X + 2 && mouseX <= HIST_X + HIST_MENU_W - 2
                    && mouseY >= ry - 1 && mouseY <= ry + HIST_ROW_H - 3) {
                return idx;
            }
        }
        return -1;
    }

    private void renderHistoryTab(GuiGraphics g, int mouseX, int mouseY) {
        int y = this.height - 52;
        boolean hover = historyOpen || overHistoryTab(mouseX, mouseY);
        g.fill(HIST_X, y, HIST_X + HIST_W, y + HIST_H, hover ? 0xFF3A3A3A : 0xF0202020);
        g.renderOutline(HIST_X, y, HIST_W, HIST_H, hover ? 0xFFAAAAAA : 0xFF555555);
        int tw = this.font.width("History");
        g.drawString(this.font, "History", HIST_X + (HIST_W - tw) / 2, y + 6, hover ? 0xFFFFFF : 0xFFCFCFCF, false);
    }

    private void renderHistoryPanel(GuiGraphics g, int mouseX, int mouseY) {
        int total = historyEntries.size();
        int visible = Math.min(HIST_MAX_VISIBLE, total);
        int maxScroll = Math.max(0, total - HIST_MAX_VISIBLE);
        historyScroll = Math.max(0, Math.min(historyScroll, maxScroll));
        int x = HIST_X;
        int bottom = this.height - 54;
        int top = historyPanelTop();
        g.fill(x, top, x + HIST_MENU_W, bottom, 0xF0080808);
        g.fill(x, top, x + HIST_MENU_W, top + HIST_HEADER_H, 0xF0202020);
        g.drawString(this.font, "Crafting history", x + 5, top + 3, 0xFFFFFF, false);
        int listTop = top + HIST_HEADER_H + 3;
        if (total == 0) {
            g.drawString(this.font, "No history yet", x + 8, listTop + 4, 0xFF888888, false);
            return;
        }
        for (int r = 0; r < visible; r++) {
            int idx = r + historyScroll;
            if (idx >= total) break;
            CraftHistory.Entry entry = historyEntries.get(idx);
            ItemStack stack = entry.stack();
            int ry = listTop + r * HIST_ROW_H;
            boolean rowHover = mouseX >= x + 2 && mouseX <= x + HIST_MENU_W - 2 && mouseY >= ry - 1 && mouseY <= ry + HIST_ROW_H - 3;
            if (rowHover) g.fill(x + 2, ry - 1, x + HIST_MENU_W - 2, ry + HIST_ROW_H - 3, 0x40FFFFFF);
            g.renderItem(stack, x + 4, ry - 1);
            g.drawString(this.font, trim(stack.getHoverName().getString(), 24) + " x" + entry.count(), x + 24, ry + 3, 0xFFFFFF, false);
        }
        if (total > HIST_MAX_VISIBLE) {
            if (historyScroll > 0) g.drawString(this.font, "▲", x + HIST_MENU_W - 11, listTop, 0xFFFFFF, false);
            if (historyScroll + HIST_MAX_VISIBLE < total) g.drawString(this.font, "▼", x + HIST_MENU_W - 11, bottom - 10, 0xFFFFFF, false);
        }
    }

    private String ellipsize(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        int ellipsisWidth = this.font.width("…");
        String head = this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - ellipsisWidth));
        return head + "…";
    }

    private void drawNodeScaled(GuiGraphics g, NodeView view, float scale) {
        float cx = view.x + view.width / 2.0F;
        float cy = view.y + NodeView.HEIGHT / 2.0F;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.pose().translate(-cx, -cy, 0);
        drawNode(g, view);
        g.pose().popPose();
    }

    private void drawTreeAnimated(GuiGraphics g) {
        long elapsed = Util.getMillis() - treeAnimStart;
        animData.clear();
        NodeView rootView = layout.views.get(root);
        computeAnim(root, rootView.x + rootView.width / 2.0, rootView.y + NodeView.HEIGHT / 2.0, elapsed);

        for (NodeView view : layout.ordered) {
            double[] pa = animData.get(view.node);
            if (pa == null) continue;
            for (CraftNode child : view.node.children) {
                double[] ca = animData.get(child);
                if (ca == null || ca[2] <= 0.001) continue;
                drawEdge(g, view, layout.views.get(child), ca[2]);
            }
        }
        boolean bulge = QuickCraftConfig.hoverBulge() && hoveredView != null;
        for (NodeView view : layout.ordered) {
            double[] a = animData.get(view.node);
            if (a == null || a[2] <= 0.001) continue;
            double s = bulge && view == hoveredView ? a[2] * HOVER_BULGE : a[2];
            g.pose().pushPose();
            g.pose().translate(a[0], a[1], 0);
            g.pose().scale((float) s, (float) s, 1.0F);
            g.pose().translate(-(view.x + view.width / 2.0), -(view.y + NodeView.HEIGHT / 2.0), 0);
            drawNode(g, view);
            g.pose().popPose();
        }
    }

    private void computeAnim(CraftNode node, double parentCx, double parentCy, long elapsed) {
        NodeView v = layout.views.get(node);
        if (v == null) return;
        int depth = node.depth;
        double fcx = v.x + v.width / 2.0;
        double fcy = v.y + NodeView.HEIGHT / 2.0;
        double p = clamp((elapsed - depth * TREE_DEPTH_DELAY) / (double) TREE_NODE_SLIDE);
        double e = easeOut(p);
        double cx = parentCx + (fcx - parentCx) * e;
        double cy = parentCy + (fcy - parentCy) * e;
        animData.put(node, new double[]{cx, cy, e});
        for (CraftNode child : node.children) {
            computeAnim(child, cx, cy, elapsed);
        }
    }

    private static double clamp(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static double easeOut(double p) {
        double inv = 1 - p;
        return 1 - inv * inv * inv;
    }

    private static double easeIn(double p) {
        return p * p * p;
    }

    private void renderSummary(GuiGraphics g, int mouseX, int mouseY) {
        boolean opening = showSummary;
        long now = Util.getMillis();
        boolean swapping = animate && showSummary && (now - summarySwapStart) < 2 * SUMMARY_SWAP;
        long elapsed = animate ? now - summaryAnimStart : 0;

        int px = summaryPanelX();
        int top = 26;
        int bottom = this.height - 58;
        g.fill(px, top, px + PANEL_W, bottom, 0xF0080808);
        g.fill(px, top, px + PANEL_W, top + 15, 0xF0202020);
        g.drawString(this.font, "Ingredients (" + summaryItems.size() + ")", px + 6, top + 4, 0xFFFFFF, false);

        int pinX = px + PANEL_W - 30;
        int pinY = top + 2;
        boolean pinned = BookmarkOverlay.isActive();
        g.fill(pinX, pinY, pinX + TAB_BTN_W, pinY + 11, pinned ? 0xFF4A3A10 : 0x80000000);
        drawTabLabel(g, "Pin", pinX, pinY, pinned ? COLOR_CRAFT : 0xFFB0B0B0);

        int copyX = px + PANEL_W - 60;
        boolean copyHover = overCopyButton(mouseX, mouseY);
        g.fill(copyX, pinY, copyX + TAB_BTN_W, pinY + 11, (copyMenuOpen || copyHover) ? 0xFF3A3A3A : 0x80000000);
        drawTabLabel(g, "Copy", copyX, pinY, 0xFFB0B0B0);

        int listTop = top + 17;
        int maxRows = Math.max(0, (bottom - listTop) / PANEL_ROW_H);
        int maxScroll = Math.max(0, summaryItems.size() - maxRows);
        summaryScroll = Math.max(0, Math.min(summaryScroll, maxScroll));
        int visN = Math.min(maxRows, Math.max(0, summaryItems.size() - summaryScroll));
        summaryRowCount = visN;

        g.enableScissor(px, listTop, px + PANEL_W, bottom);
        if (swapping) {
            long se = now - summarySwapStart;
            int outDx = (int) (easeIn(clamp(se / (double) SUMMARY_SWAP)) * PANEL_W);
            int inDx = (int) ((1 - easeOut(clamp((se - SUMMARY_SWAP) / (double) SUMMARY_SWAP))) * PANEL_W);
            drawSummaryList(g, outgoingItems, px, listTop, maxRows, outgoingScroll, outDx);
            drawSummaryList(g, summaryItems, px, listTop, maxRows, summaryScroll, inDx);
        } else {
            if (!outgoingItems.isEmpty()) outgoingItems = List.of();
            for (int r = 0; r < maxRows; r++) {
                int idx = r + summaryScroll;
                if (idx >= summaryItems.size()) break;
                int dx = rowOffset(opening, elapsed, r, visN);
                drawSummaryRow(g, summaryItems.get(idx), px, listTop + r * PANEL_ROW_H, dx);
            }
        }
        g.disableScissor();

        if (copyMenuOpen) {
            int dx = copyDropX();
            int dy = copyDropY();
            int dh = COPY_OPTIONS.length * COPY_ROW_H + 2;
            g.fill(dx, dy, dx + COPY_DROP_W, dy + dh, 0xF0080808);
            g.renderOutline(dx, dy, COPY_DROP_W, dh, 0xFF555555);
            for (int r = 0; r < COPY_OPTIONS.length; r++) {
                int ry = dy + 1 + r * COPY_ROW_H;
                boolean sel = r == copySelected;
                boolean hov = mouseX >= dx && mouseX <= dx + COPY_DROP_W && mouseY >= ry && mouseY <= ry + COPY_ROW_H;
                if (sel) g.fill(dx + 1, ry, dx + COPY_DROP_W - 1, ry + COPY_ROW_H, 0x5000FF00);
                else if (hov) g.fill(dx + 1, ry, dx + COPY_DROP_W - 1, ry + COPY_ROW_H, 0x40FFFFFF);
                g.drawString(this.font, COPY_OPTIONS[r], dx + 6, ry + 3, sel ? 0x55FF55 : 0xFFFFFF, false);
            }
        }
    }

    private void drawTabLabel(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(this.font, text, x + (TAB_BTN_W - this.font.width(text)) / 2, y + 2, color, false);
    }

    private int summaryPx() {
        return this.width - PANEL_W - 6;
    }

    private int summaryPanelX() {
        long now = Util.getMillis();
        boolean swapping = animate && showSummary && (now - summarySwapStart) < 2 * SUMMARY_SWAP;
        long elapsed = animate ? now - summaryAnimStart : 0;
        long rowsSpan = (long) Math.max(0, summaryRowCount - 1) * ROW_STAGGER + ROW_SLIDE;
        double containerFrac;
        if (!animate || swapping) {
            containerFrac = 1.0;
        } else if (showSummary) {
            containerFrac = easeOut(clamp(elapsed / (double) SUMMARY_SLIDE));
        } else {
            containerFrac = 1 - easeOut(clamp((elapsed - rowsSpan) / (double) SUMMARY_SLIDE));
        }
        return summaryPx() + (int) ((1 - containerFrac) * (PANEL_W + 12));
    }

    private boolean overCopyButton(double mx, double my) {
        if (!showSummary) return false;
        int copyX = summaryPx() + PANEL_W - 60;
        return mx >= copyX && mx <= copyX + TAB_BTN_W && my >= 28 && my <= 39;
    }

    private int copyDropX() {
        return summaryPx() + PANEL_W - 4 - COPY_DROP_W;
    }

    private int copyDropY() {
        return 40;
    }

    private int copyRowAt(double mx, double my) {
        int dx = copyDropX();
        int dy = copyDropY();
        if (mx < dx || mx > dx + COPY_DROP_W) return -1;
        for (int r = 0; r < COPY_OPTIONS.length; r++) {
            int ry = dy + 1 + r * COPY_ROW_H;
            if (my >= ry && my <= ry + COPY_ROW_H) return r;
        }
        return -1;
    }

    private void doCopy(int mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("Quick Craft - ").append(maxMode ? "Max" : String.valueOf(quantity)).append("x ")
                .append(target.getHoverName().getString()).append(":\n");
        for (Map.Entry<ItemKey, Integer> e : summaryItems) {
            int need = e.getValue();
            int have = haveCounts.getOrDefault(e.getKey(), 0);
            String name = e.getKey().toStack(1).getHoverName().getString();
            if (mode == 0) {
                sb.append("- ").append(need).append("x ").append(name).append('\n');
            } else if (mode == 1) {
                if (have <= 0) sb.append("- ").append(need).append("x ").append(name).append('\n');
            } else if (have < need) {
                sb.append("- ").append(name).append(": ").append(have).append('/').append(need)
                        .append(" (need ").append(need - have).append(" more)\n");
            }
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(sb.toString().stripTrailing());
        playClick();
    }

    private void drawSummaryList(GuiGraphics g, List<Map.Entry<ItemKey, Integer>> items,
                                 int px, int listTop, int maxRows, int scroll, int dx) {
        for (int r = 0; r < maxRows; r++) {
            int idx = r + scroll;
            if (idx >= items.size()) break;
            drawSummaryRow(g, items.get(idx), px, listTop + r * PANEL_ROW_H, dx);
        }
    }

    private void drawSummaryRow(GuiGraphics g, Map.Entry<ItemKey, Integer> entry, int px, int ry, int dx) {
        ItemStack stack = displayStack(entry.getKey(), entry.getKey().toStack(1));
        int need = entry.getValue();
        int have = haveCounts.getOrDefault(entry.getKey(), 0);
        int fromEmc = emcSupplied.getOrDefault(entry.getKey(), 0);
        int rx = px + dx;
        g.renderItem(stack, rx + 4, ry + 1);
        g.renderItemDecorations(this.font, stack, rx + 4, ry + 1);
        g.drawString(this.font, trim(stack.getHoverName().getString(), 15), rx + 24, ry, 0xFFFFFF, false);
        Integer rowColor = summaryEmcColor.get(entry.getKey());
        boolean covered = have + fromEmc >= need || (rowColor != null && rowColor == COLOR_EMC);
        boolean viaEmc = have <= 0 && covered;
        int color = have >= need ? COLOR_HAVE : (covered ? COLOR_EMC : (have > 0 ? COLOR_CRAFT : COLOR_MISSING));
        g.drawString(this.font, viaEmc ? "EMC" : have + " / " + need, rx + 24, ry + 10, color, false);
        String emcText = summaryEmcText.get(entry.getKey());
        if (emcText != null) {
            int ex = rx + PANEL_W - 6 - this.font.width(emcText);
            g.drawString(this.font, emcText, ex, ry + 10, summaryEmcColor.getOrDefault(entry.getKey(), COLOR_EMC), false);
        }
    }

    private int rowOffset(boolean opening, long elapsed, int r, int visN) {
        if (!animate) return 0;
        double frac;
        if (opening) {
            frac = easeOut(clamp((elapsed - SUMMARY_SLIDE - (long) r * ROW_STAGGER) / (double) ROW_SLIDE));
        } else {
            long start = (long) (visN - 1 - r) * ROW_STAGGER;
            frac = 1 - easeOut(clamp((elapsed - start) / (double) ROW_SLIDE));
        }
        return (int) ((1 - frac) * PANEL_W);
    }

    private boolean summaryVisible() {
        if (showSummary) return true;
        if (!animate) return false;
        long elapsed = Util.getMillis() - summaryAnimStart;
        long total = (long) Math.max(0, summaryRowCount - 1) * ROW_STAGGER + ROW_SLIDE + SUMMARY_SLIDE;
        return elapsed < total;
    }

    private boolean overSummaryPanel(double mouseX, double mouseY) {
        int px = this.width - PANEL_W - 6;
        return mouseX >= px && mouseX <= px + PANEL_W && mouseY >= 26 && mouseY <= this.height - 58;
    }

    private boolean overSummaryPin(double mouseX, double mouseY) {
        int pinX = this.width - PANEL_W - 6 + PANEL_W - 30;
        int pinY = 28;
        return mouseX >= pinX && mouseX <= pinX + TAB_BTN_W && mouseY >= pinY && mouseY <= pinY + 11;
    }

    private int nodeWidth(CraftNode node) {
        if (node.isMobSource()) return mobNodeWidth(node);
        if (!QuickCraftConfig.sizeTabToFit()) return NodeView.WIDTH;
        String badge = node.alternatives.size() > 1 && !node.children.isEmpty()
                ? "[" + (node.selectedRecipe + 1) + "/" + node.alternatives.size() + "]" : "";
        int badgeExtra = badge.isEmpty() ? 0 : this.font.width(badge) + 4;
        int nameNeeded = 30 + this.font.width(node.output.getHoverName().getString()) + badgeExtra;

        RecipeOption recipe = node.selected();
        boolean hasStationIcon = recipe == null
                ? hasOriginIcon(node)
                : !node.owned && !StationIcons.icon(recipe.station()).isEmpty();
        StringBuilder sub = new StringBuilder();
        if (node != root) {
            sub.append(node.catalyst ? "keep " : "need ").append(node.requiredCount);
            if (node.isTagChoice()) sub.append(" •").append(node.tagOptions.size());
            if (node.cyclic) sub.append(" ~");
            if (node.reference != null) sub.append(" ^");
        }
        int subNeeded = 26 + this.font.width(sub.toString()) + (hasStationIcon ? 16 : 4);

        int needed = Math.max(nameNeeded, subNeeded);
        return Math.max(NodeView.WIDTH, Math.min(MAX_NODE_WIDTH, needed));
    }

    private int mobNodeWidth(CraftNode node) {
        int badge = node.mobSources.size() > 1 ? this.font.width("[9/9]") + 6 : 0;
        int max = 0;
        for (MobItemSource src : node.mobSources) {
            int nameW = this.font.width(src.mob.mobName);
            int labelW = this.font.width(mobDropLabel(src.drop));
            max = Math.max(max, Math.max(nameW, labelW));
        }
        int needed = 34 + max + badge;
        return Math.max(NodeView.WIDTH, Math.min(MAX_NODE_WIDTH, needed));
    }

    private String mobDropLabel(DropLine drop) {
        String label = drop.rangeLabel();
        if (!drop.chanceLabel.isEmpty()) {
            String pct = drop.chanceLabel.endsWith("%")
                    ? drop.chanceLabel.substring(0, drop.chanceLabel.length() - 1)
                    : drop.chanceLabel;
            label += "  (" + pct + "%)";
        }
        if (drop.looting) label += "  +Loot";
        return label;
    }

    private ItemStack stationIconFor(Station station) {
        if (detectedStations != null) {
            var source = detectedStations.sourceFor(station);
            if (source != null) return new ItemStack(source);
        }
        return StationIcons.icon(station);
    }

    private boolean emcCovered(CraftNode node) {
        ItemKey key = ItemKey.of(node.output);
        if (effectiveHave(key) >= node.requiredCount) return true;
        Integer color = summaryEmcColor.get(key);
        return color != null && color == COLOR_EMC;
    }

    private boolean isCompleted(CraftNode node) {
        if (node == root) return false;
        if (node.owned) return true;
        if (node.emcBuy) return emcCovered(node);
        if (node.isBlockedByStation()) return false;
        return nodeHave(node) >= node.requiredCount;
    }

    private ItemStack displayStack(CraftNode node) {
        if (node == root) return node.output;
        return displayStack(ItemKey.of(node.output), node.output);
    }

    private ItemStack displayStack(ItemKey key, ItemStack fallback) {
        ItemStack sample = damageSamples.get(key);
        return sample == null || sample.isEmpty() ? fallback : sample;
    }

    private ItemStack cornerIconFor(CraftNode node) {
        if (isCompleted(node)) {
            ItemStack source = sourceIcons.getOrDefault(ItemKey.of(node.output), ItemStack.EMPTY);
            return source.isEmpty() ? originIcon(node) : source;
        }
        RecipeOption recipe = node.selected();
        if (recipe == null) return originIcon(node);
        if (recipe.station() == Station.CRAFTING && recipe.fitsInventory()) return PlayerHeadIcon.get();
        return stationIconFor(recipe.station());
    }

    private List<OriginHint> originsFor(CraftNode node) {
        if (node.isCraftable() || node.isMobSource()) return List.of();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        return ItemOrigins.of(level.getRecipeManager(), level.registryAccess(), node.output);
    }

    private void primeOrigins(CraftNode node) {
        if (node == null) return;
        originsFor(node);
        for (CraftNode child : node.children) primeOrigins(child);
    }

    private boolean hasOriginIcon(CraftNode node) {
        for (OriginHint hint : originsFor(node)) {
            if (!hint.icon().isEmpty()) return true;
        }
        return false;
    }

    private ItemStack originIcon(CraftNode node) {
        List<OriginHint> hints = originsFor(node);
        int count = 0;
        for (OriginHint hint : hints) {
            if (!hint.icon().isEmpty()) count++;
        }
        if (count == 0) return ItemStack.EMPTY;
        int index = animate ? (int) ((Util.getMillis() / ORIGIN_CYCLE) % count) : 0;
        for (OriginHint hint : hints) {
            if (hint.icon().isEmpty()) continue;
            if (index-- == 0) return hint.icon();
        }
        return ItemStack.EMPTY;
    }

    private void drawNode(GuiGraphics g, NodeView view) {
        if (view.node.isMobSource()) {
            drawMobNode(g, view);
            return;
        }
        int sx = view.x;
        int sy = view.y;
        int w = view.width;

        int base = colorFor(view.node);
        boolean dimmed = base != COLOR_MISSING && isDimmed(view.node);
        int border = dimmed ? dim(base) : base;
        g.fill(sx - 1, sy - 1, sx + w + 1, sy + NodeView.HEIGHT + 1, border);
        g.fill(sx, sy, sx + w, sy + NodeView.HEIGHT, COLOR_NODE_BG);

        ItemStack icon = displayStack(view.node);
        g.renderItem(icon, sx + 5, sy + 7);
        g.renderItemDecorations(this.font, icon, sx + 5, sy + 7);

        ItemStack stationIcon = cornerIconFor(view.node);
        boolean hasStationIcon = !stationIcon.isEmpty();

        String badge = view.node.alternatives.size() > 1 && !view.node.children.isEmpty()
                ? "[" + (view.node.selectedRecipe + 1) + "/" + view.node.alternatives.size() + "]" : "";
        int nameWidth = w - 30;
        if (!badge.isEmpty()) {
            int badgeWidth = this.font.width(badge);
            g.drawString(this.font, badge, sx + w - 4 - badgeWidth, sy + 5, COLOR_CRAFT, false);
            nameWidth -= badgeWidth + 4;
        }
        String name = ellipsize(icon.getHoverName().getString(), nameWidth);
        int nameColor = (base == COLOR_DISABLED || dimmed) ? 0xFF9A9A9A : 0xFFFFFF;
        g.drawString(this.font, name, sx + 26, sy + 5, nameColor, false);

        StringBuilder sub = new StringBuilder();
        if (view.node != root) {
            sub.append(view.node.catalyst ? "keep " : "need ").append(view.node.requiredCount);
            if (view.node.isTagChoice()) sub.append(" •").append(view.node.tagOptions.size());
            if (view.node.cyclic) sub.append(" ~");
            if (view.node.reference != null) sub.append(" ^");
        }
        int subWidth = (hasStationIcon ? w - 42 : w - 30);
        String subText = this.font.plainSubstrByWidth(sub.toString(), subWidth);
        g.drawString(this.font, subText, sx + 26, sy + 17, 0xB0B0B0, false);

        if (hasStationIcon) {
            g.pose().pushPose();
            g.pose().translate(sx + w - 14, sy + NodeView.HEIGHT - 14, 0);
            g.pose().scale(0.72F, 0.72F, 1.0F);
            g.renderItem(stationIcon, 0, 0);
            g.pose().popPose();
        }
    }

    private void drawMobNode(GuiGraphics g, NodeView view) {
        int sx = view.x;
        int sy = view.y;
        int w = view.width;
        MobItemSource src = view.node.currentMob();

        g.fill(sx - 1, sy - 1, sx + w + 1, sy + NodeView.HEIGHT + 1, COLOR_MOB);
        g.fill(sx, sy, sx + w, sy + NodeView.HEIGHT, COLOR_NODE_BG);

        EntityIcon.render(g, sx + 1, sy + 1, 28, NodeView.HEIGHT - 2, src.mob.entityId);

        int textX = sx + 32;
        int textRight = sx + w - 4;
        if (view.node.mobSources.size() > 1) {
            String badge = "[" + (Math.floorMod(view.node.mobIndex, view.node.mobSources.size()) + 1)
                    + "/" + view.node.mobSources.size() + "]";
            int bw = this.font.width(badge);
            g.drawString(this.font, badge, textRight - bw, sy + 5, COLOR_MOB, false);
            textRight -= bw + 4;
        }
        String name = ellipsize(src.mob.mobName, Math.max(0, textRight - textX));
        g.drawString(this.font, name, textX, sy + 5, 0xFFFFFF, false);
        String sub = this.font.plainSubstrByWidth(mobDropLabel(src.drop), Math.max(0, sx + w - 4 - textX));
        g.drawString(this.font, sub, textX, sy + 17, 0xB0B0B0, false);
    }

    private void drawEdge(GuiGraphics g, NodeView parent, NodeView child, double alpha) {
        if (alpha <= 0.001) return;
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        int color = (a << 24) | (COLOR_EDGE & 0x00FFFFFF);
        int px = parent.x + parent.width;
        int py = parent.y + NodeView.HEIGHT / 2;
        int cx = child.x;
        int cy = child.y + NodeView.HEIGHT / 2;
        int midX = cx - layout.hGap / 2;
        g.hLine(Math.min(px, midX), Math.max(px, midX), py, color);
        g.vLine(midX, Math.min(py, cy), Math.max(py, cy), color);
        g.hLine(Math.min(midX, cx), Math.max(midX, cx), cy, color);
    }

    private void drawTooltip(GuiGraphics g, NodeView view, int mouseX, int mouseY) {
        CraftNode node = view.node;
        if (node.isMobSource()) {
            drawMobTooltip(g, node, mouseX, mouseY);
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(node.output.getHoverName());
        if (node == root) {
            lines.add(Component.literal("End result").withStyle(ChatFormatting.BLUE));
        }
        int have = haveCounts.getOrDefault(ItemKey.of(node.output), 0);
        int fromEmc = emcSupplied.getOrDefault(ItemKey.of(node.output), 0);
        String requiredLine = "Required: " + node.requiredCount;
        String requiredStacks = stackBreakdown(node.requiredCount, node.output.getMaxStackSize());
        if (!requiredStacks.isEmpty()) requiredLine += " " + requiredStacks;
        lines.add(Component.literal(requiredLine).withStyle(ChatFormatting.GRAY));
        String availLine = "Available: " + have;
        if (fromEmc > 0) availLine += " (+" + fromEmc + " from EMC)";
        if (node != root && node.freeStock < have) availLine += " (" + node.freeStock + " free after other uses)";
        lines.add(Component.literal(availLine).withStyle(ChatFormatting.GRAY));
        if (node.catalyst) {
            lines.add(Component.literal("Catalyst - kept, not consumed").withStyle(ChatFormatting.GOLD));
        }
        ItemStack wear = displayStack(node);
        if (wear.isDamaged()) {
            int left = wear.getMaxDamage() - wear.getDamageValue();
            lines.add(Component.literal("Durability: " + left + " / " + wear.getMaxDamage()).withStyle(ChatFormatting.GRAY));
        }
        if (isCompleted(node)) {
            String from;
            if (have < node.requiredCount && fromEmc > 0) {
                from = "EMC";
            } else {
                ItemStack src = sourceIcons.get(ItemKey.of(node.output));
                from = (src != null && !src.isEmpty()) ? src.getHoverName().getString() : "your inventory";
            }
            lines.add(Component.literal("You have enough - using " + from).withStyle(ChatFormatting.GREEN));
        }
        if (node.isBlockedByStation()) {
            lines.add(Component.literal("Needs a " + StationIcons.name(node.requiredStation()) + " nearby").withStyle(ChatFormatting.RED));
        }
        if (node.isTagChoice()) {
            lines.add(Component.literal("Accepts (W/S or Up/Down to switch):").withStyle(ChatFormatting.AQUA));
            int shown = 0;
            for (ItemStack option : node.tagOptions) {
                if (shown++ >= 8) {
                    lines.add(Component.literal("   … +" + (node.tagOptions.size() - 8) + " more").withStyle(ChatFormatting.DARK_GRAY));
                    break;
                }
                boolean current = option.getItem() == node.output.getItem();
                int owned = haveCounts.getOrDefault(ItemKey.of(option), 0);
                ChatFormatting fmt = current ? ChatFormatting.WHITE : (owned > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY);
                String prefix = current ? " ▶ " : "    ";
                String suffix = owned > 0 ? " (" + owned + ")" : "";
                lines.add(Component.literal(prefix + option.getHoverName().getString() + suffix).withStyle(fmt));
            }
        }
        if (node.emcBuy) {
            long unit = emcSource == null ? 0L : emcSource.value(node.output);
            String each = unit > 0L ? " - " + ProjectEClient.format(BigInteger.valueOf(unit)) + " each" : "";
            lines.add(Component.literal("Bought with EMC" + each).withStyle(ChatFormatting.AQUA));
            if (!isCompleted(node)) lines.add(Component.literal("Not enough EMC").withStyle(ChatFormatting.RED));
        }
        if (node.reference != null) {
            lines.add(Component.literal("Recipe expanded at its first use in this tree").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (node.truncated) {
            lines.add(Component.literal("Tree limit reached - raise maxTreeNodes or maxTreeDepth in the config").withStyle(ChatFormatting.RED));
        }
        if (node.isCraftable()) {
            RecipeOption selected = node.selected();
            if (selected != null) {
                lines.add(Component.literal("Recipe: " + selected.id()).withStyle(ChatFormatting.DARK_GRAY));
                lines.add(Component.literal("Yields " + node.resultPerCraft + " per craft (" + StationIcons.name(selected.station()) + ")").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                lines.add(Component.literal("Supplying yourself (not crafted)").withStyle(ChatFormatting.YELLOW));
            }
            if (node.alternatives.size() > 1) {
                lines.add(Component.literal("Left-click: swap recipe (" + node.alternatives.size() + " options)").withStyle(ChatFormatting.AQUA));
            }
            if (node != root && !node.truncated && node.reference == null && !node.emcBuy) {
                boolean expanded = !node.children.isEmpty();
                lines.add(Component.literal(expanded ? "Right-click: hide recipe (supply yourself)" : "Right-click: reveal recipe")
                        .withStyle(ChatFormatting.AQUA));
            }
        } else {
            lines.add(Component.literal("Base material - no recipe").withStyle(ChatFormatting.DARK_GRAY));
            String origins = originLabel(node);
            if (!origins.isEmpty()) {
                lines.add(Component.literal("Made in: " + origins).withStyle(ChatFormatting.GRAY));
            }
        }
        if (!ItemKey.of(node.output).equals(ItemKey.of(target))) {
            lines.add(Component.literal("Middle-click: view this item's tree").withStyle(ChatFormatting.AQUA));
        }
        g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private String originLabel(CraftNode node) {
        List<String> labels = new ArrayList<>();
        for (OriginHint hint : originsFor(node)) {
            if (hint.label() != null && !hint.label().isEmpty()) labels.add(hint.label());
        }
        if (labels.isEmpty()) return "";
        int shown = Math.min(labels.size(), MAX_ORIGIN_LABELS);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            if (i > 0) out.append(", ");
            out.append(labels.get(i));
        }
        if (labels.size() > shown) out.append(", +").append(labels.size() - shown).append(" more");
        return out.toString();
    }

    private void drawMobTooltip(GuiGraphics g, CraftNode node, int mouseX, int mouseY) {
        MobItemSource src = node.currentMob();
        MobDropInfo mob = src.mob;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(mob.mobName).withStyle(ChatFormatting.WHITE));
        lines.add(Component.literal("Drops " + node.output.getHoverName().getString() + ": " + mobDropLabel(src.drop))
                .withStyle(ChatFormatting.GRAY));
        if (!mob.biomes.isEmpty()) {
            List<String> shownBiomes = mob.biomes.size() > 6 ? mob.biomes.subList(0, 6) : mob.biomes;
            String biomes = String.join(", ", shownBiomes);
            if (mob.biomes.size() > 6) biomes += ", …";
            lines.add(Component.literal("Biomes: " + biomes).withStyle(ChatFormatting.GRAY));
        }
        if (mob.lightLevel != null && !mob.lightLevel.isEmpty()) {
            lines.add(Component.literal(mob.lightLevel).withStyle(ChatFormatting.GRAY));
        }
        if (mob.exp != null && !mob.exp.isEmpty()) {
            lines.add(Component.literal("Experience: " + mob.exp).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.literal("All drops:").withStyle(ChatFormatting.AQUA));
        int shown = 0;
        for (DropLine drop : mob.drops) {
            if (shown++ >= 12) {
                lines.add(Component.literal("   … +" + (mob.drops.size() - 12) + " more").withStyle(ChatFormatting.DARK_GRAY));
                break;
            }
            lines.add(Component.literal("  " + drop.item.getHoverName().getString() + "  " + mobDropLabel(drop))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (node.mobSources.size() > 1) {
            lines.add(Component.literal("Left-click: next mob (" + node.mobSources.size() + ")").withStyle(ChatFormatting.AQUA));
        }
        g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private int colorFor(CraftNode node) {
        if (node.isMobSource()) return COLOR_MOB;
        if (node == root) return COLOR_ROOT;
        if (!node.craftReachable) return COLOR_DISABLED;
        if (node.owned) return COLOR_HAVE;
        if (node.emcBuy) return emcCovered(node) ? COLOR_EMC : COLOR_MISSING;
        int have = nodeHave(node);
        if (have >= node.requiredCount) return COLOR_HAVE;
        if (node.selected() == null || node.truncated) return COLOR_MISSING;
        return COLOR_CRAFT;
    }

    private boolean isDimmed(CraftNode node) {
        return node != root && node.craftReachable && !achievable(node);
    }

    private static int dim(int argb) {
        int a = argb >>> 24;
        int r = (int) (((argb >> 16) & 0xFF) * 0.42);
        int g = (int) (((argb >> 8) & 0xFF) * 0.42);
        int b = (int) ((argb & 0xFF) * 0.42);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private boolean achievable(CraftNode node) {
        Boolean cached = achievableCache.get(node);
        if (cached != null) return cached;
        achievableCache.put(node, Boolean.TRUE);
        boolean result = computeAchievable(node);
        achievableCache.put(node, result);
        return result;
    }

    private boolean computeAchievable(CraftNode node) {
        if (node.owned) return true;
        if (node.emcBuy) return emcCovered(node);
        if (node.reference != null) return achievable(node.reference);
        int have = nodeHave(node);
        if (have >= node.requiredCount) return true;
        if (!node.fitsStation) return false;
        if (node.selected() == null || node.children.isEmpty()) return false;
        for (CraftNode child : node.children) {
            if (!achievable(child)) return false;
        }
        return true;
    }

    private NodeView nodeAt(double mouseX, double mouseY) {
        double wx = (mouseX - panX) / zoom;
        double wy = (mouseY - panY) / zoom;
        for (NodeView view : layout.ordered) {
            if (wx >= view.x && wx <= view.x + view.width && wy >= view.y && wy <= view.y + NodeView.HEIGHT) {
                return view;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overDepositBox(mouseX, mouseY)) {
            toggleDepositMenu();
            playClick();
            return true;
        }
        if (depositMenuOpen && button == 0) {
            if (overDepositMenu(mouseX, mouseY)) {
                handleDepositRowClick(mouseY);
                return true;
            }
            depositMenuOpen = false;
        }
        if (button == 0 && overControlsArea(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && overHistoryTab(mouseX, mouseY)) {
            historyOpen = !historyOpen;
            if (historyOpen) {
                historyEntries = CraftHistory.entries();
                historyScroll = 0;
            }
            playClick();
            return true;
        }
        if (historyOpen && button == 0) {
            if (overHistoryPanel(mouseX, mouseY)) {
                int idx = historyRowAt(mouseX, mouseY);
                if (idx >= 0 && idx < historyEntries.size()) {
                    ItemStack chosen = historyEntries.get(idx).stack().copy();
                    historyOpen = false;
                    playClick();
                    retarget(chosen, true);
                }
                return true;
            }
            historyOpen = false;
        }
        if (button == 0 && BookmarkOverlay.handleClick(this.width, this.height, mouseX, mouseY)) {
            playClick();
            return true;
        }
        if (button == 0 && BookmarkOverlay.isActive()
                && BookmarkOverlay.overHeaderDragZone(this.width, this.height, mouseX, mouseY)) {
            double[] origin = BookmarkOverlay.origin(this.width, this.height);
            bmGrabX = mouseX - origin[0];
            bmGrabY = mouseY - origin[1];
            bookmarkDragging = true;
            return true;
        }
        if (button == 0 && overCopyButton(mouseX, mouseY)) {
            copyMenuOpen = !copyMenuOpen;
            if (copyMenuOpen) copySelected = 0;
            playClick();
            return true;
        }
        if (button == 0 && copyMenuOpen) {
            int row = copyRowAt(mouseX, mouseY);
            if (row >= 0) {
                doCopy(row);
                copyMenuOpen = false;
                return true;
            }
            copyMenuOpen = false;
        }
        if (button == 0 && showSummary && overSummaryPin(mouseX, mouseY)) {
            BookmarkOverlay.set(summaryItems);
            playClick();
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (showSummary && overSummaryPanel(mouseX, mouseY)) return true;
        NodeView view = nodeAt(mouseX, mouseY);
        if (view != null) {
            handleNodeClick(view.node, button);
            return true;
        }
        if (button == 0) {
            panning = true;
            return true;
        }
        return false;
    }

    private void handleNodeClick(CraftNode node, int button) {
        if (node.isMobSource()) {
            if (button == 0 && node.mobSources.size() > 1) {
                node.mobIndex = Math.floorMod(node.mobIndex + 1, node.mobSources.size());
                mobSelection.put(ItemKey.of(node.output), node.mobIndex);
                playClick();
            }
            return;
        }
        ItemKey key = ItemKey.of(node.output);
        if (button == 2) {
            if (!key.equals(ItemKey.of(target))) {
                playClick();
                retarget(node.output.copy(), true);
            }
            return;
        }
        if (hasControlDown()) {
            boolean changed = overrides.remove(key) != null;
            RecipePreferences.clearRecipe(key);
            if (!node.tagSignature.isEmpty()) {
                if (ingredientChoices.remove(node.tagSignature) != null) changed = true;
                RecipePreferences.clearIngredient(node.tagSignature);
            }
            if (changed) {
                playClick();
                rebuild();
            }
            return;
        }
        if (button == 1) {
            if (node == root || !node.isCraftable()) return;
            boolean expanded = !node.children.isEmpty();
            net.minecraft.resources.ResourceLocation choice;
            if (expanded) {
                choice = TreeBuilder.MANUAL;
            } else if (node.autoRecipe >= 0) {
                choice = node.alternatives.get(node.autoRecipe).id();
            } else {
                return;
            }
            overrides.put(key, choice);
            RecipePreferences.setRecipe(key, choice);
            playClick();
            rebuild();
            return;
        }
        if (button == 0 && node.alternatives.size() > 1) {
            int current = Math.max(0, node.selectedRecipe);
            int next = (current + 1) % node.alternatives.size();
            net.minecraft.resources.ResourceLocation chosen = node.alternatives.get(next).id();
            overrides.put(key, chosen);
            RecipePreferences.setRecipe(key, chosen);
            playClick();
            rebuild();
        }
    }

    private void cycleTagOption(CraftNode node, int direction) {
        List<ItemStack> options = node.tagOptions;
        if (options.size() <= 1) return;
        int current = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).getItem() == node.output.getItem()) {
                current = i;
                break;
            }
        }
        int size = options.size();
        int next = ((current + direction) % size + size) % size;
        Item chosen = options.get(next).getItem();
        ingredientChoices.put(node.tagSignature, chosen);
        RecipePreferences.setIngredient(node.tagSignature, chosen);
        playClick();
        rebuild();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (bookmarkDragging && button == 0) {
            BookmarkOverlay.setOrigin(this.width, this.height, mouseX - bmGrabX, mouseY - bmGrabY);
            return true;
        }
        if (panning && button == 0) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            panning = false;
            bookmarkDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (depositMenuOpen && overDepositMenu(mouseX, mouseY)) {
            depositScroll = Math.max(0, depositScroll - (int) Math.signum(delta));
            return true;
        }
        if (controlsHovered && overControlsPanelRect(mouseX, mouseY)) {
            return true;
        }
        if (historyOpen && overHistoryPanel(mouseX, mouseY)) {
            historyScroll = Math.max(0, historyScroll - (int) Math.signum(delta));
            return true;
        }
        if (BookmarkOverlay.isActive() && BookmarkOverlay.overPanel(this.width, this.height, mouseX, mouseY)) {
            BookmarkOverlay.scroll(-(int) Math.signum(delta));
            return true;
        }
        if (showSummary && overSummaryPanel(mouseX, mouseY)) {
            summaryScroll = Math.max(0, summaryScroll - (int) Math.signum(delta));
            return true;
        }
        double previous = zoom;
        double factor = delta > 0 ? 1.1 : 1.0 / 1.1;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        if (zoom != previous) {
            double treeX = (mouseX - panX) / previous;
            double treeY = (mouseY - panY) / previous;
            panX = mouseX - treeX * zoom;
            panY = mouseY - treeY * zoom;
        }
        return true;
    }

    private boolean confirmPending;

    private void onConfirm() {
        Minecraft mc = Minecraft.getInstance();
        if (QuickCraftConfig.creativeBypass() && mc.player != null && mc.player.getAbilities().instabuild) {
            CraftHistory.record(target, quantity);
            com.sxilverr.quickcraft.network.QuickCraftNetwork.sendCraftRequest(target, quantity, overrides, ingredientChoices, ClientDepositTargets.selectedId());
            closeAfterCraft();
            return;
        }
        if (maxMode) {
            onCraftMax();
            return;
        }
        if (confirmPending) return;
        confirmPending = true;
        com.sxilverr.quickcraft.network.QuickCraftNetwork.sendCraftPreviewRequest(target, quantity, overrides, ingredientChoices);
    }

    public void onCraftPreviewResult(CraftPreview.Result preview) {
        confirmPending = false;
        if (preview.full()) {
            CraftHistory.record(target, quantity);
            com.sxilverr.quickcraft.network.QuickCraftNetwork.sendCraftRequest(target, quantity, overrides, ingredientChoices, ClientDepositTargets.selectedId());
            closeAfterCraft();
            return;
        }
        Minecraft.getInstance().setScreen(
                new ForceCraftConfirmScreen(this, target, quantity, overrides, ingredientChoices, preview));
    }

    private void onCraftMax() {
        CraftHistory.record(target, maxCraftable());
        com.sxilverr.quickcraft.network.QuickCraftNetwork.sendCraftRequest(target, CRAFT_MAX, overrides, ingredientChoices, ClientDepositTargets.selectedId());
        closeAfterCraft();
    }

    private static String stackBreakdown(int amount, int stackSize) {
        if (stackSize <= 1 || amount <= stackSize) return "";
        int stacks = amount / stackSize;
        int remainder = amount % stackSize;
        if (remainder == 0) return stacks + "(" + stackSize + ")";
        return stacks + "(" + stackSize + ") + " + remainder;
    }

    private static String trim(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
