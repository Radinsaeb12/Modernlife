package com.modernlife.client.screen;

import com.modernlife.data.world.ModernLifeWorldData.EconomyMode;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.SetEconomyModePacket;
import dev.architectury.platform.Platform;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EconomySelectScreen extends Screen {

    public EconomySelectScreen() {
        super(Component.translatable("modernlife.gui.economy_select.title"));
    }

    @Override
    protected void init() {
        super.init();

        int cx = this.width / 2;
        int cy = this.height / 2;
        int btnWidth = 220;
        int btnHeight = 24;

        this.addRenderableWidget(Button.builder(
                Component.translatable("modernlife.gui.economy_select.native_btn"),
                b -> selectMode(EconomyMode.MODERNLIFE_NATIVE)
        ).bounds(cx - btnWidth / 2, cy - 25, btnWidth, btnHeight).build());

        boolean isLightmansLoaded = Platform.isModLoaded("lightmanscurrency");
        Button lightmanBtn = Button.builder(
                Component.translatable("modernlife.gui.economy_select.lightmans_btn"),
                b -> selectMode(EconomyMode.LIGHTMANS_CURRENCY)
        ).bounds(cx - btnWidth / 2, cy + 15, btnWidth, btnHeight).build();

        lightmanBtn.active = isLightmansLoaded;
        this.addRenderableWidget(lightmanBtn);
    }

    private void selectMode(EconomyMode mode) {
        ModNetwork.CHANNEL.sendToServer(new SetEconomyModePacket(mode));
        this.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gui);

        gui.fill(this.width / 2 - 130, this.height / 2 - 95, this.width / 2 + 130, this.height / 2 + 85, 0xEE111111);

        gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.economy_select.header"), this.width / 2, this.height / 2 - 80, 0xFFAA00);
        gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.economy_select.sub_header"), this.width / 2, this.height / 2 - 65, 0xFFFFFF);
        gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.economy_select.native_desc"), this.width / 2, this.height / 2 - 40, 0x888888);

        if (!Platform.isModLoaded("lightmanscurrency")) {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.economy_select.lightmans_not_found"), this.width / 2, this.height / 2 + 45, 0xFF5555);
        } else {
            gui.drawCenteredString(this.font, Component.translatable("modernlife.gui.economy_select.lightmans_desc"), this.width / 2, this.height / 2 + 45, 0x888888);
        }

        super.render(gui, mouseX, mouseY, partialTicks);
    }
}