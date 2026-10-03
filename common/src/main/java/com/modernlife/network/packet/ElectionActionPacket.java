package com.modernlife.network.packet;

import com.modernlife.election.ElectionSavedData;
import com.modernlife.registry.ModItems;
import com.modernlife.service.WorldStateService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class ElectionActionPacket {
    private final int actionType;
    private final UUID targetUuid;

    public ElectionActionPacket(int actionType, UUID targetUuid) {
        this.actionType = actionType;
        this.targetUuid = targetUuid;
    }

    public static void encode(ElectionActionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.actionType);
        buf.writeBoolean(msg.targetUuid != null);
        if (msg.targetUuid != null) {
            buf.writeUUID(msg.targetUuid);
        }
    }

    public static ElectionActionPacket decode(FriendlyByteBuf buf) {
        int actionType = buf.readInt();
        UUID targetUuid = buf.readBoolean() ? buf.readUUID() : null;
        return new ElectionActionPacket(actionType, targetUuid);
    }

    public static void handle(ElectionActionPacket msg, Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.getPlayer();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            ElectionSavedData data = ElectionSavedData.get(level);

            boolean isOpOrHoldingAdminBallot = player.hasPermissions(2) || 
                    player.getMainHandItem().getItem() == ModItems.ADMIN_PRESIDENTIAL_BALLOT.get() || 
                    player.getOffhandItem().getItem() == ModItems.ADMIN_PRESIDENTIAL_BALLOT.get();

            if (msg.actionType == 0) {
                if (isOpOrHoldingAdminBallot) {
                    if (data.isActive()) {
                        player.sendSystemMessage(Component.translatable("modernlife.election.error.already_active"));
                        player.closeContainer();
                        return;
                    }

                    data.startElection();
                    player.server.getPlayerList().broadcastSystemMessage(
                            Component.translatable("modernlife.msg.election_started"), false);
                } else {
                    player.sendSystemMessage(Component.translatable("modernlife.election.error.admin_required"));
                }
            } 
            else if (msg.actionType == 1) {
                if (isOpOrHoldingAdminBallot) {
                    if (!data.isActive()) {
                        player.sendSystemMessage(Component.translatable("modernlife.election.error.no_active"));
                        player.closeContainer();
                        return;
                    }

                    Map<UUID, Integer> votes = data.getVotes();
                    UUID winnerUuid = null;
                    int maxVotes = -1;

                    if (votes != null && !votes.isEmpty()) {
                        for (Map.Entry<UUID, Integer> entry : votes.entrySet()) {
                            if (entry.getValue() > maxVotes) {
                                maxVotes = entry.getValue();
                                winnerUuid = entry.getKey();
                            }
                        }
                    }

                    data.stopElection();

                    if (winnerUuid != null && maxVotes >= 0) {
                        String winnerName = player.server.getProfileCache().get(winnerUuid)
                                .map(GameProfile::getName).orElse("Bilinmeyen Oyuncu");

                        player.server.getCommands().performPrefixedCommand(player.server.createCommandSourceStack(), "clear @a modernlife:tax_panel");
                        player.server.getCommands().performPrefixedCommand(player.server.createCommandSourceStack(), "kill @e[type=item,nbt={Item:{id:\"modernlife:tax_panel\"}}]");

                        ServerPlayer winnerPlayer = player.server.getPlayerList().getPlayer(winnerUuid);
                        if (winnerPlayer != null) {
                            WorldStateService.setPresident(winnerPlayer);
                            
                            ItemStack taxPanel = new ItemStack(ModItems.TAX_PANEL.get());
                            if (!winnerPlayer.getInventory().add(taxPanel)) {
                                winnerPlayer.drop(taxPanel, false);
                            }
                            winnerPlayer.sendSystemMessage(Component.translatable("modernlife.msg.president_elected"));
                            winnerPlayer.sendSystemMessage(Component.translatable("modernlife.msg.tax_panel_received"));
                        } else {
                            WorldStateService.setPresidentByUuid(player.server, winnerUuid);
                            player.server.getCommands().performPrefixedCommand(player.server.createCommandSourceStack(), "give " + winnerName + " modernlife:tax_panel");
                        }

                        player.server.getPlayerList().broadcastSystemMessage(
                                Component.translatable("modernlife.election.finished", winnerName, maxVotes), false);

                    } else {
                        player.server.getPlayerList().broadcastSystemMessage(
                                Component.translatable("modernlife.election.stopped_no_votes"), false);
                    }
                        
                    for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                        if (p.getMainHandItem().getItem() == ModItems.PRESIDENTIAL_BALLOT.get() || 
                            p.getOffhandItem().getItem() == ModItems.ADMIN_PRESIDENTIAL_BALLOT.get() || 
                            p.getOffhandItem().getItem() == ModItems.PRESIDENTIAL_BALLOT.get()) {
                            p.closeContainer();
                        }
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("modernlife.election.error.admin_required"));
                }
            } 
            else if (msg.actionType == 2) {
                if (!data.isActive()) {
                    player.sendSystemMessage(Component.translatable("modernlife.election.error.no_active"));
                    player.closeContainer();
                    return;
                }

                boolean holdingAdminBallot = player.getMainHandItem().getItem() == ModItems.ADMIN_PRESIDENTIAL_BALLOT.get() || 
                                             player.getOffhandItem().getItem() == ModItems.ADMIN_PRESIDENTIAL_BALLOT.get();
                if (holdingAdminBallot) {
                    player.sendSystemMessage(Component.translatable("modernlife.election.error.admin_cannot_vote"));
                    player.closeContainer();
                    return;
                }

                if (data.hasVoted(player.getUUID())) {
                    player.sendSystemMessage(Component.translatable("modernlife.election.error.already_voted"));
                    player.closeContainer();
                    return;
                }

                if (msg.targetUuid != null) {
                    data.addVote(player.getUUID(), msg.targetUuid);
                    player.sendSystemMessage(Component.translatable("modernlife.election.vote_success"));
                    player.closeContainer();
                }
            }
        });
    }
}