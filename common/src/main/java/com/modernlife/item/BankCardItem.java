package com.modernlife.item;

import com.modernlife.service.EconomyService;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BankCardItem extends Item {
    public BankCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!stack.hasTag()) {
            tooltip.add(Component.translatable("tooltip.modernlife.bank_card.unregistered").withStyle(ChatFormatting.GRAY));
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        if (tag.getBoolean("Cancelled") || tag.getBoolean("IptalEdildi")) {
            tooltip.add(Component.translatable("tooltip.modernlife.bank_card.cancelled").withStyle(ChatFormatting.RED, ChatFormatting.STRIKETHROUGH));
            return;
        }

        String owner = tag.contains("CardOwner") ? tag.getString("CardOwner") : (tag.contains("realName") ? tag.getString("realName") : "");
        String cardId = tag.getString("CardID");

        // HAZİNE KARTI DİL KONTROLÜ
        if (EconomyService.isTreasury(owner) || EconomyService.TREASURY_ID.equals(owner)) {
            // Sahibini yerelleştir: Türkçe -> Devlet Hazinesi, İngilizce -> State Treasury
            tooltip.add(Component.translatable("tooltip.modernlife.bank_card.owner", 
                    Component.translatable("modernlife.treasury.title")).withStyle(ChatFormatting.GOLD));

            // Card ID hazine öneki: "HAZI-XXXX" veya "HAZINE-XXXX" kısmını alıp dinamik önekle gösterir
            String suffix = cardId.contains("-") ? cardId.substring(cardId.indexOf("-") + 1) : cardId;
            Component localizedId = Component.translatable("modernlife.treasury.card_id_format", suffix);
            
            tooltip.add(Component.translatable("tooltip.modernlife.bank_card.id", localizedId).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("tooltip.modernlife.bank_card.access_notice").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        // NORMAL KARTLAR
        if (!owner.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.modernlife.bank_card.owner", owner).withStyle(ChatFormatting.AQUA));
        }
        if (!cardId.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.modernlife.bank_card.id", cardId).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.modernlife.bank_card.access_notice").withStyle(ChatFormatting.DARK_GRAY));
    }
}