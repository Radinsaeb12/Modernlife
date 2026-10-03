package com.modernlife.client.screen;

import com.modernlife.data.world.ModernLifeWorldData.DifficultyLevel;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.SetDifficultyPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DifficultySelectScreen extends Screen {

    public DifficultySelectScreen() {
        super(Component.translatable("modernlife.gui.difficulty.title"));
    }

    @Override
    protected void init() {
        super.init();

        int cx = this.width / 2;
        int cy = this.height / 2;
        int btnWidth = 190;
        int btnHeight = 20;
        int startY = cy - 50;

        this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.difficulty.very_easy"), b -> selectDifficulty(DifficultyLevel.VERY_EASY))
                .bounds(cx - btnWidth / 2, startY, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.difficulty.easy"), b -> selectDifficulty(DifficultyLevel.EASY))
                .bounds(cx - btnWidth / 2, startY + 24, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.difficulty.normal"), b -> selectDifficulty(DifficultyLevel.NORMAL))
                .bounds(cx - btnWidth / 2, startY + 48, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.difficulty.hard"), b -> selectDifficulty(DifficultyLevel.HARD))
                .bounds(cx - btnWidth / 2, startY + 72, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.difficulty.very_hard"), b -> selectDifficulty(DifficultyLevel.VERY_HARD))
                .bounds(cx - btnWidth / 2, startY + 96, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("modernlife.gui.difficulty.impossible"), b -> selectDifficulty(DifficultyLevel.IMPOSSIBLE))
                .bounds(cx - btnWidth / 2, startY + 120, btnWidth, btnHeight).build());
    }

    private void selectDifficulty(DifficultyLevel level) {
        ModNetwork.CHANNEL.sendToServer(new SetDifficultyPacket(level));
        this.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // ESC ile kapatmayı yasaklar
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gui);

        gui.fill(this.width / 2 - 115, this.height / 2 - 105, this.width / 2 + 115, this.height / 2 + 105, 0xEE111111);

        gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.difficulty.header"), this.width / 2, this.height / 2 - 95, 0xFFAA00);
        gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.difficulty.sub_header"), this.width / 2, this.height / 2 - 80, 0xFFFFFF);
        gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.difficulty.desc"), this.width / 2, this.height / 2 - 68, 0xAAAAAA);

        super.render(gui, mouseX, mouseY, partialTicks);
    }
}