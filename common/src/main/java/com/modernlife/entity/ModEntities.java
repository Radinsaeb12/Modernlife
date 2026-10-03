package com.modernlife.registry;

import com.modernlife.ModernLifeMod;
import com.modernlife.entity.BankerEntity;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
            DeferredRegister.create(ModernLifeMod.MODID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<BankerEntity>> BANKER = ENTITIES.register("banker",
            () -> EntityType.Builder.of(BankerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .build("banker"));

    public static void register() {
        ENTITIES.register();
        // Banker mobunun can, hız ve direnç özelliklerini ortak modülde kaydet
        EntityAttributeRegistry.register(BANKER, BankerEntity::createAttributes);
    }
}