package com.modernlife.event;

import com.modernlife.client.renderer.BankerRenderer;
import com.modernlife.client.screen.TaxPanelScreen;
import com.modernlife.registry.ModEntities;
import com.modernlife.registry.ModMenus;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;

public class ClientModEvents {

    public static void initClient() {
        try {
            MenuRegistry.registerScreenFactory(ModMenus.TAX_PANEL.get(), TaxPanelScreen::new);
        } catch (Exception e) {
            System.err.println("KANKA HATA: TaxPanelScreen kaydı sırasında sorun oluştu: " + e.getMessage());
            e.printStackTrace();
        }

        // BANKACININ DIŞ GÖRÜNÜMÜNÜ (RENDERER) İSTEMCİYE BAĞLIYORUZ
        EntityRendererRegistry.register(ModEntities.BANKER, BankerRenderer::new);
    }
}