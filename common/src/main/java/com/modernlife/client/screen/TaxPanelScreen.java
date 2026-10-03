package com.modernlife.client.screen;

import com.modernlife.menu.TaxPanelMenu;
import com.modernlife.network.ModNetwork;
import com.modernlife.network.packet.SetTaxRatesPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TaxPanelScreen extends AbstractContainerScreen<TaxPanelMenu> {
    private EditBox vehicleField;
    private EditBox mountField;
    private EditBox livingField;
    private EditBox penaltyField;
    private EditBox unlicensedPenaltyField;
    private EditBox unlicensedMountPenaltyField;

    public TaxPanelScreen(final TaxPanelMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = 220;
        this.imageHeight = 300;
    }

    @Override
    protected void init() {
        super.init();
        final int left = this.leftPos + 20;
        int rowTop = this.topPos + 24;

        this.vehicleField = createField(left, rowTop + 12, Long.toString(menu.getInitialVehicleTax()), "\\d{0,12}");
        rowTop += 38;
        this.mountField = createField(left, rowTop + 12, Long.toString(menu.getInitialMountTax()), "\\d{0,12}");
        rowTop += 38;
        this.livingField = createField(left, rowTop + 12, Long.toString(menu.getInitialLivingTax()), "\\d{0,12}");
        rowTop += 38;
        this.penaltyField = createField(left, rowTop + 12, formatDouble(menu.getInitialPenaltyRate() * 100.0), "[0-9]+(\\.[0-9]{0,2})?");
        rowTop += 38;
        this.unlicensedPenaltyField = createField(left, rowTop + 12, Long.toString(menu.getInitialUnlicensedPenalty()), "\\d{0,12}");
        rowTop += 38;
        this.unlicensedMountPenaltyField = createField(left, rowTop + 12, Long.toString(menu.getInitialUnlicensedMountPenalty()), "\\d{0,12}");

        addRenderableWidget(this.vehicleField);
        addRenderableWidget(this.mountField);
        addRenderableWidget(this.livingField);
        addRenderableWidget(this.penaltyField);
        addRenderableWidget(this.unlicensedPenaltyField);
        addRenderableWidget(this.unlicensedMountPenaltyField);

        addRenderableWidget(Button.builder(
                Component.translatable("modernlife.tax_panel.apply"),
                button -> applyTaxRates()
        ).bounds(this.leftPos + 50, this.topPos + 265, 120, 20).build());
    }

    private EditBox createField(final int x, final int y, final String initialValue, String regex) {
        final EditBox field = new EditBox(this.font, x, y, 180, 18, Component.empty());
        field.setValue(initialValue);
        field.setFilter(text -> text.isEmpty() || text.matches(regex));
        field.setMaxLength(12);
        return field;
    }

    private static String formatDouble(double value) {
        DecimalFormat df = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(value);
    }

    private static long roundUpTo5(long value) {
        if (value <= 0L) return 0L;
        long rem = value % 5L;
        return rem == 0L ? value : value + (5L - rem);
    }

    private void applyTaxRates() {
        ModNetwork.CHANNEL.sendToServer(new SetTaxRatesPacket(
                roundUpTo5(parseAmount(vehicleField)),
                roundUpTo5(parseAmount(mountField)),
                roundUpTo5(parseAmount(livingField)),
                parsePenalty(penaltyField),
                roundUpTo5(parseAmount(unlicensedPenaltyField)),
                roundUpTo5(parseAmount(unlicensedMountPenaltyField))
        ));
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
    }

    private static long parseAmount(final EditBox field) {
        try {
            return field.getValue().isBlank() ? 0L : Long.parseLong(field.getValue());
        } catch (final NumberFormatException exception) {
            return 0L;
        }
    }

    private static double parsePenalty(final EditBox field) {
        try {
            String val = field.getValue().trim();
            if (val.isBlank() || val.equals(".")) return 0.0;
            return Double.parseDouble(val) / 100.0;
        } catch (final NumberFormatException exception) {
            return 0.0;
        }
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY) {
        renderBackground(graphics);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xCC101010);
        graphics.fill(this.leftPos + 6, this.topPos + 6, this.leftPos + this.imageWidth - 6, this.topPos + this.imageHeight - 6, 0xFF2B2B2B);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        graphics.drawString(this.font, this.title, 12, 8, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("modernlife.tax_panel.vehicle"), 20, 22, 0xCFCFCF, false);
        graphics.drawString(this.font, Component.translatable("modernlife.tax_panel.mount"), 20, 60, 0xCFCFCF, false);
        graphics.drawString(this.font, Component.translatable("modernlife.tax_panel.living"), 20, 98, 0xCFCFCF, false);
        graphics.drawString(this.font, Component.translatable("modernlife.tax_panel.penalty_rate"), 20, 136, 0xFF5555, false);
        graphics.drawString(this.font, Component.translatable("modernlife.tax_panel.unlicensed_penalty"), 20, 174, 0xFF5555, false);
        graphics.drawString(this.font, Component.translatable("modernlife.tax_panel.unlicensed_mount_penalty"), 20, 212, 0xFF5555, false);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}