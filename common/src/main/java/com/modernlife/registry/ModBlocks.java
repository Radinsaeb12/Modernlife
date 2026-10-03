package com.modernlife.registry;

import com.modernlife.ModernLifeMod;
import com.modernlife.block.AtmBlock;
import com.modernlife.block.YazarKasaBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ModernLifeMod.MODID, Registries.BLOCK);

    public static final RegistrySupplier<Block> ATM = BLOCKS.register("atm",
            () -> new AtmBlock(BlockBehaviour.Properties.of().noOcclusion().strength(1.5f)));

    public static final RegistrySupplier<Item> ATM_ITEM = ModItems.ITEMS.register("atm", 
            () -> new BlockItem(ATM.get(), new Item.Properties()));

    public static final RegistrySupplier<Block> YAZAR_KASA = BLOCKS.register("yazar_kasa",
            () -> new YazarKasaBlock(BlockBehaviour.Properties.of().noOcclusion().strength(1.5f)));

    public static final RegistrySupplier<Item> YAZAR_KASA_ITEM = ModItems.ITEMS.register("yazar_kasa", 
            () -> new BlockItem(YAZAR_KASA.get(), new Item.Properties()));

    private ModBlocks() {}

    public static void register() {
        BLOCKS.register();
    }
}