package com.modernlife.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModCapabilities {
    public static final String NBT_KEY = "modernlife_economy_data";
    private static final Map<UUID, PlayerEconomy> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, CompoundTag> PERSISTENT_DATA = new ConcurrentHashMap<>();

    public static final Object PLAYER_ECONOMY = new Object();

    private ModCapabilities() {}

    /**
     * Forge'un getPersistentData() metodunun hem Fabric hem Forge'da çalışan ortak karşılığı.
     */
    public static CompoundTag getPersistentData(Player player) {
        if (player == null) return new CompoundTag();
        return PERSISTENT_DATA.computeIfAbsent(player.getUUID(), uuid -> new CompoundTag());
    }

    /**
     * Oyuncuya ait ekonomi verisini getirir veya oluşturur.
     */
    public static PlayerEconomy get(Player player) {
        if (player == null) return new PlayerEconomy();
        
        return CACHE.computeIfAbsent(player.getUUID(), uuid -> {
            PlayerEconomy economy = new PlayerEconomy();
            CompoundTag persistentData = getPersistentData(player);
            if (persistentData.contains(NBT_KEY, CompoundTag.TAG_COMPOUND)) {
                economy.deserializeNBT(persistentData.getCompound(NBT_KEY));
            }
            return economy;
        });
    }

    /**
     * Verileri oyuncunun kalıcı NBT etiketine kaydeder.
     */
    public static void save(Player player) {
        if (player == null) return;
        PlayerEconomy economy = CACHE.get(player.getUUID());
        if (economy != null) {
            getPersistentData(player).put(NBT_KEY, economy.serializeNBT());
        }
    }

    public static Optional<PlayerEconomy> getOptional(Player player) {
        return Optional.ofNullable(get(player));
    }
}