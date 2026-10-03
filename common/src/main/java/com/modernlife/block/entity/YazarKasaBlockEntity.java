package com.modernlife.block.entity;

import com.modernlife.menu.YazarKasaMenu;
import com.modernlife.registry.ModBlockEntities;
import com.modernlife.service.EconomyService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class YazarKasaBlockEntity extends BlockEntity implements MenuProvider {
    private UUID ownerUUID = null;
    private int totalCiro = 0;
    private int mevcutSiparis = 0;
    private int kalanTutar = 0;
    private int nakitHavuzu = 0;
    private int odemeTipi = 0; 

    public YazarKasaBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.YAZAR_KASA.get(), pPos, pBlockState);
    }

    public void guncelleVeKaydet() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Ödeme yapacak müşterinin veya dükkan sahibinin kimliğinin geçerli ve iptal edilmemiş olup olmadığını doğrular.
     */
    public boolean isIdentityValidForPayment(Player player, String identity) {
        return EconomyService.isValidIdentityForBank(player, identity) 
                && EconomyService.hasBankAccount(player, identity);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public UUID getOwnerUUID() { return this.ownerUUID; }
    public void setOwnerUUID(UUID owner) { this.ownerUUID = owner; guncelleVeKaydet(); }

    public int getOdemeTipi() { return this.odemeTipi; }
    public void setOdemeTipi(int tip) { this.odemeTipi = tip; guncelleVeKaydet(); }

    public int getTotalCiro() { return this.totalCiro; }
    public void addCiro(int miktar) { this.totalCiro += miktar; guncelleVeKaydet(); }

    public int getMevcutSiparisTutari() { return this.mevcutSiparis; }
    public void setMevcutSiparisTutari(int miktar) { 
        this.mevcutSiparis = miktar; 
        this.kalanTutar = miktar; 
        this.odemeTipi = 0; 
        guncelleVeKaydet(); 
    }

    public int getKalanTutar() { return this.kalanTutar; }

    public void odemeYap(int miktar) { 
        this.kalanTutar = Math.max(0, this.kalanTutar - miktar); 
        
        if (this.kalanTutar == 0) {
            this.odemeTipi = 0; 
        }
        
        guncelleVeKaydet(); 
    }

    public int getNakitHavuzu() { return this.nakitHavuzu; }
    public void addNakit(int miktar) { this.nakitHavuzu += miktar; guncelleVeKaydet(); }
    
    public void setNakitHavuzu(int miktar) { this.nakitHavuzu = miktar; guncelleVeKaydet(); }

    public int getKomisyonOrani() {
        if (this.totalCiro >= 100000) return 5;
        if (this.totalCiro >= 50000) return 7;
        return 10;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("modernlife.yazar_kasa.display_name");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new YazarKasaMenu(pContainerId, pPlayerInventory, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        this.totalCiro = tag.getInt("TotalCiro");
        this.mevcutSiparis = tag.getInt("MevcutSiparis");
        this.kalanTutar = tag.getInt("KalanTutar");
        this.nakitHavuzu = tag.getInt("NakitHavuzu");
        this.odemeTipi = tag.getInt("OdemeTipi");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putInt("TotalCiro", this.totalCiro);
        tag.putInt("MevcutSiparis", this.mevcutSiparis);
        tag.putInt("KalanTutar", this.kalanTutar);
        tag.putInt("NakitHavuzu", this.nakitHavuzu);
        tag.putInt("OdemeTipi", this.odemeTipi);
    }
}