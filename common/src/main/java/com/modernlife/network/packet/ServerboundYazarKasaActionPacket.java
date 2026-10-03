package com.modernlife.network.packet;

import com.modernlife.block.entity.YazarKasaBlockEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class ServerboundYazarKasaActionPacket {
    private final BlockPos pos;
    private final int actionType;
    private final int miktar;

    public ServerboundYazarKasaActionPacket(BlockPos pos, int actionType, int miktar) {
        this.pos = pos;
        this.actionType = actionType;
        this.miktar = miktar;
    }

    public ServerboundYazarKasaActionPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.actionType = buf.readInt();
        this.miktar = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(actionType);
        buf.writeInt(miktar);
    }

    public void handle(Supplier<NetworkManager.PacketContext> supplier) {
        NetworkManager.PacketContext context = supplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof YazarKasaBlockEntity yazarKasa) {
                    
                    if (actionType == 1) {
                        yazarKasa.setOwnerUUID(player.getUUID());
                    } 
                    else if (actionType == 2) {
                        yazarKasa.odemeYap(yazarKasa.getKalanTutar());
                    }
                    else if (actionType == 3) {
                        yazarKasa.odemeYap(miktar);
                        yazarKasa.addNakit(miktar);
                    }
                    
                    yazarKasa.setChanged();
                    player.level().sendBlockUpdated(pos, yazarKasa.getBlockState(), yazarKasa.getBlockState(), 3);
                }
            }
        });
    }
}