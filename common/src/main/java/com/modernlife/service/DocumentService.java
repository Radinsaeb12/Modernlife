package com.modernlife.service;

import com.modernlife.data.DocumentData;
import com.modernlife.data.LicenseType;
import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DocumentService {
    private DocumentService() {
    }

    public static ItemStack issueIdentityCard(final ServerPlayer player) {
        final long issueDay = getCurrentWorldDay(player);
        final ItemStack stack = new ItemStack(ModItems.IDENTITY_CARD.get());
        DocumentData.write(stack, new DocumentData(
                player.getUUID(),
                player.getName().getString(),
                issueDay,
                null
        ));
        return stack;
    }

    public static ItemStack issueLicense(final ServerPlayer player, final LicenseType licenseType) {
        final long issueDay = getCurrentWorldDay(player);
        final Item item = licenseType == LicenseType.VEHICLE
                ? ModItems.VEHICLE_LICENSE.get()
                : ModItems.MOUNT_LICENSE.get();
        final ItemStack stack = new ItemStack(item);
        DocumentData.write(stack, new DocumentData(
                player.getUUID(),
                player.getName().getString(),
                issueDay,
                licenseType
        ));
        return stack;
    }

    public static boolean hasValidLicense(final Player player, final LicenseType licenseType) {
        final Item expectedItem = licenseType == LicenseType.VEHICLE
                ? ModItems.VEHICLE_LICENSE.get()
                : ModItems.MOUNT_LICENSE.get();

        for (final ItemStack stack : player.getInventory().items) {
            if (isValidDocument(stack, player, expectedItem, licenseType)) {
                return true;
            }
        }
        for (final ItemStack stack : player.getInventory().offhand) {
            if (isValidDocument(stack, player, expectedItem, licenseType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidDocument(
            final ItemStack stack,
            final Player player,
            final Item expectedItem,
            final LicenseType licenseType
    ) {
        if (stack.isEmpty() || !stack.is(expectedItem)) {
            return false;
        }

        // 1. DÜZEY: DocumentData NBT Kontrolü (Sistem tarafından resmen verilmiş belgelere bakar)
        boolean validByData = DocumentData.read(stack)
                .filter(data -> data.licenseType() == licenseType)
                .filter(data -> data.belongsTo(player.getUUID()))
                .isPresent();

        if (validByData) {
            return true;
        }

        // 2. DÜZEY: DocumentItem Sağ Tık Tescil Kontrolü (Oyuncunun elde doldurduğu belgelere bakar)
        CompoundTag tag = stack.getTag();
        if (tag != null && !tag.getBoolean("Cancelled") && tag.contains("realName")) {
            String activeIdentity = EconomyService.getActiveIdentity(player);
            String cardOwner = tag.getString("realName");

            // Karttaki isim aktif kimlikle VEYA Minecraft kullanıcı adıyla eşleşiyorsa geçerlidir
            return cardOwner.equalsIgnoreCase(activeIdentity) || cardOwner.equalsIgnoreCase(player.getName().getString());
        }

        return false;
    }

    public static boolean isValidIdentityCard(final ItemStack stack, final Player player) {
        if (stack.isEmpty() || !stack.is(ModItems.IDENTITY_CARD.get())) {
            return false;
        }

        // DocumentData kontrolü
        boolean validByData = DocumentData.read(stack)
                .filter(data -> data.belongsTo(player.getUUID()))
                .isPresent();

        if (validByData) {
            return true;
        }

        // NBT Tescil kontrolü
        CompoundTag tag = stack.getTag();
        if (tag != null && !tag.getBoolean("Cancelled") && tag.contains("realName")) {
            String activeIdentity = EconomyService.getActiveIdentity(player);
            String cardOwner = tag.getString("realName");
            return cardOwner.equalsIgnoreCase(activeIdentity) || cardOwner.equalsIgnoreCase(player.getName().getString());
        }

        return false;
    }

    private static long getCurrentWorldDay(final ServerPlayer player) {
        return ModernLifeWorldData.get(player.serverLevel()).getWorldDayCounter();
    }
}