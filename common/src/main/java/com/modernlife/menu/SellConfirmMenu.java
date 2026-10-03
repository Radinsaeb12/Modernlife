package com.modernlife.menu;

import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.economy.BankerEconomyMath;
import com.modernlife.economy.EconomyRegistry;
import com.modernlife.item.BankCardItem;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.AddTransactionPacket;
import com.modernlife.service.EconomyService;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class SellConfirmMenu extends AbstractContainerMenu {
    private final List<ItemStack> itemsToSell;
    private final SimpleContainer buttonContainer = new SimpleContainer(9);
    private boolean isConfirmed = false;

    public SellConfirmMenu(int containerId, Inventory playerInventory, List<ItemStack> itemsToSell) {
        super(MenuType.GENERIC_9x1, containerId);
        this.itemsToSell = itemsToSell;

        setupMenuSlots(playerInventory);
    }

    private void setupMenuSlots(Inventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(buttonContainer, i, 8 + i * 18, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
                @Override
                public boolean mayPickup(Player player) { return false; }
            });
        }

        ItemStack cancelBtn = new ItemStack(Items.RED_CONCRETE);
        cancelBtn.setHoverName(Component.translatable("modernlife.banker.cancel_btn").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        buttonContainer.setItem(2, cancelBtn);

        buttonContainer.setItem(4, buildInfoChest(playerInventory.player));

        ItemStack confirmBtn = new ItemStack(Items.LIME_CONCRETE);
        confirmBtn.setHoverName(Component.translatable("modernlife.banker.confirm_btn").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        buttonContainer.setItem(6, confirmBtn);

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 109));
        }
    }

    private boolean hasPhysicalBankCard(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof BankCardItem && stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                if (tag.getBoolean("Cancelled") || tag.getBoolean("IptalEdildi")) {
                    continue;
                }
                String owner = tag.contains("CardOwner") ? tag.getString("CardOwner") : tag.getString("realName");
                if (!owner.isEmpty() && !"DEVLET_HAZINESI".equals(owner) && !owner.startsWith("IPTAL_")) {
                    return true;
                }
            }
        }
        return false;
    }

    private long calculateTotal(Player player) {
        long rawTotal = 0;
        ModernLifeWorldData.DifficultyLevel difficulty = ModernLifeWorldData.DifficultyLevel.NORMAL;
        
        if (player.level() instanceof ServerLevel serverLevel) {
            difficulty = ModernLifeWorldData.get(serverLevel).getDifficulty();
        }

        for (ItemStack stack : itemsToSell) {
            double basePrice = EconomyRegistry.getItemPrice(stack.getItem());
            if (basePrice > 0) {
                rawTotal += BankerEconomyMath.calculateBankerPrice((long) basePrice, stack.getCount(), difficulty);
            }
        }
        
        boolean isLightman = EconomyService.isLightmansActive(player);
        if (isLightman) {
            return rawTotal;
        }
        return Math.round(rawTotal / 5.0) * 5;
    }

    private ItemStack buildInfoChest(Player player) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7-------------------"));

        ModernLifeWorldData.DifficultyLevel difficulty = ModernLifeWorldData.DifficultyLevel.NORMAL;
        if (player.level() instanceof ServerLevel serverLevel) {
            difficulty = ModernLifeWorldData.get(serverLevel).getDifficulty();
        }

        for (ItemStack stack : itemsToSell) {
            double basePrice = EconomyRegistry.getItemPrice(stack.getItem());
            if (basePrice > 0) {
                long itemTotal = BankerEconomyMath.calculateBankerPrice((long) basePrice, stack.getCount(), difficulty);
                Component formatliItemTotal = EconomyService.formatMoney(player, itemTotal);
                lore.add(Component.literal("§f" + stack.getCount() + "x ")
                        .append(stack.getHoverName())
                        .append(Component.literal(" §a+ ").append(formatliItemTotal)));
            } else {
                lore.add(Component.literal("§c" + stack.getCount() + "x ")
                        .append(stack.getHoverName())
                        .append(Component.literal(" "))
                        .append(Component.translatable("modernlife.banker.unsellable_refund")));
            }
        }

        lore.add(Component.literal("§7-------------------"));
        long grossPrice = calculateTotal(player);
        boolean hasCard = hasPhysicalBankCard(player);
        boolean isLightman = EconomyService.isLightmansActive(player);

        if (!hasCard) {
            long tax = isLightman ? Math.round(grossPrice * 0.15) : Math.round((grossPrice * 0.15) / 5.0) * 5;
            long netEarned = grossPrice - tax;
            lore.add(Component.translatable("modernlife.banker.gross_earnings", EconomyService.formatMoney(player, grossPrice)));
            lore.add(Component.translatable("modernlife.banker.state_commission", EconomyService.formatMoney(player, tax)));
            lore.add(Component.translatable("modernlife.banker.net_deposited", EconomyService.formatMoney(player, netEarned)));
        } else {
            lore.add(Component.translatable("modernlife.banker.net_earnings_bonus", EconomyService.formatMoney(player, grossPrice)));
        }

        ItemStack chest = new ItemStack(Items.CHEST);
        chest.setHoverName(Component.translatable("modernlife.banker.sale_summary_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        
        net.minecraft.nbt.ListTag loreTag = new net.minecraft.nbt.ListTag();
        for (Component c : lore) {
            loreTag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        chest.getOrCreateTagElement("display").put("Lore", loreTag);

        return chest;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (slotId == 2) {
            player.closeContainer();
        } else if (slotId == 6) {
            this.isConfirmed = true;
            
            if (player instanceof ServerPlayer serverPlayer) {
                long grossPrice = calculateTotal(serverPlayer);

                if (grossPrice <= 0) {
                    for (ItemStack stack : itemsToSell) {
                        serverPlayer.getInventory().placeItemBackInInventory(stack);
                    }
                    serverPlayer.sendSystemMessage(Component.translatable("modernlife.banker.no_valid_items"));
                    serverPlayer.closeContainer();
                    return;
                }

                boolean hasCard = hasPhysicalBankCard(serverPlayer);
                boolean isLightman = EconomyService.isLightmansActive(serverPlayer);
                long finalPayout = grossPrice;

                if (!hasCard) {
                    long taxAmount = isLightman ? Math.round(grossPrice * 0.15) : Math.round((grossPrice * 0.15) / 5.0) * 5;
                    finalPayout = grossPrice - taxAmount;

                    if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                        ModernLifeWorldData worldData = ModernLifeWorldData.get(serverLevel);
                        worldData.addStateTreasuryBalance(taxAmount);
                        worldData.setDirty();
                    }

                    serverPlayer.sendSystemMessage(Component.translatable("modernlife.banker.tax_deducted_msg", EconomyService.formatMoney(serverPlayer, taxAmount)));
                }

                String activeIdentity = EconomyService.getActiveIdentity(serverPlayer);
                if (activeIdentity.isEmpty() || activeIdentity.startsWith("IPTAL_")) {
                    List<String> registered = EconomyService.getRegisteredIdentities(serverPlayer);
                    for (String id : registered) {
                        if (EconomyService.isUsableIdentity(id)) {
                            activeIdentity = id;
                            break;
                        }
                    }
                }

                if (!activeIdentity.isEmpty() && !activeIdentity.startsWith("IPTAL_")) {
                    EconomyService.getOrCreateIdentity(serverPlayer, activeIdentity);
                    boolean success = EconomyService.deposit(serverPlayer, activeIdentity, finalPayout);
                    
                    if (success) {
                        EconomyService.syncToClient(serverPlayer);
                        serverPlayer.sendSystemMessage(Component.translatable("modernlife.banker.sale_success_msg", EconomyService.formatMoney(serverPlayer, finalPayout)));
                        
                        ModNetwork.CHANNEL.sendToPlayer(serverPlayer, 
                            new AddTransactionPacket(activeIdentity, Component.translatable("modernlife.transaction.banker_sale", EconomyService.formatMoney(serverPlayer, finalPayout))));
                    } else {
                        serverPlayer.sendSystemMessage(Component.translatable("modernlife.bank.error_deposit_failed"));
                        for (ItemStack stack : itemsToSell) {
                            serverPlayer.getInventory().placeItemBackInInventory(stack);
                        }
                        serverPlayer.closeContainer();
                        return;
                    }
                } else {
                    serverPlayer.sendSystemMessage(Component.translatable("modernlife.msg.no_bank_account"));
                    for (ItemStack stack : itemsToSell) {
                        serverPlayer.getInventory().placeItemBackInInventory(stack);
                    }
                    serverPlayer.closeContainer();
                    return;
                }

                for (ItemStack stack : itemsToSell) {
                    double basePrice = EconomyRegistry.getItemPrice(stack.getItem());
                    if (basePrice <= 0) {
                        serverPlayer.getInventory().placeItemBackInInventory(stack);
                    }
                }
            }
            player.closeContainer();
        } else {
            super.clicked(slotId, button, clickType, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && !isConfirmed) {
            for (ItemStack stack : itemsToSell) {
                player.getInventory().placeItemBackInInventory(stack);
            }
            player.sendSystemMessage(Component.translatable("modernlife.banker.sale_cancelled_refund"));
        }
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    public static void openConfirmMenu(ServerPlayer player, List<ItemStack> items) {
        MenuRegistry.openMenu(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("modernlife.banker.confirm_title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new SellConfirmMenu(id, inv, items);
            }
        });
    }
}