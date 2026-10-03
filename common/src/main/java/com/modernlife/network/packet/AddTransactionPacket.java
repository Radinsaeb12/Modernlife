package com.modernlife.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.function.Supplier;

public class AddTransactionPacket {
    private final String identity;
    private final Component logComponent;

    public AddTransactionPacket(String identity, Component logComponent) {
        this.identity = identity;
        this.logComponent = logComponent;
    }

    public AddTransactionPacket(String identity, String rawText) {
        this.identity = identity;
        this.logComponent = Component.literal(rawText);
    }

    public static AddTransactionPacket decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        Component comp = buf.readComponent();
        return new AddTransactionPacket(id, comp);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(identity);
        buf.writeComponent(logComponent);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            com.modernlife.client.screen.BankScreen.islemGecmisi
                .computeIfAbsent(identity, k -> new ArrayList<>())
                .add(logComponent.getString());
        });
    }
}