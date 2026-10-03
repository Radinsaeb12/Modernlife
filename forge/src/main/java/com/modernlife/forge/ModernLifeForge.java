package com.modernlife.forge;

import com.modernlife.ModernLifeMod;
import com.modernlife.client.ModernLifeClient;
import com.modernlife.client.renderer.BankerRenderer;
import com.modernlife.registry.ModEntities;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ModernLifeMod.MODID)
public class ModernLifeForge {
    public ModernLifeForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. Architectury event bus kaydı
        EventBuses.registerModEventBus(ModernLifeMod.MODID, modEventBus);

        // 2. Ortak mod altyapısını başlat
        ModernLifeMod.init();

        // 3. Forge İstemci Olayları
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Ekranlar ve genel istemci hazırlığı için:
            modEventBus.addListener(this::onClientSetup);
            // Varlık (Banker) Renderer kaydı için Forge'un zorunlu kıldığı olay:
            modEventBus.addListener(this::registerEntityRenderers);
        }
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModernLifeClient.init();
        });
    }

    private void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        // Forge'un beklediği Entity Renderer kaydı doğrudan burada yapılır
        event.registerEntityRenderer(ModEntities.BANKER.get(), BankerRenderer::new);
    }
}