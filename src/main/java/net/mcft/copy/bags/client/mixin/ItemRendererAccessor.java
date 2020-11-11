package net.mcft.copy.bags.client.mixin;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemRenderer.class)
public interface ItemRendererAccessor {

	@Invoker("renderInGui")
	void invokeRenderInGui(ItemStack stack, int x, int y);

	@Invoker("renderGuiQuad")
	void invokeRenderGuiQuad(BufferBuilder buffer, int x, int y, int width, int height, int r, int g, int b, int a);

}
