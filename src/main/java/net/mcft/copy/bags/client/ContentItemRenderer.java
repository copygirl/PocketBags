package net.mcft.copy.bags.client;

import java.util.function.Function;

import net.mcft.copy.bags.client.mixin.ItemRendererAccessor;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public class ContentItemRenderer<T extends Item> implements ICustomItemRenderer<T> {

	private final Function<ItemStack, ItemStack> getContentsFunc;

	public ContentItemRenderer(Function<ItemStack, ItemStack> getContentsFunc) {
		this.getContentsFunc = getContentsFunc;
	}

	@Override
	public void render(T item, ItemStack stack, int x, int y, TextRenderer renderer) {
		ItemStack contents = this.getContentsFunc.apply(stack);
		if (contents.isEmpty())
			return;

		RenderSystem.pushMatrix();
		RenderSystem.translatef(x, y, 0);
		RenderSystem.scalef(0.5F, 0.5F, 1.0f);
		RenderSystem.translatef(-x, -y, 50);
		ItemRendererAccessor itemRenderer = (ItemRendererAccessor) MinecraftClient.getInstance().getItemRenderer();
		itemRenderer.invokeRenderInGui(contents, x + 8, y + 12);
		RenderSystem.popMatrix();
	}

}
