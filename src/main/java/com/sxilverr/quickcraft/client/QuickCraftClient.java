package com.sxilverr.quickcraft.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class QuickCraftClient {
    public static final KeyMapping OPEN_KEY = new KeyMapping(
            "key.quickcraft.open", GLFW.GLFW_KEY_G, "key.categories.quickcraft");
    public static final KeyMapping SCROLL_UP_KEY = new KeyMapping(
            "key.quickcraft.scroll_up", GLFW.GLFW_KEY_UP, "key.categories.quickcraft");
    public static final KeyMapping SCROLL_DOWN_KEY = new KeyMapping(
            "key.quickcraft.scroll_down", GLFW.GLFW_KEY_DOWN, "key.categories.quickcraft");
    public static final KeyMapping SHOW_RECIPE_KEY = new KeyMapping(
            "key.quickcraft.show_recipe", GLFW.GLFW_KEY_R, "key.categories.quickcraft");
    public static final KeyMapping SHOW_USES_KEY = new KeyMapping(
            "key.quickcraft.show_uses", GLFW.GLFW_KEY_U, "key.categories.quickcraft");

    public static final List<KeyMapping> KEYS = List.of(OPEN_KEY, SCROLL_UP_KEY, SCROLL_DOWN_KEY, SHOW_RECIPE_KEY, SHOW_USES_KEY);

    private QuickCraftClient() {
    }
}
