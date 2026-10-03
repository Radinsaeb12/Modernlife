package com.modernlife.command;

import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.AddTransactionPacket;
import com.modernlife.network.packet.AtmActionPacket;
import com.modernlife.registry.ModItems;
import com.modernlife.service.EconomyService;
import com.modernlife.service.WorldStateService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModCommands {
    private ModCommands() {}

    public static void register() {
        CommandRegistrationEvent.EVENT.register(ModCommands::registerCommands);
    }

    public static void syncTreasury(MinecraftServer server) {
        if (server == null) return;
        WorldStateService.syncToAll(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            EconomyService.syncToClient(player);
        }
    }

    private static long calculateCashInInventory(ServerPlayer player) {
        long totalCash = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            int count = stack.getCount();

            if (item == ModItems.TL_5.get()) totalCash += count * 5L;
            else if (item == ModItems.TL_10.get()) totalCash += count * 10L;
            else if (item == ModItems.TL_20.get()) totalCash += count * 20L;
            else if (item == ModItems.TL_50.get()) totalCash += count * 50L;
            else if (item == ModItems.TL_100.get()) totalCash += count * 100L;
            else if (item == ModItems.TL_200.get()) totalCash += count * 200L;
        }
        return totalCash;
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        final LiteralArgumentBuilder<CommandSourceStack> modernlifeCmd = Commands.literal("modernlife");

        modernlifeCmd.then(Commands.literal("vote")
                .then(Commands.argument("choice", StringArgumentType.word())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String choice = StringArgumentType.getString(ctx, "choice").toLowerCase();
                    ModernLifeWorldData worldData = ModernLifeWorldData.get(player.serverLevel());

                    if (!worldData.isVoteActive()) {
                        player.sendSystemMessage(Component.translatable("modernlife.command.vote.no_active"));
                        return 0;
                    }

                    if (worldData.hasVoted(player.getUUID())) {
                        player.sendSystemMessage(Component.translatable("modernlife.command.vote.already"));
                        return 0;
                    }

                    boolean isYes = choice.equals("yes");
                    worldData.addVote(player.getUUID(), isYes);
                    player.sendSystemMessage(Component.translatable("modernlife.election.vote_success"));

                    int totalOnline = player.server.getPlayerList().getPlayerCount();
                    int totalVotes = worldData.getYesVotes() + worldData.getNoVotes();

                    if (totalVotes >= totalOnline) {
                        finalizeTreasuryVote(player.server, worldData);
                    }
                    return 1;
                })));

        modernlifeCmd.then(Commands.literal("donate_treasury")
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                .executes(ctx -> {
                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                    long miktar = LongArgumentType.getLong(ctx, "amount");
                    String activeId = EconomyService.getActiveIdentity(sender);

                    if (activeId.isEmpty() || EconomyService.isTreasury(activeId)) {
                        sender.sendSystemMessage(Component.translatable("modernlife.command.no_active_identity").withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    if (EconomyService.withdraw(sender, activeId, miktar)) {
                        ModernLifeWorldData worldData = ModernLifeWorldData.get(sender.serverLevel());
                        worldData.addStateTreasuryBalance(miktar);
                        worldData.setDirty();

                        syncTreasury(sender.server);

                        sender.displayClientMessage(Component.translatable("modernlife.command.treasury.donate_success", miktar), false);

                        ModNetwork.CHANNEL.sendToPlayer(sender,
                            new AddTransactionPacket(activeId, Component.translatable("modernlife.transaction.treasury_donation", miktar)));
                        ModNetwork.CHANNEL.sendToPlayers(sender.server.getPlayerList().getPlayers(),
                            new AddTransactionPacket(EconomyService.TREASURY_ID, Component.translatable("modernlife.transaction.treasury_received", activeId, miktar)));
                        return 1;
                    } else {
                        sender.displayClientMessage(Component.translatable("modernlife.bank.not_enough_money").withStyle(ChatFormatting.RED), false);
                        return 0;
                    }
                })));

        modernlifeCmd.then(Commands.literal("president")
                .executes(ctx -> {
                    final ServerPlayer player = ctx.getSource().getPlayerOrException();
                    final var server = player.getServer();
                    if (server == null) return 0;

                    final Component presidentName = WorldStateService.getPresidentDisplayName(server);
                    if (presidentName == null) {
                        ctx.getSource().sendSuccess(() -> Component.translatable("modernlife.command.president.none"), false);
                    } else {
                        final boolean isPresident = WorldStateService.isPresident(player);
                        ctx.getSource().sendSuccess(() -> Component.translatable(isPresident ? "modernlife.command.president.self" : "modernlife.command.president.other", presidentName), false);
                    }
                    return 1;
                })
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    final ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    WorldStateService.setPresident(target);
                                    syncTreasury(target.server);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("modernlife.command.president.set", target.getDisplayName()), true);
                                    return 1;
                                }))));

        modernlifeCmd.then(Commands.literal("world")
                .executes(ctx -> {
                    final ServerPlayer player = ctx.getSource().getPlayerOrException();
                    final var server = player.getServer();
                    if (server == null) return 0;

                    final var worldData = WorldStateService.getWorldData(server);
                    final var taxRates = worldData.getTaxRates();
                    ctx.getSource().sendSuccess(() -> Component.translatable("modernlife.command.world.info", worldData.getWorldDayCounter(), taxRates.vehicleDaily(), taxRates.mountDaily(), taxRates.livingDaily()), false);
                    return 1;
                }));

        modernlifeCmd.then(Commands.literal("balance")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String activeId = EconomyService.getActiveIdentity(player);
                    if (activeId.isEmpty()) {
                        player.sendSystemMessage(Component.translatable("modernlife.command.no_active_identity").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    long bankBalance = EconomyService.getBalance(player, activeId);
                    long cashInHand = calculateCashInInventory(player);

                    player.sendSystemMessage(Component.translatable("modernlife.command.balance.bank_info", activeId, bankBalance));
                    player.sendSystemMessage(Component.translatable("modernlife.command.balance.cash_info", cashInHand));
                    player.sendSystemMessage(Component.translatable("modernlife.command.balance.cash_note").withStyle(ChatFormatting.GRAY));
                    return 1;
                })
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            long amount = LongArgumentType.getLong(ctx, "amount");
                            String activeId = EconomyService.getActiveIdentity(target);

                            if (activeId.isEmpty()) {
                                ctx.getSource().sendFailure(Component.translatable("modernlife.command.no_active_identity"));
                                return 0;
                            }

                            EconomyService.setBalance(target, activeId, amount);
                            ctx.getSource().sendSuccess(() -> Component.translatable("modernlife.command.balance.set_success", target.getDisplayName(), activeId, amount), true);
                            return 1;
                        })))));

        dispatcher.register(modernlifeCmd);
    }

    private static void finalizeTreasuryVote(MinecraftServer server, ModernLifeWorldData worldData) {
        int yes = worldData.getYesVotes();
        int no = worldData.getNoVotes();
        int amount = worldData.getPendingAmount();
        String target = worldData.getPendingTarget();
        String reason = worldData.getPendingReason();

        if (yes > no) {
            if ("ATM_CASH".equals(reason) || target.startsWith("ATM_CASH:")) {
                String playerName = target.startsWith("ATM_CASH:") ? target.substring("ATM_CASH:".length()) : target;
                ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);

                if (worldData.getStateTreasuryBalance() >= amount) {
                    worldData.removeStateTreasuryBalance(amount);

                    if (player != null) {
                        AtmActionPacket.giveCashToPlayer(player, amount);
                        Component receipt = Component.translatable("modernlife.transaction.atm_withdraw", amount);
                        ModNetwork.CHANNEL.sendToPlayer(player, new AddTransactionPacket(EconomyService.TREASURY_ID, receipt));
                    }

                    ModNetwork.CHANNEL.sendToPlayers(server.getPlayerList().getPlayers(),
                            new AddTransactionPacket(EconomyService.TREASURY_ID,
                                    Component.translatable("modernlife.transaction.treasury_spent", playerName, amount)));

                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        p.sendSystemMessage(Component.translatable("modernlife.command.vote.withdraw_passed", amount, playerName));
                    }
                }
            } else if ("ATM_DEPOSIT".equals(reason) || target.startsWith("ATM_DEPOSIT:")) {
                String playerName = target.startsWith("ATM_DEPOSIT:") ? target.substring("ATM_DEPOSIT:".length()) : target;
                worldData.addStateTreasuryBalance(amount);
                Component receipt = Component.translatable("modernlife.transaction.atm_deposit", amount);
                ModNetwork.CHANNEL.sendToPlayers(server.getPlayerList().getPlayers(),
                        new AddTransactionPacket(EconomyService.TREASURY_ID, receipt));

                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.sendSystemMessage(Component.translatable("modernlife.command.vote.deposit_passed", amount, playerName));
                }
            } else {
                if (worldData.getStateTreasuryBalance() >= amount) {
                    worldData.removeStateTreasuryBalance(amount);

                    ServerPlayer targetPlayer = null;
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        if (EconomyService.hasValidBankCard(p, target)) {
                            targetPlayer = p;
                            break;
                        }
                    }

                    if (targetPlayer != null) {
                        final ServerPlayer finalTarget = targetPlayer;
                        EconomyService.deposit(finalTarget, target, amount);
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.sendSystemMessage(Component.translatable("modernlife.command.vote.transfer_passed", amount, target));
                        }

                        ModNetwork.CHANNEL.sendToPlayers(server.getPlayerList().getPlayers(),
                                new AddTransactionPacket(EconomyService.TREASURY_ID,
                                        Component.translatable("modernlife.transaction.treasury_spent", target, amount)));
                        ModNetwork.CHANNEL.sendToPlayer(finalTarget,
                                new AddTransactionPacket(target,
                                        Component.translatable("modernlife.transaction.treasury_received_vote", amount, reason)));
                    } else {
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.sendSystemMessage(Component.translatable("modernlife.command.vote.target_offline", target));
                        }
                    }
                }
            }
        } else {
            String displayName = target;
            if ("ATM_DEPOSIT".equals(reason) || target.startsWith("ATM_DEPOSIT:")) {
                String playerName = target.startsWith("ATM_DEPOSIT:") ? target.substring("ATM_DEPOSIT:".length()) : target;
                displayName = playerName;
                ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                if (player != null) {
                    AtmActionPacket.giveCashToPlayer(player, amount);
                }
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.sendSystemMessage(Component.translatable("modernlife.command.vote.deposit_rejected", amount, displayName));
                }
            } else {
                if ("ATM_CASH".equals(reason) || target.startsWith("ATM_CASH:")) {
                    displayName = target.startsWith("ATM_CASH:") ? target.substring("ATM_CASH:".length()) : target;
                }
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.sendSystemMessage(Component.translatable("modernlife.command.vote.rejected", amount, displayName));
                }
            }
        }

        worldData.endVote();
        syncTreasury(server);
    }
}