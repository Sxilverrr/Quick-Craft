package com.sxilverr.quickcraft.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sxilverr.quickcraft.config.QuickCraftClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public final class EntityIcon {
    private static final long SPIN_PERIOD_MS = 15000L;
    private static final long ANIM_TICK_INTERVAL_MS = 50L;
    private static final float WALK_ANIM_SPEED = 0.6F;
    private static final float TILT_DEGREES = 15.0F;

    private static final Map<ResourceLocation, Holder> CACHE = new HashMap<>();
    private static Level cacheLevel;

    private EntityIcon() {
    }

    private static final class Holder {
        Entity entity;
        long lastAnimTickMs;
    }

    private static Holder holder(ResourceLocation entityId) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return null;
        if (cacheLevel != level) {
            cacheLevel = level;
            CACHE.clear();
        }
        Holder holder = CACHE.get(entityId);
        if (holder == null) {
            holder = new Holder();
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
            if (type != null) {
                try {
                    holder.entity = type.create(level);
                } catch (Throwable ignored) {
                }
            }
            CACHE.put(entityId, holder);
        }
        return holder;
    }

    public static void render(GuiGraphics graphics, int x, int y, int w, int h, ResourceLocation entityId) {
        if (entityId == null) {
            fallback(graphics, x, y, w, h, null);
            return;
        }
        Holder holder = holder(entityId);
        if (holder == null || holder.entity == null) {
            fallback(graphics, x, y, w, h, entityId);
            return;
        }
        Entity entity = holder.entity;
        advance(entity, holder);

        float bbHeight = Math.max(entity.getBbHeight(), 0.1F);
        float bbWidth = Math.max(entity.getBbWidth(), 0.1F);
        float scale = Math.min(h / bbHeight, w / bbWidth);
        if (scale <= 0.0F) {
            fallback(graphics, x, y, w, h, entityId);
            return;
        }

        double cx = x + w / 2.0;
        double cy = y + h / 2.0 + bbHeight * scale / 2.0;

        boolean spinning = QuickCraftClientConfig.entitySpin();
        float spinSpeed = (float) QuickCraftClientConfig.entitySpinSpeed();
        float yaw = spinning
                ? (System.currentTimeMillis() % SPIN_PERIOD_MS) / (float) SPIN_PERIOD_MS * 360.0F * spinSpeed
                : 0.0F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, cy, 64.0);
        Matrix4f matrix = pose.last().pose();
        float axisX = (float) Math.sqrt(matrix.m00() * matrix.m00() + matrix.m10() * matrix.m10() + matrix.m20() * matrix.m20());
        float axisZ = (float) Math.sqrt(matrix.m02() * matrix.m02() + matrix.m12() * matrix.m12() + matrix.m22() * matrix.m22());
        float depthFix = axisZ > 1.0E-5F ? axisX / axisZ : 1.0F;
        pose.scale(scale, scale, -scale * depthFix);
        pose.mulPose(Axis.XP.rotationDegrees(180.0F + TILT_DEGREES));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));

        LivingEntity living = entity instanceof LivingEntity le ? le : null;
        float prevYRot = entity.getYRot();
        float prevXRot = entity.getXRot();
        float prevYBodyRot = 0.0F;
        float prevYHeadRotO = 0.0F;
        float prevYHeadRot = 0.0F;
        if (living != null) {
            prevYBodyRot = living.yBodyRot;
            prevYHeadRotO = living.yHeadRotO;
            prevYHeadRot = living.yHeadRot;
            living.yBodyRot = 0.0F;
            living.yHeadRot = 0.0F;
            living.yHeadRotO = 0.0F;
        }
        entity.setYRot(0.0F);
        entity.setXRot(0.0F);

        int packedLight = LightTexture.FULL_BRIGHT;
        float[] prevShaderColor = RenderSystem.getShaderColor().clone();

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        try {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, pose, graphics.bufferSource(), packedLight));
            graphics.flush();
        } catch (Throwable ignored) {
        } finally {
            RenderSystem.setShaderColor(prevShaderColor[0], prevShaderColor[1], prevShaderColor[2], prevShaderColor[3]);
            dispatcher.setRenderShadow(true);
            pose.popPose();
            Lighting.setupFor3DItems();
            entity.setYRot(prevYRot);
            entity.setXRot(prevXRot);
            if (living != null) {
                living.yBodyRot = prevYBodyRot;
                living.yHeadRotO = prevYHeadRotO;
                living.yHeadRot = prevYHeadRot;
            }
        }
    }

    private static void advance(Entity entity, Holder holder) {
        boolean idle = QuickCraftClientConfig.entityIdle();
        boolean walk = QuickCraftClientConfig.entityWalk();
        if (!idle && !walk) return;
        long now = System.currentTimeMillis();
        if (holder.lastAnimTickMs == 0L) {
            holder.lastAnimTickMs = now;
            return;
        }
        long elapsed = now - holder.lastAnimTickMs;
        if (elapsed < ANIM_TICK_INTERVAL_MS) return;
        long ticks;
        if (elapsed > ANIM_TICK_INTERVAL_MS * 4L) {
            ticks = 1L;
            holder.lastAnimTickMs = now;
        } else {
            ticks = elapsed / ANIM_TICK_INTERVAL_MS;
            holder.lastAnimTickMs += ticks * ANIM_TICK_INTERVAL_MS;
        }
        LivingEntity living = entity instanceof LivingEntity le ? le : null;
        for (long i = 0; i < ticks; i++) {
            if (idle) entity.tickCount++;
            if (walk && living != null) living.walkAnimation.update(WALK_ANIM_SPEED, 1.0F);
        }
    }

    private static void fallback(GuiGraphics graphics, int x, int y, int w, int h, ResourceLocation entityId) {
        EntityType<?> type = entityId == null ? null : BuiltInRegistries.ENTITY_TYPE.get(entityId);
        SpawnEggItem egg = type != null ? SpawnEggItem.byId(type) : null;
        ItemStack stack = new ItemStack(egg != null ? egg : Items.SPAWNER);
        graphics.renderFakeItem(stack, x + (w - 16) / 2, y + (h - 16) / 2);
    }
}
