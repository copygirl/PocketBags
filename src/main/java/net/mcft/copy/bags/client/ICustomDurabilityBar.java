package net.mcft.copy.bags.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * Implemented by any {@link net.minecraft.item.Item Item} which wants to render
 * a custom durability bar, either replacing the default when it doesn't have a
 * damaged state, or an additional one on top of that. Note that this durability
 * bar renders before the default one.
 *
 * @see net.mcft.copy.bags.client.mixin.MixinItemRenderer MixinItemRenderer
 */
@Environment(EnvType.CLIENT)
public interface ICustomDurabilityBar {

	/**
	 * Gets the custom durability fullness to display on this item when it is
	 * rendered in an inventory.
	 *
	 * @param stack The stack to display a durability bar for.
	 * @return Fullness of the drawn durability bar, truncated to [0.0, 1.0]. When
	 *         {@link Float#NaN} is returned, no bar is displayed.
	 */
	float getCustomDurability(ItemStack stack);

	/**
	 * @param fraction Fullness of the durability bar, ranging [0.0, 1.0].
	 * @return The RGB color value that should be drawn for this durabiltiy bar.
	 */
	default int getCustomDurabilityColor(float fraction) {
		return MathHelper.hsvToRgb(fraction / 3, 1.0F, 1.0F);
	}

	/**
	 * Gets the offset to draw the custom durability bar at, for example when you
	 * want to draw an additional durability bar above the default one.
	 *
	 * Defaults to {@code 13} (default durability bar position). Can use a value of
	 * {@code 12} or {@code 11} to display a second bar directly above the default
	 * one, or {@code 1} to display above the item (towards the top of the slot).
	 *
	 * @return The vertical offset in (scaled) pixels relative to the slot.
	 */
	default int getCustomDurabilityOffset() {
		return 13;
	}

}
