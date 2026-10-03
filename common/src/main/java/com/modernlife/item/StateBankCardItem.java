package com.modernlife.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StateBankCardItem extends Item {
    public StateBankCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        // Kart envantere girdiği an kendini otomatik olarak DEVLET_HAZINESI hesabına bağlar
        if (!stack.hasTag() || !stack.getTag().contains("realName")) {
            stack.getOrCreateTag().putString("realName", "DEVLET_HAZINESI");
            stack.getOrCreateTag().putString("CardOwner", "DEVLET_HAZINESI");
        }
        super.inventoryTick(stack, level, entity, slot, isSelected);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("modernlife.item.state_treasury_card_tooltip_line1"));
        tooltip.add(Component.translatable("modernlife.item.state_treasury_card_tooltip_line2"));
        tooltip.add(Component.translatable("modernlife.item.state_treasury_card_tooltip_line3"));
    }
}
