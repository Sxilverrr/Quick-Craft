package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class BookmarkOverlay {
    private static final float SCALE = 0.75f;
    private static final int MARGIN = 4;
    private static final int LOCAL_W = 100;
    private static final int HEADER_H = 13;
    private static final int ROW_H = 18;
    private static final int VISIBLE_ROWS = 8;
    private static final long APPEAR_ROW_STAGGER = 30;
    private static final long APPEAR_ROW_SLIDE = 170;
    private static final int COLOR_BG = 0xB0080808;
    private static final int COLOR_HEADER = 0xC0202020;
    private static final int X_LX = 2;
    private static final int X_LY = 2;
    private static final int X_LW = 11;
    private static final int X_LH = 10;
    private static final int LOC_LX = LOCAL_W - 24;
    private static final int LOC_LY = 2;
    private static final int LOC_LW = 22;
    private static final int LOC_LH = 10;
    private static final double[][] CORNERS = {{1, 0}, {0, 0}, {0, 1}, {1, 1}};
    private static final String[] CORNER_LABELS = {"TR", "TL", "BL", "BR"};

    private static boolean active;
    private static int cornerIndex;
    private static double fx = 1.0;
    private static double fy = 0.0;
    private static int scroll;
    private static long appearStart;
    private static final List<Map.Entry<ItemKey, Integer>> entries = new ArrayList<Map.Entry<ItemKey, Integer>>();
    private static final Map<ItemKey, Integer> availability = new HashMap<ItemKey, Integer>();
    private static boolean hasAvailability;

    private BookmarkOverlay() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void set(List<Map.Entry<ItemKey, Integer>> items) {
        entries.clear();
        for (Map.Entry<ItemKey, Integer> e : items) {
            entries.add(new AbstractMap.SimpleImmutableEntry<ItemKey, Integer>(e.getKey(), e.getValue()));
        }
        sortByAvailability();
        active = !entries.isEmpty();
        scroll = 0;
        appearStart = now();
    }

    public static void clear() {
        active = false;
        entries.clear();
        scroll = 0;
    }

    public static void setAvailability(Map<ItemKey, Integer> counts) {
        availability.putAll(counts);
        hasAvailability = true;
        sortByAvailability();
    }

    private static void sortByAvailability() {
        if (entries.size() < 2) return;
        Map<ItemKey, Integer> have = availabilityCounts();
        entries.sort(Comparator
                .comparingInt((Map.Entry<ItemKey, Integer> e) -> tier(have, e.getKey(), e.getValue()))
                .thenComparingInt(e -> -e.getValue()));
    }

    private static int tier(Map<ItemKey, Integer> have, ItemKey key, int need) {
        Integer value = have.get(key);
        int has = value == null ? 0 : value;
        if (has >= need) return 0;
        if (has > 0) return 1;
        return 2;
    }

    private static Map<ItemKey, Integer> availabilityCounts() {
        if (hasAvailability) return availability;
        Minecraft mc = Minecraft.getMinecraft();
        return mc.player == null ? Collections.<ItemKey, Integer>emptyMap() : inventoryCounts(mc.player.inventory);
    }

    public static List<ItemKey> requestedKeys() {
        return entries.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public static void cycleCorner() {
        cornerIndex = (cornerIndex + 1) % CORNERS.length;
        fx = CORNERS[cornerIndex][0];
        fy = CORNERS[cornerIndex][1];
    }

    public static void scroll(int delta) {
        scroll = MathHelper.clamp(scroll + delta, 0, maxScroll());
    }

    public static boolean overPanel(int screenW, int screenH, double mx, double my) {
        double ox = originX(screenW);
        double oy = originY(screenH);
        return mx >= ox && mx <= ox + scaledW() && my >= oy && my <= oy + scaledH();
    }

    public static boolean overHeaderDragZone(int screenW, int screenH, double mx, double my) {
        double ox = originX(screenW);
        double oy = originY(screenH);
        boolean inHeader = mx >= ox && mx <= ox + scaledW() && my >= oy && my <= oy + HEADER_H * SCALE;
        return inHeader
                && !inLocalRect(mx, my, ox, oy, X_LX, X_LY, X_LW, X_LH)
                && !inLocalRect(mx, my, ox, oy, LOC_LX, LOC_LY, LOC_LW, LOC_LH);
    }

    public static double[] origin(int screenW, int screenH) {
        return new double[]{originX(screenW), originY(screenH)};
    }

    public static void setOrigin(int screenW, int screenH, double ox, double oy) {
        double rangeX = Math.max(1, screenW - scaledW() - 2 * MARGIN);
        double rangeY = Math.max(1, screenH - scaledH() - 2 * MARGIN);
        fx = MathHelper.clamp((ox - MARGIN) / rangeX, 0.0, 1.0);
        fy = MathHelper.clamp((oy - MARGIN) / rangeY, 0.0, 1.0);
    }

    public static boolean handleClick(int screenW, int screenH, double mx, double my) {
        if (!active) return false;
        double ox = originX(screenW);
        double oy = originY(screenH);
        if (inLocalRect(mx, my, ox, oy, X_LX, X_LY, X_LW, X_LH)) {
            clear();
            return true;
        }
        if (inLocalRect(mx, my, ox, oy, LOC_LX, LOC_LY, LOC_LW, LOC_LH)) {
            cycleCorner();
            return true;
        }
        return false;
    }

    public static void render(int screenW, int screenH, boolean interactive, int mouseX, int mouseY) {
        if (!active) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        Map<ItemKey, Integer> have = hasAvailability ? availability : inventoryCounts(mc.player.inventory);

        double ox = originX(screenW);
        double oy = originY(screenH);
        scroll = MathHelper.clamp(scroll, 0, maxScroll());

        Draw.push();
        Draw.translate(ox, oy, 0);
        Draw.scale(SCALE, SCALE, 1.0);

        int h = localHeight();
        Draw.fill(0, 0, LOCAL_W, h, COLOR_BG);
        Draw.fill(0, 0, LOCAL_W, HEADER_H, COLOR_HEADER);

        boolean xHover = interactive && inLocalRect(mouseX, mouseY, ox, oy, X_LX, X_LY, X_LW, X_LH);
        Draw.fill(X_LX, X_LY, X_LX + X_LW, X_LY + X_LH, xHover ? 0xFFAA3030 : 0x80000000);
        Draw.string(mc.fontRenderer, "x", X_LX + 3, X_LY + 1, 0xFFFFFFFF, false);

        boolean locHover = interactive && inLocalRect(mouseX, mouseY, ox, oy, LOC_LX, LOC_LY, LOC_LW, LOC_LH);
        Draw.fill(LOC_LX, LOC_LY, LOC_LX + LOC_LW, LOC_LY + LOC_LH, locHover ? 0xFF3060AA : 0x80000000);
        Draw.string(mc.fontRenderer, CORNER_LABELS[cornerIndex], LOC_LX + 3, LOC_LY + 1, 0xFFFFFFFF, false);

        int bodyTop = HEADER_H + 1;
        boolean anim = QuickCraftConfig.pinnedListAnimation();
        long elapsed = now() - appearStart;

        Draw.scissorOn((int) Math.floor(ox), (int) Math.floor(oy + bodyTop * SCALE),
                (int) Math.ceil(scaledW()), (int) Math.ceil((h - bodyTop) * SCALE));
        int vis = visibleRows();
        for (int r = 0; r < vis; r++) {
            int idx = r + scroll;
            if (idx >= entries.size()) break;
            Map.Entry<ItemKey, Integer> entry = entries.get(idx);
            ItemStack stack = entry.getKey().toStack(1);
            int need = entry.getValue();
            Integer owned = have.get(entry.getKey());
            int has = owned == null ? 0 : owned;
            int y = bodyTop + r * ROW_H;
            int rx = 0;
            if (anim) {
                double p = MathHelper.clamp((elapsed - (long) r * APPEAR_ROW_STAGGER) / (double) APPEAR_ROW_SLIDE, 0.0, 1.0);
                rx = (int) ((1 - easeIn(p)) * LOCAL_W);
            }
            Draw.item(stack, rx + 2, y);
            int color = has >= need ? QuickCraftConfig.colorAvailable()
                    : (has > 0 ? QuickCraftConfig.colorCrafted() : QuickCraftConfig.colorMissing());
            Draw.string(mc.fontRenderer, has + "/" + need, rx + 21, y + 4, color, false);
        }
        Draw.scissorOff();

        if (scroll > 0) {
            Draw.string(mc.fontRenderer, "^", LOCAL_W - 8, HEADER_H + 2, 0xFFFFFFFF, false);
        }
        if (scroll < maxScroll()) {
            Draw.string(mc.fontRenderer, "v", LOCAL_W - 8, h - 9, 0xFFFFFFFF, false);
        }

        Draw.pop();
    }

    private static long now() {
        return Minecraft.getSystemTime();
    }

    private static int maxScroll() {
        return Math.max(0, entries.size() - VISIBLE_ROWS);
    }

    private static int visibleRows() {
        return Math.min(VISIBLE_ROWS, entries.size());
    }

    private static int localHeight() {
        return HEADER_H + visibleRows() * ROW_H + 3;
    }

    private static double scaledW() {
        return LOCAL_W * SCALE;
    }

    private static double scaledH() {
        return localHeight() * SCALE;
    }

    private static double originX(int screenW) {
        return MARGIN + MathHelper.clamp(fx, 0.0, 1.0) * Math.max(0, screenW - scaledW() - 2 * MARGIN);
    }

    private static double originY(int screenH) {
        return MARGIN + MathHelper.clamp(fy, 0.0, 1.0) * Math.max(0, screenH - scaledH() - 2 * MARGIN);
    }

    private static boolean inLocalRect(double mouseX, double mouseY, double ox, double oy,
                                       int lx, int ly, int lw, int lh) {
        double x = ox + lx * SCALE;
        double y = oy + ly * SCALE;
        return mouseX >= x && mouseX <= x + lw * SCALE && mouseY >= y && mouseY <= y + lh * SCALE;
    }

    private static double easeIn(double p) {
        return p * p * p;
    }

    static Map<ItemKey, Integer> inventoryCounts(InventoryPlayer inv) {
        Map<ItemKey, Integer> counts = new HashMap<ItemKey, Integer>();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ItemKey key = ItemKey.of(stack);
            Integer existing = counts.get(key);
            counts.put(key, existing == null ? stack.getCount() : existing + stack.getCount());
        }
        return counts;
    }
}
