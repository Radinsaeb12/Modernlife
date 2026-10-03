package com.modernlife.entity;

import com.modernlife.item.BankCardItem;
import com.modernlife.menu.SellInputMenu;
import com.modernlife.service.EconomyService;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BankerEntity extends PathfinderMob {

    public BankerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.translatable("entity.modernlife.banker").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public void kill() {
        this.remove(Entity.RemovalReason.KILLED);
        this.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_DIE);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        String msgId = source.getMsgId();
        if (msgId.equals("genericKill") || msgId.equals("outOfWorld")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.modernlife.banker").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD);
    }

    private boolean hasValidPhysicalBankCard(Player player) {
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

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            
            boolean hasPhysicalBankCard = hasValidPhysicalBankCard(serverPlayer);

            String activeIdentity = EconomyService.getActiveIdentity(serverPlayer);
            boolean hasIdentity = activeIdentity != null && !activeIdentity.isEmpty() && !activeIdentity.startsWith("IPTAL_");

            if (!hasPhysicalBankCard && !hasIdentity) {
                serverPlayer.sendSystemMessage(Component.translatable("message.modernlife.banker.no_identity_or_card"));
                return InteractionResult.SUCCESS;
            }

            if (hasPhysicalBankCard) {
                serverPlayer.sendSystemMessage(Component.translatable("message.modernlife.banker.welcome_with_card"));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("message.modernlife.banker.welcome_no_card"));
            }

            MenuRegistry.openMenu(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("menu.modernlife.banker_sell");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p) {
                    return new SellInputMenu(containerId, playerInventory);
                }
            });

            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
}