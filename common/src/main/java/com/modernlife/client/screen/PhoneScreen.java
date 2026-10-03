package com.modernlife.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PhoneScreen extends Screen {
    
    private static final ResourceLocation BACKGROUND = new ResourceLocation("modernlife", "textures/gui/phone_background.png");
    private static final ResourceLocation MAP_ICON = new ResourceLocation("modernlife", "textures/gui/map_icon.png");
    private static final ResourceLocation BANK_ICON = new ResourceLocation("modernlife", "textures/gui/bank_icon.png");
    private static final ResourceLocation IDENTITY_ICON = new ResourceLocation("modernlife", "textures/gui/identity_icon.png");
    private static final ResourceLocation PHONE_ICON = new ResourceLocation("modernlife", "textures/gui/phone_icon.png");

    private final int imageWidth = 140;
    private final int imageHeight = 250;
    private int leftPos;
    private int topPos;

    public PhoneScreen() {
        super(Component.translatable("modernlife.phone.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        // 1. Harita
        this.addRenderableWidget(new ImageButton(
            this.leftPos + 22, this.topPos + 38, 
            36, 36, 0, 0, 0, MAP_ICON, 32, 32, 
            (btn) -> { if (this.minecraft != null) this.minecraft.setScreen(new MapScreen()); }
        ) {
            @Override
            public void renderWidget(GuiGraphics g, int mX, int mY, float pt) { renderAppIcon(this, g, MAP_ICON); }
        });

        // 2. Banka
        this.addRenderableWidget(new ImageButton(
            this.leftPos + 80, this.topPos + 38, 
            36, 36, 0, 0, 0, BANK_ICON, 32, 32, 
            (btn) -> { if (this.minecraft != null) this.minecraft.setScreen(new BankScreen()); }
        ) {
            @Override
            public void renderWidget(GuiGraphics g, int mX, int mY, float pt) { renderAppIcon(this, g, BANK_ICON); }
        });

        // 3. Kimlik
        this.addRenderableWidget(new ImageButton(
            this.leftPos + 22, this.topPos + 88, 
            36, 36, 0, 0, 0, IDENTITY_ICON, 32, 32, 
            (btn) -> { if (this.minecraft != null) this.minecraft.setScreen(new IdentityManagerScreen()); }
        ) {
            @Override
            public void renderWidget(GuiGraphics g, int mX, int mY, float pt) { renderAppIcon(this, g, IDENTITY_ICON); }
        });

        // 4. TELEFON / REHBER UYGULAMASI
        this.addRenderableWidget(new ImageButton(
            this.leftPos + 80, this.topPos + 88, 
            36, 36, 0, 0, 0, PHONE_ICON, 32, 32, 
            (btn) -> { if (this.minecraft != null) this.minecraft.setScreen(new PhoneAppScreen()); }
        ) {
            @Override
            public void renderWidget(GuiGraphics g, int mX, int mY, float pt) { renderAppIcon(this, g, PHONE_ICON); }
        });
    }

    private void renderAppIcon(ImageButton btn, GuiGraphics guiGraphics, ResourceLocation iconTexture) {
        int borderColor = btn.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF555555;
        guiGraphics.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), 0x44000000);
        guiGraphics.renderOutline(btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight(), borderColor);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(iconTexture, btn.getX() + 2, btn.getY() + 2, 0, 0, 32, 32, 32, 32);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}