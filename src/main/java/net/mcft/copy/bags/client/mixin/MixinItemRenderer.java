package net.mcft.copy.bags.client.mixin;

import net.mcft.copy.bags.ItemPouch;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

	@Shadow
	public abstract void renderInGui(ItemStack stack, int x, int y);

	@Shadow
	public abstract void renderGuiQuad(BufferBuilder buffer, int x, int y, int width, int height, int red, int green,
			int blue, int alpha);

	@Inject(method = "renderGuiItemOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
	public void pocketbags$renderGuiItemOverlay(TextRenderer renderer, ItemStack stack, int x, int y,
			@Nullable String countLabel, CallbackInfo info) {
		if (!(stack.getItem() instanceof ItemPouch))
			return;
		ItemStack contents = ItemPouch.getContents(stack);
		if (contents.isEmpty())
			return;
		float count = contents.getCount();
		float maxCount = contents.getMaxCount() * 9;

		RenderSystem.pushMatrix();
		RenderSystem.translatef(x, y, 0);
		RenderSystem.scalef(0.5F, 0.5F, 1.0f);
		RenderSystem.translatef(-x, -y, 50);
		this.renderInGui(contents, x + 8, y + 12);
		RenderSystem.popMatrix();

		RenderSystem.disableDepthTest();
		RenderSystem.disableTexture();
		RenderSystem.disableAlphaTest();
		RenderSystem.disableBlend();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		float fraction = Math.max(0.0F, count / maxCount);
		int width = Math.round(fraction * 13);
		int color = MathHelper.hsvToRgb(fraction / 3, 1.0F, 1.0F);
		this.renderGuiQuad(bufferBuilder, x + 2, y + 13, 13, 2, 0, 0, 0, 255);
		this.renderGuiQuad(bufferBuilder, x + 2, y + 13, width, 1, color >> 16 & 255, color >> 8 & 255, color & 255,
				255);
		RenderSystem.enableBlend();
		RenderSystem.enableAlphaTest();
		RenderSystem.enableTexture();
		RenderSystem.enableDepthTest();
	}

}
