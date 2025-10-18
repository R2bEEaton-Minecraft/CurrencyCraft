package cc.spea.currencycraft.gui.ATM;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.network.ModMessages;
import cc.spea.currencycraft.network.packets.C2SDisableDebitCard;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Screen for fingerprint authentication to disable a debit card.
 */
public class ATMFingerprintScreen extends AbstractContainerScreen<ATMFingerprintMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, "textures/gui/atm_fingerprint.png");

    private Button disableButton;
    private Player player;

    public ATMFingerprintScreen(ATMFingerprintMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Disable card button
        this.disableButton = Button.builder(
                Component.translatable("text.currencycraft.atm.disable_card"),
                button -> this.onDisable()
        ).bounds(x + 38, y + 50, 100, 20).build();
        this.addRenderableWidget(this.disableButton);
    }

    private void onDisable() {
        ModMessages.sendToServer(new C2SDisableDebitCard());
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
        gui.drawString(this.font, Component.translatable("text.currencycraft.atm.fingerprint_title"),
                8, 6, 0x404040, false);
        gui.drawString(this.font, Component.literal(player.getName().getString()),
                46, 35, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
        this.renderTooltip(gui, mouseX, mouseY);
    }
}
