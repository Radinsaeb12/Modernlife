package com.modernlife.item;

import com.modernlife.election.ElectionSavedData;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.OpenElectionMenuPacket;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PresidentialBallotItem extends Item {
    public PresidentialBallotItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                ElectionSavedData data = ElectionSavedData.get(serverPlayer.serverLevel());
                List<OpenElectionMenuPacket.Candidate> candidates = new ArrayList<>();
                Set<UUID> addedUuids = new HashSet<>();

                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    String name = p.getGameProfile().getName();
                    candidates.add(new OpenElectionMenuPacket.Candidate(p.getUUID(), name));
                    addedUuids.add(p.getUUID());
                }

                File playerdataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
                if (playerdataDir.exists() && playerdataDir.isDirectory()) {
                    File[] files = playerdataDir.listFiles((dir, name) -> name.endsWith(".dat"));
                    if (files != null) {
                        for (File file : files) {
                            try {
                                String filename = file.getName();
                                String uuidStr = filename.substring(0, filename.length() - 4);
                                UUID uuid = UUID.fromString(uuidStr);

                                if (!addedUuids.contains(uuid)) {
                                    String name = server.getProfileCache().get(uuid)
                                            .map(GameProfile::getName).orElse(null);
                                    if (name != null && !name.isBlank() && !name.equals("Bilinmeyen Oyuncu")) {
                                        candidates.add(new OpenElectionMenuPacket.Candidate(uuid, name));
                                        addedUuids.add(uuid);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }

                if (candidates.isEmpty()) {
                    candidates.add(new OpenElectionMenuPacket.Candidate(serverPlayer.getUUID(), serverPlayer.getName().getString()));
                }

                ModNetwork.CHANNEL.sendToPlayer(
                        serverPlayer,
                        new OpenElectionMenuPacket(data.isActive(), false, candidates)
                );
            }
        }
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}