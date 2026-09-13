package com.sxilverr.quickcraft.client;

import com.google.common.base.Predicate;
import com.sxilverr.quickcraft.QuickCraftConfig;
import com.sxilverr.quickcraft.craft.CraftPlanner;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.craft.EmcSource;
import com.sxilverr.quickcraft.craft.VirtualPool;
import com.sxilverr.quickcraft.crafting.Availability;
import com.sxilverr.quickcraft.crafting.CraftNode;
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
import com.sxilverr.quickcraft.integration.OriginHint;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import com.sxilverr.quickcraft.integration.jer.DropLine;
import com.sxilverr.quickcraft.integration.jer.JerIntegration;
import com.sxilverr.quickcraft.integration.jer.MobDropInfo;
import com.sxilverr.quickcraft.integration.jer.MobItemSource;
import com.sxilverr.quickcraft.integration.projecte.ProjectEClient;
import com.sxilverr.quickcraft.integration.projecte.ProjectESupport;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class QuickCraftScreen extends GuiScreen {
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
    private static final int EYE_W = 14;
    private static final int EYE_H = 12;
    private static final ResourceLocation VIEW_TREE = new ResourceLocation("quickcraft", "textures/gui/view_tree.png");
    private static final ResourceLocation HIDE_TREE = new ResourceLocation("quickcraft", "textures/gui/hide_tree.png");
    private static final String[] COPY_OPTIONS = {"Copy full list", "Copy missing materials", "Copy material shortages"};
    private static final String[] CONTROL_LINES = {
            "Drag/Scroll: pan/zoom",
            "Left-click: swap recipe",
            "Middle-click: view item's tree",
            "W/S or Up/Down: switch tag item",
            "Right-click: show/hide recipe",
            "Ctrl-click: reset preferences",
    };

    private static final int ID_MINUS = 10;
    private static final int ID_PLUS = 11;
    private static final int ID_CRAFT_MAX = 12;
    private static final int ID_CONFIRM = 13;
    private static final int ID_CLOSE = 14;
    private static final int ID_INGREDIENTS = 15;
    private static final int ID_REFRESH = 16;
    private static final int ID_SHOW_MOBS = 17;

    private ItemStack target;
    private int quantity;
    private final Map<ItemKey, Integer> mobSelection = new HashMap<ItemKey, Integer>();
    private final Map<ItemKey, ResourceLocation> overrides = new HashMap<ItemKey, ResourceLocation>();
    private final Map<String, Item> ingredientChoices = new HashMap<String, Item>();
    private final Map<ItemKey, Integer> haveCounts = new HashMap<ItemKey, Integer>();
    private final Map<ItemKey, ItemStack> sourceIcons = new HashMap<ItemKey, ItemStack>();
    private final Map<ItemKey, ItemStack> damageSamples = new HashMap<ItemKey, ItemStack>();

    private RecipeResolver resolver;
    private TreeBuilder builder;
    private CraftNode root;
    private TreeLayout layout;

    private double panX;
    private double panY;
    private double zoom = 1.0;
    private boolean panning;
    private double lastDragX;
    private double lastDragY;
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
    private List<Map.Entry<ItemKey, Integer>> summaryItems = Collections.emptyList();
    private List<Map.Entry<ItemKey, Integer>> outgoingItems = Collections.emptyList();
    private int outgoingScroll;
    private long summarySwapStart;

    private boolean animate;
    private long treeAnimStart;
    private long summaryAnimStart;
    private int summaryRowCount;
    private final Map<CraftNode, double[]> animData = new IdentityHashMap<CraftNode, double[]>();
    private final Map<CraftNode, Boolean> achievableCache = new IdentityHashMap<CraftNode, Boolean>();
    private final Map<CraftNode, Boolean> peekEmcCache = new IdentityHashMap<CraftNode, Boolean>();
    private int maxCraftableCache = -1;

    private boolean bookmarkDragging;
    private double bmGrabX;
    private double bmGrabY;

    private GuiTextField quantityBox;
    private String lastQuantityText = "";

    private final Deque<ItemStack> history = new ArrayDeque<ItemStack>();

    private boolean showMobs;
    private GuiButton showMobsButton;
    private GuiButton minusButton;
    private GuiButton plusButton;
    private GuiButton closeButton;
    private String emcTotalText;
    private String emcCostText;
    private boolean emcAffordable = true;
    private BigInteger lastEmc;
    private BigInteger emcRequired = BigInteger.ZERO;
    private boolean planFull;
    private int emcPollTicks;
    private static final int EMC_POLL_TICKS = 20;
    private final Map<ItemKey, Integer> emcSupplied = new HashMap<ItemKey, Integer>();
    private EmcSource emcSource;
    private List<CraftPlanner.Blocker> blockers = Collections.<CraftPlanner.Blocker>emptyList();
    private final Map<ItemKey, String> summaryEmcText = new HashMap<ItemKey, String>();
    private final Map<ItemKey, Integer> summaryEmcColor = new HashMap<ItemKey, Integer>();
    private static final BigInteger EMC_UNLIMITED = BigInteger.ONE.shiftLeft(96);
    private boolean historyOpen;
    private int historyScroll;
    private List<CraftHistory.Entry> historyEntries = Collections.emptyList();
    private final Set<ItemKey> peekOpen = new HashSet<ItemKey>();
    private final Map<ItemKey, Long> peekStarts = new HashMap<ItemKey, Long>();
    private boolean eyeHovered;
    private boolean confirmPending;

    public QuickCraftScreen(ItemStack target, int quantity) {
        this.target = target.copy();
        this.quantity = Math.max(1, quantity);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.fontRenderer = BatchedFontRenderer.get(this.mc);
        this.buttonList.clear();

        resolver = ClientRecipeCache.get();
        builder = new TreeBuilder(resolver, QuickCraftConfig.preferredItems(),
                QuickCraftConfig.maxTreeDepth(), QuickCraftConfig.maxTreeNodes());

        overrides.clear();
        overrides.putAll(RecipePreferences.recipeOverrides());
        ingredientChoices.clear();
        ingredientChoices.putAll(RecipePreferences.ingredientChoices());

        quantityBox = new GuiTextField(0, this.fontRenderer, this.width - 146, 4, 46, 16);
        quantityBox.setMaxStringLength(12);
        quantityBox.setValidator(new Predicate<String>() {
            @Override
            public boolean apply(String s) {
                if (s == null) return false;
                return s.matches("\\d{0,6}") || s.equalsIgnoreCase("max") || s.matches("(?i)max \\(\\d+\\)");
            }
        });
        quantityBox.setText(String.valueOf(quantity));
        lastQuantityText = quantityBox.getText();

        minusButton = new ScalingButton(ID_MINUS, this.width - 164, 4, 16, 16, "-");
        plusButton = new ScalingButton(ID_PLUS, this.width - 98, 4, 16, 16, "+");
        closeButton = new ScalingButton(ID_CLOSE, 8, this.height - 28, 60, 22, history.isEmpty() ? "Close" : "Back");

        this.buttonList.add(minusButton);
        this.buttonList.add(plusButton);
        this.buttonList.add(new ScalingButton(ID_CRAFT_MAX, this.width - 78, 4, 72, 16, "Craft Max"));
        this.buttonList.add(new ScalingButton(ID_CONFIRM, this.width - 124, this.height - 28, 116, 22, "Confirm Craft"));
        this.buttonList.add(closeButton);
        this.buttonList.add(new ScalingButton(ID_INGREDIENTS, this.width / 2 - 172, this.height - 52, 112, 20, "Ingredients"));
        this.buttonList.add(new ScalingButton(ID_REFRESH, this.width / 2 - 56, this.height - 52, 112, 20, "Refresh"));
        if (JerIntegration.available()) {
            showMobsButton = new ScalingButton(ID_SHOW_MOBS, this.width / 2 + 62, this.height - 52, 96, 20,
                    showMobs ? "Hide Mobs" : "Show Mobs");
            this.buttonList.add(showMobsButton);
        }

        loadColors();
        computeHaveCounts();
        baseQuantity = quantity;
        applyShiftState(isShiftKeyDown());
        autoFit();
        animate = QuickCraftConfig.animationsEnabled();
        treeAnimStart = now();
        peekStarts.clear();
        QuickCraftNetwork.requestDepositTargets();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        quantityBox.updateCursorCounter();

        String text = quantityBox.getText();
        if (!text.equals(lastQuantityText)) {
            lastQuantityText = text;
            onQuantityChanged(text);
        }

        boolean shift = isShiftKeyDown();
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
        if (QuickCraftConfig.showEmc()) emcTotalText = ProjectEClient.format(owned);
        computeSummaryEmc(summaryItems);
        peekEmcCache.clear();
    }

    private void close() {
        if (!history.isEmpty()) {
            retarget(history.pop(), false);
        } else {
            this.mc.displayGuiScreen(null);
        }
    }

    private void closeAfterCraft() {
        history.clear();
        this.mc.displayGuiScreen(null);
    }

    private void retarget(ItemStack newTarget, boolean pushHistory) {
        if (pushHistory) history.push(target.copy());
        if (animate && showSummary) {
            outgoingItems = summaryItems;
            outgoingScroll = summaryScroll;
            summarySwapStart = now();
        }
        target = newTarget.copy();
        quantity = 1;
        baseQuantity = 1;
        maxMode = false;
        summaryScroll = 0;
        hoveredView = null;
        panning = false;
        initGui();
    }

    private static long now() {
        return Minecraft.getSystemTime();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case ID_MINUS:
                stepQuantity(-1);
                break;
            case ID_PLUS:
                stepQuantity(1);
                break;
            case ID_CRAFT_MAX:
                onCraftMax();
                break;
            case ID_CONFIRM:
                onConfirm();
                break;
            case ID_CLOSE:
                close();
                break;
            case ID_INGREDIENTS:
                toggleSummary();
                break;
            case ID_REFRESH:
                onRefresh();
                break;
            case ID_SHOW_MOBS:
                toggleShowMobs();
                break;
            default:
                break;
        }
    }

    private void toggleSummary() {
        showSummary = !showSummary;
        if (!showSummary) copyMenuOpen = false;
        summaryAnimStart = now();
    }

    private void onRefresh() {
        playClick();
        computeHaveCounts();
        rebuild();
    }

    private void loadColors() {
        COLOR_HAVE = QuickCraftConfig.colorAvailable();
        COLOR_CRAFT = QuickCraftConfig.colorCrafted();
        COLOR_MISSING = QuickCraftConfig.colorMissing();
        COLOR_DISABLED = QuickCraftConfig.colorNoStation();
        COLOR_ROOT = QuickCraftConfig.colorTarget();
        COLOR_NODE_BG = QuickCraftConfig.colorNodeBackground();
        COLOR_EDGE = QuickCraftConfig.colorLines();
    }

    private void toggleShowMobs() {
        showMobs = !showMobs;
        if (showMobsButton != null) showMobsButton.displayString = showMobs ? "Hide Mobs" : "Show Mobs";
        hoveredView = null;
        playClick();
        if (root != null) {
            if (showMobs && JerIntegration.available()) attachMobSources(root);
            else detachMobSources(root);
            achievableCache.clear();
            peekEmcCache.clear();
            layout = new TreeLayout(root, widthFn());
        }
    }

    private void applyShiftState(boolean shift) {
        shiftActive = shift;
        maxMode = shift && QuickCraftConfig.shiftCraftIsMax();
        quantity = shift ? (maxMode ? CRAFT_MAX : QuickCraftConfig.shiftCraftAmount()) : baseQuantity;
        suppressResponder = true;
        quantityBox.setText(maxMode ? "Max" : String.valueOf(quantity));
        lastQuantityText = quantityBox.getText();
        quantityBox.setEnabled(!shift);
        if (minusButton != null) minusButton.enabled = !shift;
        if (plusButton != null) plusButton.enabled = !shift;
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
        quantityBox.setText(String.valueOf(next));
    }

    private static int safeParse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private TreeLayout.WidthFn widthFn() {
        return new TreeLayout.WidthFn() {
            @Override
            public int widthOf(CraftNode node) {
                return nodeWidth(node);
            }
        };
    }

    private void rebuild() {
        if (builder == null) return;
        Stations stations = serverStations != null ? serverStations : StationScan.detect(this.mc.world, this.mc.player);
        detectedStations = stations;
        boolean collapse = QuickCraftConfig.collapseOwnedItems();
        boolean hideLoop = QuickCraftConfig.hideLoopingRecipes();
        emcSource = ProjectEClient.session(this.mc.player, QuickCraftConfig.containerScanRange());
        int qty = quantity;
        if (maxMode) {
            qty = Math.max(1, planFor(CRAFT_MAX, stations, collapse, hideLoop, null, false).craftable());
            if (quantityBox != null) {
                suppressResponder = true;
                quantityBox.setText("Max (" + qty + ")");
                lastQuantityText = quantityBox.getText();
                suppressResponder = false;
            }
        }
        CraftPlanner.Plan plan = planFor(qty, stations, collapse, hideLoop, null, true);
        root = plan.root();
        blockers = plan.blockers();
        Set<ItemKey> requestKeys = relevantKeys();
        applyEmcPlan(plan, qty, stations, collapse, hideLoop);

        if (showMobs && JerIntegration.available()) attachMobSources(root);
        primeOrigins(root);
        Station missing = CraftTrees.missingStation(root);
        if (missing != missingStation) stationSelectedIndex = 0;
        stationProblem = missing != null;
        missingStation = missing;
        stationWarning = missing == null ? "" : StationIcons.name(missing);
        treeLimited = CraftTrees.truncated(root);
        achievableCache.clear();
        peekEmcCache.clear();
        maxCraftableCache = -1;
        computeSummary();
        attachPeeks(root, stations, collapse, hideLoop, requestKeys, new HashSet<ItemKey>());
        layout = new TreeLayout(root, widthFn());
        if (!applyingAvailability) {
            QuickCraftNetwork.requestAvailability(requestKeys);
        }
    }

    private void attachMobSources(CraftNode node) {
        List<CraftNode> original = new ArrayList<CraftNode>(node.children);
        for (CraftNode child : original) attachMobSources(child);
        if (node.isMobSource()) return;
        List<MobItemSource> sources = JerIntegration.sourcesFor(node.output.getItem());
        if (sources.isEmpty()) return;
        if (sources.size() > MAX_MOB_SOURCES) sources = new ArrayList<MobItemSource>(sources.subList(0, MAX_MOB_SOURCES));
        CraftNode mobNode = new CraftNode(node.output.copy(), 0, Collections.<RecipeOption>emptyList(), node.depth + 1);
        mobNode.mobSources = sources;
        Integer selected = mobSelection.get(ItemKey.of(node.output));
        mobNode.mobIndex = selected == null ? 0 : selected;
        node.children.add(mobNode);
    }

    private void detachMobSources(CraftNode node) {
        for (int i = node.children.size() - 1; i >= 0; i--) {
            if (node.children.get(i).isMobSource()) node.children.remove(i);
        }
        for (CraftNode child : node.children) detachMobSources(child);
    }

    private boolean hasEye(CraftNode node) {
        return (node.owned || node.emcBuy) && node.selected() != null;
    }

    private void attachPeeks(CraftNode node, Stations stations, boolean collapse, boolean hideLoop,
                             Set<ItemKey> keys, Set<ItemKey> path) {
        ItemKey key = ItemKey.of(node.output);
        if (hasEye(node) && peekOpen.contains(key) && !path.contains(key)) {
            CraftNode fresh = builder.build(node.output, node.requiredCount, overrides, ingredientChoices,
                    Availability.Factory.exact(haveCounts), stations, collapse, hideLoop);
            if (fresh.selected() == null || fresh.children.isEmpty()) return;
            node.selectedRecipe = fresh.selectedRecipe;
            node.autoRecipe = fresh.autoRecipe;
            node.resultPerCraft = fresh.resultPerCraft;
            node.craftsNeeded = fresh.craftsNeeded;
            node.children.addAll(0, fresh.children);
            path.add(key);
            for (CraftNode child : fresh.children) {
                collectKeys(child, keys);
                if (showMobs && JerIntegration.available()) attachMobSources(child);
                primeOrigins(child);
                attachPeeks(child, stations, collapse, hideLoop, keys, path);
            }
            path.remove(key);
            return;
        }
        for (CraftNode child : node.children) attachPeeks(child, stations, collapse, hideLoop, keys, path);
    }

    private void togglePeek(CraftNode node) {
        ItemKey key = ItemKey.of(node.output);
        if (peekOpen.remove(key)) {
            peekStarts.remove(key);
        } else {
            peekOpen.add(key);
            peekStarts.put(key, now());
        }
        hoveredView = null;
        eyeHovered = false;
        playClick();
        rebuild();
    }

    private boolean overEye(NodeView view, double mouseX, double mouseY) {
        double wx = (mouseX - panX) / zoom;
        double wy = (mouseY - panY) / zoom;
        if (QuickCraftConfig.hoverBulge() && view == hoveredView) {
            double cx = view.x + view.width / 2.0;
            double cy = view.y + NodeView.HEIGHT / 2.0;
            wx = cx + (wx - cx) / HOVER_BULGE;
            wy = cy + (wy - cy) / HOVER_BULGE;
        }
        int ex = view.x + view.width - 2 - EYE_W;
        int ey = view.y + 2;
        return wx >= ex - 1 && wx <= ex + EYE_W + 1 && wy >= ey - 1 && wy <= ey + EYE_H + 1;
    }

    private Set<ItemKey> relevantKeys() {
        Set<ItemKey> keys = new LinkedHashSet<ItemKey>();
        keys.add(ItemKey.of(target));
        collectKeys(root, keys);
        keys.addAll(builder.loopIngredientKeys());
        return keys;
    }

    private void collectKeys(CraftNode node, Set<ItemKey> keys) {
        if (node == null) return;
        keys.add(ItemKey.of(node.output));
        for (ItemStack option : node.tagOptions) keys.add(ItemKey.of(option));
        for (CraftNode child : node.children) collectKeys(child, keys);
    }

    private void computeSummary() {
        Map<ItemKey, Integer> totals = CraftTrees.leafTotals(root);
        List<Map.Entry<ItemKey, Integer>> list = new ArrayList<Map.Entry<ItemKey, Integer>>(totals.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<ItemKey, Integer>>() {
            @Override
            public int compare(Map.Entry<ItemKey, Integer> a, Map.Entry<ItemKey, Integer> b) {
                int tierA = summaryTier(a.getKey(), a.getValue());
                int tierB = summaryTier(b.getKey(), b.getValue());
                if (tierA != tierB) return tierA - tierB;
                return b.getValue() - a.getValue();
            }
        });
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
            Integer haveValue = haveCounts.get(entry.getKey());
            int missing = entry.getValue() - (haveValue == null ? 0 : haveValue);
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
        Minecraft.getMinecraft().getSoundHandler().playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void computeHaveCounts() {
        haveCounts.clear();
        if (this.mc.player == null) return;
        InventoryPlayer inv = this.mc.player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ItemKey key = ItemKey.of(stack);
            Integer existing = haveCounts.get(key);
            haveCounts.put(key, existing == null ? stack.getCount() : existing + stack.getCount());
        }
    }

    private void autoFit() {
        if (root == null || layout == null || layout.ordered.isEmpty()) {
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

    private CraftPlanner.Plan planFor(int qty, Stations stations, boolean collapse, boolean hideLoop,
                                      BigInteger budget, boolean withEmc) {
        VirtualPool pool = new VirtualPool();
        for (Map.Entry<ItemKey, Integer> entry : haveCounts.entrySet()) pool.add(entry.getKey(), entry.getValue());
        return CraftPlanner.plan(builder, target, qty, overrides, ingredientChoices, pool,
                Availability.Factory.exact(haveCounts), stations, collapse, hideLoop, withEmc ? emcSource : null, budget);
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
        if (!QuickCraftConfig.showEmc()) return;
        emcTotalText = ProjectEClient.format(owned);
        emcCostText = required.signum() > 0 ? ProjectEClient.format(required) : null;
    }

    private int nodeHave(CraftNode node) {
        return node.freeStock;
    }

    private int effectiveHave(ItemKey key) {
        Integer owned = haveCounts.get(key);
        Integer emc = emcSupplied.get(key);
        return (owned == null ? 0 : owned) + (emc == null ? 0 : emc);
    }

    private void renderEmc() {
        if (emcTotalText == null && emcCostText == null) return;
        String totalPart = emcTotalText == null ? "" : "EMC: " + emcTotalText;
        String costPart = emcCostText == null ? "" : "Uses " + emcCostText;
        String gap = (!totalPart.isEmpty() && !costPart.isEmpty()) ? "   " : "";

        int w = this.fontRenderer.getStringWidth(totalPart + gap + costPart);
        int right = this.width - 8;
        if (summaryVisible()) right = Math.min(right, summaryPanelX() - 4);
        int x = Math.max(8, right - w);
        boolean stationBar = (stationProblem && missingStation != null) || treeLimited;
        if (!stationBar) Draw.fill(x - 6, 24, right + 8, 38, COLOR_BAR);

        if (!totalPart.isEmpty()) Draw.string(this.fontRenderer, totalPart, x, 27, COLOR_EMC, false);
        if (!costPart.isEmpty()) {
            int costX = x + this.fontRenderer.getStringWidth(totalPart + gap);
            Draw.string(this.fontRenderer, costPart, costX, 27,
                    emcAffordable ? COLOR_EMC : COLOR_MISSING, false);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        updateControlsHover(mouseX, mouseY);
        boolean overChrome = overDepositBox(mouseX, mouseY) || (depositMenuOpen && overDepositMenu(mouseX, mouseY))
                || overControlsArea(mouseX, mouseY) || overHistoryTab(mouseX, mouseY) || overHistoryPanel(mouseX, mouseY);
        hoveredView = (overChrome || (showSummary && overSummaryPanel(mouseX, mouseY))) ? null : nodeAt(mouseX, mouseY);
        eyeHovered = hoveredView != null && hasEye(hoveredView.node) && overEye(hoveredView, mouseX, mouseY);
        boolean bulge = QuickCraftConfig.hoverBulge() && hoveredView != null;

        Draw.push();
        Draw.translate(panX, panY, 0);
        Draw.scale(zoom, zoom, 1.0);
        if (animate && root != null) {
            drawTreeAnimated();
        } else if (layout != null) {
            Draw.beginFills();
            for (NodeView view : layout.ordered) {
                for (CraftNode child : view.node.children) {
                    drawEdge(view, layout.views.get(child), 1.0);
                }
            }
            Draw.endFills();
            for (NodeView view : layout.ordered) {
                if (bulge && view == hoveredView) drawNodeScaled(view, HOVER_BULGE);
                else drawNode(view);
            }
        }
        Draw.pop();

        Draw.push();
        Draw.translate(0, 0, Z_OVERLAY);

        Draw.fill(0, 0, this.width, 24, COLOR_BAR);
        Draw.fill(0, this.height - 56, this.width, this.height, COLOR_BAR);

        String title = Draw.trimToWidth(this.fontRenderer,
                "Quick Craft - " + target.getDisplayName(), this.width - 232);
        Draw.string(this.fontRenderer, title, DEP_X + DEP_W + 6, 8, 0xFFFFFF, false);
        Draw.string(this.fontRenderer, "Qty:", this.width - 186, 8, 0xFFFFFF, false);
        renderDepositControl(mouseX, mouseY);
        renderControlsTab(mouseX, mouseY);
        renderHistoryTab(mouseX, mouseY);

        int stationNameX = -1;
        if (stationProblem && missingStation != null) {
            Draw.fill(0, 24, this.width, 38, COLOR_BAR);
            List<ItemStack> options = StationProviders.icons(missingStation);
            String name = options.isEmpty()
                    ? stationWarning
                    : options.get(Math.floorMod(stationSelectedIndex, options.size())).getDisplayName();
            String prefix = "Missing: ";
            Draw.string(this.fontRenderer, prefix, 8, 27, COLOR_MISSING, false);
            stationNameX = 8 + this.fontRenderer.getStringWidth(prefix);
            int nameW = this.fontRenderer.getStringWidth(name);
            hoveringStationName = mouseX >= stationNameX && mouseX <= stationNameX + nameW
                    && mouseY >= 25 && mouseY <= 37;
            Draw.string(this.fontRenderer, name, stationNameX, 27,
                    hoveringStationName ? 0xFFFFFF55 : COLOR_MISSING, false);
        } else if (treeLimited) {
            hoveringStationName = false;
            Draw.fill(0, 24, this.width, 38, COLOR_BAR);
            Draw.string(this.fontRenderer, "Tree limit reached: raise maxTreeNodes or maxTreeDepth in the config", 8, 27,
                    COLOR_MISSING, false);
        } else {
            hoveringStationName = false;
        }

        renderEmc();

        super.drawScreen(mouseX, mouseY, partialTicks);
        quantityBox.drawTextBox();

        if (summaryVisible()) renderSummary(mouseX, mouseY);

        if (BookmarkOverlay.isActive()) {
            BookmarkOverlay.render(this.width, this.height, true, mouseX, mouseY);
        }

        if (depositMenuOpen) renderDepositMenu(mouseX, mouseY);
        renderControlsPanel();
        if (historyOpen) renderHistoryPanel(mouseX, mouseY);

        if (hoveringStationName && stationNameX >= 0) {
            renderStationDropdown(stationNameX);
        } else if (hoveredView != null && eyeHovered) {
            boolean open = peekOpen.contains(ItemKey.of(hoveredView.node.output));
            Draw.tooltip(this.fontRenderer,
                    Collections.singletonList(open ? "Hide crafting tree" : "Show crafting tree"), mouseX, mouseY);
        } else if (hoveredView != null) {
            drawTooltip(hoveredView, mouseX, mouseY);
        } else if (!depositMenuOpen && overDepositBox(mouseX, mouseY)) {
            drawDepositTooltip(mouseX, mouseY);
        } else if (depositMenuOpen && overDepositMenu(mouseX, mouseY)) {
            drawDepositRowTooltip(mouseX, mouseY);
        } else if (overCraftMax(mouseX, mouseY)) {
            Draw.tooltip(this.fontRenderer,
                    Collections.singletonList("can craft: " + maxCraftable()), mouseX, mouseY);
        } else if (overRefresh(mouseX, mouseY)) {
            Draw.tooltip(this.fontRenderer,
                    Collections.singletonList("Scan for items available"), mouseX, mouseY);
        } else if (!copyMenuOpen && overCopyButton(mouseX, mouseY)) {
            Draw.tooltip(this.fontRenderer,
                    Collections.singletonList("Copy material list to clipboard."), mouseX, mouseY);
        }

        Draw.pop();
    }

    private boolean overCraftMax(double mouseX, double mouseY) {
        return mouseX >= this.width - 78 && mouseX <= this.width - 6 && mouseY >= 4 && mouseY <= 20;
    }

    private boolean overRefresh(double mouseX, double mouseY) {
        int x = this.width / 2 - 56;
        int y = this.height - 52;
        return mouseX >= x && mouseX <= x + 112 && mouseY >= y && mouseY <= y + 20;
    }

    private int maxCraftable() {
        if (maxCraftableCache >= 0) return maxCraftableCache;
        Stations stations = detectedStations != null
                ? detectedStations : StationScan.detect(this.mc.world, this.mc.player);
        maxCraftableCache = Math.max(0, planFor(CRAFT_MAX, stations, QuickCraftConfig.collapseOwnedItems(),
                QuickCraftConfig.hideLoopingRecipes(), null, false).craftable());
        return maxCraftableCache;
    }

    private void renderStationDropdown(int anchorX) {
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
        Draw.fill(x, top, x + w, top + h, 0xF0080808);
        Draw.fill(x, top, x + w, top + headerH, 0xF0202020);
        Draw.string(this.fontRenderer, "Can craft this at (W/S):", x + 5, top + 3, 0xFFFFFF, false);
        int listTop = top + headerH + 2;
        for (int r = 0; r < visible; r++) {
            int idx = r + scroll;
            if (idx >= total) break;
            ItemStack icon = options.get(idx);
            int ry = listTop + r * rowH;
            if (idx == selected) Draw.fill(x + 2, ry - 1, x + w - 2, ry + rowH - 3, 0x50FFFFFF);
            Draw.item(icon, x + 5, ry);
            Draw.string(this.fontRenderer, trim(icon.getDisplayName(), 24), x + 26, ry + 4,
                    idx == selected ? 0xFFFF55 : 0xFFFFFF, false);
        }
        if (total > maxVisible) {
            if (scroll > 0) Draw.string(this.fontRenderer, "^", x + w - 11, listTop, 0xFFFFFF, false);
            if (scroll + maxVisible < total) Draw.string(this.fontRenderer, "v", x + w - 11, top + h - 10, 0xFFFFFF, false);
        }
    }

    private void renderDepositControl(int mouseX, int mouseY) {
        boolean active = depositMenuOpen || overDepositBox(mouseX, mouseY);
        Draw.fill(DEP_X, DEP_Y, DEP_X + DEP_W, DEP_Y + DEP_H, active ? 0xFF3A3A3A : 0x90000000);
        Draw.outline(DEP_X, DEP_Y, DEP_W, DEP_H, active ? 0xFFAAAAAA : 0xFF555555);
        ItemStack icon = depositIcon(ClientDepositTargets.selected());
        if (!icon.isEmpty()) Draw.item(icon, DEP_X + 1, DEP_Y);
        Draw.string(this.fontRenderer, "v", DEP_X + DEP_W - 8, DEP_Y + 4, 0xFFDDDDDD, false);
    }

    private List<ClientDepositTargets.Target> depositFiltered() {
        List<ClientDepositTargets.Target> all = ClientDepositTargets.targets();
        if (depositSearch.isEmpty()) return all;
        String q = depositSearch.toLowerCase(Locale.ROOT);
        List<ClientDepositTargets.Target> out = new ArrayList<ClientDepositTargets.Target>();
        for (ClientDepositTargets.Target t : all) {
            if (t.label().toLowerCase(Locale.ROOT).contains(q)) out.add(t);
        }
        return out;
    }

    private int depositListTop() {
        return DEP_Y + DEP_H + 2 + DEP_HEADER_H + DEP_SEARCH_H + 2;
    }

    private void renderDepositMenu(int mouseX, int mouseY) {
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
        Draw.fill(x, top, x + DEP_MENU_W, top + h, 0xF0080808);
        Draw.fill(x, top, x + DEP_MENU_W, top + DEP_HEADER_H, 0xF0202020);
        Draw.string(this.fontRenderer, "Deposit results into:", x + 5, top + 3, 0xFFFFFF, false);

        Draw.fill(x + 4, searchTop + 1, x + DEP_MENU_W - 4, searchTop + DEP_SEARCH_H - 1, 0xFF101010);
        Draw.outline(x + 4, searchTop + 1, DEP_MENU_W - 8, DEP_SEARCH_H - 2, 0xFF555555);
        boolean empty = depositSearch.isEmpty();
        Draw.string(this.fontRenderer, trim(empty ? "Search..." : depositSearch, 34), x + 8, searchTop + 4,
                empty ? 0xFF777777 : 0xFFFFFFFF, false);

        String selectedId = ClientDepositTargets.selectedId();
        for (int r = 0; r < visible; r++) {
            int idx = r + depositScroll;
            if (idx >= total) break;
            ClientDepositTargets.Target target = list.get(idx);
            int ry = listTop + r * DEP_ROW_H;
            boolean rowHover = mouseX >= x + 2 && mouseX <= x + DEP_MENU_W - 2
                    && mouseY >= ry - 1 && mouseY <= ry + DEP_ROW_H - 3;
            boolean isSelected = target.id().equals(selectedId);
            if (isSelected) Draw.fill(x + 2, ry - 1, x + DEP_MENU_W - 2, ry + DEP_ROW_H - 3, 0x5000FF00);
            else if (rowHover) Draw.fill(x + 2, ry - 1, x + DEP_MENU_W - 2, ry + DEP_ROW_H - 3, 0x40FFFFFF);
            ItemStack rowIcon = depositIcon(target);
            if (!rowIcon.isEmpty()) Draw.item(rowIcon, x + 4, ry);
            Draw.string(this.fontRenderer, trim(target.label(), 30), x + 25, ry + 4,
                    isSelected ? 0x55FF55 : 0xFFFFFF, false);
        }
        if (total == 0) {
            Draw.string(this.fontRenderer, "No matches", x + 8, listTop + 4, 0xFF888888, false);
        }
        if (total > DEP_MAX_VISIBLE) {
            if (depositScroll > 0) Draw.string(this.fontRenderer, "^", x + DEP_MENU_W - 11, listTop, 0xFFFFFF, false);
            if (depositScroll + DEP_MAX_VISIBLE < total) {
                Draw.string(this.fontRenderer, "v", x + DEP_MENU_W - 11, top + h - 10, 0xFFFFFF, false);
            }
        }
    }

    private void drawDepositRowTooltip(int mouseX, int mouseY) {
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
                List<String> lines = new ArrayList<String>();
                lines.add(TextFormatting.WHITE + target.label());
                if (target.totalSlots() >= 0) {
                    lines.add(TextFormatting.GRAY + "" + target.freeSlots() + "/" + target.totalSlots()
                            + " slots available");
                }
                Draw.tooltip(this.fontRenderer, lines, mouseX, mouseY);
                return;
            }
        }
    }

    private void drawDepositTooltip(int mouseX, int mouseY) {
        ClientDepositTargets.Target selected = ClientDepositTargets.selected();
        List<String> lines = new ArrayList<String>();
        lines.add(TextFormatting.WHITE + "Deposit results");
        lines.add(TextFormatting.GREEN + "Into: " + selected.label());
        lines.add(TextFormatting.AQUA + "Click to choose a container");
        Draw.tooltip(this.fontRenderer, lines, mouseX, mouseY);
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
            quantityBox.setFocused(false);
            QuickCraftNetwork.requestDepositTargets();
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
            controlsAnimStart = now();
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

    private void renderControlsTab(int mouseX, int mouseY) {
        int y = this.height - 52;
        boolean hover = overControlsArea(mouseX, mouseY);
        Draw.fill(CT_X, y, CT_X + CT_W, y + CT_H, hover ? 0xFF3A3A3A : 0xF0202020);
        Draw.outline(CT_X, y, CT_W, CT_H, hover ? 0xFFAAAAAA : 0xFF555555);
        int tw = this.fontRenderer.getStringWidth("Controls");
        Draw.string(this.fontRenderer, "Controls", CT_X + (CT_W - tw) / 2, y + 6,
                hover ? 0xFFFFFF : 0xFFCFCFCF, false);
    }

    private void renderControlsPanel() {
        long elapsed = now() - controlsAnimStart;
        double p = clamp(elapsed / (double) CONTROLS_SLIDE);
        double slide = controlsHovered ? easeOut(p) : 1 - easeOut(p);
        if (slide <= 0.001) return;

        int h = controlsPanelHeight();
        int x = CT_X;
        int restBottom = this.height - 54;
        int top = restBottom - h + (int) ((1 - slide) * (h + 40));

        Draw.scissorOn(x, 0, CTRL_W, restBottom);
        Draw.fill(x, top, x + CTRL_W, top + h, 0xF0080808);
        Draw.outline(x, top, CTRL_W, h, 0xFF555555);

        int pad = 5;
        int[] colors = {COLOR_ROOT, COLOR_HAVE, COLOR_CRAFT, COLOR_MISSING, COLOR_DISABLED};
        String[] labels = {"End result item", "Items you have", "Items being crafted", "Missing materials",
                "Can't craft (no station)"};
        int y = top + pad;
        for (int i = 0; i < colors.length; i++) {
            Draw.fill(x + pad, y, x + CTRL_W - pad, y + 12, colors[i]);
            Draw.string(this.fontRenderer, labels[i], x + pad + 5, y + 2, 0xFFFFFFFF, true);
            y += 15;
        }
        Draw.string(this.fontRenderer, "Dimmed tab = branch can't be finished", x + pad, y, 0xFF808080, false);
        y += 12;
        Draw.fill(x + pad, y, x + CTRL_W - pad, y + 1, 0x40FFFFFF);
        y += 4;
        for (String line : CONTROL_LINES) {
            Draw.string(this.fontRenderer, line, x + pad, y, 0xFFC8C8C8, false);
            y += 11;
        }
        Draw.scissorOff();
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

    private void renderHistoryTab(int mouseX, int mouseY) {
        int y = this.height - 52;
        boolean hover = historyOpen || overHistoryTab(mouseX, mouseY);
        Draw.fill(HIST_X, y, HIST_X + HIST_W, y + HIST_H, hover ? 0xFF3A3A3A : 0xF0202020);
        Draw.outline(HIST_X, y, HIST_W, HIST_H, hover ? 0xFFAAAAAA : 0xFF555555);
        int tw = this.fontRenderer.getStringWidth("History");
        Draw.string(this.fontRenderer, "History", HIST_X + (HIST_W - tw) / 2, y + 6,
                hover ? 0xFFFFFF : 0xFFCFCFCF, false);
    }

    private void renderHistoryPanel(int mouseX, int mouseY) {
        int total = historyEntries.size();
        int visible = Math.min(HIST_MAX_VISIBLE, total);
        int maxScroll = Math.max(0, total - HIST_MAX_VISIBLE);
        historyScroll = Math.max(0, Math.min(historyScroll, maxScroll));
        int x = HIST_X;
        int bottom = this.height - 54;
        int top = historyPanelTop();
        Draw.fill(x, top, x + HIST_MENU_W, bottom, 0xF0080808);
        Draw.fill(x, top, x + HIST_MENU_W, top + HIST_HEADER_H, 0xF0202020);
        Draw.string(this.fontRenderer, "Crafting history", x + 5, top + 3, 0xFFFFFF, false);
        int listTop = top + HIST_HEADER_H + 3;
        if (total == 0) {
            Draw.string(this.fontRenderer, "No history yet", x + 8, listTop + 4, 0xFF888888, false);
            return;
        }
        for (int r = 0; r < visible; r++) {
            int idx = r + historyScroll;
            if (idx >= total) break;
            CraftHistory.Entry entry = historyEntries.get(idx);
            ItemStack stack = entry.stack();
            int ry = listTop + r * HIST_ROW_H;
            boolean rowHover = mouseX >= x + 2 && mouseX <= x + HIST_MENU_W - 2
                    && mouseY >= ry - 1 && mouseY <= ry + HIST_ROW_H - 3;
            if (rowHover) Draw.fill(x + 2, ry - 1, x + HIST_MENU_W - 2, ry + HIST_ROW_H - 3, 0x40FFFFFF);
            Draw.item(stack, x + 4, ry - 1);
            Draw.string(this.fontRenderer, trim(stack.getDisplayName(), 24) + " x" + entry.count(),
                    x + 24, ry + 3, 0xFFFFFF, false);
        }
        if (total > HIST_MAX_VISIBLE) {
            if (historyScroll > 0) Draw.string(this.fontRenderer, "^", x + HIST_MENU_W - 11, listTop, 0xFFFFFF, false);
            if (historyScroll + HIST_MAX_VISIBLE < total) {
                Draw.string(this.fontRenderer, "v", x + HIST_MENU_W - 11, bottom - 10, 0xFFFFFF, false);
            }
        }
    }

    private void drawNodeScaled(NodeView view, float scale) {
        float cx = view.x + view.width / 2.0F;
        float cy = view.y + NodeView.HEIGHT / 2.0F;
        Draw.push();
        Draw.translate(cx, cy, 0);
        Draw.scale(scale, scale, 1.0);
        Draw.translate(-cx, -cy, 0);
        drawNode(view);
        Draw.pop();
    }

    private void drawTreeAnimated() {
        if (layout == null) return;
        long clock = now();
        long elapsed = clock - treeAnimStart;
        animData.clear();
        NodeView rootView = layout.views.get(root);
        if (rootView == null) return;
        computeAnim(root, rootView.x + rootView.width / 2.0, rootView.y + NodeView.HEIGHT / 2.0, elapsed, clock);

        Draw.beginFills();
        for (NodeView view : layout.ordered) {
            double[] pa = animData.get(view.node);
            if (pa == null) continue;
            for (CraftNode child : view.node.children) {
                double[] ca = animData.get(child);
                if (ca == null || ca[2] <= 0.001) continue;
                drawEdge(view, layout.views.get(child), ca[2]);
            }
        }
        Draw.endFills();
        boolean bulge = QuickCraftConfig.hoverBulge() && hoveredView != null;
        for (NodeView view : layout.ordered) {
            double[] a = animData.get(view.node);
            if (a == null || a[2] <= 0.001) continue;
            double s = bulge && view == hoveredView ? a[2] * HOVER_BULGE : a[2];
            Draw.push();
            Draw.translate(a[0], a[1], 0);
            Draw.scale(s, s, 1.0);
            Draw.translate(-(view.x + view.width / 2.0), -(view.y + NodeView.HEIGHT / 2.0), 0);
            drawNode(view);
            Draw.pop();
        }
    }

    private void computeAnim(CraftNode node, double parentCx, double parentCy, long elapsed, long clock) {
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
        long childElapsed = elapsed;
        if (!node.children.isEmpty() && hasEye(node)) {
            ItemKey key = ItemKey.of(node.output);
            if (peekOpen.contains(key)) {
                Long start = peekStarts.get(key);
                childElapsed = start == null ? elapsed - depth * TREE_DEPTH_DELAY : clock - start;
            }
        }
        for (CraftNode child : node.children) {
            computeAnim(child, cx, cy, childElapsed, clock);
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

    private void renderSummary(int mouseX, int mouseY) {
        boolean opening = showSummary;
        long time = now();
        boolean swapping = animate && showSummary && (time - summarySwapStart) < 2 * SUMMARY_SWAP;
        long elapsed = animate ? time - summaryAnimStart : 0;

        int px = summaryPanelX();
        int top = 26;
        int bottom = this.height - 58;
        Draw.fill(px, top, px + PANEL_W, bottom, 0xF0080808);
        Draw.fill(px, top, px + PANEL_W, top + 15, 0xF0202020);
        Draw.string(this.fontRenderer, "Ingredients (" + summaryItems.size() + ")", px + 6, top + 4, 0xFFFFFF, false);

        int pinX = px + PANEL_W - 30;
        int pinY = top + 2;
        boolean pinned = BookmarkOverlay.isActive();
        Draw.fill(pinX, pinY, pinX + TAB_BTN_W, pinY + 11, pinned ? 0xFF4A3A10 : 0x80000000);
        drawTabLabel("Pin", pinX, pinY, pinned ? COLOR_CRAFT : 0xFFB0B0B0);

        int copyX = px + PANEL_W - 60;
        boolean copyHover = overCopyButton(mouseX, mouseY);
        Draw.fill(copyX, pinY, copyX + TAB_BTN_W, pinY + 11, (copyMenuOpen || copyHover) ? 0xFF3A3A3A : 0x80000000);
        drawTabLabel("Copy", copyX, pinY, 0xFFB0B0B0);

        int listTop = top + 17;
        int maxRows = Math.max(0, (bottom - listTop) / PANEL_ROW_H);
        int maxScroll = Math.max(0, summaryItems.size() - maxRows);
        summaryScroll = Math.max(0, Math.min(summaryScroll, maxScroll));
        int visN = Math.min(maxRows, Math.max(0, summaryItems.size() - summaryScroll));
        summaryRowCount = visN;

        Draw.scissorOn(px, listTop, PANEL_W, bottom - listTop);
        if (swapping) {
            long se = time - summarySwapStart;
            int outDx = (int) (easeIn(clamp(se / (double) SUMMARY_SWAP)) * PANEL_W);
            int inDx = (int) ((1 - easeOut(clamp((se - SUMMARY_SWAP) / (double) SUMMARY_SWAP))) * PANEL_W);
            drawSummaryList(outgoingItems, px, listTop, maxRows, outgoingScroll, outDx);
            drawSummaryList(summaryItems, px, listTop, maxRows, summaryScroll, inDx);
        } else {
            if (!outgoingItems.isEmpty()) outgoingItems = Collections.emptyList();
            for (int r = 0; r < maxRows; r++) {
                int idx = r + summaryScroll;
                if (idx >= summaryItems.size()) break;
                int dx = rowOffset(opening, elapsed, r, visN);
                drawSummaryRow(summaryItems.get(idx), px, listTop + r * PANEL_ROW_H, dx);
            }
        }
        Draw.scissorOff();

        if (copyMenuOpen) {
            int dx = copyDropX();
            int dy = copyDropY();
            int dh = COPY_OPTIONS.length * COPY_ROW_H + 2;
            Draw.fill(dx, dy, dx + COPY_DROP_W, dy + dh, 0xF0080808);
            Draw.outline(dx, dy, COPY_DROP_W, dh, 0xFF555555);
            for (int r = 0; r < COPY_OPTIONS.length; r++) {
                int ry = dy + 1 + r * COPY_ROW_H;
                boolean sel = r == copySelected;
                boolean hov = mouseX >= dx && mouseX <= dx + COPY_DROP_W && mouseY >= ry && mouseY <= ry + COPY_ROW_H;
                if (sel) Draw.fill(dx + 1, ry, dx + COPY_DROP_W - 1, ry + COPY_ROW_H, 0x5000FF00);
                else if (hov) Draw.fill(dx + 1, ry, dx + COPY_DROP_W - 1, ry + COPY_ROW_H, 0x40FFFFFF);
                Draw.string(this.fontRenderer, COPY_OPTIONS[r], dx + 6, ry + 3, sel ? 0x55FF55 : 0xFFFFFF, false);
            }
        }
    }

    private void drawTabLabel(String text, int x, int y, int color) {
        int offset = (TAB_BTN_W - this.fontRenderer.getStringWidth(text)) / 2;
        Draw.string(this.fontRenderer, text, x + offset, y + 2, color, false);
    }

    private int summaryPx() {
        return this.width - PANEL_W - 6;
    }

    private int summaryPanelX() {
        long time = now();
        boolean swapping = animate && showSummary && (time - summarySwapStart) < 2 * SUMMARY_SWAP;
        long elapsed = animate ? time - summaryAnimStart : 0;
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
                .append(target.getDisplayName()).append(":\n");
        for (Map.Entry<ItemKey, Integer> e : summaryItems) {
            int need = e.getValue();
            Integer owned = haveCounts.get(e.getKey());
            int have = owned == null ? 0 : owned;
            String name = e.getKey().toStack(1).getDisplayName();
            if (mode == 0) {
                sb.append("- ").append(need).append("x ").append(name).append('\n');
            } else if (mode == 1) {
                if (have <= 0) sb.append("- ").append(need).append("x ").append(name).append('\n');
            } else if (have < need) {
                sb.append("- ").append(name).append(": ").append(have).append('/').append(need)
                        .append(" (need ").append(need - have).append(" more)\n");
            }
        }
        setClipboardString(sb.toString().trim());
        playClick();
    }

    private void drawSummaryList(List<Map.Entry<ItemKey, Integer>> items,
                                 int px, int listTop, int maxRows, int scroll, int dx) {
        for (int r = 0; r < maxRows; r++) {
            int idx = r + scroll;
            if (idx >= items.size()) break;
            drawSummaryRow(items.get(idx), px, listTop + r * PANEL_ROW_H, dx);
        }
    }

    private void drawSummaryRow(Map.Entry<ItemKey, Integer> entry, int px, int ry, int dx) {
        ItemStack stack = displayStack(entry.getKey(), entry.getKey().toStack(1));
        int need = entry.getValue();
        Integer owned = haveCounts.get(entry.getKey());
        int have = owned == null ? 0 : owned;
        Integer emcValue = emcSupplied.get(entry.getKey());
        int fromEmc = emcValue == null ? 0 : emcValue;
        int rx = px + dx;
        Draw.item(stack, rx + 4, ry + 1);
        Draw.itemDecorations(this.fontRenderer, stack, rx + 4, ry + 1);
        Draw.string(this.fontRenderer, trim(stack.getDisplayName(), 15), rx + 24, ry, 0xFFFFFF, false);
        Integer rowColor = summaryEmcColor.get(entry.getKey());
        boolean covered = have + fromEmc >= need || (rowColor != null && rowColor == COLOR_EMC);
        boolean viaEmc = have <= 0 && covered;
        int color = have >= need ? COLOR_HAVE : (covered ? COLOR_EMC : (have > 0 ? COLOR_CRAFT : COLOR_MISSING));
        Draw.string(this.fontRenderer, viaEmc ? "EMC" : have + " / " + need, rx + 24, ry + 10, color, false);
        String emcText = summaryEmcText.get(entry.getKey());
        if (emcText != null) {
            int ex = rx + PANEL_W - 6 - this.fontRenderer.getStringWidth(emcText);
            Integer emcColor = summaryEmcColor.get(entry.getKey());
            Draw.string(this.fontRenderer, emcText, ex, ry + 10, emcColor == null ? COLOR_EMC : emcColor, false);
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
        long elapsed = now() - summaryAnimStart;
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
        int badgeExtra = badge.isEmpty() ? 0 : this.fontRenderer.getStringWidth(badge) + 4;
        int nameNeeded = 30 + this.fontRenderer.getStringWidth(node.output.getDisplayName()) + badgeExtra
                + (hasEye(node) ? EYE_W + 2 : 0);

        RecipeOption recipe = node.selected();
        boolean hasStationIcon = recipe == null
                ? hasOriginIcon(node)
                : !node.owned && !StationIcons.icon(recipe.station()).isEmpty();
        StringBuilder sub = new StringBuilder();
        if (node != root) {
            sub.append(node.catalyst ? "keep " : "need ").append(node.requiredCount);
            if (node.isTagChoice()) sub.append(" *").append(node.tagOptions.size());
            if (node.cyclic) sub.append(" ~");
            if (node.reference != null) sub.append(" ^");
        }
        int subNeeded = 26 + this.fontRenderer.getStringWidth(sub.toString()) + (hasStationIcon ? 16 : 4);

        int needed = Math.max(nameNeeded, subNeeded);
        return Math.max(NodeView.WIDTH, Math.min(MAX_NODE_WIDTH, needed));
    }

    private int mobNodeWidth(CraftNode node) {
        int badge = node.mobSources.size() > 1 ? this.fontRenderer.getStringWidth("[9/9]") + 6 : 0;
        int max = 0;
        for (MobItemSource src : node.mobSources) {
            int nameW = this.fontRenderer.getStringWidth(src.mob.mobName);
            int labelW = this.fontRenderer.getStringWidth(mobDropLabel(src.drop));
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
            ItemStack source = detectedStations.sourceFor(station);
            if (!source.isEmpty()) return source;
        }
        return StationIcons.icon(station);
    }

    private boolean emcCovered(CraftNode node) {
        ItemKey key = ItemKey.of(node.output);
        if (effectiveHave(key) >= node.requiredCount) return true;
        Integer color = summaryEmcColor.get(key);
        if (color != null) return color == COLOR_EMC;
        return peekEmcCovered(node);
    }

    private boolean peekEmcCovered(CraftNode node) {
        Boolean cached = peekEmcCache.get(node);
        if (cached != null) return cached;
        boolean result = computePeekEmcCovered(node);
        peekEmcCache.put(node, result);
        return result;
    }

    private boolean computePeekEmcCovered(CraftNode node) {
        if (emcSource == null || lastEmc == null) return false;
        if (!emcSource.learned(node.output)) return false;
        long unit = emcSource.value(node.output);
        if (unit <= 0L) return false;
        return BigInteger.valueOf(unit).multiply(BigInteger.valueOf(node.requiredCount)).compareTo(lastEmc) <= 0;
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
            ItemStack icon = sourceIcons.get(ItemKey.of(node.output));
            if (icon != null && !icon.isEmpty()) return icon;
            return originIcon(node);
        }
        RecipeOption recipe = node.selected();
        if (recipe == null) return originIcon(node);
        if (recipe.station() == Station.CRAFTING && recipe.fitsInventory()) return PlayerHeadIcon.get();
        return stationIconFor(recipe.station());
    }

    private List<OriginHint> originsFor(CraftNode node) {
        if (node.isCraftable() || node.isMobSource()) return Collections.emptyList();
        return ItemOrigins.of(node.output);
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
        int index = animate ? (int) ((now() / ORIGIN_CYCLE) % count) : 0;
        for (OriginHint hint : hints) {
            if (hint.icon().isEmpty()) continue;
            if (index-- == 0) return hint.icon();
        }
        return ItemStack.EMPTY;
    }

    private String originLabel(CraftNode node) {
        List<String> labels = new ArrayList<String>();
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

    private void drawNode(NodeView view) {
        if (view.node.isMobSource()) {
            drawMobNode(view);
            return;
        }
        int sx = view.x;
        int sy = view.y;
        int w = view.width;

        int base = colorFor(view.node);
        boolean dimmed = base != COLOR_MISSING && isDimmed(view.node);
        int border = dimmed ? dim(base) : base;
        Draw.fill(sx - 1, sy - 1, sx + w + 1, sy + NodeView.HEIGHT + 1, border);
        Draw.fill(sx, sy, sx + w, sy + NodeView.HEIGHT, COLOR_NODE_BG);

        ItemStack icon = displayStack(view.node);
        Draw.item(icon, sx + 5, sy + 7);
        Draw.itemDecorations(this.fontRenderer, icon, sx + 5, sy + 7);

        ItemStack stationIcon = cornerIconFor(view.node);
        boolean hasStationIcon = !stationIcon.isEmpty();

        String badge = view.node.alternatives.size() > 1 && !view.node.children.isEmpty()
                ? "[" + (view.node.selectedRecipe + 1) + "/" + view.node.alternatives.size() + "]" : "";
        int nameWidth = w - 30;
        int textRight = sx + w - 4;
        if (hasEye(view.node)) {
            drawEye(view, sx + w - 2 - EYE_W, sy + 2);
            textRight -= EYE_W + 2;
            nameWidth -= EYE_W + 2;
        }
        if (!badge.isEmpty()) {
            int badgeWidth = this.fontRenderer.getStringWidth(badge);
            Draw.string(this.fontRenderer, badge, textRight - badgeWidth, sy + 5, COLOR_CRAFT, false);
            nameWidth -= badgeWidth + 4;
        }
        String name = Draw.ellipsize(this.fontRenderer, icon.getDisplayName(), nameWidth);
        int nameColor = (base == COLOR_DISABLED || dimmed) ? 0xFF9A9A9A : 0xFFFFFF;
        Draw.string(this.fontRenderer, name, sx + 26, sy + 5, nameColor, false);

        StringBuilder sub = new StringBuilder();
        if (view.node != root) {
            sub.append(view.node.catalyst ? "keep " : "need ").append(view.node.requiredCount);
            if (view.node.isTagChoice()) sub.append(" *").append(view.node.tagOptions.size());
            if (view.node.cyclic) sub.append(" ~");
            if (view.node.reference != null) sub.append(" ^");
        }
        int subWidth = (hasStationIcon ? w - 42 : w - 30);
        String subText = Draw.trimToWidth(this.fontRenderer, sub.toString(), subWidth);
        Draw.string(this.fontRenderer, subText, sx + 26, sy + 17, 0xB0B0B0, false);

        if (hasStationIcon) {
            Draw.push();
            Draw.translate(sx + w - 14, sy + NodeView.HEIGHT - 14, 0);
            Draw.scale(0.72, 0.72, 1.0);
            Draw.item(stationIcon, 0, 0);
            Draw.pop();
        }
    }

    private void drawEye(NodeView view, int x, int y) {
        if (eyeHovered && view == hoveredView) Draw.fill(x - 1, y - 1, x + EYE_W + 1, y + EYE_H + 1, 0x40FFFFFF);
        boolean open = peekOpen.contains(ItemKey.of(view.node.output));
        Draw.texture(open ? HIDE_TREE : VIEW_TREE, x, y, EYE_W, EYE_H);
    }

    private void drawMobNode(NodeView view) {
        int sx = view.x;
        int sy = view.y;
        int w = view.width;
        MobItemSource src = view.node.currentMob();

        Draw.fill(sx - 1, sy - 1, sx + w + 1, sy + NodeView.HEIGHT + 1, COLOR_MOB);
        Draw.fill(sx, sy, sx + w, sy + NodeView.HEIGHT, COLOR_NODE_BG);

        EntityIcon.render(sx + 1, sy + 1, 28, NodeView.HEIGHT - 2, src.mob.entityId);

        int textX = sx + 32;
        int textRight = sx + w - 4;
        if (view.node.mobSources.size() > 1) {
            String badge = "[" + (Math.floorMod(view.node.mobIndex, view.node.mobSources.size()) + 1)
                    + "/" + view.node.mobSources.size() + "]";
            int bw = this.fontRenderer.getStringWidth(badge);
            Draw.string(this.fontRenderer, badge, textRight - bw, sy + 5, COLOR_MOB, false);
            textRight -= bw + 4;
        }
        String name = Draw.ellipsize(this.fontRenderer, src.mob.mobName, Math.max(0, textRight - textX));
        Draw.string(this.fontRenderer, name, textX, sy + 5, 0xFFFFFF, false);
        String sub = Draw.trimToWidth(this.fontRenderer, mobDropLabel(src.drop), Math.max(0, sx + w - 4 - textX));
        Draw.string(this.fontRenderer, sub, textX, sy + 17, 0xB0B0B0, false);
    }

    private void drawEdge(NodeView parent, NodeView child, double alpha) {
        if (child == null || alpha <= 0.001) return;
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        int color = (a << 24) | (COLOR_EDGE & 0x00FFFFFF);
        int px = parent.x + parent.width;
        int py = parent.y + NodeView.HEIGHT / 2;
        int cx = child.x;
        int cy = child.y + NodeView.HEIGHT / 2;
        int midX = cx - layout.hGap / 2;
        Draw.hLine(Math.min(px, midX), Math.max(px, midX), py, color);
        Draw.vLine(midX, Math.min(py, cy), Math.max(py, cy), color);
        Draw.hLine(Math.min(midX, cx), Math.max(midX, cx), cy, color);
    }

    private void drawTooltip(NodeView view, int mouseX, int mouseY) {
        CraftNode node = view.node;
        if (node.isMobSource()) {
            drawMobTooltip(node, mouseX, mouseY);
            return;
        }
        List<String> lines = new ArrayList<String>();
        lines.add(node.output.getDisplayName());
        if (node == root) {
            lines.add(TextFormatting.BLUE + "End result");
        }
        Integer ownedValue = haveCounts.get(ItemKey.of(node.output));
        int have = ownedValue == null ? 0 : ownedValue;
        Integer emcValue = emcSupplied.get(ItemKey.of(node.output));
        int fromEmc = emcValue == null ? 0 : emcValue;
        String requiredLine = "Required: " + node.requiredCount;
        String requiredStacks = stackBreakdown(node.requiredCount, node.output.getMaxStackSize());
        if (!requiredStacks.isEmpty()) requiredLine += " " + requiredStacks;
        lines.add(TextFormatting.GRAY + requiredLine);
        String availLine = "Available: " + have;
        if (fromEmc > 0) availLine += " (+" + fromEmc + " from EMC)";
        if (node != root && node.freeStock < have) availLine += " (" + node.freeStock + " free after other uses)";
        lines.add(TextFormatting.GRAY + availLine);
        if (node.catalyst) lines.add(TextFormatting.GOLD + "Catalyst - kept, not consumed");
        ItemStack wear = displayStack(node);
        if (wear.isItemDamaged()) {
            int left = wear.getMaxDamage() - wear.getItemDamage();
            lines.add(TextFormatting.GRAY + "Durability: " + left + " / " + wear.getMaxDamage());
        }
        if (isCompleted(node)) {
            String from;
            if (have < node.requiredCount && fromEmc > 0) {
                from = "EMC";
            } else {
                ItemStack src = sourceIcons.get(ItemKey.of(node.output));
                from = (src != null && !src.isEmpty()) ? src.getDisplayName() : "your inventory";
            }
            lines.add(TextFormatting.GREEN + "You have enough - using " + from);
        }
        if (node.isBlockedByStation()) {
            lines.add(TextFormatting.RED + "Needs a " + StationIcons.name(node.requiredStation()) + " nearby");
        }
        if (node.isTagChoice()) {
            lines.add(TextFormatting.AQUA + "Accepts (W/S or Up/Down to switch):");
            int shown = 0;
            for (ItemStack option : node.tagOptions) {
                if (shown++ >= 8) {
                    lines.add(TextFormatting.DARK_GRAY + "   ... +" + (node.tagOptions.size() - 8) + " more");
                    break;
                }
                boolean current = option.getItem() == node.output.getItem()
                        && option.getItemDamage() == node.output.getItemDamage();
                Integer ownedOption = haveCounts.get(ItemKey.of(option));
                int owned = ownedOption == null ? 0 : ownedOption;
                TextFormatting fmt = current ? TextFormatting.WHITE
                        : (owned > 0 ? TextFormatting.GREEN : TextFormatting.GRAY);
                String prefix = current ? " > " : "    ";
                String suffix = owned > 0 ? " (" + owned + ")" : "";
                lines.add(fmt + prefix + option.getDisplayName() + suffix);
            }
        }
        if (node.emcBuy) {
            long unit = emcSource == null ? 0L : emcSource.value(node.output);
            String each = unit > 0L ? " - " + ProjectEClient.format(BigInteger.valueOf(unit)) + " each" : "";
            lines.add(TextFormatting.AQUA + "Bought with EMC" + each);
            if (!isCompleted(node)) lines.add(TextFormatting.RED + "Not enough EMC");
        }
        if (node.reference != null) {
            lines.add(TextFormatting.DARK_AQUA + "Recipe expanded at its first use in this tree");
        }
        if (node.truncated) {
            lines.add(TextFormatting.RED + "Tree limit reached - raise maxTreeNodes or maxTreeDepth in the config");
        }
        if (node.isCraftable()) {
            RecipeOption selected = node.selected();
            if (selected != null) {
                lines.add(TextFormatting.DARK_GRAY + "Recipe: " + selected.id());
                lines.add(TextFormatting.DARK_GRAY + "Yields " + node.resultPerCraft + " per craft ("
                        + StationIcons.name(selected.station()) + ")");
            } else {
                lines.add(TextFormatting.YELLOW + "Supplying yourself (not crafted)");
            }
            if (node.alternatives.size() > 1) {
                lines.add(TextFormatting.AQUA + "Left-click: swap recipe (" + node.alternatives.size() + " options)");
            }
            if (hasEye(node)) {
                lines.add(TextFormatting.AQUA + "Eye icon: show or hide the crafting tree");
            } else if (node != root && !node.truncated && node.reference == null && !node.emcBuy) {
                boolean expanded = !node.children.isEmpty();
                lines.add(TextFormatting.AQUA + (expanded
                        ? "Right-click: hide recipe (supply yourself)"
                        : "Right-click: reveal recipe"));
            }
        } else {
            lines.add(TextFormatting.DARK_GRAY + "Base material - no recipe");
            String origins = originLabel(node);
            if (!origins.isEmpty()) lines.add(TextFormatting.GRAY + "Made in: " + origins);
        }
        if (!ItemKey.of(node.output).equals(ItemKey.of(target))) {
            lines.add(TextFormatting.AQUA + "Middle-click: view this item's tree");
        }
        Draw.tooltip(this.fontRenderer, lines, mouseX, mouseY);
    }

    private void drawMobTooltip(CraftNode node, int mouseX, int mouseY) {
        MobItemSource src = node.currentMob();
        MobDropInfo mob = src.mob;
        List<String> lines = new ArrayList<String>();
        lines.add(TextFormatting.WHITE + mob.mobName);
        lines.add(TextFormatting.GRAY + "Drops " + node.output.getDisplayName() + ": " + mobDropLabel(src.drop));
        if (!mob.biomes.isEmpty()) {
            List<String> shownBiomes = mob.biomes.size() > 6 ? mob.biomes.subList(0, 6) : mob.biomes;
            StringBuilder biomes = new StringBuilder();
            for (int i = 0; i < shownBiomes.size(); i++) {
                if (i > 0) biomes.append(", ");
                biomes.append(shownBiomes.get(i));
            }
            if (mob.biomes.size() > 6) biomes.append(", ...");
            lines.add(TextFormatting.GRAY + "Biomes: " + biomes);
        }
        if (mob.lightLevel != null && !mob.lightLevel.isEmpty()) {
            lines.add(TextFormatting.GRAY + mob.lightLevel);
        }
        if (mob.exp != null && !mob.exp.isEmpty()) {
            lines.add(TextFormatting.GRAY + "Experience: " + mob.exp);
        }
        lines.add(TextFormatting.AQUA + "All drops:");
        int shown = 0;
        for (DropLine drop : mob.drops) {
            if (shown++ >= 12) {
                lines.add(TextFormatting.DARK_GRAY + "   ... +" + (mob.drops.size() - 12) + " more");
                break;
            }
            lines.add(TextFormatting.DARK_GRAY + "  " + drop.item.getDisplayName() + "  " + mobDropLabel(drop));
        }
        if (node.mobSources.size() > 1) {
            lines.add(TextFormatting.AQUA + "Left-click: next mob (" + node.mobSources.size() + ")");
        }
        Draw.tooltip(this.fontRenderer, lines, mouseX, mouseY);
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
        if (layout == null) return null;
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
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (depositMenuOpen) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                depositMenuOpen = false;
                return;
            }
            if (keyCode == Keyboard.KEY_BACK) {
                if (!depositSearch.isEmpty()) depositSearch = depositSearch.substring(0, depositSearch.length() - 1);
                depositScroll = 0;
                return;
            }
            if (typedChar >= ' ' && typedChar != 167) {
                depositSearch += typedChar;
                depositScroll = 0;
                return;
            }
        }
        if (copyMenuOpen) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                copyMenuOpen = false;
                return;
            }
            int dir = tagDirection(keyCode);
            if (dir != 0) {
                copySelected = Math.floorMod(copySelected + dir, COPY_OPTIONS.length);
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                doCopy(copySelected);
                copyMenuOpen = false;
                return;
            }
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            close();
            return;
        }

        boolean typing = quantityBox.isFocused();
        if (!typing && hoveringStationName && missingStation != null) {
            int dir = tagDirection(keyCode);
            if (dir != 0) {
                int count = StationProviders.icons(missingStation).size();
                if (count > 0) stationSelectedIndex = Math.floorMod(stationSelectedIndex + dir, count);
                return;
            }
        }
        if (!typing && hoveredView != null && hoveredView.node.isTagChoice()) {
            int dir = tagDirection(keyCode);
            if (dir != 0) {
                cycleTagOption(hoveredView.node, dir);
                return;
            }
        }
        if (!typing && hoveredView != null && QuickCraftIntegrations.canShowRecipes()) {
            if (QuickCraftKeys.matches(QuickCraftKeys.SHOW_RECIPE, keyCode)) {
                QuickCraftIntegrations.showRecipe(hoveredView.node.output);
                return;
            }
            if (QuickCraftKeys.matches(QuickCraftKeys.SHOW_USES, keyCode)) {
                QuickCraftIntegrations.showUses(hoveredView.node.output);
                return;
            }
        }

        if (quantityBox.textboxKeyTyped(typedChar, keyCode)) return;
        super.keyTyped(typedChar, keyCode);
    }

    private static int tagDirection(int keyCode) {
        if (keyCode == Keyboard.KEY_W || keyCode == Keyboard.KEY_UP) return -1;
        if (keyCode == Keyboard.KEY_S || keyCode == Keyboard.KEY_DOWN) return 1;
        return 0;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && overDepositBox(mouseX, mouseY)) {
            toggleDepositMenu();
            playClick();
            return;
        }
        if (depositMenuOpen && mouseButton == 0) {
            if (overDepositMenu(mouseX, mouseY)) {
                handleDepositRowClick(mouseY);
                return;
            }
            depositMenuOpen = false;
        }
        if (mouseButton == 0 && overControlsArea(mouseX, mouseY)) {
            return;
        }
        if (mouseButton == 0 && overHistoryTab(mouseX, mouseY)) {
            historyOpen = !historyOpen;
            if (historyOpen) {
                historyEntries = CraftHistory.entries();
                historyScroll = 0;
            }
            playClick();
            return;
        }
        if (historyOpen && mouseButton == 0) {
            if (overHistoryPanel(mouseX, mouseY)) {
                int idx = historyRowAt(mouseX, mouseY);
                if (idx >= 0 && idx < historyEntries.size()) {
                    ItemStack chosen = historyEntries.get(idx).stack().copy();
                    historyOpen = false;
                    playClick();
                    retarget(chosen, true);
                }
                return;
            }
            historyOpen = false;
        }
        if (mouseButton == 0 && BookmarkOverlay.handleClick(this.width, this.height, mouseX, mouseY)) {
            playClick();
            return;
        }
        if (mouseButton == 0 && BookmarkOverlay.isActive()
                && BookmarkOverlay.overHeaderDragZone(this.width, this.height, mouseX, mouseY)) {
            double[] origin = BookmarkOverlay.origin(this.width, this.height);
            bmGrabX = mouseX - origin[0];
            bmGrabY = mouseY - origin[1];
            bookmarkDragging = true;
            return;
        }
        if (mouseButton == 0 && overCopyButton(mouseX, mouseY)) {
            copyMenuOpen = !copyMenuOpen;
            if (copyMenuOpen) copySelected = 0;
            playClick();
            return;
        }
        if (mouseButton == 0 && copyMenuOpen) {
            int row = copyRowAt(mouseX, mouseY);
            if (row >= 0) {
                doCopy(row);
                copyMenuOpen = false;
                return;
            }
            copyMenuOpen = false;
        }
        if (mouseButton == 0 && showSummary && overSummaryPin(mouseX, mouseY)) {
            BookmarkOverlay.set(summaryItems);
            playClick();
            return;
        }

        quantityBox.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (showSummary && overSummaryPanel(mouseX, mouseY)) return;
        if (overButtons(mouseX, mouseY)) return;

        NodeView view = nodeAt(mouseX, mouseY);
        if (view != null) {
            if (mouseButton == 0 && hasEye(view.node) && overEye(view, mouseX, mouseY)) {
                togglePeek(view.node);
                return;
            }
            handleNodeClick(view.node, mouseButton);
            return;
        }
        if (mouseButton == 0) {
            panning = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
        }
    }

    private boolean overButtons(int mouseX, int mouseY) {
        for (GuiButton button : this.buttonList) {
            if (button.visible && mouseX >= button.x && mouseY >= button.y
                    && mouseX < button.x + button.width && mouseY < button.y + button.height) {
                return true;
            }
        }
        return quantityBox.getVisible() && mouseX >= this.width - 146 && mouseX <= this.width - 100
                && mouseY >= 4 && mouseY <= 20;
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
        if (isCtrlKeyDown()) {
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
            ResourceLocation choice;
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
            ResourceLocation chosen = node.alternatives.get(next).id();
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
            if (options.get(i).getItem() == node.output.getItem()
                    && options.get(i).getItemDamage() == node.output.getItemDamage()) {
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
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (bookmarkDragging && clickedMouseButton == 0) {
            BookmarkOverlay.setOrigin(this.width, this.height, mouseX - bmGrabX, mouseY - bmGrabY);
            return;
        }
        if (panning && clickedMouseButton == 0) {
            panX += mouseX - lastDragX;
            panY += mouseY - lastDragY;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            panning = false;
            bookmarkDragging = false;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int delta = Integer.signum(wheel);

        if (depositMenuOpen && overDepositMenu(mouseX, mouseY)) {
            depositScroll = Math.max(0, depositScroll - delta);
            return;
        }
        if (controlsHovered && overControlsPanelRect(mouseX, mouseY)) {
            return;
        }
        if (historyOpen && overHistoryPanel(mouseX, mouseY)) {
            historyScroll = Math.max(0, historyScroll - delta);
            return;
        }
        if (BookmarkOverlay.isActive() && BookmarkOverlay.overPanel(this.width, this.height, mouseX, mouseY)) {
            BookmarkOverlay.scroll(-delta);
            return;
        }
        if (showSummary && overSummaryPanel(mouseX, mouseY)) {
            summaryScroll = Math.max(0, summaryScroll - delta);
            return;
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
    }

    private void onConfirm() {
        if (QuickCraftConfig.creativeBypass() && this.mc.player != null
                && this.mc.player.capabilities.isCreativeMode) {
            CraftHistory.record(target, quantity);
            QuickCraftNetwork.sendCraftRequest(target, quantity, overrides, ingredientChoices,
                    ClientDepositTargets.selectedId());
            closeAfterCraft();
            return;
        }
        if (maxMode) {
            onCraftMax();
            return;
        }
        if (confirmPending) return;
        confirmPending = true;
        QuickCraftNetwork.sendCraftPreviewRequest(target, quantity, overrides, ingredientChoices);
    }

    public void onCraftPreviewResult(CraftPreview.Result preview) {
        confirmPending = false;
        if (preview.full()) {
            CraftHistory.record(target, quantity);
            QuickCraftNetwork.sendCraftRequest(target, quantity, overrides, ingredientChoices,
                    ClientDepositTargets.selectedId());
            closeAfterCraft();
            return;
        }
        this.mc.displayGuiScreen(
                new ForceCraftConfirmScreen(this, target, quantity, overrides, ingredientChoices, preview));
    }

    private void onCraftMax() {
        CraftHistory.record(target, maxCraftable());
        QuickCraftNetwork.sendCraftRequest(target, CRAFT_MAX, overrides, ingredientChoices,
                ClientDepositTargets.selectedId());
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
        return text.length() <= max ? text : text.substring(0, max - 1) + "...";
    }
}
