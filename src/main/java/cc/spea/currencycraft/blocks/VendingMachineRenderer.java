package cc.spea.currencycraft.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class VendingMachineRenderer implements BlockEntityRenderer<VendingMachineBlockEntity> {

    // <<< THIS constructor signature is required for the method reference VendingMachineRenderer::new
    public VendingMachineRenderer(BlockEntityRendererProvider.Context context) {
        // Use context if you need model/texture/font access later.
    }

    @Override
    public void render(VendingMachineBlockEntity be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int combinedLight, int combinedOverlay) {

        if (be == null) return;
        ItemStackHandler inv = be.getInventory();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        // Get block facing direction
        var state = be.getBlockState();
        var facing = state.hasProperty(VendingMachineBlock.FACING) ? state.getValue(VendingMachineBlock.FACING) : null;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            poseStack.pushPose();

            int row = i / 3;
            int col = i % 3;

            // Example layout (tweak to taste). These are block-space offsets.
            float tx = (1 - col) * 0.25f; // left/right
            float ty = (1 - row) * 0.3125f; // up/down
            float tz = 0.25f; // slightly in front of block center (behind glass)

            // Move to block center (0.5,0.5,0.5) then offset
            // Adjust translation based on block facing direction
            float x = 0.53125f + tx;
            float y = 1.46875f + ty;
            float z = 0.5f + tz;

            if (facing != null) {
                switch (facing) {
                    case NORTH -> poseStack.translate(x, y, 1.0f - z);
                    case SOUTH -> poseStack.translate(1.0f - x, y, z);
                    case EAST  -> poseStack.translate(z, y, x);
                    case WEST  -> poseStack.translate(1.0f - z, y, 1.0f - x);
                    default    -> poseStack.translate(x, y, z);
                }
            } else {
                poseStack.translate(x, y, z);
            }

            // Optionally orient the item itself to face outward
            if (facing != null) {
                switch (facing) {
                    case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                    case NORTH -> { }
                    case EAST  -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
                    case WEST  -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
                    default    -> { /* do nothing */ }
                }
            }

            // Scale items small
            poseStack.scale(0.25f, 0.25f, 0.25f);

            // Render the item
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED,
                    combinedLight, combinedOverlay, poseStack, buffer, be.getLevel(), 0);

            poseStack.popPose();
        }
    }
}
