package com.sxilverr.quickcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class Draw {
    private static final BufferBuilder FILLS = new BufferBuilder(4096);
    private static final WorldVertexBufferUploader UPLOADER = new WorldVertexBufferUploader();
    private static boolean batchingFills;

    private Draw() {
    }

    public static void beginFills() {
        if (batchingFills) return;
        FILLS.begin(7, DefaultVertexFormats.POSITION_COLOR);
        batchingFills = true;
    }

    public static void endFills() {
        if (!batchingFills) return;
        batchingFills = false;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        FILLS.finishDrawing();
        UPLOADER.draw(FILLS);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void fill(int left, int top, int right, int bottom, int color) {
        if (!batchingFills) {
            Gui.drawRect(left, top, right, bottom, color);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            return;
        }
        int x0 = Math.min(left, right);
        int x1 = Math.max(left, right);
        int y0 = Math.min(top, bottom);
        int y1 = Math.max(top, bottom);
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        FILLS.pos(x0, y1, 0.0D).color(r, g, b, a).endVertex();
        FILLS.pos(x1, y1, 0.0D).color(r, g, b, a).endVertex();
        FILLS.pos(x1, y0, 0.0D).color(r, g, b, a).endVertex();
        FILLS.pos(x0, y0, 0.0D).color(r, g, b, a).endVertex();
    }

    public static void outline(int x, int y, int width, int height, int color) {
        fill(x, y, x + width, y + 1, color);
        fill(x, y + height - 1, x + width, y + height, color);
        fill(x, y + 1, x + 1, y + height - 1, color);
        fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public static void hLine(int startX, int endX, int y, int color) {
        fill(Math.min(startX, endX), y, Math.max(startX, endX) + 1, y + 1, color);
    }

    public static void vLine(int x, int startY, int endY, int color) {
        fill(x, Math.min(startY, endY), x + 1, Math.max(startY, endY) + 1, color);
    }

    public static void string(FontRenderer font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) font.drawStringWithShadow(text, x, y, color);
        else font.drawString(text, x, y, color);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void centeredString(FontRenderer font, String text, int centerX, int y, int color) {
        string(font, text, centerX - font.getStringWidth(text) / 2, y, color, true);
    }

    public static void item(ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        RenderItem renderItem = mc.getRenderItem();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        renderItem.renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void itemDecorations(FontRenderer font, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableDepth();
        mc.getRenderItem().renderItemOverlayIntoGUI(font, stack, x, y, null);
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void texture(ResourceLocation texture, int x, int y, int width, int height) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, width, height, width, height);
        GlStateManager.disableBlend();
    }

    public static void tooltip(FontRenderer font, List<String> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        GuiUtils.drawHoveringText(lines, mouseX, mouseY, sr.getScaledWidth(), sr.getScaledHeight(), -1, font);
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void scissorOn(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        int factor = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * factor, mc.displayHeight - (y + height) * factor,
                Math.max(0, width * factor), Math.max(0, height * factor));
    }

    public static void scissorOff() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void push() {
        GlStateManager.pushMatrix();
    }

    public static void pop() {
        GlStateManager.popMatrix();
    }

    public static void translate(double x, double y, double z) {
        GlStateManager.translate(x, y, z);
    }

    public static void scale(double x, double y, double z) {
        GlStateManager.scale(x, y, z);
    }

    public static String trimToWidth(FontRenderer font, String text, int maxWidth) {
        return font.trimStringToWidth(text, Math.max(0, maxWidth));
    }

    public static String ellipsize(FontRenderer font, String text, int maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        int dots = font.getStringWidth("...");
        return font.trimStringToWidth(text, Math.max(0, maxWidth - dots)) + "...";
    }
}
