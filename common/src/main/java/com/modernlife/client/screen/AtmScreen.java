package com.modernlife.client.screen;

import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.AtmActionPacket;
import com.modernlife.registry.ModItems;
import com.modernlife.service.EconomyService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class AtmScreen extends Screen {
    private boolean hasIdentity = false;
    private boolean hasBankCard = false;
    private boolean hasForeignCard = false; 
    
    private List<String> bulunanKimlikler = new ArrayList<>();
    private String secilenAktifKimlik = "";
    
    private List<ItemStack> myCards = new ArrayList<>();
    private String selectedCardId = "";
    
    private int screenState = 3;
    private int creationTimer = 0;
    private String errorMessage = "";

    private Button btnIssueCard;
    private Button btnTreasuryCard;
    private Button btnDeposit;
    private Button btnWithdraw;
    private Button btnCancel;
    private Button btnTakeCard;
    
    private Button btnIdentity1, btnIdentity2, btnIdentity3;
    private Button btnCard1, btnCard2, btnCard3;
    
    private EditBox txtAmount;

    public AtmScreen() {
        super(Component.translatable("modernlife.atm.title"));
    }

    @Override
    protected void init() {
        super.init();
        checkSystemStates();

        int cx = this.width / 2;
        int cy = this.height / 2;

        btnIdentity1 = this.addRenderableWidget(Button.builder(Component.literal(""), b -> selectIdentity(0)).bounds(cx - 80, cy - 30, 160, 20).build());
        btnIdentity2 = this.addRenderableWidget(Button.builder(Component.literal(""), b -> selectIdentity(1)).bounds(cx - 80, cy - 5, 160, 20).build());
        btnIdentity3 = this.addRenderableWidget(Button.builder(Component.literal(""), b -> selectIdentity(2)).bounds(cx - 80, cy + 20, 160, 20).build());

        btnCard1 = this.addRenderableWidget(Button.builder(Component.literal(""), b -> selectCard(0)).bounds(cx - 100, cy - 30, 200, 20).build());
        btnCard2 = this.addRenderableWidget(Button.builder(Component.literal(""), b -> selectCard(1)).bounds(cx - 100, cy - 5, 200, 20).build());
        btnCard3 = this.addRenderableWidget(Button.builder(Component.literal(""), b -> selectCard(2)).bounds(cx - 100, cy + 20, 200, 20).build());

        boolean isLightman = EconomyService.isLightmansActive(this.minecraft.player);
        Component issueBtnText = Component.translatable(isLightman ? "modernlife.gui.atm.open_acc_btn.lc" : "modernlife.gui.atm.open_acc_btn");

        btnIssueCard = this.addRenderableWidget(Button.builder(issueBtnText, b -> {
            this.screenState = 1;
            this.creationTimer = 40; 
            updateWidgetVisibility();
        }).bounds(cx - 75, cy + (screenState == 3 && hasBankCard ? 65 : 15), 150, 20).build());

        btnTreasuryCard = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.atm.issue_treasury_card").withStyle(ChatFormatting.GOLD), b -> {
            ModNetwork.CHANNEL.sendToServer(new AtmActionPacket(5, 0, EconomyService.TREASURY_ID, ""));
            this.onClose();
        }).bounds(cx - 75, cy + 90, 150, 20).build());

        txtAmount = new EditBox(this.font, cx - 70, cy - 15, 140, 20, Component.translatable("modernlife.gui.atm.amount_box_hint"));
        txtAmount.setMaxLength(8);
        this.addRenderableWidget(txtAmount);

        btnDeposit = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.atm.deposit_btn"), b -> {
            int miktar = getAmountFromInput();
            boolean isLm = EconomyService.isLightmansActive(this.minecraft.player);
            if (!isLm && (miktar < 5 || miktar % 5 != 0)) {
                this.errorMessage = Component.translatable("modernlife.atm.error_multiple_of_5").withStyle(ChatFormatting.RED).getString();
                return;
            }
            if (miktar > 0) {
                ModNetwork.CHANNEL.sendToServer(new AtmActionPacket(1, miktar, secilenAktifKimlik, selectedCardId));
                this.onClose(); 
            }
        }).bounds(cx - 70, cy + 10, 65, 20).build());

        btnWithdraw = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.atm.withdraw_btn"), b -> {
            int miktar = getAmountFromInput();
            boolean isLm = EconomyService.isLightmansActive(this.minecraft.player);
            if (!isLm && (miktar < 5 || miktar % 5 != 0)) {
                this.errorMessage = Component.translatable("modernlife.atm.error_multiple_of_5").withStyle(ChatFormatting.RED).getString();
                return;
            }
            if (miktar > 0) {
                ModNetwork.CHANNEL.sendToServer(new AtmActionPacket(2, miktar, secilenAktifKimlik, selectedCardId));
                this.onClose();
            }
        }).bounds(cx + 5, cy + 10, 65, 20).build());

        btnCancel = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.atm.cancel_btn"), b -> {
            ModNetwork.CHANNEL.sendToServer(new AtmActionPacket(3, 0, secilenAktifKimlik, selectedCardId));
            this.onClose();
        }).bounds(cx - 75, cy + 40, 150, 20).build());

        btnTakeCard = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.atm.take_card_btn"), b -> {
            ModNetwork.CHANNEL.sendToServer(new AtmActionPacket(0, 0, secilenAktifKimlik, ""));
            this.onClose(); 
        }).bounds(cx - 75, cy + 10, 150, 20).build());

        updateWidgetVisibility();
    }
    
    private int getAmountFromInput() {
        try {
            return Integer.parseInt(txtAmount.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void checkSystemStates() {
        if (this.minecraft == null || this.minecraft.player == null) return;
        
        this.hasIdentity = false;
        bulunanKimlikler.clear();

        for (ItemStack stack : this.minecraft.player.getInventory().items) {
            if (stack.getItem() == ModItems.IDENTITY_CARD.get() && stack.hasTag() && stack.getTag().contains("realName")) {
                String name = stack.getTag().getString("realName");
                if (EconomyService.hasValidBankCard(this.minecraft.player, name)) {
                    if (!bulunanKimlikler.contains(name)) {
                        bulunanKimlikler.add(name);
                        this.hasIdentity = true;
                    }
                }
            }
        }

        for (ItemStack stack : this.minecraft.player.getInventory().items) {
            if (stack.getItem() == ModItems.BANK_CARD.get() && stack.hasTag() && stack.getTag().contains("CardOwner")) {
                String owner = stack.getTag().getString("CardOwner");
                boolean isCancelled = stack.getTag().getBoolean("Cancelled");
                
                if (!isCancelled && EconomyService.hasValidBankCard(this.minecraft.player, owner)) {
                    if (!bulunanKimlikler.contains(owner)) {
                        bulunanKimlikler.add(owner);
                        this.hasIdentity = true;
                    }
                }
            }
        }

        if (!hasIdentity) return;

        if (secilenAktifKimlik.isEmpty() || !bulunanKimlikler.contains(secilenAktifKimlik)) {
            if (bulunanKimlikler.size() > 1) {
                this.screenState = 4; 
                return;
            } else {
                this.secilenAktifKimlik = bulunanKimlikler.get(0); 
            }
        }

        myCards.clear();
        hasForeignCard = false;
        for (ItemStack stack : this.minecraft.player.getInventory().items) {
            if (stack.getItem() == ModItems.BANK_CARD.get() && stack.hasTag()) {
                String owner = stack.getTag().getString("CardOwner");
                boolean isCancelled = stack.getTag().getBoolean("Cancelled");
                
                if (owner.equals(secilenAktifKimlik) && !isCancelled) {
                    myCards.add(stack);
                } else if (!owner.equals(secilenAktifKimlik) && !isCancelled) {
                    hasForeignCard = true;
                }
            }
        }

        if (myCards.isEmpty()) {
            this.hasBankCard = false;
            this.screenState = 3; 
        } else {
            this.hasBankCard = true;
            if (selectedCardId.isEmpty()) {
                if (myCards.size() > 1) {
                    this.screenState = 0; 
                } else {
                    setCardData(0); 
                    this.screenState = 3;
                }
            }
        }
    }

    private void selectIdentity(int idx) {
        if (idx < bulunanKimlikler.size()) {
            this.secilenAktifKimlik = bulunanKimlikler.get(idx);
            this.selectedCardId = ""; 
            this.clearWidgets();
            this.init(); 
        }
    }

    private void selectCard(int idx) {
        if (idx < myCards.size()) {
            setCardData(idx);
            this.screenState = 3; 
            this.clearWidgets();
            this.init();
        }
    }

    private void setCardData(int idx) {
        ItemStack activeCard = myCards.get(idx);
        if (activeCard.hasTag()) {
            this.selectedCardId = activeCard.getTag().getString("CardID");
        }
    }

    private Component localizeIdentity(String identity) {
        if (identity != null && EconomyService.isTreasury(identity)) {
            return Component.translatable("modernlife.identity.state_treasury");
        }
        return Component.literal(identity == null ? "" : identity);
    }

    private void updateWidgetVisibility() {
        btnIssueCard.visible = false; btnDeposit.visible = false; txtAmount.visible = false;
        btnWithdraw.visible = false; btnCancel.visible = false; btnTakeCard.visible = false;
        btnIdentity1.visible = false; btnIdentity2.visible = false; btnIdentity3.visible = false;
        btnCard1.visible = false; btnCard2.visible = false; btnCard3.visible = false;
        btnTreasuryCard.visible = false;

        if (!hasIdentity) return;

        if (screenState == 4) {
            if (bulunanKimlikler.size() > 0) { btnIdentity1.setMessage(Component.translatable("modernlife.gui.atm.login_as", localizeIdentity(bulunanKimlikler.get(0)))); btnIdentity1.visible = true; }
            if (bulunanKimlikler.size() > 1) { btnIdentity2.setMessage(Component.translatable("modernlife.gui.atm.login_as", localizeIdentity(bulunanKimlikler.get(1)))); btnIdentity2.visible = true; }
            if (bulunanKimlikler.size() > 2) { btnIdentity3.setMessage(Component.translatable("modernlife.gui.atm.login_as", localizeIdentity(bulunanKimlikler.get(2)))); btnIdentity3.visible = true; }
            return;
        }

        if (screenState == 0) {
            if (myCards.size() > 0) { btnCard1.setMessage(Component.literal("ID: " + myCards.get(0).getTag().getString("CardID"))); btnCard1.visible = true; }
            if (myCards.size() > 1) { btnCard2.setMessage(Component.literal("ID: " + myCards.get(1).getTag().getString("CardID"))); btnCard2.visible = true; }
            if (myCards.size() > 2) { btnCard3.setMessage(Component.literal("ID: " + myCards.get(2).getTag().getString("CardID"))); btnCard3.visible = true; }
            return;
        }

        if (screenState == 2) {
            btnTakeCard.visible = true;
            return;
        } 
        
        if (screenState == 3) {
            if (!EconomyService.isTreasury(secilenAktifKimlik)) {
                btnIssueCard.visible = true;
                btnTreasuryCard.visible = true; 
            }
            if (hasBankCard && !myCards.isEmpty()) {
                txtAmount.visible = true;
                btnDeposit.visible = true;
                btnWithdraw.visible = true;
                if (!EconomyService.isTreasury(secilenAktifKimlik)) {
                    btnCancel.visible = true;
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.minecraft != null && this.minecraft.player != null) {
            String activeIdentity = EconomyService.getActiveIdentity(this.minecraft.player);
            if (activeIdentity.isEmpty()) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("modernlife.atm.session_closed_no_id").withStyle(ChatFormatting.RED), 
                    true
                );
                this.onClose();
                return;
            }
        }

        if (screenState == 1) {
            creationTimer--;
            if (creationTimer <= 0) {
                screenState = 2; 
                updateWidgetVisibility();
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float pt) {
        this.renderBackground(gui);
        gui.fill(width/2 - 120, height/2 - 120, width/2 + 120, height/2 + 120, 0xFF111111);

        if (!hasIdentity) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.atm.identity_error"), width/2, height/2 - 50, 0xFFFFFF);
            gui.drawCenteredString(this.font, Component.translatable("modernlife.atm.identity_error_sub"), width/2, height/2 - 35, 0xAAAAAA);
            return;
        }

        if (screenState == 4) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.atm.select_id_title"), width/2, height/2 - 60, 0xFFFFFF);
            super.render(gui, mouseX, mouseY, pt);
            return;
        }

        if (screenState == 0) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.atm.select_card_title"), width/2, height/2 - 60, 0xFFFFFF);
            super.render(gui, mouseX, mouseY, pt);
            return;
        }

        if (screenState == 1) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.atm.processing"), width/2, height/2 - 20, 0xFFFFFF);
            super.render(gui, mouseX, mouseY, pt);
            return;
        }
        
        if (screenState == 2) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.atm.success"), width/2, height/2 - 30, 0xFFFFFF);
            super.render(gui, mouseX, mouseY, pt);
            return;
        }

        if (!hasBankCard || selectedCardId.isEmpty()) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.atm.no_card_title"), width/2, height/2 - 50, 0xFFFFFF);
            gui.drawCenteredString(this.font, Component.translatable("modernlife.atm.no_card_sub"), width/2, height/2 - 35, 0xAAAAAA);
            
            boolean isLm = EconomyService.isLightmansActive(this.minecraft.player);
            gui.drawCenteredString(this.font, Component.translatable(isLm ? "modernlife.atm.card_price_sub.lc" : "modernlife.atm.card_price_sub"), width/2, height/2 - 15, 0xFFFFFF);
            
            if (hasForeignCard) {
                gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.atm.foreign_card_warn"), width/2, height/2 + 5, 0xFF5555);
            }
                
        } else {
            long aktifBakiye = EconomyService.getBalance(this.minecraft.player, secilenAktifKimlik);
            Component formatliBakiye = EconomyService.formatMoney(this.minecraft.player, aktifBakiye);
                
            gui.drawCenteredString(this.font, Component.translatable("modernlife.atm.bank_title"), width/2, height/2 - 90, 0xFFFFFF);
            gui.drawCenteredString(this.font, Component.translatable("modernlife.atm.customer", localizeIdentity(secilenAktifKimlik)), width/2, height/2 - 75, 0xAAAAAA);
            gui.drawCenteredString(this.font, Component.translatable("modernlife.atm.balance_text", formatliBakiye), width/2, height/2 - 55, 0xFFFFFF);
                
            int girilenTutar = getAmountFromInput();
            if (!errorMessage.isEmpty()) {
                if (this.font.width(errorMessage) > 220) {
                    gui.drawWordWrap(this.font, Component.literal(errorMessage), width/2 - 110, height/2 - 40, 220, 0xFF5555);
                } else {
                    gui.drawCenteredString(this.font, errorMessage, width/2, height/2 - 35, 0xFF5555);
                }
            } else if (girilenTutar > 0) {
                long kalan = aktifBakiye - girilenTutar;
                String color = kalan < 0 ? "§c" : "§7";
                Component formatliKalan = EconomyService.formatMoney(this.minecraft.player, kalan);
                gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.atm.est_balance", color, formatliKalan), width/2, height/2 - 35, 0xFFFFFF);
            } else {
                gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.atm.enter_amount_hint"), width/2, height/2 - 35, 0xFFFFFF);
            }
        }
            
        super.render(gui, mouseX, mouseY, pt);
    }
}