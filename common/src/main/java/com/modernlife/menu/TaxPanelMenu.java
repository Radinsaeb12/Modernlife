package com.modernlife.menu;

import com.modernlife.data.TaxRates;
import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.registry.ModMenus;
import com.modernlife.service.WorldStateService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class TaxPanelMenu extends AbstractContainerMenu {
    private final long initialVehicleTax;
    private final long initialMountTax;
    private final long initialLivingTax;
    private final double initialPenaltyRate;
    private final long initialUnlicensedPenalty;
    private final long initialUnlicensedMountPenalty; // long

    public TaxPanelMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, 0L, 0L, 0L, 1.0, 100L, 100L);
    }

    public TaxPanelMenu(final int containerId, final Inventory playerInventory, final FriendlyByteBuf buffer) {
        this(
                containerId,
                playerInventory,
                buffer.readLong(),
                buffer.readLong(),
                buffer.readLong(),
                buffer.readDouble(),
                buffer.readLong(),
                buffer.readLong() // long
        );
    }

    public TaxPanelMenu(
            final int containerId,
            final Inventory playerInventory,
            final long vehicleTax,
            final long mountTax,
            final long livingTax,
            final double penaltyRate,
            final long unlicensedPenalty,
            final long unlicensedMountPenalty
    ) {
        super(ModMenus.TAX_PANEL.get(), containerId);
        this.initialVehicleTax = vehicleTax;
        this.initialMountTax = mountTax;
        this.initialLivingTax = livingTax;
        this.initialPenaltyRate = penaltyRate;
        this.initialUnlicensedPenalty = unlicensedPenalty;
        this.initialUnlicensedMountPenalty = unlicensedMountPenalty;
    }

    public static void writeOpeningData(final FriendlyByteBuf buffer, final ServerPlayer player) {
        final ModernLifeWorldData worldData = WorldStateService.getWorldData(player.serverLevel());
        final TaxRates taxRates = worldData.getTaxRates();
        
        // DÜZELTME: Ceza oranı kesir olarak saklanır (0.10 = %10). Üst sınır eskiden 200.0 idi;
        // bu da panelde %20000'e kadar değer gösterebiliyordu. Sınır 2.0 (= %200) yapıldı.
        // Eski kayıtlardaki yüzdesel değerler (örn. 10) de bu sayede doğru yüzdeye (10) yansır.
        double penalty = worldData.getPenaltyRate();
        if (penalty > 2.0) penalty = 2.0;

        buffer.writeLong(taxRates.vehicleDaily());
        buffer.writeLong(taxRates.mountDaily());
        buffer.writeLong(taxRates.livingDaily());
        buffer.writeDouble(penalty);
        buffer.writeLong(worldData.getUnlicensedPenalty());
        buffer.writeLong(worldData.getUnlicensedMountPenalty()); // long
    }

    public long getInitialVehicleTax() { return initialVehicleTax; }
    public long getInitialMountTax() { return initialMountTax; }
    public long getInitialLivingTax() { return initialLivingTax; }
    public double getInitialPenaltyRate() { return initialPenaltyRate; }
    public long getInitialUnlicensedPenalty() { return initialUnlicensedPenalty; }
    public long getInitialUnlicensedMountPenalty() { return initialUnlicensedMountPenalty; }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        return WorldStateService.isPresident(player);
    }
}
