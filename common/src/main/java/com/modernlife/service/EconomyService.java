package com.modernlife.service;

import com.modernlife.capability.ModCapabilities;
import com.modernlife.capability.PlayerEconomy;
import com.modernlife.data.LicenseType;
import com.modernlife.data.TaxRates;
import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.economy.compat.ClientEconomyState;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.SyncEconomyModePacket;
import com.modernlife.network.packet.SyncPlayerEconomyPacket;
import com.modernlife.registry.ModItems;
import dev.architectury.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EconomyService {
    public static final String TREASURY_ID = "DEVLET_HAZINESI";

    private EconomyService() {}

    public static boolean isLightmansActive(Player player) {
        if (player != null && player.level().isClientSide()) {
            return ClientEconomyState.isLightmansMode;
        }
        if (player != null && player.level() instanceof ServerLevel serverLevel) {
            ModernLifeWorldData worldData = ModernLifeWorldData.get(serverLevel);
            return worldData.getEconomyMode() == ModernLifeWorldData.EconomyMode.LIGHTMANS_CURRENCY
                    && Platform.isModLoaded("lightmanscurrency");
        }
        return false;
    }

    public static Component formatMoney(Player player, long amount) {
        if (isLightmansActive(player)) {
            return Component.literal(String.valueOf(amount));
        }
        return Component.translatable("gui.modernlife.currency_format", String.valueOf(amount));
    }

    public static boolean isTreasury(final String identity) {
        return TREASURY_ID.equalsIgnoreCase(identity);
    }

    public static boolean isValidIdentityForBank(final Player player, final String identity) {
        if (isTreasury(identity)) {
            return WorldStateService.isPresident(player);
        }
        return isUsableIdentity(identity);
    }

    public static boolean hasIdentity(final Player player, final String identity) {
        if (isTreasury(identity)) {
            return WorldStateService.isPresident(player);
        }
        if (!isValidIdentityForBank(player, identity)) return false;
        return getEconomy(player)
                .map(economy -> economy.hasIdentityBalance(identity) || identity.equals(economy.getRealName()))
                .orElse(false);
    }

    public static boolean hasValidBankCard(final Player player, final String identity) {
        if (isTreasury(identity)) {
            return WorldStateService.isPresident(player);
        }
        if (!isValidIdentityForBank(player, identity)) return false;
        
        return getEconomy(player)
                .map(economy -> economy.hasIdentityBalance(identity) || identity.equals(economy.getRealName()))
                .orElse(false);
    }

    public static boolean hasBankAccount(final Player player, final String identity) {
        if (isTreasury(identity)) {
            return WorldStateService.isPresident(player);
        }
        if (!isValidIdentityForBank(player, identity)) return false;
        
        return getEconomy(player)
                .map(economy -> economy.hasIdentityBalance(identity))
                .orElse(false);
    }

    public static long getBalance(final Player player, final String identity) {
        if (isTreasury(identity)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                return ModernLifeWorldData.get(serverLevel).getStateTreasuryBalance();
            }
            return getEconomy(player).map(economy -> economy.getBalance(TREASURY_ID)).orElse(0L);
        }

        if (!isValidIdentityForBank(player, identity)) return 0L;
        getOrCreateIdentity(player, identity);
        migrateLegacyCardBalances(player, identity);
        return getEconomy(player).map(economy -> economy.getBalance(identity)).orElse(0L);
    }

    public static long getBalance(final Player player) {
        return getBalance(player, getActiveIdentity(player));
    }

    public static boolean getOrCreateIdentity(final Player player) {
        final String identity = getActiveIdentity(player);
        if (!isUsableIdentity(identity)) {
            final List<String> fromCards = getIdentitiesFromCards(player);
            if (fromCards.isEmpty()) return false;
            return getOrCreateIdentity(player, fromCards.get(0));
        }
        return getOrCreateIdentity(player, identity);
    }

    public static boolean getOrCreateIdentity(final Player player, final String identity) {
        if (isTreasury(identity)) return true;
        if (!isValidIdentityForBank(player, identity)) return false;
        if (!(player instanceof ServerPlayer)) return false;

        return getEconomy(player).map(economy -> {
            final boolean alreadyRegistered = economy.hasIdentityBalance(identity);

            migrateLegacyCardBalances(player, identity);

            if (!economy.hasIdentityBalance(identity)) {
                economy.setBalance(identity, 0L);
            }

            if (!alreadyRegistered) {
                syncToClient(player);
            }
            return true;
        }).orElse(false);
    }

    public static boolean setBalance(final Player player, final String identity, final long amount) {
        if (isTreasury(identity)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                ModernLifeWorldData data = ModernLifeWorldData.get(serverLevel);
                data.addStateTreasuryBalance(amount - data.getStateTreasuryBalance());
                for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
                    syncToClient(p);
                }
                WorldStateService.syncToAll(serverLevel.getServer());
                return true;
            }
            return false;
        }

        if (!isValidIdentityForBank(player, identity)) return false;
        migrateLegacyCardBalances(player, identity);
        return getEconomy(player).map(economy -> {
            economy.setBalance(identity, amount);
            syncToClient(player);
            return true;
        }).orElse(false);
    }

    public static boolean setBalance(final Player player, final long amount) {
        return setBalance(player, getActiveIdentity(player), amount);
    }

    public static boolean deposit(final Player player, final String identity, final long amount) {
        if (amount <= 0L) return false;

        if (isTreasury(identity)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                ModernLifeWorldData data = ModernLifeWorldData.get(serverLevel);
                data.addStateTreasuryBalance(amount);
                for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
                    syncToClient(p);
                }
                WorldStateService.syncToAll(serverLevel.getServer());
                return true;
            }
            return false;
        }

        boolean isLightman = isLightmansActive(player);
        if (!isLightman && (amount < 5L || amount % 5 != 0)) return false;
        if (!hasValidBankCard(player, identity)) return false;

        migrateLegacyCardBalances(player, identity);
        return getEconomy(player).map(economy -> {
            long currentBalance = economy.getBalance(identity);
            economy.setBalance(identity, currentBalance + amount);
            syncToClient(player);
            return true;
        }).orElse(false);
    }

    public static boolean deposit(final Player player, final long amount) {
        return deposit(player, getActiveIdentity(player), amount);
    }

    public static boolean withdraw(final Player player, final String identity, final long amount) {
        if (amount <= 0L) return false;

        if (isTreasury(identity)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                ModernLifeWorldData data = ModernLifeWorldData.get(serverLevel);
                if (data.getStateTreasuryBalance() >= amount) {
                    data.removeStateTreasuryBalance(amount);
                    for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
                        syncToClient(p);
                    }
                    WorldStateService.syncToAll(serverLevel.getServer());
                    return true;
                } else {
                    if (!player.level().isClientSide()) {
                        player.sendSystemMessage(Component.translatable("gui.modernlife.insufficient_balance").withStyle(ChatFormatting.RED));
                    }
                    return false;
                }
            }
            return false;
        }

        boolean isLightman = isLightmansActive(player);
        if (!isLightman && (amount < 5L || amount % 5 != 0)) return false;
        if (!hasValidBankCard(player, identity)) return false;

        migrateLegacyCardBalances(player, identity);
        return getEconomy(player).map(economy -> {
            long currentBalance = economy.getBalance(identity);
            
            if (currentBalance < amount) {
                if (!player.level().isClientSide()) {
                    player.sendSystemMessage(Component.translatable("gui.modernlife.insufficient_balance").withStyle(ChatFormatting.RED));
                }
                return false;
            }

            economy.setBalance(identity, currentBalance - amount);
            syncToClient(player);
            return true;
        }).orElse(false);
    }

    public static boolean withdraw(final Player player, final long amount) {
        return withdraw(player, getActiveIdentity(player), amount);
    }

    public static String getActiveIdentity(final Player player) {
        List<ItemStack> allEquipped = new ArrayList<>();
        allEquipped.addAll(player.getInventory().items);
        allEquipped.addAll(player.getInventory().offhand);
        allEquipped.addAll(player.getInventory().armor);

        for (ItemStack stack : allEquipped) {
            if (!stack.isEmpty() && stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                if (tag.getBoolean("IptalEdildi") || tag.getBoolean("Cancelled")) continue;

                if (tag.contains("realName") && !tag.contains("CardOwner")) {
                    String kimlikIsmi = tag.getString("realName");
                    if (isUsableIdentity(kimlikIsmi) && hasValidBankCard(player, kimlikIsmi)) {
                        ModCapabilities.getPersistentData(player).putString("AktifKimlikIsmi", kimlikIsmi);
                        return kimlikIsmi;
                    }
                }
            }
        }

        for (ItemStack stack : allEquipped) {
            if (!stack.isEmpty() && stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                if (tag.getBoolean("IptalEdildi") || tag.getBoolean("Cancelled")) continue;

                if (tag.contains("CardOwner")) {
                    String kartSahibi = tag.getString("CardOwner");
                    if (isTreasury(kartSahibi) && WorldStateService.isPresident(player)) {
                        ModCapabilities.getPersistentData(player).putString("AktifKimlikIsmi", kartSahibi);
                        return kartSahibi;
                    }
                    if (!isTreasury(kartSahibi) && isUsableIdentity(kartSahibi) && hasValidBankCard(player, kartSahibi)) {
                        ModCapabilities.getPersistentData(player).putString("AktifKimlikIsmi", kartSahibi);
                        return kartSahibi;
                    }
                }
            }
        }

        ModCapabilities.getPersistentData(player).remove("AktifKimlikIsmi");
        return ""; 
    }

    public static List<String> getRegisteredIdentities(final Player player) {
        List<String> validList = new ArrayList<>();

        getEconomy(player).ifPresent(economy -> {
            for (String id : economy.getIdentityBalances().keySet()) {
                if (!id.equals("__unassigned__") && isValidIdentityForBank(player, id)) {
                    if (!validList.contains(id)) validList.add(id);
                }
            }

            String mainRealName = economy.getRealName();
            if (isValidIdentityForBank(player, mainRealName) && isUsableIdentity(mainRealName)) {
                if (!validList.contains(mainRealName)) validList.add(mainRealName);
            }
        });

        if (WorldStateService.isPresident(player)) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() == ModItems.BANK_CARD.get() && stack.hasTag()) {
                    if (isTreasury(stack.getTag().getString("CardOwner")) && !stack.getTag().getBoolean("Cancelled")) {
                        if (!validList.contains(TREASURY_ID)) validList.add(TREASURY_ID);
                        break;
                    }
                }
            }
        }

        if (validList.isEmpty()) {
            for (String cardIdentity : getIdentitiesFromCards(player)) {
                if (!validList.contains(cardIdentity)) validList.add(cardIdentity);
                getOrCreateIdentity(player, cardIdentity);
            }
        }

        return validList;
    }

    public static List<String> getIdentitiesFromCards(final Player player) {
        List<String> identities = new ArrayList<>();
        List<ItemStack> allEquipped = new ArrayList<>();
        allEquipped.addAll(player.getInventory().items);
        allEquipped.addAll(player.getInventory().offhand);
        allEquipped.addAll(player.getInventory().armor);

        for (ItemStack stack : allEquipped) {
            if (stack.isEmpty() || !stack.hasTag()) continue;

            CompoundTag tag = stack.getTag();
            if (tag.getBoolean("IptalEdildi") || tag.getBoolean("Cancelled")) continue;

            if (tag.contains("realName")) {
                String name = tag.getString("realName");
                if (isUsableIdentity(name) && !isTreasury(name) && !identities.contains(name)) {
                    identities.add(name);
                }
            }
            if (tag.contains("CardOwner")) {
                String owner = tag.getString("CardOwner");
                if (isTreasury(owner) && WorldStateService.isPresident(player)) {
                    if (!identities.contains(owner)) identities.add(owner);
                } else if (isUsableIdentity(owner) && !isTreasury(owner) && !identities.contains(owner)) {
                    identities.add(owner);
                }
            }
        }
        return identities;
    }

    public static boolean cancelIdentity(final Player player, final String identity) {
        if (isTreasury(identity)) return false;
        if (!(player instanceof ServerPlayer) || !isUsableIdentity(identity)) return false;

        getEconomy(player).ifPresent(economy -> {
            economy.setBalance(identity, 0L); 
            economy.removeIdentity(identity); 
            if (identity.equals(economy.getRealName())) {
                economy.setRealName(""); 
                economy.setBirthYear(0); 
            }
        });

        CompoundTag persistentData = ModCapabilities.getPersistentData(player);
        persistentData.remove("AktifKimlikIsmi");
        persistentData.remove("realName");
        persistentData.remove("HasFirstBankCard_" + identity);

        player.setCustomName(null);
        player.setCustomNameVisible(false);

        ruinIdentityStacks(player, identity);

        syncToClient(player);
        return true;
    }

    private static void ruinIdentityStacks(Player player, String identity) {
        List<ItemStack> allSlots = new ArrayList<>();
        allSlots.addAll(player.getInventory().items);
        allSlots.addAll(player.getInventory().offhand);
        allSlots.addAll(player.getInventory().armor);

        for (int i = 0; i < player.getEnderChestInventory().getContainerSize(); i++) {
            allSlots.add(player.getEnderChestInventory().getItem(i));
        }

        for (ItemStack stack : allSlots) {
            markIfMatches(stack, identity);
        }
    }

    private static void markIfMatches(ItemStack stack, String identity) {
        if (!stack.isEmpty() && stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag.getBoolean("IptalEdildi") || tag.getBoolean("Cancelled")) return;

            if (identity.equals(tag.getString("realName")) || identity.equals(tag.getString("CardOwner"))) {
                tag.putBoolean("IptalEdildi", true);
                tag.putBoolean("Cancelled", true);
                tag.putString("realName", "IPTAL_" + identity);
                tag.putString("CardOwner", "IPTAL_" + identity);
                
                Component cancelledName = Component.translatable("modernlife.document.cancelled_prefix")
                        .append(Component.literal(" " + identity))
                        .withStyle(ChatFormatting.RED, ChatFormatting.STRIKETHROUGH);
                stack.setHoverName(cancelledName);
            }
        }
    }

    public static boolean hasPhysicalIdentity(final Player player, final String identity) {
        if (!isUsableIdentity(identity)) return false;
        List<ItemStack> allEquipped = new ArrayList<>();
        allEquipped.addAll(player.getInventory().items);
        allEquipped.addAll(player.getInventory().offhand);
        allEquipped.addAll(player.getInventory().armor);

        for (ItemStack stack : allEquipped) {
            if (matchesIdentity(stack, identity)) return true;
        }
        return false;
    }

    private static void matchesIdentityInternal(final ItemStack stack, final String identity) {
    }

    private static boolean matchesIdentity(final ItemStack stack, final String identity) {
        if (!stack.isEmpty() && stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag.getBoolean("IptalEdildi") || tag.getBoolean("Cancelled")) {
                return false;
            }
            return identity.equals(tag.getString("realName")) || identity.equals(tag.getString("CardOwner"));
        }
        return false;
    }

    public static void migrateLegacyCardBalances(final Player player, final String identity) {
        if (isTreasury(identity)) return;
        if (!(player instanceof ServerPlayer) || !isValidIdentityForBank(player, identity)) return;
        getEconomy(player).ifPresent(economy -> {
            long legacyTotal = 0L;
            boolean hadLegacyCardBalance = false;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() == ModItems.BANK_CARD.get() && stack.hasTag()
                        && identity.equals(stack.getTag().getString("CardOwner"))) {
                    if (stack.getTag().contains("BankBalance")) {
                        hadLegacyCardBalance = true;
                        legacyTotal = safeAdd(legacyTotal, Math.max(0L, stack.getTag().getLong("BankBalance")));
                    }
                    stack.getTag().remove("BankBalance");
                }
            }
            if (!economy.hasIdentityBalance(identity)) {
                economy.setBalance(identity, hadLegacyCardBalance ? legacyTotal : economy.takeLegacyBalance(identity));
            }
        });
    }

    public record TaxDueSummary(long unpaidDays, long livingTax, long vehicleTax, long mountTax,
                                long baseDailyTax, long penaltyAmount, long totalToPay) {
    }

    public static TaxDueSummary calculateTaxDueSummary(final ServerPlayer player) {
        final PlayerEconomy economy = ModCapabilities.get(player);
        final long unpaidDays = economy.getTaxDue();
        if (unpaidDays <= 0L) return new TaxDueSummary(0L, 0L, 0L, 0L, 0L, 0L, 0L);

        final ModernLifeWorldData worldData = WorldStateService.getWorldData(player.serverLevel());
        final TaxRates taxRates = worldData.getTaxRates();
        final double rawPenaltyRate = worldData.getPenaltyRate();

        final double penaltyFraction = (rawPenaltyRate > 2.0) ? (rawPenaltyRate / 100.0) : rawPenaltyRate;

        final long livingTax = taxRates.livingDaily();
        final long vehicleTax = DocumentService.hasValidLicense(player, LicenseType.VEHICLE) ? taxRates.vehicleDaily() : 0L;
        final long mountTax = DocumentService.hasValidLicense(player, LicenseType.MOUNT) ? taxRates.mountDaily() : 0L;
        final long baseDailyTax = livingTax + vehicleTax + mountTax;

        long totalToPay = 0L;
        long penaltyAmount = 0L;

        for (long i = 1; i <= unpaidDays; i++) {
            if (i == unpaidDays) {
                totalToPay += baseDailyTax;
            } else {
                final long penalizedDayTax = roundUpTo5((long) Math.ceil(baseDailyTax * (1.0 + penaltyFraction)));
                totalToPay += penalizedDayTax;
                penaltyAmount += (penalizedDayTax - baseDailyTax);
            }
        }

        totalToPay = roundUpTo5(totalToPay);
        return new TaxDueSummary(unpaidDays, livingTax, vehicleTax, mountTax, baseDailyTax, penaltyAmount, totalToPay);
    }

    private static long roundUpTo5(long value) {
        if (value <= 0L) return 0L;
        long rem = value % 5L;
        return rem == 0L ? value : value + (5L - rem);
    }

    public static void syncToClient(final Player player) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return;
        final Map<String, Long> balances = getEconomy(player)
                .map(economy -> new HashMap<>(economy.getIdentityBalances()))
                .orElseGet(HashMap::new);

        if (WorldStateService.isPresident(serverPlayer)) {
            balances.put(TREASURY_ID, ModernLifeWorldData.get(serverPlayer.serverLevel()).getStateTreasuryBalance());
        }

        final long taxDueDays = getEconomy(player)
                .map(PlayerEconomy::getTaxDue)
                .orElse(0L);
        final long taxDueAmount = calculateTaxDueSummary(serverPlayer).totalToPay();
        ModNetwork.CHANNEL.sendToPlayer(serverPlayer, new SyncPlayerEconomyPacket(balances, taxDueDays, taxDueAmount));
        ModNetwork.CHANNEL.sendToPlayer(serverPlayer, new SyncEconomyModePacket(isLightmansActive(player)));
    }

    private static Optional<PlayerEconomy> getEconomy(final Player player) {
        return ModCapabilities.getOptional(player);
    }

    public static boolean isUsableIdentity(final String identity) {
        return identity != null && !identity.isBlank() && !identity.startsWith("IPTAL_");
    }

    private static long safeAdd(final long left, final long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}