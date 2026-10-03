package com.modernlife;

import com.modernlife.advancement.CustomAdvancementTriggers;
import com.modernlife.command.ModCommands;
import com.modernlife.economy.EconomyRegistry;
import com.modernlife.event.EconomyEvents;
import com.modernlife.event.ServerPlayerEventHandler;
import com.modernlife.network.ModNetwork;
import com.modernlife.registry.*;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class ModernLifeMod {
    public static final String MODID = "modernlife";
    public static final String VERSION = "0.5.0-phase4";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        // 1. Kayıtlar (Sıralama: Bloklar -> Eşyalar ve Sekmeler -> Diğerleri)
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModEntities.register();
        ModMenus.register();

        // 2. Ağ, Komutlar ve Olaylar
        ModNetwork.register();
        ModCommands.register();
        ServerPlayerEventHandler.register();
        EconomyEvents.register();

        // 3. Ekonomi ve Başarımlar
        EconomyRegistry.loadOrGenerateConfig();
        CustomAdvancementTriggers.register();

        LOGGER.info("Modern Life ortak altyapısı başarıyla başlatıldı.");
    }
}