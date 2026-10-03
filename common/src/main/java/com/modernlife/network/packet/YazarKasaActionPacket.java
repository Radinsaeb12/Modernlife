package com.modernlife.network.packet;

import com.modernlife.block.entity.YazarKasaBlockEntity;
import com.modernlife.economy.compat.EconomyInventoryHelper;
import com.modernlife.service.EconomyService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class YazarKasaActionPacket {
    private final BlockPos pos;
    private final int actionType; 
    private final int miktar;

    public YazarKasaActionPacket(BlockPos pos, int actionType, int miktar) {
        this.pos = pos;
        this.actionType = actionType;
        this.miktar = miktar;
    }

    public YazarKasaActionPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.actionType = buf.readInt();
        this.miktar = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.actionType);
        buf.writeInt(this.miktar);
    }

    public void handle(Supplier<NetworkManager.PacketContext> supplier) {
        NetworkManager.PacketContext context = supplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof YazarKasaBlockEntity yazarKasa) {
                    
                    boolean isLightman = EconomyService.isLightmansActive(player);

                    if (actionType == 1) {
                        if (!isLightman && (miktar < 5 || miktar % 5 != 0)) {
                            player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.error_multiple_of_5"));
                        } else if (miktar <= 0) {
                            player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.invalid_price"));
                        } else {
                            yazarKasa.setMevcutSiparisTutari(miktar);
                            Component formatliMiktar = EconomyService.formatMoney(player, miktar);
                            player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.requested_amount", formatliMiktar));
                        }
                    } 
                    else if (actionType == 2) {
                        yazarKasa.setOdemeTipi(1);
                        player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.cash_selected"));
                    } 
                    else if (actionType == 3) {
                        yazarKasa.setOdemeTipi(2);
                        player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.card_selected"));
                    }
                    else if (actionType == 4) {
                        if (yazarKasa.getOwnerUUID() != null && yazarKasa.getOwnerUUID().equals(player.getUUID())) {
                            int kasadakiNakit = yazarKasa.getNakitHavuzu(); 
                            if (kasadakiNakit > 0) {
                                EconomyInventoryHelper.giveCash(player, kasadakiNakit);
                                yazarKasa.setNakitHavuzu(0); 
                                Component formatliNakit = EconomyService.formatMoney(player, kasadakiNakit);
                                player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.cash_withdrawn", formatliNakit));
                            } else {
                                player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.no_cash_to_withdraw"));
                            }
                        } else {
                            player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.not_owner"));
                        }
                    }
                    
                    player.level().sendBlockUpdated(pos, yazarKasa.getBlockState(), yazarKasa.getBlockState(), 3);
                }
            }
        });
    }
}