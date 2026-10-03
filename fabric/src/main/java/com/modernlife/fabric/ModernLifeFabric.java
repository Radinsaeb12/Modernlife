package com.modernlife.fabric;

import com.modernlife.ModernLifeMod;
import net.fabricmc.api.ModInitializer;

public class ModernLifeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModernLifeMod.init();
    }
}