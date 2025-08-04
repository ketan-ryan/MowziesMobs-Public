package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelRockSling;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityRockSling;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RenderRockSling extends GeoEntityRenderer<EntityRockSling> {

    public RenderRockSling(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ModelRockSling());
    }

    @Override
    public void render(EntityRockSling entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        float risingAnim = 2 * (float) (Math.pow(0.6 * (entity.tickCount + partialTick + 1), -3));
        poseStack.translate(0, 0.25 - risingAnim, 0);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
