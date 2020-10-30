package net.mcft.copy.bags.mixin;

import net.mcft.copy.bags.ItemPouch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

	@Shadow
	private ItemStack currentStack;
	@Shadow
	private int heldItemTooltipFade;
	@Shadow
	private int scaledWidth;
	@Shadow
	private MinecraftClient client;

	@Shadow
	public abstract TextRenderer getFontRenderer();

	// Pouches show the contained item on a separate line in the held item
	// tooltip - the one above the hotbar when the selected item changes.
	@ModifyVariable(ordinal = 2, method = "renderHeldItemTooltip", at = @At(value = "INVOKE", by = 1, target = "Lcom/mojang/blaze3d/systems/RenderSystem;defaultBlendFunc()V"))
	public int pocketbags$renderHeldItemTooltip(int y, MatrixStack matrices) {
		if (!(this.currentStack.getItem() instanceof ItemPouch))
			return y;
		ItemStack contents = ItemPouch.getContents(this.currentStack);
		if (contents.isEmpty())
			return y;
		MutableText text = new LiteralText("").append(contents.getName())
				.setStyle(Style.EMPTY.withColor(Formatting.GRAY));
		int x = (this.scaledWidth - getFontRenderer().getWidth(text)) / 2;
		int alpha = Math.min(255, (int) (this.heldItemTooltipFade * 256.0F / 10.0F));
		this.getFontRenderer().drawWithShadow(matrices, text, x, y, 0xFFFFFF | (alpha << 24));
		return y - this.getFontRenderer().fontHeight - 2;
	}

	// When switching from one pouch to another, cause held item tooltip to update.
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
	public void pocketbags$tick(CallbackInfo info) {
		ItemStack newStack = this.client.player.inventory.getMainHandStack();
		if ((this.currentStack.getItem() instanceof ItemPouch) && (newStack.getItem() instanceof ItemPouch)) {
			ItemStack currentContents = ItemPouch.getContents(this.currentStack);
			ItemStack newContents = ItemPouch.getContents(newStack);
			if (!ItemStack.areItemsEqual(currentContents, newContents))
				this.currentStack = ItemStack.EMPTY;
		}
	}
}
