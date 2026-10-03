package com.modernlife.item;

import com.modernlife.capability.ModCapabilities;
import com.modernlife.capability.PlayerEconomy;
import com.modernlife.gui.IdentityRegisterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DocumentItem extends Item {
    private final String documentType;

    public DocumentItem(Properties properties) {
        super(properties);
        this.documentType = "identity";
    }

    public DocumentItem(Properties properties, Object licenseType) {
        super(properties);
        this.documentType = licenseType.toString().toLowerCase();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        CompoundTag tag = itemStack.getOrCreateTag();

        if (this.documentType.equals("identity")) {
            CompoundTag persistentData = ModCapabilities.getPersistentData(player);
            if (tag.getBoolean("Cancelled") || (tag.contains("realName") && persistentData.getBoolean("IptalEdilmisKimlik_" + tag.getString("realName")))) {
                if (!level.isClientSide) {
                    player.sendSystemMessage(Component.translatable("modernlife.document.cancelled"));
                }
                return InteractionResultHolder.fail(itemStack);
            }

            if (!tag.contains("realName")) {
                if (level.isClientSide) {
                    Minecraft.getInstance().setScreen(new IdentityRegisterScreen());
                }
            } 
            else {
                if (!level.isClientSide) {
                    String karttakiIsim = tag.getString("realName");
                    persistentData.putString("AktifKimlikIsmi", karttakiIsim);
                    player.sendSystemMessage(Component.translatable("modernlife.document.registered_notice", karttakiIsim));
                }
            }
        } else { 
            if (!level.isClientSide && !tag.contains("realName")) { 
                PlayerEconomy economy = ModCapabilities.get(player);
                if (economy.getRealName().isEmpty()) { 
                    player.sendSystemMessage(Component.translatable("modernlife.msg.need_identity"));
                } else { 
                    tag.putString("realName", economy.getRealName());
                    tag.putInt("birthYear", economy.getBirthYear());
                    player.sendSystemMessage(Component.translatable("modernlife.msg.document_registered"));
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.getBoolean("Cancelled")) {
                tooltip.add(Component.translatable("modernlife.document.tooltip.cancelled"));
            } else if (tag.contains("realName")) {
                tooltip.add(Component.translatable("modernlife.document.tooltip.owner", tag.getString("realName")));
                tooltip.add(Component.translatable("modernlife.document.tooltip.birth_year", tag.getInt("birthYear")));
            } else {
                tooltip.add(Component.translatable("modernlife.tooltip.empty_document"));
            }
        } else {
            tooltip.add(Component.translatable("modernlife.tooltip.empty_document"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}