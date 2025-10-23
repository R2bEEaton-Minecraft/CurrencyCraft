// VendingMachineRestockScreen

package cc.spea.currencycraft.gui.VendingMachine;

import com.mojang.blaze3d.systems.RenderSystem;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.blocks.VendingMachine.VendingMachineBlockEntity;
import cc.spea.currencycraft.network.ModMessages;
import cc.spea.currencycraft.network.packets.C2SPurchaseVendingMachineItem;
import cc.spea.currencycraft.network.packets.C2SSetVendingPricePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack; // <-- Import ItemStack

public class VendingMachinePurchaseScreen extends AbstractContainerScreen<VendingMachinePurchaseMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, "textures/gui/vending_machine_purchase.png");    
    private Button[] priceButtons;

    private final VendingMachineBlockEntity blockEntity;


    public VendingMachinePurchaseScreen(VendingMachinePurchaseMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 122;
        this.imageHeight = 108;

        this.blockEntity = menu.getBlockEntity();

        this.priceButtons = new Button[menu.getBlockEntity().getProductSlots()];
    }

    @Override
    protected void init() {
        super.init();

        int buttonGridX = this.leftPos + 34;
        int buttonGridY = this.topPos + 29;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                final int slotIndex = row * 3 + col;
                int x = buttonGridX + col * 18; // Spacing them out a bit
                int y = buttonGridY + row * 18;

                long priceInCents = this.blockEntity.getPriceInCents(slotIndex);

                Tooltip priceTooltip = generateTooltip(priceInCents, slotIndex);

                Button priceButton = Button.builder(Component.empty(), button -> selectSlotForPurchase(slotIndex, false))
                        .bounds(x + 1, y + 1, 16, 16)
                        .tooltip(priceTooltip)
                        .build();
                priceButtons[slotIndex] = priceButton;
                this.addRenderableWidget(priceButton);
            }
        }
    }

    private void selectSlotForPurchase(int slotIndex, boolean buyAll) {
        if (this.blockEntity.getPriceInCents(slotIndex) <= this.blockEntity.calculateTotalCurrencyValueInCents()) {
            // System.out.println("Purchase attempt for slot " + slotIndex);
            ModMessages.sendToServer(new C2SPurchaseVendingMachineItem(this.blockEntity.getBlockPos(), slotIndex, buyAll));
            return;
        }
        this.blockEntity.getLevel().playLocalSound(this.blockEntity.getBlockPos(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if shift is held when clicking a button
        boolean shiftHeld = hasShiftDown();

        // Check if we're clicking on a price button
        for (int i = 0; i < priceButtons.length; i++) {
            if (priceButtons[i] != null && priceButtons[i].isMouseOver(mouseX, mouseY)) {
                final int slotIndex = i;
                if (shiftHeld) {
                    selectSlotForPurchase(slotIndex, true);
                    return true;
                } else {
                    selectSlotForPurchase(slotIndex, false);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        renderProductItems(guiGraphics);

        for (int i = 0; i < this.blockEntity.getProductSlots(); i++) {
            if (this.blockEntity.getPriceInCents(i) > this.blockEntity.calculateTotalCurrencyValueInCents()) {
                int row = i / 3;
                int col = i % 3;
                int slotX = this.leftPos + 36 + col * 18;
                int slotY = this.topPos + 31 + row * 18;
                guiGraphics.fill(slotX, slotY, slotX + 14, slotY + 14, 0x80FF0000);
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderProductItems(GuiGraphics guiGraphics) {
        int buttonGridX = this.leftPos + 34;
        int buttonGridY = this.topPos + 29;

        for (int i = 0; i < this.blockEntity.getProductSlots(); i++) {
            // NOTE: You might need to change getStackInSlot(i) to match your BlockEntity's method
            // for getting items. Common alternatives are:
            // - this.blockEntity.getItemHandler().getStackInSlot(i)
            // - this.menu.getSlot(i).getItem() if the product slots are part of the menu
            ItemStack productStack = this.blockEntity.getItem(i);

            if (!productStack.isEmpty()) {
                int row = i / 3;
                int col = i % 3;
                int x = buttonGridX + col * 18;
                int y = buttonGridY + row * 18;

                // Render the item centered on top of the button area
                // The button is at (x + 1, y + 1), so we draw the item there too.
                guiGraphics.renderFakeItem(productStack, x + 1, y + 1);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);

        Component insertedMoneyComponent = Component.translatable("gui.currencycraft.vending_machine.total",
                String.format("%.2f", this.menu.getBlockEntity().calculateTotalCurrencyValueInCents() / 100.0f));

        int textWidth = this.font.width(insertedMoneyComponent);
        int centeredX = (this.imageWidth / 2) - (textWidth / 2);

        int y = this.titleLabelY + 12;

        graphics.drawString(this.font, insertedMoneyComponent, centeredX, y, 4210752, false);
    }

    private Tooltip generateTooltip(long priceInCents, int slotIndex) {
        double priceInUnits = priceInCents / 100.0f;
        Component priceText = Component.literal(String.format("%.2f", priceInUnits));

        // Calculate how many can be purchased
        long insertedValue = this.blockEntity.calculateTotalCurrencyValueInCents();
        ItemStack itemStack = this.blockEntity.getItem(slotIndex);
        int available = itemStack.getCount();

        if (priceInCents > 0 && available > 0) {
            int canAfford = (int) (insertedValue / priceInCents);
            int maxPurchasable = Math.min(canAfford, available);

            if (maxPurchasable > 1) {
                Component tooltipText = Component.empty()
                    .append(priceText)
                    .append("\n")
                    .append(Component.translatable("tooltip.currencycraft.vending_machine.available", available).withStyle(ChatFormatting.GRAY))
                    .append("\n")
                    .append(Component.translatable("tooltip.currencycraft.vending_machine.shift_buy", maxPurchasable).withStyle(ChatFormatting.YELLOW));
                return Tooltip.create(tooltipText);
            }
        }

        return Tooltip.create(priceText);
    }
}