package com.modernlife.network.packet;

import com.modernlife.client.ClientWorldState;
import com.modernlife.data.TaxRates;
import com.modernlife.data.world.ModernLifeWorldData;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public record SyncWorldStatePacket(
        @Nullable UUID presidentUuid,
        TaxRates taxRates,
        long worldDayCounter
) {
    public static SyncWorldStatePacket fromWorldData(final ModernLifeWorldData worldData) {
        return new SyncWorldStatePacket(
                worldData.getPresidentUuid(),
                worldData.getTaxRates(),
                worldData.getWorldDayCounter()
        );
    }

    public static void encode(final SyncWorldStatePacket packet, final FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.presidentUuid() != null);
        if (packet.presidentUuid() != null) {
            buffer.writeUUID(packet.presidentUuid());
        }
        buffer.writeLong(packet.taxRates().vehicleDaily());
        buffer.writeLong(packet.taxRates().mountDaily());
        buffer.writeLong(packet.taxRates().livingDaily());
        buffer.writeLong(packet.worldDayCounter());
    }

    public static SyncWorldStatePacket decode(final FriendlyByteBuf buffer) {
        final UUID presidentUuid = buffer.readBoolean() ? buffer.readUUID() : null;
        final TaxRates taxRates = new TaxRates(
                buffer.readLong(),
                buffer.readLong(),
                buffer.readLong()
        );
        final long worldDayCounter = buffer.readLong();
        return new SyncWorldStatePacket(presidentUuid, taxRates, worldDayCounter);
    }

    public static void handle(final SyncWorldStatePacket packet, final Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext context = ctxSupplier.get();
        context.queue(() -> {
            ClientWorldState.apply(packet);
        });
    }
}