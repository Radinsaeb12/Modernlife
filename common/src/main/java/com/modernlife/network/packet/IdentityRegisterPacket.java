package com.modernlife.network.packet;

import com.modernlife.capability.ModCapabilities;
import com.modernlife.capability.PlayerEconomy;
import com.modernlife.service.EconomyService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class IdentityRegisterPacket {
    private final String name;
    private final int birthYear;

    public IdentityRegisterPacket(String name, int birthYear) {
        this.name = name;
        this.birthYear = birthYear;
    }

    public IdentityRegisterPacket(FriendlyByteBuf buf) {
        this.name = buf.readUtf();
        this.birthYear = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.name);
        buf.writeInt(this.birthYear);
    }

    public void handle(Supplier<NetworkManager.PacketContext> supplier) {
        NetworkManager.PacketContext ctx = supplier.get();
        ctx.queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.getPlayer();
            if (player != null) {
                PlayerEconomy economy = ModCapabilities.get(player);
                economy.setRealName(this.name);
                economy.setBirthYear(this.birthYear);
                if (!economy.hasIdentityBalance(this.name)) {
                    economy.setBalance(this.name, 0L);
                }
                ModCapabilities.save(player);

                ModCapabilities.getPersistentData(player).putString("AktifKimlikIsmi", this.name);

                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.hasTag() || !mainHand.isEmpty()) {
                    mainHand.getOrCreateTag().putString("realName", this.name);
                    mainHand.getOrCreateTag().putInt("birthYear", this.birthYear);
                }

                EconomyService.syncToClient(player);
                player.sendSystemMessage(Component.translatable("modernlife.document.registered_notice", this.name));
            }
        });
    }
}