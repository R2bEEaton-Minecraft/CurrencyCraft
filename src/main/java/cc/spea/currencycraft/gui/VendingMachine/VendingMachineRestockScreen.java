// VendingMachineRestockScreen

package cc.spea.currencycraft.gui.VendingMachine;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.blocks.VendingMachine.VendingMachineBlockEntity;
import cc.spea.currencycraft.network.ModMessages; // Example import for your network handler
import cc.spea.currencycraft.network.packets.C2SSetVendingPricePacket; // Example import for your packet
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class VendingMachineRestockScreen extends AbstractContainerScreen<VendingMachineRestockMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, "textures/gui/vending_machine.png");

    private EditBox priceField;
    private Button saveButton;

    // State for which slot is being edited
    private int selectedSlot = -1;
    private int hoveredSlot = -1;
    
    private Button[] priceButtons;

    private final VendingMachineBlockEntity blockEntity;


    public VendingMachineRestockScreen(VendingMachineRestockMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 230;
        this.imageHeight = 216;
        this.inventoryLabelY = this.imageHeight - 94;
        this.blockEntity = menu.getBlockEntity();

        this.priceButtons = new Button[menu.getBlockEntity().getProductSlots()];
    }

    @Override
    protected void init() {
        super.init();

        // Text box and Save button for editing prices (initially hidden)
        int priceFieldX = this.leftPos + 8;
        int priceFieldY = this.topPos + 106;
        this.priceField = new EditBox(this.font, priceFieldX, priceFieldY, 52, 12, Component.translatable("gui.currencycraft.price"));
        this.priceField.setVisible(false);
        this.addRenderableWidget(this.priceField);

        this.saveButton = Button.builder(Component.translatable("gui.currencycraft.vending_machine.save"), button -> savePrice())
            .bounds(priceFieldX + 55, priceFieldY - 1, 40, 14).build();
        this.saveButton.visible = false;
        this.addRenderableWidget(this.saveButton);

        // Create the 3x4 grid of buttons for setting prices
        int buttonGridX = this.leftPos + 7;
        int buttonGridY = this.topPos + 29;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                final int slotIndex = row * 3 + col;
                int x = buttonGridX + col * 18; // Spacing them out a bit
                int y = buttonGridY + row * 18;
                
                long priceInCents = this.blockEntity.getPriceInCents(slotIndex);

                Tooltip priceTooltip = generateTooltip(priceInCents);

                Button priceButton = Button.builder(Component.translatable("gui.currencycraft.vending_machine.price_button"), button -> selectSlotForPricing(slotIndex))
                    .bounds(x + 1, y + 1, 16, 16)
                    .tooltip(priceTooltip)
                    .build();
                priceButtons[slotIndex] = priceButton;
                this.addRenderableWidget(priceButton);
            }
        }
    }

    private void selectSlotForPricing(int slotIndex) {
        this.selectedSlot = slotIndex;
        
        // Show the editor widgets
        this.priceField.setVisible(true);
        this.saveButton.visible = true;
        
        // Get the current price from the block entity and format it
        long currentPrice = this.blockEntity.getPriceInCents(slotIndex);
        if (currentPrice > 0) {
            BigDecimal priceDecimal = BigDecimal.valueOf(currentPrice).divide(new BigDecimal("100"));
            this.priceField.setValue(priceDecimal.toPlainString());
        } else {
            this.priceField.setValue("");
        }

        this.priceField.setTextColor(0xE0E0E0); // Reset color
        this.setFocused(this.priceField); // Focus the text box for typing
    }

    private void savePrice() {
        if (this.selectedSlot == -1) return;

        String text = this.priceField.getValue();
        long priceInCents;

        try {
            if (text.trim().isEmpty()) {
                priceInCents = 0; // Setting an empty price means it's free
            } else {
                // Use BigDecimal for accurate currency calculations
                BigDecimal priceDecimal = new BigDecimal(text);
                if (priceDecimal.signum() < 0) { // Check for negative price
                    throw new NumberFormatException("Price cannot be negative.");
                }
                
                // Ensure there are no more than 2 decimal places
                if (priceDecimal.scale() > 2) {
                     throw new NumberFormatException("Price has too many decimal places.");
                }

                priceInCents = priceDecimal.multiply(new BigDecimal("100")).longValueExact();
            }
        } catch (NumberFormatException | ArithmeticException e) {
            this.priceField.setTextColor(0xFFFF5555); // Set text color to red on error
            return;
        }

        // Send the update to the server via a network packet
        // NOTE: You will need to create this packet and a network handler (e.g., ModMessages)
        ModMessages.sendToServer(new C2SSetVendingPricePacket(this.blockEntity.getBlockPos(), this.selectedSlot, priceInCents));
        priceButtons[selectedSlot].setTooltip(generateTooltip(priceInCents));

        // Hide the editor UI after saving
        hidePriceEditor();
    }

    private void hidePriceEditor() {
        this.selectedSlot = -1;
        this.priceField.setVisible(false);
        this.saveButton.visible = false;
        this.priceField.setFocused(false);
        this.setFocused(null);
    }
    
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        // First, let all the widgets (Buttons, EditBoxes, etc.) try to handle the click.
        // The super method returns true if a widget was successfully clicked.
        if (super.mouseClicked(pMouseX, pMouseY, pButton)) {
            return true; // A widget handled the click, so we don't need to do anything else.
        }

        // If we get here, it means no widget was clicked.
        // Now, we can safely check if we should hide the price editor.
        if (this.priceField.isFocused()) {
            // Since we know a widget wasn't clicked, any click means we should hide the editor.
            hidePriceEditor();
            return true;
        }

        return false;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.selectedSlot != -1) {
            int row = this.selectedSlot / 3;
            int col = this.selectedSlot % 3;
            // This should highlight the actual item slot, which is to the left of the buttons
            int slotX = this.leftPos + 71 + col * 18;
            int slotY = this.topPos + 30 + row * 18;
            // The highlight is a semi-transparent white box drawn over the item slot
            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
        }

        if (this.hoveredSlot != -1 && this.selectedSlot != this.hoveredSlot) {
            int row = this.hoveredSlot / 3;
            int col = this.hoveredSlot % 3;
            // This should highlight the actual item slot, which is to the left of the buttons
            int slotX = this.leftPos + 71 + col * 18;
            int slotY = this.topPos + 30 + row * 18;
            // The highlight is a semi-transparent white box drawn over the item slot
            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        this.hoveredSlot = -1;
        for (int i = 0; i < priceButtons.length; i++) {
            if (priceButtons[i].isHovered()) {
                this.hoveredSlot = i;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle input for the price text box
        if (this.priceField.isFocused()) {
            // Enter/Return saves the price
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                savePrice();
                return true;
            }
            // Escape cancels editing
            if (keyCode == InputConstants.KEY_ESCAPE) {
                hidePriceEditor();
                return true;
            }
            // Let the text box handle other key presses
            this.priceField.setTextColor(0xE0E0E0);
            return this.priceField.keyPressed(keyCode, scanCode, modifiers);
        }

        // Default behavior for other cases (e.g., Escape to close screen)
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw the main inventory title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        
        // Draw the player inventory title
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // --- NEW CODE FOR DISPLAYING THE TOTAL ---
        drawStrings(graphics);
    }

    private void drawStrings(GuiGraphics graphics) {
        Component pricesComponent = Component.translatable("gui.currencycraft.vending_machine.prices");
        Component productComponent = Component.translatable("gui.currencycraft.vending_machine.product");
        Component profitComponent = Component.translatable("gui.currencycraft.vending_machine.profit");

        int x = this.titleLabelX;
        int y = this.titleLabelY + 12;
        
        graphics.drawString(this.font, pricesComponent, x, y, 4210752, false);
        graphics.drawString(this.font, productComponent, x + 63, y, 4210752, false);
        graphics.drawString(this.font, profitComponent, x + 126, y, 4210752, false);
    }

    private Tooltip generateTooltip(long priceInCents) {
        double priceInUnits = priceInCents / 100.0f;
        return priceInUnits == 0
            ? Tooltip.create(Component.translatable("gui.currencycraft.vending_machine.edit").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC))
            : Tooltip.create(Component.literal(String.format("%.2f", priceInUnits))
                .append("\n")
                .append(Component.translatable("gui.currencycraft.vending_machine.edit").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)));
    }
}