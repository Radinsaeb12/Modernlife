package com.modernlife.client;

import com.modernlife.client.screen.ElectionScreen; // Import yeni klasöre yönlendirildi!
import com.modernlife.network.packet.OpenElectionMenuPacket;
import net.minecraft.client.Minecraft;
import java.util.List;

public class ClientAccess {
    public static void openElectionScreen(boolean isActive, boolean isAdmin, List<OpenElectionMenuPacket.Candidate> candidates) {
        Minecraft.getInstance().tell(() -> {
            Minecraft.getInstance().setScreen(new ElectionScreen(isActive, isAdmin, candidates));
        });
    }
}