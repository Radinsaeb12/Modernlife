package com.modernlife.item;

import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GuideBookItem extends Item {

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    public static void setupBookPages(ItemStack bookStack) {
        CompoundTag bookTag = bookStack.getOrCreateTag();
        if (!bookTag.contains("pages")) {
            bookTag.putString("author", Component.translatable("modernlife.guide_book.author").getString());
            bookTag.putString("title", Component.translatable("item.modernlife.guide_book").getString());
            bookTag.putInt("generation", 0);
            bookTag.putBoolean("resolved", true);

            ListTag pages = new ListTag();
            for (int i = 1; i <= 16; i++) {
                String pageJson = Component.Serializer.toJson(Component.translatable("modernlife.book.page" + i));
                pages.add(StringTag.valueOf(pageJson));
            }
            bookTag.put("pages", pages);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Vanilla yazılı kitap gibi mor parlama efekti verir
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        setupBookPages(itemStack);

        if (level.isClientSide()) {
            if (Platform.getEnv() == EnvType.CLIENT) {
                GuideBookClientHelper.openBookScreen(itemStack);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}