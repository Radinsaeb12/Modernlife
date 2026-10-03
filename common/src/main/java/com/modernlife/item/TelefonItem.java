package com.modernlife.item;

import com.modernlife.client.screen.PhoneScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TelefonItem extends Item {
    public TelefonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Sağ tıklama sadece istemci (Client) tarafında ekranı açmalıdır
        if (level.isClientSide()) {
            openPhoneScreen();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    // Ekranı açan ayrı bir metod (Server/Client çakışmalarını önlemek için en güvenli yoldur)
    private void openPhoneScreen() {
        Minecraft.getInstance().setScreen(new PhoneScreen());
    }
}