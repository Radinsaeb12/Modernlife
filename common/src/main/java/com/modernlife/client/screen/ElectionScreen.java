package com.modernlife.client.screen;

import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.ElectionActionPacket;
import com.modernlife.network.packet.OpenElectionMenuPacket;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;

import java.util.List;

public class ElectionScreen extends Screen {
    private final boolean isElectionActive;
    private final boolean isAdmin;
    private final List<OpenElectionMenuPacket.Candidate> candidates;
    
    private int currentPage = 0;
    private final int itemsPerPage = 4;

    public ElectionScreen(boolean isElectionActive, boolean isAdmin, List<OpenElectionMenuPacket.Candidate> candidates) {
        super(Component.translatable("modernlife.election.title"));
        this.isElectionActive = isElectionActive;
        this.isAdmin = isAdmin;
        this.candidates = candidates;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int centerX = this.width / 2;

        if (isAdmin) {
            this.addRenderableWidget(Button.builder(Component.translatable("modernlife.election.start_btn").withStyle(ChatFormatting.GREEN), b -> {
                ModNetwork.CHANNEL.sendToServer(new ElectionActionPacket(0, null));
                this.onClose();
            }).bounds(centerX - 125, 20, 120, 20).build());

            this.addRenderableWidget(Button.builder(Component.translatable("modernlife.election.stop_btn").withStyle(ChatFormatting.RED), b -> {
                ModNetwork.CHANNEL.sendToServer(new ElectionActionPacket(1, null));
                this.onClose();
            }).bounds(centerX + 5, 20, 120, 20).build());
        }

        int startX = centerX - 150;
        int startY = isAdmin ? 55 : 40;
        
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, candidates.size());

        for (int i = startIndex; i < endIndex; i++) {
            OpenElectionMenuPacket.Candidate candidate = candidates.get(i);
            int rowY = startY + ((i - startIndex) * 35);

            Button voteButton = Button.builder(Component.translatable("modernlife.election.vote_btn"), b -> {
                ModNetwork.CHANNEL.sendToServer(new ElectionActionPacket(2, candidate.uuid));
            }).bounds(startX + 220, rowY + 4, 70, 20).build();
            
            voteButton.active = isElectionActive;
            this.addRenderableWidget(voteButton);
        }

        int footerY = startY + (itemsPerPage * 35) + 10;
        
        Button prevButton = Button.builder(Component.literal("<"), b -> {
            if (currentPage > 0) {
                currentPage--;
                this.init(this.minecraft, this.width, this.height);
            }
        }).bounds(centerX - 50, footerY, 20, 20).build();
        prevButton.active = currentPage > 0;
        this.addRenderableWidget(prevButton);

        Button nextButton = Button.builder(Component.literal(">"), b -> {
            if ((currentPage + 1) * itemsPerPage < candidates.size()) {
                currentPage++;
                this.init(this.minecraft, this.width, this.height);
            }
        }).bounds(centerX + 30, footerY, 20, 20).build();
        nextButton.active = (currentPage + 1) * itemsPerPage < candidates.size();
        this.addRenderableWidget(nextButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        int centerX = this.width / 2;
        int startX = centerX - 150;
        int startY = isAdmin ? 55 : 40;

        MutableComponent status = isElectionActive 
            ? Component.translatable("modernlife.election.status_open").withStyle(ChatFormatting.GREEN) 
            : Component.translatable("modernlife.election.status_closed").withStyle(ChatFormatting.RED);
            
        MutableComponent titleFull = Component.translatable("modernlife.election.title").withStyle(ChatFormatting.WHITE).append(" ").append(status);
        
        guiGraphics.drawCenteredString(this.font, titleFull, centerX, isAdmin ? 6 : 15, 0xFFFFFF);

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, candidates.size());

        for (int i = startIndex; i < endIndex; i++) {
            OpenElectionMenuPacket.Candidate candidate = candidates.get(i);
            int rowY = startY + ((i - startIndex) * 35);

            guiGraphics.fill(startX, rowY, startX + 300, rowY + 30, 0x55000000);
            
            GameProfile profile = new GameProfile(candidate.uuid, candidate.name);
            ResourceLocation skinLocation = Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(profile);

            if (skinLocation == null) {
                skinLocation = DefaultPlayerSkin.getDefaultSkin(candidate.uuid);
            }

            guiGraphics.blit(skinLocation, startX + 5, rowY + 3, 24, 24, 8.0F, 8.0F, 8, 8, 64, 64);
            guiGraphics.drawString(this.font, candidate.name, startX + 35, rowY + 11, 0xFFAA00);
        }

        int totalPages = (int) Math.ceil((double) candidates.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1; 
        guiGraphics.drawCenteredString(this.font, Component.literal((currentPage + 1) + " / " + totalPages), centerX, startY + (itemsPerPage * 35) + 16, 0xAAAAAA);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}