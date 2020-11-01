package net.mcft.copy.bags.mixin;

import java.util.UUID;

import net.mcft.copy.bags.IItemPickupSink;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity extends Entity {

	@Shadow
	private int pickupDelay;
	@Shadow
	private UUID owner;

	@Shadow
	public abstract ItemStack getStack();

	public MixinItemEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
	}

	/**
	 * Before picking up an item into the player's inventory, try to see if there's
	 * any item implementing {@see IItemPickupSink} in their inventory, and allow it
	 * to modify the pickup behavior.
	 */
	@Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
	private void pocketbags$onPlayerCollision(PlayerEntity player, CallbackInfo info) {
		if (player.world.isClient)
			return;
		ItemStack stack = this.getStack();
		Item item = stack.getItem();
		int count = stack.getCount();
		if ((this.pickupDelay == 0) && ((this.owner == null) || this.owner.equals(player.getUuid()))) {
			for (int i = 0; i < player.inventory.size(); i++) {
				ItemStack invStack = player.inventory.getStack(i);
				if (!(invStack.getItem() instanceof IItemPickupSink))
					continue;
				if (((IItemPickupSink) invStack.getItem()).collect((ServerPlayerEntity) player, invStack, stack)) {
					player.sendPickup(this, count);
					player.increaseStat(Stats.PICKED_UP.getOrCreateStat(item), count);
					if (stack.isEmpty()) {
						this.remove();
						stack.setCount(count);
						info.cancel();
						return;
					}
				}
			}
		}
	}
}
