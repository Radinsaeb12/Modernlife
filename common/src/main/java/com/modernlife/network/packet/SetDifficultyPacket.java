package com.modernlife.network.packet;

import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.data.world.ModernLifeWorldData.DifficultyLevel;
import com.modernlife.data.world.ModernLifeWorldData.EconomyMode;
import com.modernlife.event.SpawnBankEvents;
import com.modernlife.service.WorldStateService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class SetDifficultyPacket {
    private final DifficultyLevel difficulty;

    public SetDifficultyPacket(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public SetDifficultyPacket(FriendlyByteBuf buf) {
        this.difficulty = buf.readEnum(DifficultyLevel.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.difficulty);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.getPlayer();
            if (player != null && (player.hasPermissions(2) || player.getServer().isSingleplayer())) {
                ModernLifeWorldData data = ModernLifeWorldData.get(player.serverLevel());
                data.setDifficulty(this.difficulty);
                WorldStateService.syncToAll(player.getServer());

                if (data.getEconomyMode() == EconomyMode.MODERNLIFE_NATIVE) {
                    SpawnBankEvents.spawnBankNearPlayer(player.serverLevel(), player);
                }
            }
        });
    }
}