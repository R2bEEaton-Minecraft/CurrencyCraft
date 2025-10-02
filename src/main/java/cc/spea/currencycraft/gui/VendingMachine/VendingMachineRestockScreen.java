package cc.spea.currencycraft.gui.VendingMachine;

import com.mojang.blaze3d.systems.RenderSystem;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class VendingMachineRestockScreen extends AbstractContainerScreen<VendingMachineRestockMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, "textures/gui/vending_machine.png");

    public VendingMachineRestockScreen(VendingMachineRestockMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 230;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Bind the GUI texture
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f); // full white tint
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render the dark background behind the GUI
        this.renderBackground(guiGraphics);

        // Draw the container and slots
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render tooltips (like hovered item stacks)
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
