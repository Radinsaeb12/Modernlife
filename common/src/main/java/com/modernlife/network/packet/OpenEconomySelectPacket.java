package com.modernlife.network.packet;

import com.modernlife.client.screen.EconomySelectScreen;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class OpenEconomySelectPacket {

    public OpenEconomySelectPacket() {}

    public static void encode(OpenEconomySelectPacket msg, FriendlyByteBuf buf) {}

    public static OpenEconomySelectPacket decode(FriendlyByteBuf buf) {
        return new OpenEconomySelectPacket();
    }

    public static void handle(OpenEconomySelectPacket msg, Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            Minecraft.getInstance().setScreen(new EconomySelectScreen());
        });
    }
}