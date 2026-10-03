package com.modernlife.client.screen;

import com.modernlife.capability.ModCapabilities;
import com.modernlife.capability.PlayerEconomy;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.DeleteIdentityPacket;
import com.modernlife.service.EconomyService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class IdentityManagerScreen extends Screen {

    private final List<String> identityList = new ArrayList<>();
    private int birthYear = 0;

    public IdentityManagerScreen() {
        super(Component.translatable("modernlife.gui.identity_portal.title"));
    }

    @Override
    protected void init() {
        super.init();
        identityList.clear();

        if (Minecraft.getInstance().player != null) {
            var player = Minecraft.getInstance().player;

            List<String> registered = EconomyService.getRegisteredIdentities(player);
            for (String id : registered) {
                if (EconomyService.isTreasury(id)) continue;
                if (!identityList.contains(id)) identityList.add(id);
            }

            for (String cardId : EconomyService.getIdentitiesFromCards(player)) {
                if (EconomyService.isTreasury(cardId)) continue;
                if (!identityList.contains(cardId)) identityList.add(cardId);
            }

            PlayerEconomy eco = ModCapabilities.get(player);
            this.birthYear = eco.getBirthYear();
        }

        int centerX = this.width / 2;
        int startY = (this.height / 2) - 50;

        for (int i = 0; i < Math.min(identityList.size(), 4); i++) {
            final String idName = identityList.get(i);
            int rowY = startY + (i * 24);

            this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.identity_portal.delete_btn"), (btn) -> {
                ModNetwork.CHANNEL.sendToServer(new DeleteIdentityPacket(idName));
                this.onClose();
            }).bounds(centerX + 40, rowY, 45, 20).build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.identity_portal.close_btn"), (btn) -> {
            this.onClose();
        }).bounds(centerX - 50, (this.height / 2) + 65, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.fill(centerX - 120, centerY - 85, centerX + 120, centerY + 95, 0xEE1A1A1A);
        guiGraphics.renderOutline(centerX - 120, centerY - 85, 240, 180, 0xFF3498DB);

        guiGraphics.drawCenteredString(this.font, Component.translatable("modernlife.gui.identity_portal.header"), centerX, centerY - 72, 0xF1C40F);

        if (identityList.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("modernlife.gui.identity_portal.not_found_line1"), centerX, centerY - 20, 0xE74C3C);
            guiGraphics.drawCenteredString(this.font, Component.translatable("modernlife.gui.identity_portal.not_found_line2"), centerX, centerY - 5, 0xE74C3C);
        } else {
            int startY = centerY - 50;
            for (int i = 0; i < Math.min(identityList.size(), 4); i++) {
                String idName = identityList.get(i);
                int rowY = startY + (i * 24) + 6;
                guiGraphics.drawString(this.font, (i + 1) + ". " + idName, centerX - 105, rowY, 0xFFFFFF);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}