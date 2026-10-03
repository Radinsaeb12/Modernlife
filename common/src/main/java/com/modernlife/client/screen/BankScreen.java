package com.modernlife.client.screen;

import com.modernlife.capability.ModCapabilities;
import com.modernlife.capability.PlayerEconomy;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.PayTaxesPacket;
import com.modernlife.network.packet.TransferMoneyPacket;
import com.modernlife.registry.ModItems;
import com.modernlife.service.EconomyService;
import com.modernlife.service.WorldStateService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankScreen extends Screen {
    private boolean hasIdentity = false;
    private List<String> bulunanKimlikler = new ArrayList<>();
    private String secilenAktifKimlik = "";
    
    private int screenState = 3;
    private String errorMessage = "";

    private Button btnIdentity1, btnIdentity2, btnIdentity3;
    private Button btnTransfer, btnTransactions, btnTaxes, btnSwitchAccount, btnCloseApp;
    
    private EditBox txtTargetIdentity;
    private EditBox txtAmount;
    private EditBox txtDesc;
    private Button btnSendMoney, btnBack, btnPayTax;

    public static Map<String, List<String>> islemGecmisi = new HashMap<>();

    public BankScreen() {
        super(Component.translatable("modernlife.bank.title"));
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

        btnTransfer = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.bank.transfer_btn"), b -> {
            this.screenState = 5; 
            this.errorMessage = "";
            updateWidgetVisibility();
        }).bounds(cx - 75, cy - 35, 150, 20).build());
        
        btnTransactions = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.bank.actions_btn"), b -> {
            this.screenState = 6; updateWidgetVisibility();
        }).bounds(cx - 75, cy - 10, 150, 20).build());
        
        btnTaxes = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.bank.pay_taxes"), b -> {
            this.screenState = 7; updateWidgetVisibility();
        }).bounds(cx - 75, cy + 15, 150, 20).build());

        btnSwitchAccount = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.bank.select_account_title"), b -> {
            this.screenState = 4;
            updateWidgetVisibility();
        }).bounds(cx - 75, cy + 40, 150, 20).build());

        btnCloseApp = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.bank.close_btn"), b -> {
            this.onClose();
        }).bounds(cx - 75, cy + 65, 150, 20).build());

        txtTargetIdentity = new EditBox(this.font, cx - 75, cy - 40, 150, 18, Component.translatable("modernlife.gui.bank.target_id_hint"));
        txtTargetIdentity.setMaxLength(32);
        this.addRenderableWidget(txtTargetIdentity);

        txtAmount = new EditBox(this.font, cx - 75, cy - 5, 150, 18, Component.translatable("modernlife.bank.amount"));
        txtAmount.setMaxLength(8);
        this.addRenderableWidget(txtAmount);

        txtDesc = new EditBox(this.font, cx - 75, cy + 30, 150, 18, Component.translatable("modernlife.gui.bank.desc_hint"));
        txtDesc.setMaxLength(32);
        this.addRenderableWidget(txtDesc);

        btnSendMoney = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.bank.send"), b -> {
            try {
                String inputAmount = txtAmount.getValue().trim();
                if (inputAmount.isEmpty()) {
                    this.errorMessage = Component.translatable("modernlife.bank.error_empty_amount").withStyle(ChatFormatting.RED).getString();
                    return;
                }

                int amount = Integer.parseInt(inputAmount);
                String targetIdentity = txtTargetIdentity.getValue().trim();
                String desc = txtDesc.getValue().trim();
                if (desc.isEmpty()) desc = Component.translatable("modernlife.bank.eft_transfer_default_description").getString();
                
                boolean isLightman = EconomyService.isLightmansActive(this.minecraft.player);
                if (!isLightman && (amount < 5 || amount % 5 != 0)) {
                    this.errorMessage = Component.translatable("modernlife.bank.error_multiple_of_5").withStyle(ChatFormatting.RED).getString();
                    return;
                }

                if (amount > 0 && !targetIdentity.isEmpty()) {
                    ModNetwork.CHANNEL.sendToServer(new TransferMoneyPacket(secilenAktifKimlik, targetIdentity, amount, desc));
                    this.onClose();
                }
            } catch (NumberFormatException e) {
                this.errorMessage = Component.translatable("modernlife.bank.invalid_amount").withStyle(ChatFormatting.RED).getString();
            }
        }).bounds(cx - 75, cy + 55, 150, 18).build());

        btnPayTax = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.bank.confirm_payment"), b -> {
            ModNetwork.CHANNEL.sendToServer(new PayTaxesPacket(secilenAktifKimlik));
            this.onClose();
        }).bounds(cx - 75, cy - 10, 150, 20).build());

        btnBack = this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.bank.back_btn"), b -> {
            this.screenState = 3; 
            this.errorMessage = "";
            updateWidgetVisibility();
        }).bounds(cx - 75, cy + 76, 150, 18).build());

        updateWidgetVisibility();
    }

    private void checkSystemStates() {
        if (this.minecraft == null || this.minecraft.player == null) return;
        bulunanKimlikler.clear();
        this.hasIdentity = false;

        List<String> registered = EconomyService.getRegisteredIdentities(this.minecraft.player);
        for (String idName : registered) {
            if (EconomyService.isValidIdentityForBank(this.minecraft.player, idName) 
                    && EconomyService.hasBankAccount(this.minecraft.player, idName)) {
                if (!bulunanKimlikler.contains(idName)) {
                    bulunanKimlikler.add(idName);
                    this.hasIdentity = true;
                }
            }
        }

        if (WorldStateService.isPresident(this.minecraft.player)) {
            for (ItemStack stack : this.minecraft.player.getInventory().items) {
                if (stack.getItem() == ModItems.BANK_CARD.get() && stack.hasTag()) {
                    if (EconomyService.isTreasury(stack.getTag().getString("CardOwner")) && !stack.getTag().getBoolean("Cancelled")) {
                        if (!bulunanKimlikler.contains(EconomyService.TREASURY_ID)) {
                            bulunanKimlikler.add(EconomyService.TREASURY_ID);
                            this.hasIdentity = true;
                        }
                        break;
                    }
                }
            }
        }

        if (!hasIdentity) return;

        if (secilenAktifKimlik.isEmpty() || !bulunanKimlikler.contains(secilenAktifKimlik)) {
            if (bulunanKimlikler.size() > 1) {
                this.screenState = 4;
            } else {
                this.secilenAktifKimlik = bulunanKimlikler.get(0);
                this.screenState = 3;
            }
        }
    }

    private void selectIdentity(int idx) {
        if (idx < bulunanKimlikler.size()) {
            this.secilenAktifKimlik = bulunanKimlikler.get(idx);
            this.screenState = 3;
            this.clearWidgets();
            this.init();
        }
    }

    private Component localizeIdentity(String identity) {
        if (identity != null && EconomyService.isTreasury(identity)) {
            return Component.translatable("modernlife.identity.state_treasury");
        }
        return Component.literal(identity == null ? "" : identity);
    }

    private void updateWidgetVisibility() {
        btnIdentity1.visible = false; btnIdentity2.visible = false; btnIdentity3.visible = false;
        btnTransfer.visible = false; btnTransactions.visible = false; btnTaxes.visible = false; 
        btnSwitchAccount.visible = false; btnCloseApp.visible = false;
        txtTargetIdentity.visible = false; txtAmount.visible = false; txtDesc.visible = false;
        btnSendMoney.visible = false; btnBack.visible = false; btnPayTax.visible = false;

        if (!hasIdentity) return;

        if (screenState == 4) {
            if (bulunanKimlikler.size() > 0) { btnIdentity1.setMessage(Component.translatable("modernlife.gui.bank.login_as", localizeIdentity(bulunanKimlikler.get(0)))); btnIdentity1.visible = true; }
            if (bulunanKimlikler.size() > 1) { btnIdentity2.setMessage(Component.translatable("modernlife.gui.bank.login_as", localizeIdentity(bulunanKimlikler.get(1)))); btnIdentity2.visible = true; }
            if (bulunanKimlikler.size() > 2) { btnIdentity3.setMessage(Component.translatable("modernlife.gui.bank.login_as", localizeIdentity(bulunanKimlikler.get(2)))); btnIdentity3.visible = true; }
        } else if (screenState == 3) {
            btnTransfer.visible = true; 
            btnTransactions.visible = true; 
            btnTaxes.visible = !EconomyService.isTreasury(secilenAktifKimlik);
            btnSwitchAccount.visible = (bulunanKimlikler.size() > 1);
            btnCloseApp.visible = true;
        } else if (screenState == 5) {
            txtTargetIdentity.visible = true; txtAmount.visible = true; txtDesc.visible = true;
            btnSendMoney.visible = true; btnBack.visible = true;
        } else if (screenState == 6) {
            btnBack.visible = true;
        } else if (screenState == 7) {
            btnPayTax.visible = !EconomyService.isTreasury(secilenAktifKimlik); 
            btnBack.visible = true;
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float pt) {
        this.renderBackground(gui);
        gui.fill(width/2 - 120, height/2 - 130, width/2 + 120, height/2 + 130, 0xDD000000);

        if (!hasIdentity) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.bank.no_account_title"), width/2, height/2 - 20, 0xFF5555);
            gui.drawCenteredString(this.font, Component.translatable("modernlife.bank.no_account_sub1"), width/2, height/2 + 0, 0xAAAAAA);
            gui.drawCenteredString(this.font, Component.translatable("modernlife.bank.no_account_sub2"), width/2, height/2 + 15, 0xAAAAAA);
            super.render(gui, mouseX, mouseY, pt);
            return;
        }

        long aktifBakiye = EconomyService.getBalance(this.minecraft.player, secilenAktifKimlik);
        Component formatliBakiye = EconomyService.formatMoney(this.minecraft.player, aktifBakiye);

        if (screenState == 4) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.select_account_title"), width/2, height/2 - 60, 0xFFFFFF);
        } else if (screenState == 3) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.mobile_branch"), width/2, height/2 - 95, 0xFFFFFF);
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.account_lbl", localizeIdentity(secilenAktifKimlik)), width/2, height/2 - 80, 0xAAAAAA);
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.balance_lbl", formatliBakiye), width/2, height/2 - 55, 0xFFFFFF);
        } else if (screenState == 5) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.eft_title"), width/2, height/2 - 95, 0xFFFFFF);
            gui.drawCenteredString(this.font, Component.translatable("modernlife.bank.balance", formatliBakiye), width/2, height/2 - 82, 0xAAAAAA);
            
            gui.drawString(this.font, Component.translatable("modernlife.gui.bank.recipient_lbl"), width/2 - 75, height/2 - 50, 0xFFFFFF);
            gui.drawString(this.font, Component.translatable("modernlife.gui.bank.amount_lbl"), width/2 - 75, height/2 - 15, 0xFFFFFF);
            gui.drawString(this.font, Component.translatable("modernlife.gui.bank.desc_lbl"), width/2 - 75, height/2 + 20, 0xFFFFFF);
            
            if (!errorMessage.isEmpty()) {
                gui.drawWordWrap(this.font, Component.literal(errorMessage), width/2 - 100, height/2 + 96, 200, 0xFF5555);
            } else {
                int amount = 0;
                try { amount = Integer.parseInt(txtAmount.getValue()); } catch (Exception ignored) {}
                if (amount > 0 && !EconomyService.isTreasury(secilenAktifKimlik)) {
                    int fee = (amount <= 1000) ? 5 : (amount <= 5000 ? 10 : 15);
                    Component formatliFee = EconomyService.formatMoney(this.minecraft.player, fee);
                    gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.fee_lbl", formatliFee), width/2, height/2 + 98, 0xFFFFFF);
                }
            }
        } else if (screenState == 6) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.recent_actions"), width/2, height/2 - 90, 0xFFFFFF);
            
            List<String> gecmis = islemGecmisi.getOrDefault(secilenAktifKimlik, new ArrayList<>());
            
            if (gecmis.isEmpty()) {
                gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.no_actions"), width/2, height/2 - 20, 0xFFFFFF);
            } else {
                int y = height/2 - 60;
                for (int i = Math.max(0, gecmis.size() - 8); i < gecmis.size(); i++) {
                    gui.drawCenteredString(this.font, Component.literal(gecmis.get(i)), width/2, y, 0xFFFFFF);
                    y += 15;
                }
            }
        } else if (screenState == 7) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.state_taxes"), width/2, height/2 - 90, 0xFFFFFF);
            
            if (EconomyService.isTreasury(secilenAktifKimlik)) {
                gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.treasury_no_taxes"), width/2, height/2 - 50, 0xAAAAAA);
            } else if (minecraft != null && minecraft.player != null) {
                PlayerEconomy economy = ModCapabilities.get(minecraft.player);
                long taxDueAmount = economy.getTaxDueAmount();
                long unpaidDays = economy.getTaxDue();
                if (taxDueAmount > 0) {
                    Component formatliVergi = EconomyService.formatMoney(this.minecraft.player, taxDueAmount);
                    gui.drawCenteredString(this.font, Component.translatable("modernlife.bank.tax_due", formatliVergi), width/2, height/2 - 50, 0xFFFFFF);
                    gui.drawCenteredString(this.font, Component.translatable("modernlife.bank.tax_due_days", unpaidDays), width/2, height/2 - 35, 0xAAAAAA);
                } else {
                    gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.bank.no_taxes"), width/2, height/2 - 50, 0xAAAAAA);
                }
            }
        }
        
        super.render(gui, mouseX, mouseY, pt);
    }
}