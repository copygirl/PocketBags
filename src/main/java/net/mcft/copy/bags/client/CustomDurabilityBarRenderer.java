package net.mcft.copy.bags.client;

import net.mcft.copy.bags.client.mixin.ItemRendererAccessor;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class CustomDurabilityBarRenderer implements ICustomItemRenderer<ICustomDurabilityBar> {

	@Override
	public void render(ICustomDurabilityBar item, ItemStack stack, int x, int y, TextRenderer renderer) {
		float fullness = MathHelper.clamp(item.getCustomDurability(stack), 0.0F, 1.0F);
		if (Float.isNaN(fullness))
			return;

		RenderSystem.disableDepthTest();
		RenderSystem.disableTexture();
		RenderSystem.disableAlphaTest();
		RenderSystem.disableBlend();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		int offset = item.getCustomDurabilityOffset();
		int width = Math.round(fullness * 13);
		int color = item.getCustomDurabilityColor(fullness);
		ItemRendererAccessor itemRenderer = (ItemRendererAccessor) MinecraftClient.getInstance().getItemRenderer();
		itemRenderer.invokeRenderGuiQuad(bufferBuilder, x + 2, y + offset, 13, 2, 0, 0, 0, 255);
		itemRenderer.invokeRenderGuiQuad(bufferBuilder, x + 2, y + offset, width, 1, color >> 16 & 255,
				color >> 8 & 255, color & 255, 255);
		RenderSystem.enableBlend();
		RenderSystem.enableAlphaTest();
		RenderSystem.enableTexture();
		RenderSystem.enableDepthTest();
	}

}
