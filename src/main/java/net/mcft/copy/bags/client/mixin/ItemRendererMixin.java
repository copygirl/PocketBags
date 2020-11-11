package net.mcft.copy.bags.client.mixin;

import net.mcft.copy.bags.client.ICustomItemRenderer;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

import org.jetbrains.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

	@Inject(method = "renderGuiItemOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
	public void pocketbags$renderGuiItemOverlay(TextRenderer renderer, ItemStack stack, int x, int y,
			@Nullable String countLabel, CallbackInfo info) {
		for (Pair<Class<?>, ICustomItemRenderer<Object>> entry : ICustomItemRenderer.Registry.entries)
			if (entry.getLeft().isInstance(stack.getItem()))
				entry.getRight().render(stack.getItem(), stack, x, y, renderer);
	}

}
