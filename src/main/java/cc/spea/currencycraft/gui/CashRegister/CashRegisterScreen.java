package cc.spea.currencycraft.gui.CashRegister;

import cc.spea.currencycraft.CurrencyCraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CashRegisterScreen extends AbstractContainerScreen<CashRegisterMenu> {
    // Define the location of your GUI background texture
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, "textures/gui/cash_register.png");

    public CashRegisterScreen(CashRegisterMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        // Adjust these values to match your GUI texture size. Double chests are typically 222px high.
        this.imageHeight = 198;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Draw the background GUI texture
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        // --- Render Placeholders ---
        renderPlaceholders(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw the main inventory title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        
        // Draw the player inventory title
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // --- NEW CODE FOR DISPLAYING THE TOTAL ---
        drawTotalValue(graphics);
    }

    private void renderPlaceholders(GuiGraphics graphics) {
        for (int i = 0; i < this.menu.slots.size(); ++i) {
            // Check only the slots belonging to our container (not the player inventory)
            if (i < this.menu.getContainer().getContainerSize()) {
                var slot = this.menu.getSlot(i);
                
                // Check if the slot is our custom type and is empty
                if (slot instanceof PlaceholderSlot pSlot && !pSlot.hasItem()) {
                    ResourceLocation placeholderTexture = pSlot.getPlaceholder();
                    
                    if (placeholderTexture != null) {
                        // Set blend mode for transparency in PNGs
                        RenderSystem.enableBlend();
                        // Draw the placeholder texture (16x16) at the slot's position
                        graphics.blit(placeholderTexture, this.leftPos + pSlot.x, this.topPos + pSlot.y, 0, 0, 16, 16, 16, 16);
                        RenderSystem.disableBlend();
                    }
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render the background and placeholders first
        this.renderBg(graphics, partialTick, mouseX, mouseY);
        // Then let the default render handle items, tooltips, etc.
        super.render(graphics, mouseX, mouseY, partialTick);
        // Render tooltips on top of everything
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        // You can remove these if your background texture already has the labels
        this.titleLabelY = 6; 
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private void drawTotalValue(GuiGraphics graphics) {
        long totalCents = this.menu.getTotalValueInCents();
        double total = totalCents / 100.0;
        String formattedTotal = String.format("%.2f", total);
        Component totalComponent = Component.translatable("gui.currencycraft.cash_register.total", formattedTotal);

        int x = this.titleLabelX;
        int y = this.titleLabelY + 12;
        
        graphics.drawString(this.font, totalComponent, x, y, 4210752, false);
    }
}