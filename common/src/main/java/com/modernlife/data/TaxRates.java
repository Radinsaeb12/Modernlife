package com.modernlife.data;

import net.minecraft.nbt.CompoundTag;

public record TaxRates(long vehicleDaily, long mountDaily, long livingDaily) {
    public static TaxRates empty() {
        return new TaxRates(0L, 0L, 0L);
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putLong("vehicleDaily", vehicleDaily);
        tag.putLong("mountDaily", mountDaily);
        tag.putLong("livingDaily", livingDaily);
        return tag;
    }

    public static TaxRates load(final CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return empty();
        }
        return new TaxRates(
                Math.max(0L, tag.getLong("vehicleDaily")),
                Math.max(0L, tag.getLong("mountDaily")),
                Math.max(0L, tag.getLong("livingDaily"))
        );
    }
}
