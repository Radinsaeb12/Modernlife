package com.modernlife.block;

import com.modernlife.advancement.CustomAdvancementTriggers;
import com.modernlife.block.entity.YazarKasaBlockEntity;
import com.modernlife.economy.compat.EconomyInventoryHelper;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.AddTransactionPacket;
import com.modernlife.registry.ModItems;
import com.modernlife.service.EconomyService;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class YazarKasaBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public YazarKasaBlock(Properties pProperties) {
        super(pProperties.strength(2.0F, 6.0F).requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new YazarKasaBlockEntity(pPos, pState);
    }

    private void openYazarKasaScreen(ServerPlayer player, YazarKasaBlockEntity yazarKasa, BlockPos pos) {
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {
                buf.writeBlockPos(pos);
            }

            @Override
            public Component getDisplayName() {
                return yazarKasa.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
                return yazarKasa.createMenu(containerId, inventory, p);
            }
        });
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof YazarKasaBlockEntity yazarKasa) {

            UUID ownerUUID = yazarKasa.getOwnerUUID();
            if (ownerUUID != null) {
                Player ownerPlayer = level.getPlayerByUUID(ownerUUID);
                if (ownerPlayer != null) {
                    List<String> registeredAccounts = EconomyService.getRegisteredIdentities(ownerPlayer);
                    if (registeredAccounts.isEmpty()) {
                        yazarKasa.setOwnerUUID(null);
                        player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.owner_account_cancelled"));
                        return InteractionResult.CONSUME;
                    }
                }
            }

            if (yazarKasa.getOwnerUUID() == null) {
                List<String> validIdentities = EconomyService.getRegisteredIdentities(player);
                if (validIdentities.isEmpty()) {
                    player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.need_bank_account"));
                    return InteractionResult.SUCCESS;
                }
                yazarKasa.setOwnerUUID(player.getUUID());
                player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.device_linked"));
                return InteractionResult.SUCCESS;
            }

            if (player.isShiftKeyDown()) {
                openYazarKasaScreen((ServerPlayer) player, yazarKasa, pos);
                return InteractionResult.CONSUME;
            }

            ItemStack elindekiEsya = player.getItemInHand(hand);
            Item esyaItem = elindekiEsya.getItem();
            int kalan = yazarKasa.getKalanTutar();

            if ((esyaItem == ModItems.BANK_CARD.get() || esyaItem == ModItems.IDENTITY_CARD.get()) && yazarKasa.getOdemeTipi() == 2 && kalan > 0) {
                if (elindekiEsya.hasTag()) {
                    String tempKimlik = "";
                    if (elindekiEsya.getTag().contains("realName")) {
                        tempKimlik = elindekiEsya.getTag().getString("realName");
                    } else if (elindekiEsya.getTag().contains("CardOwner")) {
                        tempKimlik = elindekiEsya.getTag().getString("CardOwner");
                    }

                    final String musteriKimlik = tempKimlik;

                    if (musteriKimlik.isEmpty() || !EconomyService.hasValidBankCard(player, musteriKimlik)) {
                        player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.invalid_card"));
                        return InteractionResult.SUCCESS;
                    }

                    boolean islemBasarili = EconomyService.withdraw(player, musteriKimlik, kalan);

                    if (islemBasarili) {
                        int komisyon = (kalan * yazarKasa.getKomisyonOrani()) / 100;
                        int esnafaGidenNetPara = kalan - komisyon;

                        if (player instanceof ServerPlayer serverPlayer) {
                            Component formatliKalan = EconomyService.formatMoney(serverPlayer, kalan);
                            ModNetwork.CHANNEL.sendToPlayer(serverPlayer,
                                new AddTransactionPacket(musteriKimlik, Component.translatable("modernlife.transaction.pos_payment", formatliKalan)));
                        }

                        Player esnaf = level.getPlayerByUUID(yazarKasa.getOwnerUUID());
                        if (esnaf != null) {
                            List<String> esnafHesaplari = EconomyService.getRegisteredIdentities(esnaf);
                            if (!esnafHesaplari.isEmpty()) {
                                String esnafKimlik = esnafHesaplari.get(0);
                                EconomyService.deposit(esnaf, esnafKimlik, esnafaGidenNetPara);

                                if (esnaf instanceof ServerPlayer serverEsnaf) {
                                    Component formatliNet = EconomyService.formatMoney(serverEsnaf, esnafaGidenNetPara);
                                    ModNetwork.CHANNEL.sendToPlayer(serverEsnaf,
                                        new AddTransactionPacket(esnafKimlik, Component.translatable("modernlife.transaction.pos_revenue", formatliNet)));
                                    
                                    CustomAdvancementTriggers.FIRST_SALE.trigger(serverEsnaf);
                                    esnaf.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.card_payment_received", formatliNet));
                                }
                            }
                        }

                        yazarKasa.odemeYap(kalan);
                        yazarKasa.addCiro(esnafaGidenNetPara);

                        Component formatliKalan = EconomyService.formatMoney(player, kalan);
                        player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.payment_successful", formatliKalan));
                    } else {
                        player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.insufficient_balance"));
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.card_read_error"));
                }

                return InteractionResult.SUCCESS;
            }
            else if (yazarKasa.getOdemeTipi() == 1 && kalan > 0) {
                int nakitDeger = 0;
                boolean isLightman = EconomyService.isLightmansActive(player);

                if (isLightman) {
                    ResourceLocation regName = BuiltInRegistries.ITEM.getKey(esyaItem);
                    String id = regName.toString();
                    if ("lightmanscurrency:coin_copper".equals(id)) nakitDeger = 1;
                    else if ("lightmanscurrency:coin_iron".equals(id)) nakitDeger = 5;
                    else if ("lightmanscurrency:coin_gold".equals(id)) nakitDeger = 50;
                    else if ("lightmanscurrency:coin_emerald".equals(id)) nakitDeger = 250;
                    else if ("lightmanscurrency:coin_diamond".equals(id)) nakitDeger = 1000;
                    else if ("lightmanscurrency:coin_netherite".equals(id)) nakitDeger = 3600;
                } else {
                    if (esyaItem == ModItems.TL_5.get()) nakitDeger = 5;
                    else if (esyaItem == ModItems.TL_10.get()) nakitDeger = 10;
                    else if (esyaItem == ModItems.TL_20.get()) nakitDeger = 20;
                    else if (esyaItem == ModItems.TL_50.get()) nakitDeger = 50;
                    else if (esyaItem == ModItems.TL_100.get()) nakitDeger = 100;
                    else if (esyaItem == ModItems.TL_200.get()) nakitDeger = 200;
                }

                if (nakitDeger > 0) {
                    elindekiEsya.shrink(1);

                    int paidAmount = Math.min(nakitDeger, kalan);
                    int changeAmount = nakitDeger - paidAmount;

                    yazarKasa.odemeYap(paidAmount);
                    yazarKasa.addNakit(paidAmount);
                    yazarKasa.addCiro(paidAmount);

                    Component formatliPaid = EconomyService.formatMoney(player, paidAmount);
                    Component formatliKalan = EconomyService.formatMoney(player, yazarKasa.getKalanTutar());
                    player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.cash_paid", formatliPaid, formatliKalan));

                    if (changeAmount > 0 && player instanceof ServerPlayer serverPlayer) {
                        EconomyInventoryHelper.giveCash(serverPlayer, changeAmount);
                        Component formatliChange = EconomyService.formatMoney(serverPlayer, changeAmount);
                        player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.change_given", formatliChange));
                    }

                    if (yazarKasa.getKalanTutar() == 0) {
                        player.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.payment_complete"));

                        Player esnaf = level.getPlayerByUUID(yazarKasa.getOwnerUUID());
                        if (esnaf != null && esnaf != player) {
                            Component formatliSiparis = EconomyService.formatMoney(esnaf, yazarKasa.getMevcutSiparisTutari());
                            esnaf.sendSystemMessage(Component.translatable("modernlife.yazar_kasa.customer_paid_cash", formatliSiparis));
                            
                            if (esnaf instanceof ServerPlayer serverEsnaf) {
                                CustomAdvancementTriggers.FIRST_SALE.trigger(serverEsnaf);
                            }
                        }
                    }

                    return InteractionResult.SUCCESS;
                }
            }

            openYazarKasaScreen((ServerPlayer) player, yazarKasa, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        }
    }
}