package com.modernlife.network.packet;

import com.modernlife.client.screen.DifficultySelectScreen;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class OpenDifficultyMenuPacket {

    public OpenDifficultyMenuPacket() {}

    public OpenDifficultyMenuPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            Minecraft.getInstance().setScreen(new DifficultySelectScreen());
        });
    }
}