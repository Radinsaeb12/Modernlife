package com.modernlife.network.packet;

import com.modernlife.client.screen.PhoneAppScreen;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CPhoneCallStatePacket {
    public enum CallState { IDLE, INCOMING, CONNECTED }

    private final CallState state;
    private final String callerName;
    private final UUID callerUUID;

    public S2CPhoneCallStatePacket(CallState state, String callerName, UUID callerUUID) {
        this.state = state;
        this.callerName = callerName == null ? "" : callerName;
        this.callerUUID = callerUUID == null ? new UUID(0, 0) : callerUUID;
    }

    public S2CPhoneCallStatePacket(FriendlyByteBuf buf) {
        this.state = buf.readEnum(CallState.class);
        this.callerName = buf.readUtf();
        this.callerUUID = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(state);
        buf.writeUtf(callerName);
        buf.writeUUID(callerUUID);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            PhoneAppScreen.currentCallState = this.state;
            PhoneAppScreen.activePartnerName = this.callerName;
            PhoneAppScreen.activePartnerUUID = this.callerUUID;

            if (Minecraft.getInstance().screen instanceof PhoneAppScreen screen) {
                screen.rebuildPhoneUI();
            }
        });
    }
}