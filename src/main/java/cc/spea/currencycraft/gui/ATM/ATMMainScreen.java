package cc.spea.currencycraft.gui.ATM;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.network.ModMessages;
import cc.spea.currencycraft.network.packets.C2SATMDeposit;
import cc.spea.currencycraft.network.packets.C2SATMWithdraw;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Screen for the main ATM interface showing balance, deposit slot, and withdrawal.
 */
public class ATMMainScreen extends AbstractContainerScreen<ATMMainMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, "textures/gui/atm_main.png");

    private EditBox withdrawalInput;
    private Button depositButton;
    private Button withdrawButton;

    public ATMMainScreen(ATMMainMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Withdrawal amount input
        this.withdrawalInput = new EditBox(this.font, x + 10, y + 54, 80, 15, Component.translatable("text.currencycraft.atm.amount"));
        this.withdrawalInput.setMaxLength(10);
        this.withdrawalInput.setFilter(s -> s.matches("\\d*\\.?\\d{0,2}")); // Allow decimal for currency
        this.addRenderableWidget(this.withdrawalInput);

        // Deposit button
        this.depositButton = Button.builder(
                Component.translatable("text.currencycraft.atm.deposit"),
                button -> this.onDeposit()
        ).bounds(x + 95, y + 18, 70, 15).build();
        this.addRenderableWidget(this.depositButton);

        // Withdraw button
        this.withdrawButton = Button.builder(
                Component.translatable("text.currencycraft.atm.withdraw"),
                button -> this.onWithdraw()
        ).bounds(x + 95, y + 53, 70, 15).build();
        this.addRenderableWidget(this.withdrawButton);
    }

    private void onDeposit() {
        ModMessages.sendToServer(new C2SATMDeposit(ATMMainMenu.DEPOSIT_SLOT_INDEX));
    }

    private void onWithdraw() {
        try {
            String amountStr = this.withdrawalInput.getValue();
            if (!amountStr.isEmpty()) {
                // Use BigDecimal to avoid floating-point rounding errors (e.g. 2.3 * 100 -> 229.999...)
                BigDecimal amountBd = new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
                long amountCents = amountBd.multiply(BigDecimal.valueOf(100)).longValue();

                if (amountCents > 0) {
                    ModMessages.sendToServer(new C2SATMWithdraw(amountCents));
                    this.withdrawalInput.setValue("");
                }
            }
        } catch (NumberFormatException e) {
            // Invalid input, ignore
        }
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, 8, 6, 0x404040, false);

        // Display balance
        long balance = this.menu.getBalance();
        String balanceStr = String.format("Balance: $%.2f", balance / 100.0);
        gui.drawString(this.font, balanceStr, 10, 41, 0x00AA00, false);

        // Labels
        gui.drawString(this.font, Component.translatable("text.currencycraft.atm.deposit_slot"), 10, 18, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        String withdrawValue = this.withdrawalInput.getValue();
        super.resize(minecraft, width, height);
        this.withdrawalInput.setValue(withdrawValue);
    }
}
