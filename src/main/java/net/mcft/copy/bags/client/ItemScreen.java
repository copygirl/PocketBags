package net.mcft.copy.bags.client;

import net.mcft.copy.bags.ItemScreenHandler;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ItemScreen extends HandledScreen<ItemScreenHandler> {

	private static final Identifier TEXTURE = new Identifier("minecraft", "textures/gui/container/generic_54.png");

	private final ItemScreenHandler handler;

	public ItemScreen(ItemScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = handler.width;
		this.backgroundHeight = handler.height;
		this.playerInventoryTitleY = this.backgroundHeight - 93;
		this.handler = handler;
	}

	@Override
	protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.client.getTextureManager().bindTexture(TEXTURE);
		int x = (this.width - this.backgroundWidth) / 2;
		int y = (this.height - this.backgroundHeight) / 2;
		int yy = this.backgroundHeight
				- (handler.border + 18 + handler.hotbarOffset + 18 * 3 + handler.inventoryOffset);
		this.drawTexture(matrices, x, y, 0, 0, this.backgroundWidth, yy);
		this.drawTexture(matrices, x, y + yy, 0, 126, this.backgroundWidth, 96);
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		super.render(matrices, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(matrices, mouseX, mouseY);
	}

}
