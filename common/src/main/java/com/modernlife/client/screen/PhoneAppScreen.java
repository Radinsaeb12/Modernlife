package com.modernlife.client.screen;

import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.C2SPhoneCallPacket;
import com.modernlife.network.packet.S2CPhoneCallStatePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.UUID;

public class PhoneAppScreen extends Screen {

    private static final ResourceLocation BACKGROUND = new ResourceLocation("modernlife", "textures/gui/phone_background.png");

    public static S2CPhoneCallStatePacket.CallState currentCallState = S2CPhoneCallStatePacket.CallState.IDLE;
    public static String activePartnerName = "";
    public static UUID activePartnerUUID = null;

    private final int imageWidth = 140;
    private final int imageHeight = 250;
    private int leftPos;
    private int topPos;

    public PhoneAppScreen() {
        super(Component.translatable("modernlife.phone.directory"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.rebuildPhoneUI();
    }

    public void rebuildPhoneUI() {
        this.clearWidgets();

        if (currentCallState == S2CPhoneCallStatePacket.CallState.INCOMING) {
            this.addRenderableWidget(Button.builder(Component.translatable("modernlife.phone.btn.accept"), (btn) -> {
                ModNetwork.CHANNEL.sendToServer(new C2SPhoneCallPacket(C2SPhoneCallPacket.Action.ACCEPT, ""));
            }).bounds(this.leftPos + 15, this.topPos + 185, 110, 20).build());

            this.addRenderableWidget(Button.builder(Component.translatable("modernlife.phone.btn.decline"), (btn) -> {
                ModNetwork.CHANNEL.sendToServer(new C2SPhoneCallPacket(C2SPhoneCallPacket.Action.DECLINE, ""));
                currentCallState = S2CPhoneCallStatePacket.CallState.IDLE;
                rebuildPhoneUI();
            }).bounds(this.leftPos + 15, this.topPos + 210, 110, 20).build());

        } else if (currentCallState == S2CPhoneCallStatePacket.CallState.CONNECTED) {
            this.addRenderableWidget(Button.builder(Component.translatable("modernlife.phone.btn.end"), (btn) -> {
                ModNetwork.CHANNEL.sendToServer(new C2SPhoneCallPacket(C2SPhoneCallPacket.Action.DECLINE, ""));
                currentCallState = S2CPhoneCallStatePacket.CallState.IDLE;
                rebuildPhoneUI();
            }).bounds(this.leftPos + 15, this.topPos + 200, 110, 22).build());

        } else {
            if (this.minecraft == null || this.minecraft.getConnection() == null) return;
            Collection<PlayerInfo> players = this.minecraft.getConnection().getOnlinePlayers();
            int yOffset = this.topPos + 40;

            for (PlayerInfo player : players) {
                if (this.minecraft.player != null && player.getProfile().getName().equals(this.minecraft.player.getScoreboardName())) {
                    continue;
                }

                String playerName = player.getProfile().getName();
                this.addRenderableWidget(Button.builder(Component.translatable("modernlife.phone.call_btn"), (btn) -> {
                    ModNetwork.CHANNEL.sendToServer(new C2SPhoneCallPacket(C2SPhoneCallPacket.Action.CALL, playerName));
                }).bounds(this.leftPos + 85, yOffset, 40, 16).build());

                yOffset += 22;
                if (yOffset > this.topPos + 180) break;
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        if (currentCallState == S2CPhoneCallStatePacket.CallState.INCOMING) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("modernlife.phone.incoming_title"), this.leftPos + (this.imageWidth / 2), this.topPos + 25, 0xFFFFFF);
            ResourceLocation skin = getSkinOfPlayer(activePartnerUUID);
            PlayerFaceRenderer.draw(guiGraphics, skin, this.leftPos + (this.imageWidth / 2) - 20, this.topPos + 55, 40);
            guiGraphics.drawCenteredString(this.font, "§f§l" + activePartnerName, this.leftPos + (this.imageWidth / 2), this.topPos + 105, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, Component.translatable("modernlife.phone.calling_desc"), this.leftPos + (this.imageWidth / 2), this.topPos + 120, 0xAAAAAA);

        } else if (currentCallState == S2CPhoneCallStatePacket.CallState.CONNECTED) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("modernlife.phone.connected_title"), this.leftPos + (this.imageWidth / 2), this.topPos + 25, 0xFFFFFF);
            ResourceLocation skin = getSkinOfPlayer(activePartnerUUID);
            PlayerFaceRenderer.draw(guiGraphics, skin, this.leftPos + (this.imageWidth / 2) - 20, this.topPos + 55, 40);
            guiGraphics.drawCenteredString(this.font, "§f§l" + activePartnerName, this.leftPos + (this.imageWidth / 2), this.topPos + 105, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, Component.translatable("modernlife.phone.connected_status"), this.leftPos + (this.imageWidth / 2), this.topPos + 125, 0x55FF55);

        } else {
            guiGraphics.drawCenteredString(this.font, Component.translatable("modernlife.phone.directory"), this.leftPos + (this.imageWidth / 2), this.topPos + 15, 0x000000);
            if (this.minecraft != null && this.minecraft.getConnection() != null) {
                Collection<PlayerInfo> players = this.minecraft.getConnection().getOnlinePlayers();
                int yOffset = this.topPos + 40;

                for (PlayerInfo player : players) {
                    if (this.minecraft.player != null && player.getProfile().getName().equals(this.minecraft.player.getScoreboardName())) {
                        continue;
                    }
                    ResourceLocation skin = player.getSkinLocation();
                    PlayerFaceRenderer.draw(guiGraphics, skin, this.leftPos + 15, yOffset, 16);
                    guiGraphics.drawString(this.font, player.getProfile().getName(), this.leftPos + 35, yOffset + 4, 0xFFFFFF);
                    yOffset += 22;
                    if (yOffset > this.topPos + 180) break;
                }
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private ResourceLocation getSkinOfPlayer(UUID uuid) {
        if (uuid != null && this.minecraft != null && this.minecraft.getConnection() != null) {
            PlayerInfo info = this.minecraft.getConnection().getPlayerInfo(uuid);
            if (info != null) {
                return info.getSkinLocation();
            }
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}