package com.modernlife.network.packet;

import com.modernlife.data.TaxRates;
import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.service.TaxService;
import com.modernlife.service.WorldStateService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public record SetTaxRatesPacket(long vehicleDaily, long mountDaily, long livingDaily, double penaltyRate, long unlicensedPenalty, long unlicensedMountPenalty) {
    public static void encode(final SetTaxRatesPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeLong(packet.vehicleDaily());
        buffer.writeLong(packet.mountDaily());
        buffer.writeLong(packet.livingDaily());
        buffer.writeDouble(packet.penaltyRate());
        buffer.writeLong(packet.unlicensedPenalty());
        buffer.writeLong(packet.unlicensedMountPenalty());
    }

    public static SetTaxRatesPacket decode(final FriendlyByteBuf buffer) {
        return new SetTaxRatesPacket(
                buffer.readLong(),
                buffer.readLong(),
                buffer.readLong(),
                buffer.readDouble(),
                buffer.readLong(),
                buffer.readLong()
        );
    }

    private static long roundUpTo5(long value) {
        if (value <= 0L) return 0L;
        long rem = value % 5L;
        return rem == 0L ? value : value + (5L - rem);
    }

    public static void handle(final SetTaxRatesPacket packet, final Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext context = ctxSupplier.get();
        context.queue(() -> {
            final ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null || !WorldStateService.isPresident(player)) {
                return;
            }

            long safeVehicle = roundUpTo5(packet.vehicleDaily());
            long safeMount = roundUpTo5(packet.mountDaily());
            long safeLiving = roundUpTo5(packet.livingDaily());
            long safeUnlicensed = roundUpTo5(packet.unlicensedPenalty());
            long safeUnlicensedMount = roundUpTo5(packet.unlicensedMountPenalty());
            double safePenalty = Math.max(0.0, Math.min(packet.penaltyRate(), 2.0));

            final TaxRates taxRates = new TaxRates(safeVehicle, safeMount, safeLiving);
            TaxService.setTaxRates(player.getServer(), taxRates);

            ModernLifeWorldData worldData = WorldStateService.getWorldData(player.serverLevel());
            worldData.setPenaltyRate(safePenalty);
            worldData.setUnlicensedPenalty(safeUnlicensed);
            worldData.setUnlicensedMountPenalty(safeUnlicensedMount);
            worldData.setDirty();

            WorldStateService.syncToAll(player.getServer());
            player.displayClientMessage(Component.translatable("modernlife.tax_panel.applied"), true);
        });
    }
}