package com.modernlife.gui;

import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.IdentityRegisterPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IdentityRegisterScreen extends Screen {
    private EditBox nameField;
    private EditBox birthYearField;

    public IdentityRegisterScreen() {
        super(Component.translatable("modernlife.gui.identity_register.title"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.nameField = new EditBox(this.font, centerX - 100, centerY - 40, 200, 20, Component.literal(""));
        this.nameField.setMaxLength(32);
        this.addWidget(this.nameField);

        this.birthYearField = new EditBox(this.font, centerX - 100, centerY, 200, 20, Component.literal(""));
        this.birthYearField.setMaxLength(4);
        this.addWidget(this.birthYearField);

        this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.identity_register.submit"), (button) -> {
            String inputName = this.nameField.getValue().trim();
            String inputYearStr = this.birthYearField.getValue().trim();
            
            if (!inputName.isEmpty() && !inputYearStr.isEmpty()) {
                try {
                    int inputYear = Integer.parseInt(inputYearStr);
                    if (inputYear >= 1000 && inputYear <= 9999) {
                        ModNetwork.CHANNEL.sendToServer(new IdentityRegisterPacket(inputName, inputYear));
                        this.onClose();
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }).bounds(centerX - 100, centerY + 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        guiGraphics.drawString(this.font, Component.translatable("modernlife.gui.identity_register.name_label"), centerX - 100, centerY - 52, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("modernlife.gui.identity_register.year_label"), centerX - 100, centerY - 12, 0xFFFFFF);
        
        this.nameField.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.birthYearField.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}