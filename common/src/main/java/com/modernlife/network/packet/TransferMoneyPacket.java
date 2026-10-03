package com.modernlife.network.packet;

import com.modernlife.advancement.CustomAdvancementTriggers;
import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.network.ModNetwork;
import com.modernlife.service.EconomyService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class TransferMoneyPacket {
    private final String senderIdentity;
    private final String targetIdentity;
    private final int amount;
    private final String description;

    public TransferMoneyPacket(String senderIdentity, String targetIdentity, int amount, String description) {
        this.senderIdentity = senderIdentity;
        this.targetIdentity = targetIdentity;
        this.amount = amount;
        this.description = description != null && !description.isEmpty() ? description : "";
    }

    public static TransferMoneyPacket decode(FriendlyByteBuf buf) {
        return new TransferMoneyPacket(buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(senderIdentity);
        buf.writeUtf(targetIdentity);
        buf.writeInt(amount);
        buf.writeUtf(description);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            ServerPlayer senderPlayer = (ServerPlayer) ctx.getPlayer();
            if (senderPlayer != null && !senderIdentity.isEmpty() && amount > 0) {

                boolean isLightman = EconomyService.isLightmansActive(senderPlayer);

                if (!isLightman && (amount < 5 || amount % 5 != 0)) {
                    senderPlayer.sendSystemMessage(Component.translatable("modernlife.bank.error_multiple_of_5"));
                    return;
                }

                if (senderIdentity.equals(targetIdentity)) {
                    senderPlayer.sendSystemMessage(Component.translatable("modernlife.bank.error_cannot_send_self").withStyle(ChatFormatting.RED));
                    return;
                }

                if (senderIdentity.equals("DEVLET_HAZINESI")) {
                    ModernLifeWorldData worldData = ModernLifeWorldData.get(senderPlayer.serverLevel());

                    if (worldData.isVoteActive()) {
                        senderPlayer.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_already_active").withStyle(ChatFormatting.RED));
                        return;
                    }
                    if (worldData.getStateTreasuryBalance() < amount) {
                        senderPlayer.sendSystemMessage(Component.translatable("modernlife.command.treasury.not_enough_money").withStyle(ChatFormatting.RED));
                        return;
                    }

                    String effectiveDesc = description.isEmpty() ? Component.translatable("modernlife.bank.eft_transfer_default_description").getString() : description;
                    worldData.startVote(amount, targetIdentity, effectiveDesc);

                    Component formattedAmount = EconomyService.formatMoney(senderPlayer, amount);
                    for (ServerPlayer player : senderPlayer.server.getPlayerList().getPlayers()) {
                        player.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_announcement_header"));
                        player.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_announcement_details", formattedAmount, targetIdentity, effectiveDesc));

                        MutableComponent yesBtn = Component.translatable("modernlife.command.vote.yes")
                                .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/modernlife vote yes")));
                        MutableComponent noBtn = Component.translatable("modernlife.command.vote.no")
                                .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/modernlife vote no")));

                        player.sendSystemMessage(yesBtn.append(Component.literal("   ")).append(noBtn));
                        player.sendSystemMessage(Component.translatable("modernlife.command.treasury.vote_announcement_footer"));
                    }
                    return;
                }

                ServerPlayer foundTarget = null;
                for (ServerPlayer p : senderPlayer.server.getPlayerList().getPlayers()) {
                    if (EconomyService.hasValidBankCard(p, targetIdentity)) {
                        foundTarget = p;
                        break;
                    }
                }

                if (foundTarget == null) {
                    senderPlayer.sendSystemMessage(Component.translatable("modernlife.bank.target_offline", targetIdentity));
                    return;
                }

                int fee = (amount <= 1000) ? 5 : (amount <= 5000 ? 10 : 15);
                long totalDeduction = (long) amount + fee;

                long senderBalance = EconomyService.getBalance(senderPlayer, senderIdentity);

                if (senderBalance >= totalDeduction) {
                    boolean withdrawSuccess = EconomyService.withdraw(senderPlayer, senderIdentity, totalDeduction);
                    if (!withdrawSuccess) {
                        senderPlayer.sendSystemMessage(Component.translatable("modernlife.bank.transaction_failed").withStyle(ChatFormatting.RED));
                        return;
                    }

                    EconomyService.deposit(foundTarget, targetIdentity, amount);

                    Component formattedAmount = EconomyService.formatMoney(senderPlayer, amount);
                    Component formattedFee = EconomyService.formatMoney(senderPlayer, fee);
                    senderPlayer.sendSystemMessage(Component.translatable("modernlife.bank.transfer_success_with_fee", formattedAmount, formattedFee));

                    String finalDesc = description.trim().isEmpty() ? Component.translatable("modernlife.bank.eft_transfer_default_description").getString() : description;

                    foundTarget.sendSystemMessage(Component.translatable("modernlife.bank.received_money_header"));
                    foundTarget.sendSystemMessage(Component.translatable("modernlife.bank.received_money_amount", formattedAmount));
                    foundTarget.sendSystemMessage(Component.translatable("modernlife.bank.received_money_description", finalDesc));
                    foundTarget.sendSystemMessage(Component.translatable("modernlife.bank.received_money_footer"));

                    Component senderReceipt = Component.translatable("modernlife.transaction.eft_sent", targetIdentity, formattedAmount, finalDesc);
                    ModNetwork.CHANNEL.sendToPlayer(senderPlayer, new AddTransactionPacket(senderIdentity, senderReceipt));

                    Component receiverReceipt = Component.translatable("modernlife.transaction.eft_received", senderIdentity, formattedAmount, finalDesc);
                    ModNetwork.CHANNEL.sendToPlayer(foundTarget, new AddTransactionPacket(targetIdentity, receiverReceipt));

                    CustomAdvancementTriggers.EFT_TRANSFER.trigger(senderPlayer);

                } else {
                    senderPlayer.sendSystemMessage(Component.translatable("modernlife.bank.insufficient_balance"));
                }
            }
        });
    }
}