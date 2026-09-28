package com.sxilverr.quickcraft.config;

import com.sxilverr.quickcraft.platform.Services;

import java.util.function.Supplier;

public final class QuickCraftClientConfig {
    private static Supplier<String> colorAvailable;
    private static Supplier<String> colorCrafted;
    private static Supplier<String> colorMissing;
    private static Supplier<String> colorNoStation;
    private static Supplier<String> colorTarget;
    private static Supplier<String> colorNodeBackground;
    private static Supplier<String> colorLines;
    private static Supplier<Boolean> entitySpin;
    private static Supplier<Boolean> entityIdle;
    private static Supplier<Boolean> entityWalk;
    private static Supplier<Double> entitySpinSpeed;
    private static Supplier<Boolean> showEmc;

    private QuickCraftClientConfig() {
    }

    public static void define(ConfigBuilder b) {
        colorAvailable = color(b, "colorAvailable", "FF55FF55", "Color for items you already have. Hex like FF55FF55 or 55FF55.");
        colorCrafted = color(b, "colorCrafted", "FFFFC64B", "Color for items that will be crafted.");
        colorMissing = color(b, "colorMissing", "FFFF5555", "Color for missing materials.");
        colorNoStation = color(b, "colorNoStation", "FF5A5A5A", "Color for steps you have no station to craft.");
        colorTarget = color(b, "colorTarget", "FF4AA3FF", "Color for the item you are crafting.");
        colorNodeBackground = color(b, "colorNodeBackground", "F01A1A1A", "Fill color behind each step.");
        colorLines = color(b, "colorLines", "FF7A7A7A", "Color of the lines linking steps.");
        if (Services.PLATFORM.isModLoaded("jeresources")) {
            b.comment("3D mob preview, shown when Just Enough Resources is installed.").push("mobPreview");
            entitySpin = b.comment("Spin the mob preview.").define("entitySpin", true);
            entityIdle = b.comment("Play the mob idle animation.").define("entityIdleAnimation", true);
            entityWalk = b.comment("Play the mob walk animation.").define("entityWalkAnimation", false);
            entitySpinSpeed = b.comment("Mob preview spin speed multiplier.").defineInRange("entitySpinSpeed", 1.0, 0.0, 20.0);
            b.pop();
        }
        if (Services.PLATFORM.isModLoaded("projecte")) {
            b.comment("ProjectE integration, shown when ProjectE is installed.").push("projecte");
            showEmc = b.comment("Show your total EMC in the Quick Craft header when a transmutation table or tablet is available.")
                    .define("showEmc", true);
            b.pop();
        }
    }

    public static boolean showEmc() {
        return showEmc == null || showEmc.get();
    }

    private static Supplier<String> color(ConfigBuilder b, String name, String def, String comment) {
        return b.comment(comment).define(name, def, QuickCraftClientConfig::valid);
    }

    private static boolean valid(Object obj) {
        if (!(obj instanceof String s)) return false;
        String h = clean(s);
        if (h.isEmpty() || h.length() > 8) return false;
        try {
            Long.parseLong(h, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String clean(String s) {
        String h = s.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.length() > 1 && (h.startsWith("0x") || h.startsWith("0X"))) h = h.substring(2);
        if (h.length() == 6) h = "FF" + h;
        return h;
    }

    private static int parse(String s, int def) {
        try {
            return (int) Long.parseLong(clean(s), 16);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int colorAvailable() {
        return parse(colorAvailable.get(), 0xFF55FF55);
    }

    public static int colorCrafted() {
        return parse(colorCrafted.get(), 0xFFFFC64B);
    }

    public static int colorMissing() {
        return parse(colorMissing.get(), 0xFFFF5555);
    }

    public static int colorNoStation() {
        return parse(colorNoStation.get(), 0xFF5A5A5A);
    }

    public static int colorTarget() {
        return parse(colorTarget.get(), 0xFF4AA3FF);
    }

    public static int colorNodeBackground() {
        return parse(colorNodeBackground.get(), 0xF01A1A1A);
    }

    public static int colorLines() {
        return parse(colorLines.get(), 0xFF7A7A7A);
    }

    public static boolean entitySpin() {
        return entitySpin == null || entitySpin.get();
    }

    public static boolean entityIdle() {
        return entityIdle == null || entityIdle.get();
    }

    public static boolean entityWalk() {
        return entityWalk != null && entityWalk.get();
    }

    public static double entitySpinSpeed() {
        return entitySpinSpeed == null ? 1.0 : entitySpinSpeed.get();
    }
}
