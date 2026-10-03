package com.modernlife.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.item.ItemStack;

public class GuideBookClientHelper {

    @Environment(EnvType.CLIENT)
    public static void openBookScreen(ItemStack stack) {
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(stack)));
    }
}