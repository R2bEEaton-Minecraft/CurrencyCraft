package cc.spea.currencycraft.gui.ATM;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.network.ModMessages;
import cc.spea.currencycraft.network.packets.C2SSetupDebitCard;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for setting up a blank debit card with a 4-digit PIN.
 */
public class ATMSetupScreen extends AbstractContainerScreen<ATMSetupMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, "textures/gui/atm_setup.png");

    private EditBox pinInput;
    private Button confirmButton;

    public ATMSetupScreen(ATMSetupMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 140;
        this.imageHeight = 87;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // PIN input box
        this.pinInput = new EditBox(this.font, x + 20, y + 32, 100, 20, Component.translatable("text.currencycraft.atm.pin"));
        this.pinInput.setMaxLength(4);
        this.pinInput.setFilter(s -> s.matches("\\d*")); // Only digits
        this.addRenderableWidget(this.pinInput);

        // Confirm button
        this.confirmButton = Button.builder(
                Component.translatable("text.currencycraft.atm.confirm"),
                button -> this.onConfirm()
        ).bounds(x + 20, y + 57, 100, 20).build();
        this.addRenderableWidget(this.confirmButton);

        // Set focus to PIN input
        this.setInitialFocus(this.pinInput);
    }

    private void onConfirm() {
        String pin = this.pinInput.getValue();
        if (pin.length() == 4) {
            ModMessages.sendToServer(new C2SSetupDebitCard(pin));
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
        gui.drawString(this.font, Component.translatable("text.currencycraft.atm.setup_title"),
                8, 6, 0x404040, false);
        gui.drawString(this.font, Component.translatable("text.currencycraft.atm.enter_pin"),
                8, 18, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Allow Enter key to confirm
        if (keyCode == 257 || keyCode == 335) { // Enter or Numpad Enter
            if (this.pinInput.getValue().length() == 4) {
                this.onConfirm();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        String pinValue = this.pinInput.getValue();
        super.resize(minecraft, width, height);
        this.pinInput.setValue(pinValue);
    }
}
