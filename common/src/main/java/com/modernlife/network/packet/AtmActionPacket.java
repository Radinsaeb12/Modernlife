package com.modernlife.network.packet;

import com.modernlife.capability.ModCapabilities;
import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.economy.compat.EconomyInventoryHelper;
import com.modernlife.network.ModNetwork;
import com.modernlife.registry.ModItems;
import com.modernlife.service.EconomyService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.function.Supplier;

public class AtmActionPacket {
    private final int actionType;
    private final int amount;
    private final String selectedIdentity;
    private final String targetCardId;

    public AtmActionPacket(int actionType, int amount, String selectedIdentity, String targetCardId) {
        this.actionType = actionType;
        this.amount = amount;
        this.selectedIdentity = selectedIdentity != null ? selectedIdentity : "";
        this.targetCardId = targetCardId != null ? targetCardId : "";
    }

    public AtmActionPacket(FriendlyByteBuf buf) {
        this.actionType = buf.readInt();
        this.amount = buf.readInt();
        this.selectedIdentity = buf.readUtf();
        this.targetCardId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(actionType);
        buf.writeInt(amount);
        buf.writeUtf(selectedIdentity);
        buf.writeUtf(targetCardId);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.getPlayer();
            if (player != null && !selectedIdentity.isEmpty()) {

                boolean isLightman = EconomyService.isLightmansActive(player);

                if (actionType == 5) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        ModernLifeWorldData worldData = ModernLifeWorldData.get(serverLevel);
                        
                        if (player.getUUID().equals(worldData.getPresidentUuid())) {
                            boolean hasCard = false;
                            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                ItemStack stack = player.getInventory().getItem(i);
                                if (stack.getItem() == ModItems.BANK_CARD.get() && stack.hasTag() && EconomyService.TREASURY_ID.equals(stack.getTag().getString("CardOwner"))) {
                                    hasCard = true; 
                                    break;
                                }
                            }
                            
                            if (hasCard) {
                                player.sendSystemMessage(Component.translatable("modernlife.atm.already_has_treasury_card"));
                                return;
                            }

                            String yeniCardId = "HAZI-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                            issueBankCard(player, EconomyService.TREASURY_ID, yeniCardId);
                            player.sendSystemMessage(Component.translatable("modernlife.atm.treasury_card_issued"));
                        } else {
                            player.sendSystemMessage(Component.translatable("modernlife.atm.president_only"));
                        }
                    }
                    return;
                }

                if (actionType == 3) {
                    if (!targetCardId.isEmpty()) {
                        ModCapabilities.getPersistentData(player).putBoolean("ValidCard_" + targetCardId, false);
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack.getItem() == ModItems.BANK_CARD.get() && stack.hasTag()) {
                                if (targetCardId.equals(stack.getTag().getString("CardID"))) {
                                    stack.getTag().putBoolean("Cancelled", true);
                                    stack.getTag().putBoolean("IptalEdildi", true);
                                }
                            }
                        }
                        player.sendSystemMessage(Component.translatable("modernlife.atm.card_cancelled", targetCardId));
                    }
                    return;
                }

                if (actionType == 0) {
                    String yeniCardId = UUID.randomUUID().toString().substring(0, 8);
                    String firstCardKey = "HasFirstBankCard_" + selectedIdentity;
                    
                    boolean hasReceivedFirstCard = ModCapabilities.getPersistentData(player).getBoolean(firstCardKey);

                    if (!hasReceivedFirstCard) {
                        ModCapabilities.getPersistentData(player).putBoolean(firstCardKey, true);
                        
                        if (!EconomyService.hasBankAccount(player, selectedIdentity)) {
                            EconomyService.setBalance(player, selectedIdentity, 0L);
                        }
                        
                        issueBankCard(player, selectedIdentity, yeniCardId);
                        player.sendSystemMessage(Component.translatable("modernlife.atm.first_card_free"));
                    } else {
                        long bankaBakiyesi = EconomyService.getBalance(player, selectedIdentity);
                        long toplamCeptekiPara = EconomyInventoryHelper.getInventoryTotalCash(player);

                        if (bankaBakiyesi >= 200) {
                            EconomyService.withdraw(player, selectedIdentity, 200);
                            issueBankCard(player, selectedIdentity, yeniCardId);
                            player.sendSystemMessage(Component.translatable("modernlife.atm.card_bought_bank"));
                        } else if (toplamCeptekiPara >= 200) {
                            EconomyInventoryHelper.deductCash(player, 200);
                            issueBankCard(player, selectedIdentity, yeniCardId);
                            player.sendSystemMessage(Component.translatable("modernlife.atm.card_bought_cash"));
                        } else {
                            player.sendSystemMessage(Component.translatable("modernlife.atm.no_money_card"));
                        }
                    }
                    return;
                }

                if (!isLightman && (actionType == 1 || actionType == 2) && (amount < 5 || amount % 5 != 0)) {
                    player.sendSystemMessage(Component.translatable("modernlife.atm.error_multiple_of_5"));
                    return;
                }

                if (actionType == 1) {
                    long toplamCeptekiPara = EconomyInventoryHelper.getInventoryTotalCash(player);
                    if (toplamCeptekiPara < amount) {
                        Component formattedAmount = EconomyService.formatMoney(player, amount);
                        player.sendSystemMessage(Component.translatable("modernlife.atm.no_cash_inv", formattedAmount));
                        return;
                    }

                    if (EconomyService.isTreasury(selectedIdentity)) {
                        ModernLifeWorldData worldData = ModernLifeWorldData.get(player.serverLevel());
                        if (worldData.isVoteActive()) {
                            player.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_already_active").withStyle(ChatFormatting.RED));
                            return;
                        }

                        EconomyInventoryHelper.deductCash(player, amount);

                        String reason = "ATM_DEPOSIT";
                        String target = player.getScoreboardName();
                        worldData.startVote(amount, target, reason);

                        broadcastVote(player, amount, target, Component.translatable("modernlife.command.treasury.vote_atm_deposit_reason").getString());
                        player.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_deposit_waiting"));
                        return;
                    }

                    EconomyInventoryHelper.deductCash(player, amount);
                    EconomyService.deposit(player, selectedIdentity, amount);
                    Component formattedAmount = EconomyService.formatMoney(player, amount);
                    player.sendSystemMessage(Component.translatable("modernlife.atm.deposit_success", formattedAmount));

                    Component receipt = Component.translatable("modernlife.transaction.atm_deposit", formattedAmount);
                    ModNetwork.CHANNEL.sendToPlayer(player, new AddTransactionPacket(selectedIdentity, receipt));
                } 
                else if (actionType == 2) {
                    if (EconomyService.isTreasury(selectedIdentity)) {
                        ModernLifeWorldData worldData = ModernLifeWorldData.get(player.serverLevel());
                        if (worldData.isVoteActive()) {
                            player.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_already_active").withStyle(ChatFormatting.RED));
                            return;
                        }
                        if (worldData.getStateTreasuryBalance() < amount) {
                            player.sendSystemMessage(Component.translatable("modernlife.command.treasury.not_enough_money").withStyle(ChatFormatting.RED));
                            return;
                        }

                        String reason = "ATM_CASH";
                        String target = player.getScoreboardName();
                        worldData.startVote(amount, target, reason);

                        broadcastVote(player, amount, target, Component.translatable("modernlife.command.treasury.vote_atm_withdraw_reason").getString());
                        player.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_withdraw_waiting"));
                        return;
                    }

                    if (EconomyService.withdraw(player, selectedIdentity, amount)) {
                        EconomyInventoryHelper.giveCash(player, amount);
                        Component formattedAmount = EconomyService.formatMoney(player, amount);
                        player.sendSystemMessage(Component.translatable("modernlife.atm.withdraw_success", formattedAmount));

                        Component receipt = Component.translatable("modernlife.transaction.atm_withdraw", formattedAmount);
                        ModNetwork.CHANNEL.sendToPlayer(player, new AddTransactionPacket(selectedIdentity, receipt));
                    } else {
                        player.sendSystemMessage(Component.translatable("modernlife.atm.no_balance"));
                    }
                }
            }
        });
    }

    private void broadcastVote(ServerPlayer sender, int amount, String target, String reason) {
        Component formattedAmount = EconomyService.formatMoney(sender, amount);
        for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_announcement_header"));
            p.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_announcement_details", formattedAmount, target, reason));

            MutableComponent yesBtn = Component.translatable("modernlife.command.vote.yes")
                    .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/modernlife vote yes")));
            MutableComponent noBtn = Component.translatable("modernlife.command.vote.no")
                    .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/modernlife vote no")));

            p.sendSystemMessage(yesBtn.append(Component.literal("   ")).append(noBtn));
            p.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_announcement_footer"));
        }
    }

    private void issueBankCard(ServerPlayer player, String activeIdentity, String newCardId) {
        ModCapabilities.getPersistentData(player).putBoolean("ValidCard_" + newCardId, true);
        ItemStack newCard = new ItemStack(ModItems.BANK_CARD.get());
        newCard.getOrCreateTag().putString("CardOwner", activeIdentity);
        newCard.getTag().putString("CardID", newCardId);
        newCard.getTag().putBoolean("Cancelled", false);
        player.addItem(newCard);
    }

    public static void giveCashToPlayer(ServerPlayer player, int amount) {
        EconomyInventoryHelper.giveCash(player, amount);
    }

    public static void deductCashFromInventory(ServerPlayer player, int amount) {
        EconomyInventoryHelper.deductCash(player, amount);
    }

    public static long getInventoryTotalCash(ServerPlayer player) {
        return EconomyInventoryHelper.getInventoryTotalCash(player);
    }
}