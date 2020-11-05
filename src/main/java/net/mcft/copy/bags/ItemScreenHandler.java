package net.mcft.copy.bags;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public abstract class ItemScreenHandler extends ScreenHandler {

	public static final int SLOT_SIZE = 18;

	protected final SimpleInventory inventory;
	protected final PlayerInventory playerInventory;
	protected final int protectedSlot;

	public final int border = 7;
	public final int width = this.border * 2 + SLOT_SIZE * 9;
	public final int titleOffset = 10;
	public final int inventoryOffset = 13;
	public final int hotbarOffset = 4;
	public final int height;

	public ItemScreenHandler(ScreenHandlerType<?> handler, int syncId, PlayerInventory playerInventory,
			SimpleInventory inventory, int protectedSlot) {
		super(handler, syncId);
		this.inventory = inventory;
		this.playerInventory = playerInventory;
		this.protectedSlot = protectedSlot;
		this.inventory.addListener(i -> this.sendContentUpdates());

		this.buildInventorySlots(1 + this.border, 1 + this.border + this.titleOffset);
		int yOffset = this.slots.get(this.slots.size() - 1).y + SLOT_SIZE + this.inventoryOffset - 1;

		// Player Inventory
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 9; x++)
				this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 1 + this.border + x * SLOT_SIZE,
						1 + yOffset + y * SLOT_SIZE));
		yOffset += SLOT_SIZE * 3 + this.hotbarOffset;

		// Player Hotbar
		for (int x = 0; x < 9; x++) {
			if (x == this.protectedSlot)
				this.addSlot(new ProtectedSlot(playerInventory, x, 1 + this.border + x * SLOT_SIZE, 1 + yOffset));
			else
				this.addSlot(new Slot(playerInventory, x, 1 + this.border + x * SLOT_SIZE, 1 + yOffset));
		}
		this.height = yOffset + SLOT_SIZE + this.border;

		inventory.onOpen(playerInventory.player);
	}

	protected abstract void buildInventorySlots(int x, int y);

	@Override
	public boolean canUse(PlayerEntity player) {
		ItemStack stack = ((ItemInventory) this.inventory).stack;
		return !stack.isEmpty() && (stack == this.getCurrentStack()) && this.inventory.canPlayerUse(player);
	}

	private ItemStack getCurrentStack() {
		return (this.protectedSlot >= 0) ? this.playerInventory.getStack(this.protectedSlot)
				: this.playerInventory.player.getOffHandStack();
	}

	@Override
	public ItemStack transferSlot(PlayerEntity player, int invSlot) {
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(invSlot);
		if ((slot != null) && slot.hasStack()) {
			ItemStack originalStack = slot.getStack();
			newStack = originalStack.copy();
			if (invSlot < this.inventory.size()) {
				if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true))
					return ItemStack.EMPTY;
			} else if (!this.insertItem(originalStack, 0, this.inventory.size(), false))
				return ItemStack.EMPTY;
			if (originalStack.isEmpty())
				slot.setStack(ItemStack.EMPTY);
			else
				slot.markDirty();
		}
		return newStack;
	}

	@Override
	protected boolean insertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast) {
		// Reimplementing this method because Mojang is stupid and doesn't respect
		// slot.getMaxItemCount when stacking items together.

		boolean bl = false;
		int i = startIndex;
		if (fromLast)
			i = endIndex - 1;

		if (stack.isStackable()) {
			while (!stack.isEmpty()) {
				if (fromLast) {
					if (i < startIndex)
						break;
				} else if (i >= endIndex)
					break;

				Slot slot = this.slots.get(i);
				ItemStack invStack = slot.getStack();
				// This is where the modifications occur.
				if (ScreenHandler.canStacksCombine(stack, invStack)) {
					int maxCount = Math.min(stack.getMaxCount(), slot.getMaxItemCount(stack));
					int diff = Math.min(stack.getCount(), maxCount - invStack.getCount());
					if (diff > 0) {
						invStack.increment(diff);
						stack.decrement(diff);
						slot.markDirty();
						bl = true;
					}
				}

				if (fromLast)
					--i;
				else
					++i;
			}
		}
		if (stack.isEmpty())
			return bl;

		if (fromLast)
			i = endIndex - 1;
		else
			i = startIndex;

		while (true) {
			if (fromLast) {
				if (i < startIndex)
					break;
			} else if (i >= endIndex)
				break;

			Slot slot = this.slots.get(i);
			ItemStack invStack = slot.getStack();
			if (invStack.isEmpty() && slot.canInsert(stack)) {
				if (stack.getCount() > slot.getMaxItemCount())
					slot.setStack(stack.split(slot.getMaxItemCount()));
				else
					slot.setStack(stack.split(stack.getCount()));

				slot.markDirty();
				bl = true;
				break;
			}

			if (fromLast)
				--i;
			else
				++i;
		}

		return bl;
	}

	@Override
	public ItemStack onSlotClick(int i, int j, SlotActionType action, PlayerEntity player) {
		// Prevent items from getting swapped with the protected slot (opened item).
		if ((action == SlotActionType.SWAP) && (j == this.protectedSlot))
			return ItemStack.EMPTY;
		return super.onSlotClick(i, j, action, player);
	}

	public static class ProtectedSlot extends Slot {

		public ProtectedSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canTakeItems(PlayerEntity player) {
			return false;
		}

	}

	public static class Factory<T extends ItemScreenHandler> implements ExtendedScreenHandlerFactory {

		protected final int protectedSlot;
		protected final ItemStack stack;
		protected final Constructor<T> constructor;

		public Factory(PlayerEntity player, Class<T> clazz) {
			this.stack = player.getMainHandStack();
			this.protectedSlot = player.inventory.selectedSlot;
			try {
				this.constructor = clazz.getConstructor(int.class, PlayerInventory.class, int.class, ItemStack.class);
			} catch (NoSuchMethodException | SecurityException ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
			try {
				return constructor.newInstance(syncId, inv, this.protectedSlot, this.stack);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public Text getDisplayName() {
			return this.stack.getName();
		}

		@Override
		public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
			buf.writeByte(this.protectedSlot);
		}

	}

}
