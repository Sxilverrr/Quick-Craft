package com.sxilverr.quickcraft.neoforge.client;

import com.sxilverr.quickcraft.client.QuickCraftClient;
import com.sxilverr.quickcraft.client.QuickCraftClientEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeClientEvents {

    private NeoForgeClientEvents() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(NeoForgeClientEvents::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientEvents::onRenderGui);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientEvents::onScreenKeyPressed);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientEvents::onScreenMousePressed);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        QuickCraftClient.KEYS.forEach(event::register);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        QuickCraftClientEvents.onClientTick();
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        QuickCraftClientEvents.onRenderGui(event.getGuiGraphics());
    }

    private static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (QuickCraftClientEvents.onScreenKeyPressed(event.getScreen(), hovered(event.getScreen()), event.getKeyCode(), event.getScanCode())) event.setCanceled(true);
    }

    private static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (QuickCraftClientEvents.onScreenMousePressed(event.getScreen(), hovered(event.getScreen()), event.getButton())) event.setCanceled(true);
    }

    private static Slot hovered(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> container ? container.getSlotUnderMouse() : null;
    }
}
