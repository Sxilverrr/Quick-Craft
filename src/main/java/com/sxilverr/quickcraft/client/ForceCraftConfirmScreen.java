package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.craft.CraftPlanner;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ForceCraftConfirmScreen extends GuiScreen {
    private static final int COLOR_TARGET = 0xFF55FF55;
    private static final int COLOR_PANEL = 0xF0080808;
    private static final int COLOR_PANEL_HEAD = 0xF0202020;
    private static final int FORCE_W = 150;
    private static final int FORCE_H = 24;
    private static final int MAX_ROWS = 7;
    private static final int MAX_BLOCKER_LINES = 4;
    private static final int PANEL_MIN_W = 224;
    private static final String EMPTY_LINE = "Not enough materials to make any items";

    private static final int ID_FORCE = 0;
    private static final int ID_BACK = 1;

    private final GuiScreen parent;
    private final ItemStack target;
    private final int quantity;
    private final Map<ItemKey, ResourceLocation> overrides;
    private final Map<String, Item> ingredientChoices;
    private final CraftPreview.Result preview;

    private int forceX;
    private int forceY;
    private boolean sent;

    public ForceCraftConfirmScreen(GuiScreen parent, ItemStack target, int quantity,
                                   Map<ItemKey, ResourceLocation> overrides, Map<String, Item> ingredientChoices,
                                   CraftPreview.Result preview) {
        this.parent = parent;
        this.target = target;
        this.quantity = quantity;
        this.overrides = overrides;
        this.ingredientChoices = ingredientChoices;
        this.preview = preview;
    }

    @Override
    public void initGui() {
        this.fontRenderer = BatchedFontRenderer.get(this.mc);
        this.buttonList.clear();
        forceX = this.width / 2 - FORCE_W / 2;
        forceY = this.height / 2 - 34 + blockerLines() * 10;

        this.buttonList.add(new ScalingButton(ID_FORCE, forceX, forceY, FORCE_W, FORCE_H, "Force Craft"));
        this.buttonList.add(new ScalingButton(ID_BACK, this.width / 2 - 50, this.height - 36, 100, 20, "Back"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == ID_FORCE) forceCraft();
        else if (button.id == ID_BACK) close();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int cx = this.width / 2;
        Draw.centeredString(this.fontRenderer, "You do not have enough items to craft this fully",
                cx, this.height / 2 - 78, 0xFFFF5555);

        String sub = "Can craft " + preview.craftable() + " of " + quantity + " x " + target.getDisplayName();
        Draw.centeredString(this.fontRenderer, sub, cx, this.height / 2 - 62, 0xFFC0C0C0);
        Draw.centeredString(this.fontRenderer, "Force Craft makes as many items as your materials allow.",
                cx, this.height / 2 - 50, 0xFF909090);
        renderBlockers(cx);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (overForceButton(mouseX, mouseY)) {
            renderAcquire(cx);
        }
    }

    private int blockerLines() {
        int n = preview.blockers().size();
        return n == 0 ? 0 : Math.min(n, MAX_BLOCKER_LINES) + (n > MAX_BLOCKER_LINES ? 1 : 0);
    }

    private void renderBlockers(int cx) {
        List<CraftPlanner.Blocker> list = preview.blockers();
        if (list.isEmpty()) return;
        int y = this.height / 2 - 38;
        int shown = Math.min(list.size(), MAX_BLOCKER_LINES);
        for (int i = 0; i < shown; i++) {
            CraftPlanner.Blocker blocker = list.get(i);
            String line = "Missing " + blocker.missing() + "x " + trim(blocker.key().toStack(1).getDisplayName(), 28)
                    + blocker.reason().label;
            Draw.centeredString(this.fontRenderer, line, cx, y, 0xFFFF5555);
            y += 10;
        }
        if (list.size() > shown) {
            Draw.centeredString(this.fontRenderer, "... +" + (list.size() - shown) + " more", cx, y, 0xFF909090);
        }
    }

    private boolean overForceButton(int mouseX, int mouseY) {
        return mouseX >= forceX && mouseX <= forceX + FORCE_W
                && mouseY >= forceY && mouseY <= forceY + FORCE_H;
    }

    private void renderAcquire(int cx) {
        List<CraftPreview.Gain> list = preview.gained();
        int rowH = 18;
        int headerH = 15;
        int shown = Math.min(list.size(), MAX_ROWS);
        boolean more = list.size() > shown;
        int bodyRows = list.isEmpty() ? 1 : shown;
        int panelH = headerH + bodyRows * rowH + (more ? 11 : 0) + 4;
        String moreLabel = "... +" + (list.size() - shown) + " more";
        int panelW = panelWidth(list, shown, more, moreLabel);

        int px = cx - panelW / 2;
        int py = forceY + FORCE_H + 6;

        Draw.fill(px, py, px + panelW, py + panelH, COLOR_PANEL);
        Draw.fill(px, py, px + panelW, py + headerH, COLOR_PANEL_HEAD);
        Draw.string(this.fontRenderer, "Acquire items:", px + 6, py + 4, 0xFFFFFFFF, false);

        if (list.isEmpty()) {
            Draw.string(this.fontRenderer, EMPTY_LINE, px + 8, py + headerH + 5, 0xFFAAAAAA, false);
            return;
        }

        int y = py + headerH + 2;
        for (int i = 0; i < shown; i++) {
            CraftPreview.Gain gain = list.get(i);
            Draw.item(gain.toStack(), px + 5, y);
            int color = gain.key().item() == target.getItem() ? COLOR_TARGET : 0xFFFFFFFF;
            Draw.string(this.fontRenderer, rowLabel(gain), px + 26, y + 4, color, false);
            y += rowH;
        }
        if (more) {
            Draw.string(this.fontRenderer, moreLabel, px + 8, y + 1, 0xFF909090, false);
        }
    }

    private int panelWidth(List<CraftPreview.Gain> list, int shown, boolean more, String moreLabel) {
        int w = this.fontRenderer.getStringWidth("Acquire items:") + 12;
        if (list.isEmpty()) {
            w = Math.max(w, this.fontRenderer.getStringWidth(EMPTY_LINE) + 16);
        } else {
            for (int i = 0; i < shown; i++) {
                w = Math.max(w, this.fontRenderer.getStringWidth(rowLabel(list.get(i))) + 34);
            }
            if (more) w = Math.max(w, this.fontRenderer.getStringWidth(moreLabel) + 16);
        }
        return Math.max(PANEL_MIN_W, w);
    }

    private static String rowLabel(CraftPreview.Gain gain) {
        return trim(gain.toStack().getDisplayName(), 22) + "  x" + gain.count();
    }

    private void forceCraft() {
        if (sent) return;
        sent = true;
        CraftHistory.record(target, preview.craftable());
        QuickCraftNetwork.sendCraftRequest(target, quantity, overrides, ingredientChoices,
                ClientDepositTargets.selectedId());
        Minecraft.getMinecraft().displayGuiScreen(null);
    }

    private void close() {
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static String trim(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "...";
    }
}
