package com.modernlife.menu;

import com.modernlife.block.entity.YazarKasaBlockEntity;
import com.modernlife.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class YazarKasaMenu extends AbstractContainerMenu {
    private final YazarKasaBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public YazarKasaMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public YazarKasaMenu(int containerId, Inventory playerInventory, BlockEntity entity) {
        super(ModMenus.YAZAR_KASA.get(), containerId); 
        this.blockEntity = (YazarKasaBlockEntity) entity;
        this.access = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());
    }

    public YazarKasaBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }
}