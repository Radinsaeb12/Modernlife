package com.modernlife.event;

import com.modernlife.economy.EconomyRegistry;
import com.modernlife.service.EconomyService;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;

public final class EconomyEvents {
    private EconomyEvents() {
    }

    public static void register() {
        // JSON SİSTEMİNİ SUNUCU AÇILIRKEN YÜKLE
        LifecycleEvent.SERVER_STARTING.register(server -> {
            EconomyRegistry.loadOrGenerateConfig();
        });

        // OYUNCU GİRİŞ YAPTIĞINDA SENKRONİZE ET
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!player.level().isClientSide()) {
                EconomyService.syncToClient(player);
            }
        });
    }
}