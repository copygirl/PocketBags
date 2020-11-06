package net.mcft.copy.bags;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public final class PocketBagsUtil {

	private PocketBagsUtil() {
	}

	public static void setStackInHand(PlayerEntity player, Hand hand, ItemStack stack) {
		if (hand == Hand.MAIN_HAND)
			player.inventory.setStack(player.inventory.selectedSlot, stack);
		else
			player.inventory.offHand.set(0, stack);
	}

}
