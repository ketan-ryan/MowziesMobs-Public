package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MowziesMobs;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelGrottol;
import com.bobmowzie.mowziesmobs.server.entity.grottol.EntityGrottol;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Created by BobMowzie on 5/8/2017.
 */
public class RenderGrottol extends MobRenderer<EntityGrottol, ModelGrottol<EntityGrottol>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MowziesMobs.MODID, "textures/entity/grottol.png");
    private static final ResourceLocation TEXTURE_DEEPSLATE = new ResourceLocation(MowziesMobs.MODID, "textures/entity/grottol_deepslate.png");
    private static final ResourceLocation TEXTURE_BLACKPINK = new ResourceLocation(MowziesMobs.MODID, "textures/entity/grottol_blackpink.png");
    private static final ResourceLocation TEXTURE_DEEPSLATE_BLACKPINK = new ResourceLocation(MowziesMobs.MODID, "textures/entity/grottol_deepslate_blackpink.png");


    public RenderGrottol(EntityRendererProvider.Context mgr) {
        super(mgr, new ModelGrottol<>(), 0.6f);
    }

    @Override
    protected float getFlipDegrees(EntityGrottol entity) {
        return 0;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityGrottol entity) {
        if (entity.getBlackpink()) {
            return entity.getDeepslate() ? RenderGrottol.TEXTURE_DEEPSLATE_BLACKPINK : RenderGrottol.TEXTURE_BLACKPINK;
        } else {
            return entity.getDeepslate() ? RenderGrottol.TEXTURE_DEEPSLATE : RenderGrottol.TEXTURE;
        }
    }

    /*@Override
    public void doRender(EntityGrottol entity, double x, double y, double z, float yaw, float delta) {
        if (entity.hasMinecartBlockDisplay()) {
            if (!renderOutlines) {
                renderName(entity, x, y, z);
            }
        } else {
            super.doRender(entity, x, y, z, yaw, delta);
        }
    }

    @Override
    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float delta) {
        if (!(entity instanceof EntityGrottol) || !((EntityGrottol) entity).hasMinecartBlockDisplay()) {
            super.doRenderShadowAndFire(entity, x, y, z, yaw, delta);
        }
    }*/
}
