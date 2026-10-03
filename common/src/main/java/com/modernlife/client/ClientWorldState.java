package com.modernlife.client;

import com.modernlife.data.TaxRates;
import com.modernlife.network.packet.SyncWorldStatePacket;

import javax.annotation.Nullable;
import java.util.UUID;

public final class ClientWorldState {
    @Nullable
    private static UUID presidentUuid;
    private static TaxRates taxRates = TaxRates.empty();
    private static long worldDayCounter;

    private ClientWorldState() {}

    public static void apply(@Nullable final UUID newPresidentUuid, final TaxRates newTaxRates, final long newWorldDayCounter) {
        presidentUuid = newPresidentUuid;
        taxRates = newTaxRates != null ? newTaxRates : TaxRates.empty();
        worldDayCounter = Math.max(0L, newWorldDayCounter);
    }

    public static void apply(final SyncWorldStatePacket packet) {
        if (packet == null) return;
        apply(packet.presidentUuid(), packet.taxRates(), packet.worldDayCounter());
    }

    @Nullable
    public static UUID getPresidentUuid() {
        return presidentUuid;
    }

    public static TaxRates getTaxRates() {
        return taxRates;
    }

    public static long getWorldDayCounter() {
        return worldDayCounter;
    }
}