package com.modernlife.fabric.client;

import com.modernlife.client.ModernLifeClient;
import net.fabricmc.api.ClientModInitializer;

public class ModernLifeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModernLifeClient.init();
    }
}