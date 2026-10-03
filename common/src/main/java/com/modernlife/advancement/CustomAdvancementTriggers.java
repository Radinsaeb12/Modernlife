package com.modernlife.advancement;

import com.google.gson.JsonObject;
import com.modernlife.ModernLifeMod;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CustomAdvancementTriggers {

    public static final SimpleCustomTrigger FIRST_FINE = new SimpleCustomTrigger(new ResourceLocation(ModernLifeMod.MODID, "first_fine"));
    public static final SimpleCustomTrigger FIRST_SALE = new SimpleCustomTrigger(new ResourceLocation(ModernLifeMod.MODID, "first_sale"));
    public static final SimpleCustomTrigger PHONE_CALL = new SimpleCustomTrigger(new ResourceLocation(ModernLifeMod.MODID, "phone_call"));
    public static final SimpleCustomTrigger EFT_TRANSFER = new SimpleCustomTrigger(new ResourceLocation(ModernLifeMod.MODID, "eft_transfer"));

    public static void register() {
        CriteriaTriggers.register(FIRST_FINE);
        CriteriaTriggers.register(FIRST_SALE);
        CriteriaTriggers.register(PHONE_CALL);
        CriteriaTriggers.register(EFT_TRANSFER);
    }

    public static class SimpleCustomTrigger extends SimpleCriterionTrigger<SimpleCustomTrigger.TriggerInstance> {
        private final ResourceLocation id;

        public SimpleCustomTrigger(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate predicate, DeserializationContext context) {
            return new TriggerInstance(this.id, predicate);
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class TriggerInstance extends AbstractCriterionTriggerInstance {
            public TriggerInstance(ResourceLocation triggerId, ContextAwarePredicate playerPredicate) {
                super(triggerId, playerPredicate);
            }
        }
    }
}