package com.modernlife.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class SellInputMenu extends AbstractContainerMenu {
    private final Container inputContainer = new SimpleContainer(27); // 3 Satırlık Satış Sandığı
    private final Player player;

    public SellInputMenu(int containerId, Inventory playerInventory) {
        super(MenuType.GENERIC_9x3, containerId);
        this.player = playerInventory.player;

        // Sandık Slotları (27 Slot)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inputContainer, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Oyuncu Envanteri
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // OYUNCU ESC'YE BASTIĞINDA / EKRANI KAPATTIĞINDA ÇALIŞIR
    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            List<ItemStack> itemsToSell = new ArrayList<>();
            
            for (int i = 0; i < inputContainer.getContainerSize(); i++) {
                ItemStack stack = inputContainer.getItem(i);
                if (!stack.isEmpty()) {
                    itemsToSell.add(stack.copy());
                }
            }

            // Sandık boşsa işlem yapma
            if (itemsToSell.isEmpty()) {
                return;
            }

            // Sandıkta eşya varsa Otomatik Olarak 2. Aşama (ONAY EKRANI) Açılır!
            serverPlayer.getServer().execute(() -> {
                SellConfirmMenu.openConfirmMenu(serverPlayer, itemsToSell);
            });
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 27) {
                if (!this.moveItemStackTo(itemstack1, 27, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}