package net.mcft.copy.bags;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Implemented by any {@link net.minecraft.item.Item Item} which wishes to
 * override player pickup behavior, for example to collect the picked up item
 * into itself rather than the player's inventory.
 *
 * @see net.mcft.copy.bags.mixin.MixinItemEntity MixinItemEntity
 */
public interface IItemPickupSink {

	/**
	 * Attempts to collect the specified pickup stack into the sink stack.
	 *
	 * @param player Player who's picking up the stack.
	 * @param sink   Stack in player inventory to collect into. Modified when any
	 *               items are picked up (typically NBT data).
	 * @param pickup Stack being attempted to pick up by player. Count must be
	 *               modified when any items are picked up.
	 * @return {@code true} if any items were collected, {@code false} otherwise.
	 */
	public boolean collect(ServerPlayerEntity player, ItemStack sink, ItemStack pickup);

}
