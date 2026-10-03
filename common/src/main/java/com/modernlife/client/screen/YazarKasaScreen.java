package com.modernlife.client.screen;

import com.modernlife.ModernLifeMod;
import com.modernlife.menu.YazarKasaMenu;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.YazarKasaActionPacket;
import com.modernlife.service.EconomyService;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.ChatFormatting;

import java.util.UUID;

public class YazarKasaScreen extends AbstractContainerScreen<YazarKasaMenu> {
    
    private static final ResourceLocation TEXTURE = new ResourceLocation(ModernLifeMod.MODID, "textures/gui/yazar_kasa_gui.png");

    private EditBox fiyatGirdisi;
    private Button btnFiyatOnayla;
    private Button btnNakitAl; 
    private Button btnNakit;
    private Button btnKart;
    
    private String errorMessage = "";

    public YazarKasaScreen(YazarKasaMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        BlockPos pos = this.menu.getBlockEntity().getBlockPos();
        
        if (Minecraft.getInstance().player == null) return;

        boolean isOwner = false;
        UUID ownerId = this.menu.getBlockEntity().getOwnerUUID();
        if (ownerId != null) {
            isOwner = Minecraft.getInstance().player.getUUID().equals(ownerId);
        }

        if (isOwner) {
            this.fiyatGirdisi = new EditBox(this.font, x + 30, y + 35, 116, 18, Component.translatable("modernlife.yazar_kasa.enter_price"));
            this.fiyatGirdisi.setMaxLength(8);
            this.addRenderableWidget(this.fiyatGirdisi);

            this.btnFiyatOnayla = Button.builder(Component.translatable("modernlife.yazar_kasa.set_price"), button -> {
                String input = this.fiyatGirdisi.getValue().trim();
                if (input.isEmpty()) {
                    this.errorMessage = Component.translatable("modernlife.yazar_kasa.error_empty_price").getString();
                    return;
                }
                
                try {
                    int girilenFiyat = Integer.parseInt(input);
                    boolean isLightman = EconomyService.isLightmansActive(Minecraft.getInstance().player);
                    if (!isLightman && (girilenFiyat < 5 || girilenFiyat % 5 != 0)) {
                        this.errorMessage = Component.translatable("modernlife.yazar_kasa.error_multiple_of_5").getString();
                        return;
                    }
                    if (girilenFiyat > 0) {
                        ModNetwork.CHANNEL.sendToServer(new YazarKasaActionPacket(pos, 1, girilenFiyat));
                        this.onClose(); 
                    }
                } catch (NumberFormatException e) {
                    this.errorMessage = Component.translatable("modernlife.yazar_kasa.invalid_price").withStyle(ChatFormatting.RED).getString();
                }
            }).bounds(x + 30, y + 60, 116, 20).build();
            this.addRenderableWidget(this.btnFiyatOnayla);

            this.btnNakitAl = Button.builder(Component.translatable("modernlife.yazar_kasa.empty_register"), button -> {
                ModNetwork.CHANNEL.sendToServer(new YazarKasaActionPacket(pos, 4, 0));
                this.onClose();
            }).bounds(x + 20, y + 88, 136, 20).build();
            this.addRenderableWidget(this.btnNakitAl);
            
        } else {
            this.btnNakit = Button.builder(Component.translatable("modernlife.yazar_kasa.select_cash"), button -> {
                ModNetwork.CHANNEL.sendToServer(new YazarKasaActionPacket(pos, 2, 0));
                this.onClose(); 
            }).bounds(x + 15, y + 60, 68, 20).build();
            this.addRenderableWidget(this.btnNakit);

            this.btnKart = Button.builder(Component.translatable("modernlife.yazar_kasa.select_card"), button -> {
                ModNetwork.CHANNEL.sendToServer(new YazarKasaActionPacket(pos, 3, 0));
                this.onClose(); 
            }).bounds(x + 93, y + 60, 68, 20).build();
            this.addRenderableWidget(this.btnKart);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        boolean isOwner = false;
        UUID ownerId = this.menu.getBlockEntity().getOwnerUUID();
        if (ownerId != null && Minecraft.getInstance().player != null) {
            isOwner = Minecraft.getInstance().player.getUUID().equals(ownerId);
        }
        
        Component titleComponent = Component.translatable(isOwner ? "modernlife.yazar_kasa.owner_control" : "modernlife.yazar_kasa.select_payment");
        int titleWidth = this.font.width(titleComponent);
        guiGraphics.drawString(this.font, titleComponent, x + (this.imageWidth - titleWidth) / 2, y + 15, 4210752, false);
        
        if (!errorMessage.isEmpty()) {
            guiGraphics.drawString(this.font, errorMessage, x + (this.imageWidth - this.font.width(errorMessage)) / 2, y + 118, 0xAA0000, false);
        } else {
            int mevcutTutar = this.menu.getBlockEntity().getKalanTutar();
            Component formatliTutar = EconomyService.formatMoney(Minecraft.getInstance().player, mevcutTutar);
            Component amountDueComponent = Component.translatable("modernlife.yazar_kasa.amount_due", formatliTutar);
            int amountWidth = this.font.width(amountDueComponent);
            guiGraphics.drawString(this.font, amountDueComponent, x + (this.imageWidth - amountWidth) / 2, y + 120, 0x005500, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        guiGraphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF000000);
    }
}