package com.modernlife.registry;

import com.modernlife.ModernLifeMod;
import com.modernlife.menu.YazarKasaMenu;
import com.modernlife.menu.TaxPanelMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ModernLifeMod.MODID, Registries.MENU);

    public static final RegistrySupplier<MenuType<TaxPanelMenu>> TAX_PANEL =
            MENUS.register("tax_panel", () -> MenuRegistry.ofExtended(TaxPanelMenu::new));

    public static final RegistrySupplier<MenuType<YazarKasaMenu>> YAZAR_KASA =
            MENUS.register("yazar_kasa_menu", () -> MenuRegistry.ofExtended(YazarKasaMenu::new));

    public static void register() {
        MENUS.register();
    }
}