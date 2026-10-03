package com.modernlife.service;

import com.modernlife.client.ClientWorldState;
import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.SyncEconomyModePacket;
import com.modernlife.network.packet.SyncWorldStatePacket;
import com.modernlife.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

public final class WorldStateService {
    private static final double DEFAULT_PENALTY_RATE = 0.10;
    
    private WorldStateService() {}
    
    public static void setPenaltyRate(MinecraftServer server, double rate) {
        getWorldData(server).setPenaltyRate(rate);
    }
    
    public static double getPenaltyRate(MinecraftServer server) {
        return getWorldData(server).getPenaltyRate();
    }

    public static ModernLifeWorldData getWorldData(final ServerLevel level) {
        return ModernLifeWorldData.get(level);
    }

    public static ModernLifeWorldData getWorldData(final MinecraftServer server) {
        return ModernLifeWorldData.get(server.overworld());
    }

    public static void ensureSingleplayerPresident(final ServerPlayer player) {
        final MinecraftServer server = player.getServer();
        if (server == null || server.isDedicatedServer()) {
            return;
        }

        final ModernLifeWorldData worldData = getWorldData(server);
        if (worldData.getPresidentUuid() == null) {
            setPresident(player);
            player.sendSystemMessage(Component.translatable("modernlife.msg.president_elected"));

            ItemStack taxPanel = new ItemStack(ModItems.TAX_PANEL.get());
            if (!player.getInventory().contains(taxPanel)) {
                if (!player.getInventory().add(taxPanel)) {
                    player.drop(taxPanel, false);
                }
                player.sendSystemMessage(Component.translatable("modernlife.msg.tax_panel_received"));
            }
        }
    }

    public static boolean isPresident(final Player player) {
        if (player == null) return false;

        if (player.level().isClientSide()) {
            UUID presUuid = ClientWorldState.getPresidentUuid();
            return presUuid != null && presUuid.equals(player.getUUID());
        }

        if (player.getServer() != null) {
            UUID presidentUuid = getWorldData(player.getServer()).getPresidentUuid();
            return presidentUuid != null && presidentUuid.equals(player.getUUID());
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            final UUID presidentUuid = getWorldData(serverLevel).getPresidentUuid();
            return presidentUuid != null && presidentUuid.equals(player.getUUID());
        }
        return false;
    }

    public static void setPresident(final ServerPlayer player) {
        final MinecraftServer server = player.getServer();
        if (server == null) return;
        
        final ModernLifeWorldData worldData = getWorldData(server);
        worldData.setPresidentUuid(player.getUUID());
        
        syncToAll(server);
    }

    public static void setPresidentByUuid(final MinecraftServer server, final UUID presidentUuid) {
        if (server == null || presidentUuid == null) return;
        
        final ModernLifeWorldData worldData = getWorldData(server);
        worldData.setPresidentUuid(presidentUuid);
        
        syncToAll(server);
    }

    @Nullable
    public static UUID getPresidentUuid(final MinecraftServer server) {
        return getWorldData(server).getPresidentUuid();
    }

    @Nullable
    public static Component getPresidentDisplayName(final MinecraftServer server) {
        final UUID presidentUuid = getPresidentUuid(server);
        if (presidentUuid == null) {
            return null;
        }
        final ServerPlayer onlinePresident = server.getPlayerList().getPlayer(presidentUuid);
        if (onlinePresident != null) {
            return onlinePresident.getDisplayName();
        }
        return Component.literal(presidentUuid.toString());
    }

    public static void syncToClient(final ServerPlayer player) {
        final ModernLifeWorldData worldData = getWorldData(player.serverLevel());
        final SyncWorldStatePacket packet = SyncWorldStatePacket.fromWorldData(worldData);
        ModNetwork.CHANNEL.sendToPlayer(player, packet);

        boolean isLightmans = worldData.getEconomyMode() == ModernLifeWorldData.EconomyMode.LIGHTMANS_CURRENCY;
        ModNetwork.CHANNEL.sendToPlayer(player, new SyncEconomyModePacket(isLightmans));
    }

    public static void syncToAll(final MinecraftServer server) {
        if (server == null) return;
        final ModernLifeWorldData worldData = getWorldData(server);
        final SyncWorldStatePacket packet = SyncWorldStatePacket.fromWorldData(worldData);
        ModNetwork.CHANNEL.sendToPlayers(server.getPlayerList().getPlayers(), packet);

        boolean isLightmans = worldData.getEconomyMode() == ModernLifeWorldData.EconomyMode.LIGHTMANS_CURRENCY;
        ModNetwork.CHANNEL.sendToPlayers(server.getPlayerList().getPlayers(), new SyncEconomyModePacket(isLightmans));
    }
}