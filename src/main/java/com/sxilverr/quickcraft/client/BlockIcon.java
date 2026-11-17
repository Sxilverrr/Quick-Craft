package com.sxilverr.quickcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockIcon {
    private BlockIcon() {
    }

    public static void render(GuiGraphics graphics, int x, int y, int w, int h, BlockState state) {
        float scale = Math.min(w, h) * 0.62F;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + w / 2.0, y + h / 2.0, 100.0);
        pose.last().pose().scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        pose.translate(-0.5, -0.5, -0.5);
        try {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, graphics.bufferSource(),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        } catch (RuntimeException ignored) {
        }
        graphics.flush();
        pose.popPose();
    }
}
