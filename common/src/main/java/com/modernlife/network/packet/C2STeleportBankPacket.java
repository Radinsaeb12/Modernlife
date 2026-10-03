package com.modernlife.network.packet;

import com.modernlife.event.SpawnBankEvents;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class C2STeleportBankPacket {

    public C2STeleportBankPacket() {}

    public static void encode(C2STeleportBankPacket packet, FriendlyByteBuf buf) {}

    public static C2STeleportBankPacket decode(FriendlyByteBuf buf) {
        return new C2STeleportBankPacket();
    }

    public static void handle(C2STeleportBankPacket packet, Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                ServerLevel level = player.server.getLevel(Level.OVERWORLD);
                if (level == null) return;

                SpawnBankEvents.BankSavedData bankData = level.getDataStorage().computeIfAbsent(
                        SpawnBankEvents.BankSavedData::load,
                        SpawnBankEvents.BankSavedData::new,
                        "modernlife_bank_data"
                );

                if (bankData.bankSpawned && bankData.bankPos != null) {
                    BlockPos target = bankData.bankPos;
                    // Hileler kapalı olsa dahi sunucu doğrudan ışınlar
                    player.teleportTo(level, target.getX() + 21.5, target.getY() + 2.0, target.getZ() + 14.5, 90.0F, 0.0F);
                }
            }
        });
    }
}