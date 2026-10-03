package com.modernlife.voice;

import com.modernlife.service.PhoneCallService;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.LeaveGroupEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import net.minecraft.server.level.ServerPlayer;

public class ModernLifeVoicePlugin implements VoicechatPlugin {

    public static VoicechatServerApi VOICECHAT_SERVER_API;

    @Override
    public String getPluginId() {
        return "modernlife";
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected);
        registration.registerEvent(LeaveGroupEvent.class, this::onPlayerLeftGroup);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VOICECHAT_SERVER_API = event.getVoicechat();
    }

    private void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        if (VOICECHAT_SERVER_API != null) {
            var conn = VOICECHAT_SERVER_API.getConnectionOf(event.getPlayerUuid());
            if (conn != null && conn.getPlayer() != null) {
                Object playerObj = conn.getPlayer().getPlayer();
                if (playerObj instanceof ServerPlayer player) {
                    player.getServer().execute(() -> PhoneCallService.onPlayerDisconnectedFromVoice(player));
                }
            }
        }
    }

    private void onPlayerLeftGroup(LeaveGroupEvent event) {
        if (event.getConnection() != null && event.getConnection().getPlayer() != null) {
            Object playerObj = event.getConnection().getPlayer().getPlayer();
            if (playerObj instanceof ServerPlayer player) {
                player.getServer().execute(() -> {
                    PhoneCallService.endCallIfActive(player);
                });
            }
        }
    }
}