package com.modernlife.registry;

import com.modernlife.ModernLifeMod;
import com.modernlife.data.LicenseType;
import com.modernlife.item.*;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ModernLifeMod.MODID, Registries.ITEM);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = 
            DeferredRegister.create(ModernLifeMod.MODID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<Item> GUIDE_BOOK = ITEMS.register("guide_book", 
            () -> new GuideBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistrySupplier<Item> IDENTITY_CARD = ITEMS.register("identity_card", 
            () -> new DocumentItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> VEHICLE_LICENSE = ITEMS.register("vehicle_license", 
            () -> new DocumentItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), LicenseType.VEHICLE));
    public static final RegistrySupplier<Item> MOUNT_LICENSE = ITEMS.register("mount_license", 
            () -> new DocumentItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), LicenseType.MOUNT));
    public static final RegistrySupplier<Item> TAX_PANEL = ITEMS.register("tax_panel", 
            () -> new TaxPanelItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistrySupplier<Item> TELEFON_1 = ITEMS.register("telefon_1", 
            () -> new TelefonItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> TELEFON_2 = ITEMS.register("telefon_2", 
            () -> new TelefonItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    
    public static final RegistrySupplier<Item> BANK_CARD = ITEMS.register("bank_card", 
            () -> new BankCardItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistrySupplier<Item> PRESIDENTIAL_BALLOT = ITEMS.register("presidential_ballot", 
            () -> new PresidentialBallotItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistrySupplier<Item> ADMIN_PRESIDENTIAL_BALLOT = ITEMS.register("admin_presidential_ballot", 
            () -> new AdminPresidentialBallotItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<Item> TL_5 = ITEMS.register("tl_5", 
            () -> new Item(new Item.Properties().rarity(Rarity.COMMON)));
    public static final RegistrySupplier<Item> TL_10 = ITEMS.register("tl_10", 
            () -> new Item(new Item.Properties().rarity(Rarity.COMMON)));
    public static final RegistrySupplier<Item> TL_20 = ITEMS.register("tl_20", 
            () -> new Item(new Item.Properties().rarity(Rarity.COMMON)));
    public static final RegistrySupplier<Item> TL_50 = ITEMS.register("tl_50", 
            () -> new Item(new Item.Properties().rarity(Rarity.COMMON)));
    public static final RegistrySupplier<Item> TL_100 = ITEMS.register("tl_100", 
            () -> new Item(new Item.Properties().rarity(Rarity.COMMON)));
    public static final RegistrySupplier<Item> TL_200 = ITEMS.register("tl_200", 
            () -> new Item(new Item.Properties().rarity(Rarity.COMMON)));

    // Dummy Items (Advancement Icons)
    public static final RegistrySupplier<Item> ADV_ROOT = ITEMS.register("adv_root", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_BANK_CARD = ITEMS.register("adv_bank_card", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_FIRST_MONEY = ITEMS.register("adv_first_money", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_EFT_TRANSFER = ITEMS.register("adv_eft_transfer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_CRAFT_REGISTER = ITEMS.register("adv_craft_register", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_FIRST_SALE = ITEMS.register("adv_first_sale", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_FIRST_PENALTY = ITEMS.register("adv_first_penalty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_GET_LICENSE = ITEMS.register("adv_get_license", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_CRAFT_PHONE = ITEMS.register("adv_craft_phone", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_PHONE_CALL = ITEMS.register("adv_phone_call", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_BALLOT = ITEMS.register("adv_ballot", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADV_BECOME_PRESIDENT = ITEMS.register("adv_become_president", () -> new Item(new Item.Properties()));

    // Tab kaydı - İtemler kayıt edilip Forge hazır olana kadar get() çağrılmasını erteler
    public static final RegistrySupplier<CreativeModeTab> MODERN_LIFE_TAB = CREATIVE_TABS.register("modern_life_tab", () -> 
            CreativeTabRegistry.create(builder -> {
                builder.title(Component.translatable("itemGroup.modernlife"))
                       .icon(() -> new ItemStack(IDENTITY_CARD.get()))
                       .displayItems((parameters, output) -> {
                           // Rehber Kitabı
                           if (GUIDE_BOOK.isPresent()) {
                               ItemStack guideBook = new ItemStack(GUIDE_BOOK.get());
                               GuideBookItem.setupBookPages(guideBook);
                               output.accept(guideBook);
                           }

                           // Blok Eşyaları
                           if (ModBlocks.ATM_ITEM.isPresent()) output.accept(ModBlocks.ATM_ITEM.get());
                           if (ModBlocks.YAZAR_KASA_ITEM.isPresent()) output.accept(ModBlocks.YAZAR_KASA_ITEM.get());

                           // Belgeler & Cihazlar (isPresent ile erken get çökmesi engellendi)
                           if (IDENTITY_CARD.isPresent()) output.accept(IDENTITY_CARD.get());
                           if (VEHICLE_LICENSE.isPresent()) output.accept(VEHICLE_LICENSE.get());
                           if (MOUNT_LICENSE.isPresent()) output.accept(MOUNT_LICENSE.get());
                           if (TAX_PANEL.isPresent()) output.accept(TAX_PANEL.get());
                           if (TELEFON_1.isPresent()) output.accept(TELEFON_1.get());
                           if (TELEFON_2.isPresent()) output.accept(TELEFON_2.get());
                           if (BANK_CARD.isPresent()) output.accept(BANK_CARD.get());

                           // Seçim Eşyaları
                           if (PRESIDENTIAL_BALLOT.isPresent()) output.accept(PRESIDENTIAL_BALLOT.get());
                           if (ADMIN_PRESIDENTIAL_BALLOT.isPresent()) output.accept(ADMIN_PRESIDENTIAL_BALLOT.get());

                           // Banknotlar (TL)
                           if (TL_5.isPresent()) output.accept(TL_5.get());
                           if (TL_10.isPresent()) output.accept(TL_10.get());
                           if (TL_20.isPresent()) output.accept(TL_20.get());
                           if (TL_50.isPresent()) output.accept(TL_50.get());
                           if (TL_100.isPresent()) output.accept(TL_100.get());
                           if (TL_200.isPresent()) output.accept(TL_200.get());
                       });
            })
    );

    private ModItems() {}

    public static void register() {
        // Önce eşyalar register edilmeli, ardından sekmeler
        ITEMS.register();
        CREATIVE_TABS.register();
    }
}