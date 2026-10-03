package com.modernlife.client;

import com.modernlife.client.renderer.BankerRenderer;
import com.modernlife.client.screen.TaxPanelScreen;
import com.modernlife.client.screen.YazarKasaScreen;
import com.modernlife.registry.ModEntities;
import com.modernlife.registry.ModMenus;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;

public class ModernLifeClient {
    public static void init() {
        // Ekranları Menülere Bağla
        MenuRegistry.registerScreenFactory(ModMenus.TAX_PANEL.get(), TaxPanelScreen::new);
        MenuRegistry.registerScreenFactory(ModMenus.YAZAR_KASA.get(), YazarKasaScreen::new);

        // Fabric tarafında Architectury üzerinden kaydet (Forge tarafı ModernLifeForge'da halledildi)
        if (Platform.isFabric()) {
            EntityRendererRegistry.register(ModEntities.BANKER, BankerRenderer::new);
        }
    }
}