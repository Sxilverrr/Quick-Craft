package com.sxilverr.quickcraft.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;

public final class BlockIcon {
    private BlockIcon() {
    }

    public static void render(int x, int y, int w, int h, IBlockState state) {
        Minecraft mc = Minecraft.getMinecraft();
        float scale = Math.min(w, h) * 0.62F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + w / 2.0F, y + h / 2.0F, 100.0F);
        GlStateManager.scale(scale, -scale, scale);
        GlStateManager.rotate(30.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(225.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        RenderHelper.disableStandardItemLighting();
        try {
            mc.getBlockRendererDispatcher().renderBlockBrightness(state, 1.0F);
        } catch (RuntimeException ignored) {
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
