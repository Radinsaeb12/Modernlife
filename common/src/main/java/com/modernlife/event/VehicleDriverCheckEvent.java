package com.modernlife.event;

import com.modernlife.advancement.CustomAdvancementTriggers;
import com.modernlife.data.LicenseType;
import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.AddTransactionPacket;
import com.modernlife.service.DocumentService;
import com.modernlife.service.EconomyService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VehicleDriverCheckEvent {

    private static final Map<UUID, Integer> drivingTimes = new HashMap<>();

    public static void checkPlayerVehicle(ServerPlayer player) {
        Entity vehicle = player.getVehicle();

        if (vehicle == null) {
            drivingTimes.remove(player.getUUID());
            return;
        }

        if (vehicle instanceof Boat || vehicle instanceof Minecart) {
            drivingTimes.remove(player.getUUID());
            return;
        }

        boolean isAnimal = vehicle instanceof AbstractHorse;
        LicenseType requiredLicense = isAnimal ? LicenseType.MOUNT : LicenseType.VEHICLE;

        if (DocumentService.hasValidLicense(player, requiredLicense)) {
            drivingTimes.remove(player.getUUID());
            return;
        }

        int currentTicks = drivingTimes.getOrDefault(player.getUUID(), 0) + 1;
        drivingTimes.put(player.getUUID(), currentTicks);

        if (currentTicks >= 200) {
            String activeIdentity = EconomyService.getActiveIdentity(player);

            if (activeIdentity != null && !activeIdentity.isEmpty()) {
                long cezaMiktari = 100L;

                if (player.level() instanceof ServerLevel serverLevel) {
                    ModernLifeWorldData worldData = ModernLifeWorldData.get(serverLevel);
                    cezaMiktari = isAnimal ? worldData.getUnlicensedMountPenalty() : worldData.getUnlicensedPenalty();

                    if (cezaMiktari > 0 && cezaMiktari % 5 != 0) {
                        cezaMiktari += (5 - (cezaMiktari % 5));
                    }

                    long mevcutBakiye = EconomyService.getBalance(player, activeIdentity);
                    EconomyService.setBalance(player, activeIdentity, mevcutBakiye - cezaMiktari);

                    worldData.addStateTreasuryBalance(cezaMiktari);
                    worldData.setDirty();

                    EconomyService.syncToClient(player);

                    Component fineReceipt = Component.translatable("modernlife.traffic.unlicensed_fine", cezaMiktari);
                    ModNetwork.CHANNEL.sendToPlayer(player, new AddTransactionPacket(activeIdentity, fineReceipt));

                    Component treasuryReceipt = Component.translatable("modernlife.transaction.tax_revenue", activeIdentity, cezaMiktari);
                    ModNetwork.CHANNEL.sendToPlayers(player.server.getPlayerList().getPlayers(), 
                            new AddTransactionPacket("DEVLET_HAZINESI", treasuryReceipt));
                }

                player.sendSystemMessage(Component.translatable("modernlife.traffic.unlicensed_fine", cezaMiktari));
                CustomAdvancementTriggers.FIRST_FINE.trigger(player);
            }

            drivingTimes.put(player.getUUID(), 0);
        }
    }
}