package com.sxilverr.quickcraft.forge.client;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.client.QuickCraftClient;
import com.sxilverr.quickcraft.client.QuickCraftClientEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = QuickCraftCommon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ForgeClientEvents {

    private ForgeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) QuickCraftClientEvents.onClientTick();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        QuickCraftClientEvents.onRenderGui(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (QuickCraftClientEvents.onScreenKeyPressed(event.getScreen(), hovered(event.getScreen()), event.getKeyCode(), event.getScanCode())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (QuickCraftClientEvents.onScreenMousePressed(event.getScreen(), hovered(event.getScreen()), event.getButton())) event.setCanceled(true);
    }

    private static Slot hovered(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> container ? container.getSlotUnderMouse() : null;
    }

    @Mod.EventBusSubscriber(modid = QuickCraftCommon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            QuickCraftClient.KEYS.forEach(event::register);
        }
    }
}
