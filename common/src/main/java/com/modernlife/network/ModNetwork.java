package com.modernlife.network;

import com.modernlife.ModernLifeMod;
import com.modernlife.network.packet.*;
import dev.architectury.networking.NetworkChannel;
import net.minecraft.resources.ResourceLocation;

public final class ModNetwork {
    public static final NetworkChannel CHANNEL = NetworkChannel.create(
            new ResourceLocation(ModernLifeMod.MODID, "main")
    );

    private ModNetwork() {}

    public static void register() {
        CHANNEL.register(SyncPlayerEconomyPacket.class, SyncPlayerEconomyPacket::encode, SyncPlayerEconomyPacket::decode, SyncPlayerEconomyPacket::handle);
        CHANNEL.register(SyncWorldStatePacket.class, SyncWorldStatePacket::encode, SyncWorldStatePacket::decode, SyncWorldStatePacket::handle);
        CHANNEL.register(SetTaxRatesPacket.class, SetTaxRatesPacket::encode, SetTaxRatesPacket::decode, SetTaxRatesPacket::handle);
        CHANNEL.register(TransferMoneyPacket.class, TransferMoneyPacket::encode, TransferMoneyPacket::decode, TransferMoneyPacket::handle);
        CHANNEL.register(PayTaxesPacket.class, PayTaxesPacket::encode, PayTaxesPacket::decode, PayTaxesPacket::handle);
        CHANNEL.register(IdentityRegisterPacket.class, IdentityRegisterPacket::toBytes, IdentityRegisterPacket::new, IdentityRegisterPacket::handle);
        CHANNEL.register(AtmActionPacket.class, AtmActionPacket::toBytes, AtmActionPacket::new, AtmActionPacket::handle);
        CHANNEL.register(AddTransactionPacket.class, AddTransactionPacket::encode, AddTransactionPacket::decode, AddTransactionPacket::handle);
        CHANNEL.register(YazarKasaActionPacket.class, YazarKasaActionPacket::toBytes, YazarKasaActionPacket::new, YazarKasaActionPacket::handle);
        CHANNEL.register(OpenElectionMenuPacket.class, OpenElectionMenuPacket::encode, OpenElectionMenuPacket::decode, OpenElectionMenuPacket::handle);
        CHANNEL.register(ElectionActionPacket.class, ElectionActionPacket::encode, ElectionActionPacket::decode, ElectionActionPacket::handle);
        CHANNEL.register(DeleteIdentityPacket.class, DeleteIdentityPacket::toBytes, DeleteIdentityPacket::new, DeleteIdentityPacket::handle);
        CHANNEL.register(C2SPhoneCallPacket.class, C2SPhoneCallPacket::encode, C2SPhoneCallPacket::new, C2SPhoneCallPacket::handle);
        CHANNEL.register(S2CPhoneCallStatePacket.class, S2CPhoneCallStatePacket::encode, S2CPhoneCallStatePacket::new, S2CPhoneCallStatePacket::handle);
        CHANNEL.register(OpenDifficultyMenuPacket.class, OpenDifficultyMenuPacket::toBytes, OpenDifficultyMenuPacket::new, OpenDifficultyMenuPacket::handle);
        CHANNEL.register(SetDifficultyPacket.class, SetDifficultyPacket::toBytes, SetDifficultyPacket::new, SetDifficultyPacket::handle);
        CHANNEL.register(EnsureIdentityPacket.class, EnsureIdentityPacket::toBytes, EnsureIdentityPacket::new, EnsureIdentityPacket::handle);
        CHANNEL.register(OpenEconomySelectPacket.class, OpenEconomySelectPacket::encode, OpenEconomySelectPacket::decode, OpenEconomySelectPacket::handle);
        CHANNEL.register(SetEconomyModePacket.class, SetEconomyModePacket::encode, SetEconomyModePacket::decode, SetEconomyModePacket::handle);
        CHANNEL.register(SyncEconomyModePacket.class, SyncEconomyModePacket::encode, SyncEconomyModePacket::decode, SyncEconomyModePacket::handle);
        CHANNEL.register(SyncBalancePacket.class, SyncBalancePacket::encode, SyncBalancePacket::decode, SyncBalancePacket::handle);
        
        // Komutsuz ve hilesiz çalışan gizli ışınlanma paketi
        CHANNEL.register(C2STeleportBankPacket.class, C2STeleportBankPacket::encode, C2STeleportBankPacket::decode, C2STeleportBankPacket::handle);
    }
}