package com.modernlife.event;

import com.modernlife.capability.ModCapabilities;
import com.modernlife.capability.PlayerEconomy;
import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.item.GuideBookItem;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.OpenDifficultyMenuPacket;
import com.modernlife.network.packet.OpenEconomySelectPacket;
import com.modernlife.network.packet.SyncEconomyModePacket;
import com.modernlife.registry.ModItems;
import com.modernlife.service.EconomyService;
import com.modernlife.service.WorldStateService;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ServerPlayerEventHandler {

    private static long lastCheckDay = -1;

    public static void register() {
        // 1. Sunucu Tick Olayı (Gün değişimi ve vergi tahakkuku)
        TickEvent.SERVER_POST.register(server -> {
            ServerLevel overworld = server.overworld();
            if (overworld != null) {
                long currentDay = overworld.getDayTime() / 24000L;

                if (lastCheckDay == -1) {
                    lastCheckDay = currentDay;
                    return;
                }

                if (currentDay > lastCheckDay) {
                    long daysPassed = currentDay - lastCheckDay;
                    lastCheckDay = currentDay;

                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        PlayerEconomy economy = ModCapabilities.get(player);
                        long oldTaxDue = economy.getTaxDue();
                        economy.setTaxDue(oldTaxDue + daysPassed);
                        
                        player.sendSystemMessage(Component.translatable(
                            "modernlife.tax.notice", 
                            daysPassed, 
                            (oldTaxDue + daysPassed)
                        ));

                        EconomyService.syncToClient(player);
                    }
                } else if (currentDay < lastCheckDay) {
                    lastCheckDay = currentDay;
                }
            }
        });

        // 2. Oyuncu Tick Olayı (Ehliyet kontrolü)
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                VehicleDriverCheckEvent.checkPlayerVehicle(serverPlayer);
            }
        });

        // 3. Sunucu Kapanış Olayı
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            lastCheckDay = -1;
        });

        // 4. Oyuncu Giriş Olayı
        PlayerEvent.PLAYER_JOIN.register(player -> {
            onPlayerLoggedIn(player);
        });

        // 5. Oyuncu Çıkış Olayı
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player.level() instanceof ServerLevel serverLevel) {
                ModernLifeWorldData worldData = ModernLifeWorldData.get(serverLevel);
                if (!worldData.isDifficultySet() && worldData.isDifficultyMenuOpened()) {
                    worldData.setDifficultyMenuOpened(false);
                }
            }
        });

        // 6. Banka Sohbet Işınlanma Tetikleyicisi (Hilesiz dünyalarda da çalışır)
        CommandRegistrationEvent.EVENT.register((dispatcher, registrySelection, environment) -> {
            dispatcher.register(Commands.literal("ml_tp_secret")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.server.getLevel(Level.OVERWORLD);
                    if (level != null) {
                        SpawnBankEvents.BankSavedData bankData = level.getDataStorage().computeIfAbsent(
                            SpawnBankEvents.BankSavedData::load,
                            SpawnBankEvents.BankSavedData::new,
                            "modernlife_bank_data"
                        );
                        if (bankData.bankSpawned && bankData.bankPos != null) {
                            BlockPos target = bankData.bankPos;
                            player.teleportTo(level, target.getX() + 21.5, target.getY() + 2.0, target.getZ() + 14.5, 90.0F, 0.0F);
                        }
                    }
                    return 1;
                })
            );
        });
    }

    private static void onPlayerLoggedIn(ServerPlayer serverPlayer) {
        WorldStateService.ensureSingleplayerPresident(serverPlayer);
        WorldStateService.syncToClient(serverPlayer);

        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            ModernLifeWorldData worldData = ModernLifeWorldData.get(serverLevel);

            boolean isLightman = worldData.isEconomyModeSet() && worldData.getEconomyMode() == ModernLifeWorldData.EconomyMode.LIGHTMANS_CURRENCY;
            ModNetwork.CHANNEL.sendToPlayer(serverPlayer, new SyncEconomyModePacket(isLightman));

            if (!worldData.isEconomyModeSet()) {
                ModNetwork.CHANNEL.sendToPlayer(serverPlayer, new OpenEconomySelectPacket());
            } else if (worldData.getEconomyMode() == ModernLifeWorldData.EconomyMode.MODERNLIFE_NATIVE && !worldData.isDifficultySet()) {
                if (!worldData.isDifficultyMenuOpened()) {
                    worldData.setDifficultyMenuOpened(true);
                    ModNetwork.CHANNEL.sendToPlayer(serverPlayer, new OpenDifficultyMenuPacket());
                }
            }

            // Oyuncuya sayfaları önceden işlenmiş Modern Life Guide kitabı verilir
            CompoundTag playerData = ModCapabilities.getPersistentData(serverPlayer);
            if (!playerData.getBoolean("ML_ReceivedGuideBook")) {
                playerData.putBoolean("ML_ReceivedGuideBook", true);
                
                ItemStack book = new ItemStack(ModItems.GUIDE_BOOK.get());
                GuideBookItem.setupBookPages(book);
                serverPlayer.getInventory().add(book);
            }
        }
    }
}