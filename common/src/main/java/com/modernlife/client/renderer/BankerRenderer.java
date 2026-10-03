package com.modernlife.client.renderer;

import com.modernlife.ModernLifeMod;
import com.modernlife.entity.BankerEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BankerRenderer extends MobRenderer<BankerEntity, PlayerModel<BankerEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ModernLifeMod.MODID, "textures/entity/banker.png");

    public BankerRenderer(EntityRendererProvider.Context context) {
        // Gönderdiğin Alex (Slim) modelli skin için uyarlanmış model
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BankerEntity entity) {
        return TEXTURE;
    }
}