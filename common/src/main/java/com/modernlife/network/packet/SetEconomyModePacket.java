package com.modernlife.network.packet;

import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.data.world.ModernLifeWorldData.EconomyMode;
import com.modernlife.network.ModNetwork;
import com.modernlife.service.WorldStateService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class SetEconomyModePacket {
    private final EconomyMode mode;

    public SetEconomyModePacket(EconomyMode mode) {
        this.mode = mode;
    }

    public static void encode(SetEconomyModePacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.mode);
    }

    public static SetEconomyModePacket decode(FriendlyByteBuf buf) {
        return new SetEconomyModePacket(buf.readEnum(EconomyMode.class));
    }

    public static void handle(SetEconomyModePacket msg, Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.getPlayer();
            if (player != null && (player.hasPermissions(2) || player.getServer().isSingleplayer())) {
                ModernLifeWorldData data = ModernLifeWorldData.get(player.serverLevel());
                data.setEconomyMode(msg.mode);

                if (msg.mode == EconomyMode.LIGHTMANS_CURRENCY) {
                    data.setDifficulty(ModernLifeWorldData.DifficultyLevel.NORMAL);
                    player.sendSystemMessage(Component.translatable("modernlife.gui.economy_select.lightmans_activated"));
                } else {
                    ModNetwork.CHANNEL.sendToPlayer(player, new OpenDifficultyMenuPacket());
                }

                WorldStateService.syncToAll(player.getServer());
            }
        });
    }
}