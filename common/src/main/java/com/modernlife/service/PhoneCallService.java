package com.modernlife.service;

import com.modernlife.advancement.CustomAdvancementTriggers;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.S2CPhoneCallStatePacket;
import com.modernlife.voice.ModernLifeVoicePlugin;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import dev.architectury.utils.GameInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhoneCallService {
    private static final Map<UUID, Group> activeCalls = new HashMap<>();
    private static final Map<UUID, UUID> pendingCalls = new HashMap<>();
    private static final Map<UUID, UUID> outgoingCalls = new HashMap<>();
    private static final Map<UUID, UUID> callPartners = new HashMap<>();

    public static void initiateCallRequest(ServerPlayer caller, ServerPlayer target) {
        if (caller == null || target == null) return;

        if (callPartners.containsKey(target.getUUID()) || pendingCalls.containsKey(target.getUUID())) {
            caller.sendSystemMessage(Component.translatable("modernlife.phone.busy_notice"));
            return;
        }

        pendingCalls.put(target.getUUID(), caller.getUUID());
        outgoingCalls.put(caller.getUUID(), target.getUUID());

        caller.sendSystemMessage(Component.translatable("modernlife.phone.calling_notice", target.getScoreboardName()));
        target.sendSystemMessage(Component.translatable("modernlife.phone.incoming_call_notice", caller.getScoreboardName()));

        ModNetwork.CHANNEL.sendToPlayer(target, 
                new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.INCOMING, caller.getScoreboardName(), caller.getUUID()));
    }

    public static void acceptCall(ServerPlayer receiver) {
        if (receiver == null) return;

        UUID callerUUID = pendingCalls.remove(receiver.getUUID());
        if (callerUUID == null) return;

        outgoingCalls.remove(callerUUID);

        ServerPlayer caller = receiver.getServer().getPlayerList().getPlayer(callerUUID);
        if (caller == null) return;

        startCall(caller, receiver);
    }

    public static void startCall(ServerPlayer caller, ServerPlayer receiver) {
        if (ModernLifeVoicePlugin.VOICECHAT_SERVER_API == null) {
            caller.sendSystemMessage(Component.translatable("modernlife.phone.voice_unavailable"));
            receiver.sendSystemMessage(Component.translatable("modernlife.phone.voice_unavailable"));
        } else {
            Group phoneGroup = ModernLifeVoicePlugin.VOICECHAT_SERVER_API.groupBuilder()
                    .setName("Phone_" + caller.getScoreboardName())
                    .setType(Group.Type.ISOLATED)
                    .setPassword(UUID.randomUUID().toString())
                    .build();

            VoicechatConnection callerConn = ModernLifeVoicePlugin.VOICECHAT_SERVER_API.getConnectionOf(caller.getUUID());
            VoicechatConnection receiverConn = ModernLifeVoicePlugin.VOICECHAT_SERVER_API.getConnectionOf(receiver.getUUID());

            if (callerConn == null || receiverConn == null) {
                caller.sendSystemMessage(Component.translatable("modernlife.phone.voice_unavailable"));
                receiver.sendSystemMessage(Component.translatable("modernlife.phone.voice_unavailable"));
            } else {
                callerConn.setGroup(phoneGroup);
                receiverConn.setGroup(phoneGroup);

                activeCalls.put(caller.getUUID(), phoneGroup);
                activeCalls.put(receiver.getUUID(), phoneGroup);
            }
        }

        callPartners.put(caller.getUUID(), receiver.getUUID());
        callPartners.put(receiver.getUUID(), caller.getUUID());

        caller.sendSystemMessage(Component.translatable("modernlife.phone.connected_notice"));
        receiver.sendSystemMessage(Component.translatable("modernlife.phone.connected_notice"));

        ModNetwork.CHANNEL.sendToPlayer(caller, 
                new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.CONNECTED, receiver.getScoreboardName(), receiver.getUUID()));
        ModNetwork.CHANNEL.sendToPlayer(receiver, 
                new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.CONNECTED, caller.getScoreboardName(), caller.getUUID()));

        CustomAdvancementTriggers.PHONE_CALL.trigger(caller);
        CustomAdvancementTriggers.PHONE_CALL.trigger(receiver);
    }

    public static void endCallIfActive(UUID playerUuid) {
        if (playerUuid == null) return;

        boolean hasPending = pendingCalls.containsKey(playerUuid);
        boolean hasOutgoing = outgoingCalls.containsKey(playerUuid);
        boolean hasActive = callPartners.containsKey(playerUuid);
        if (!hasPending && !hasOutgoing && !hasActive) return;

        ServerPlayer player = getOnlinePlayer(playerUuid);
        if (player != null) {
            declineOrEndCall(player);
        } else {
            cleanupWithoutPlayer(playerUuid);
        }
    }

    public static void endCallIfActive(ServerPlayer player) {
        if (player == null) return;
        endCallIfActive(player.getUUID());
    }

    private static ServerPlayer getOnlinePlayer(UUID uuid) {
        MinecraftServer server = GameInstance.getServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayer(uuid);
    }

    private static void cleanupWithoutPlayer(UUID playerUuid) {
        UUID callerUUID = pendingCalls.remove(playerUuid);
        if (callerUUID != null) {
            outgoingCalls.remove(callerUUID);
            ServerPlayer caller = getOnlinePlayer(callerUUID);
            if (caller != null) {
                caller.sendSystemMessage(Component.translatable("modernlife.phone.declined_notice"));
                ModNetwork.CHANNEL.sendToPlayer(caller,
                        new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.IDLE, "", null));
            }
        }

        UUID targetUUID = outgoingCalls.remove(playerUuid);
        if (targetUUID != null) {
            pendingCalls.remove(targetUUID);
            ServerPlayer target = getOnlinePlayer(targetUUID);
            if (target != null) {
                target.sendSystemMessage(Component.translatable("modernlife.phone.declined_notice"));
                ModNetwork.CHANNEL.sendToPlayer(target,
                        new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.IDLE, "", null));
            }
        }

        UUID partnerUUID = callPartners.remove(playerUuid);
        if (partnerUUID != null) {
            callPartners.remove(partnerUUID);
            activeCalls.remove(playerUuid);
            activeCalls.remove(partnerUUID);

            if (ModernLifeVoicePlugin.VOICECHAT_SERVER_API != null) {
                VoicechatConnection partConn = ModernLifeVoicePlugin.VOICECHAT_SERVER_API.getConnectionOf(partnerUUID);
                if (partConn != null && partConn.getGroup() != null) partConn.setGroup(null);
            }

            ServerPlayer partner = getOnlinePlayer(partnerUUID);
            if (partner != null) {
                partner.sendSystemMessage(Component.translatable("modernlife.phone.ended_notice"));
                ModNetwork.CHANNEL.sendToPlayer(partner,
                        new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.IDLE, "", null));
            }
        }
    }

    public static void onPlayerDisconnectedFromVoice(ServerPlayer player) {
        if (player == null) return;
        if (callPartners.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("modernlife.phone.left_group_ended"));
            declineOrEndCall(player);
        }
    }

    public static void declineOrEndCall(ServerPlayer player) {
        if (player == null) return;

        if (pendingCalls.containsKey(player.getUUID())) {
            UUID callerUUID = pendingCalls.remove(player.getUUID());
            outgoingCalls.remove(callerUUID);
            ServerPlayer caller = player.getServer().getPlayerList().getPlayer(callerUUID);

            ModNetwork.CHANNEL.sendToPlayer(player, 
                    new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.IDLE, "", null));

            if (caller != null) {
                caller.sendSystemMessage(Component.translatable("modernlife.phone.declined_notice"));
                ModNetwork.CHANNEL.sendToPlayer(caller, 
                        new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.IDLE, "", null));
            }
            return;
        }

        UUID targetUUID = outgoingCalls.remove(player.getUUID());
        if (targetUUID != null) {
            pendingCalls.remove(targetUUID);
            ServerPlayer target = player.getServer().getPlayerList().getPlayer(targetUUID);

            ModNetwork.CHANNEL.sendToPlayer(player, 
                    new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.IDLE, "", null));

            if (target != null) {
                target.sendSystemMessage(Component.translatable("modernlife.phone.declined_notice"));
                ModNetwork.CHANNEL.sendToPlayer(target, 
                        new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.IDLE, "", null));
            }
            return;
        }

        UUID partnerUUID = callPartners.remove(player.getUUID());
        if (partnerUUID != null) {
            callPartners.remove(partnerUUID);
            ServerPlayer partner = player.getServer().getPlayerList().getPlayer(partnerUUID);

            if (ModernLifeVoicePlugin.VOICECHAT_SERVER_API != null) {
                VoicechatConnection pConn = ModernLifeVoicePlugin.VOICECHAT_SERVER_API.getConnectionOf(player.getUUID());
                VoicechatConnection partConn = ModernLifeVoicePlugin.VOICECHAT_SERVER_API.getConnectionOf(partnerUUID);
                if (pConn != null && pConn.getGroup() != null) pConn.setGroup(null);
                if (partConn != null && partConn.getGroup() != null) partConn.setGroup(null);
            }

            activeCalls.remove(player.getUUID());
            activeCalls.remove(partnerUUID);

            ModNetwork.CHANNEL.sendToPlayer(player, 
                    new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.IDLE, "", null));

            if (partner != null) {
                partner.sendSystemMessage(Component.translatable("modernlife.phone.ended_notice"));
                ModNetwork.CHANNEL.sendToPlayer(partner, 
                        new S2CPhoneCallStatePacket(S2CPhoneCallStatePacket.CallState.IDLE, "", null));
            }
        }
    }
}