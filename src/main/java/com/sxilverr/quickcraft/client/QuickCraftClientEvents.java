package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.config.QuickCraftConfig;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class QuickCraftClientEvents {

    private static int pollTimer;
    private static int scrollTimer;

    private QuickCraftClientEvents() {
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (BookmarkOverlay.isActive() && ++pollTimer >= 20) {
            pollTimer = 0;
            Services.PLATFORM.requestAvailability(BookmarkOverlay.requestedKeys());
        }

        if (mc.screen != null) return;

        while (QuickCraftClient.OPEN_KEY.consumeClick()) {
            ItemStack target = mc.player.getMainHandItem();
            if (target.isEmpty()) continue;
            if (QuickCraftConfig.openOnlyWithRecipe() && mc.level != null
                    && !ClientRecipeCache.hasRecipe(mc.level, target)) continue;
            mc.setScreen(new QuickCraftScreen(target.copy(), 1));
        }

        if (BookmarkOverlay.isActive()) {
            boolean up = QuickCraftClient.SCROLL_UP_KEY.isDown();
            boolean down = QuickCraftClient.SCROLL_DOWN_KEY.isDown();
            if (up ^ down) {
                if (scrollTimer <= 0) {
                    BookmarkOverlay.scroll(up ? -1 : 1);
                    scrollTimer = 3;
                } else {
                    scrollTimer--;
                }
            } else {
                scrollTimer = 0;
            }
        }
    }

    public static void onRenderGui(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || !BookmarkOverlay.isActive()) return;
        BookmarkOverlay.render(graphics,
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), false, 0, 0);
    }

    public static boolean onScreenKeyPressed(Screen screen, Slot hovered, int keyCode, int scanCode) {
        if (screen instanceof QuickCraftScreen) return false;
        if (!QuickCraftClient.OPEN_KEY.matches(keyCode, scanCode)) return false;
        if (TextInputGuard.isTyping(screen)) return false;
        return tryOpenFromScreen(screen, hovered);
    }

    public static boolean onScreenMousePressed(Screen screen, Slot hovered, int button) {
        if (screen instanceof QuickCraftScreen) return false;
        if (!QuickCraftClient.OPEN_KEY.matchesMouse(button)) return false;
        return tryOpenFromScreen(screen, hovered);
    }

    private static boolean tryOpenFromScreen(Screen screen, Slot hovered) {
        ItemStack target = hoveredTarget(hovered);
        if (target.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (QuickCraftConfig.openOnlyWithRecipe() && mc.level != null
                && !ClientRecipeCache.hasRecipe(mc.level, target)) {
            return true;
        }
        if (screen instanceof AbstractContainerScreen<?> && mc.player != null) {
            mc.player.closeContainer();
        }
        mc.setScreen(new QuickCraftScreen(target.copy(), 1));
        return true;
    }

    private static ItemStack hoveredTarget(Slot hovered) {
        ItemStack jei = QuickCraftIntegrations.hoveredItem();
        if (!jei.isEmpty()) return jei;
        return hovered != null && hovered.hasItem() ? hovered.getItem() : ItemStack.EMPTY;
    }
}
