package com.modernlife.registry;

import com.modernlife.ModernLifeMod;
import com.modernlife.block.entity.YazarKasaBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ModernLifeMod.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<YazarKasaBlockEntity>> YAZAR_KASA =
            BLOCK_ENTITIES.register("yazar_kasa_be", () ->
                    BlockEntityType.Builder.of(YazarKasaBlockEntity::new, ModBlocks.YAZAR_KASA.get()).build(null)
            );

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}