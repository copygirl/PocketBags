package net.mcft.copy.bags;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PouchScreenHandler extends ScreenHandler {

	private final Inventory inventory;
	private final PlayerInventory playerInventory;
	private final int pouchSlot;

	public PouchScreenHandler(int syncId, PlayerInventory playerInventory, int pouchSlot, ItemStack stack) {
		this(syncId, playerInventory, new PouchInventory(stack, 9), pouchSlot);
		((SimpleInventory) this.inventory).addListener(i -> sendContentUpdates());
	}

	public PouchScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
		this(syncId, playerInventory, new SimpleInventory(9), buf.readByte());
	}

	private PouchScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, int pouchSlot) {
		super(ItemPouch.SCREEN_HANDLER, syncId);
		ScreenHandler.checkSize(inventory, 9);
		this.inventory = inventory;
		this.playerInventory = playerInventory;
		this.pouchSlot = pouchSlot;

		for (int x = 0; x < 9; ++x)
			this.addSlot(new PouchSlot(inventory, x, 8 + x * 18, 18));

		for (int y = 0; y < 3; ++y)
			for (int x = 0; x < 9; ++x)
				this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 49 + y * 18));
		for (int x = 0; x < 9; ++x) {
			if (x == this.pouchSlot)
				this.addSlot(new ProtectedSlot(playerInventory, x, 8 + x * 18, 107));
			else
				this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 107));
		}

		inventory.onOpen(playerInventory.player);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		ItemStack stack = ((PouchInventory) this.inventory).stack;
		return !stack.isEmpty() && (stack == this.getCurrentStack()) && this.inventory.canPlayerUse(player);
	}

	private ItemStack getCurrentStack() {
		return (this.pouchSlot >= 0) ? this.playerInventory.getStack(this.pouchSlot)
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
				if (!insertItem(originalStack, this.inventory.size(), this.slots.size(), true))
					return ItemStack.EMPTY;
			} else if (!insertItem(originalStack, 0, this.inventory.size(), false))
				return ItemStack.EMPTY;
			if (originalStack.isEmpty())
				slot.setStack(ItemStack.EMPTY);
			else
				slot.markDirty();
		}
		return newStack;
	}

	@Override
	public ItemStack onSlotClick(int i, int j, SlotActionType action, PlayerEntity player) {
		// Prevent items from getting swapped with the currently opened pouch.
		if ((action == SlotActionType.SWAP) && (j == this.pouchSlot))
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

	public static class PouchSlot extends Slot {

		private final int index;

		public PouchSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
			this.index = index;
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			if (!ItemPouch.isPouchableItem(stack))
				return false;
			for (int i = 0; i < this.inventory.size(); i++) {
				// Ignore this slot to allow swapping if it's the only filled slot.
				if (i == this.index)
					continue;
				ItemStack invStack = this.inventory.getStack(i);
				if (!invStack.isEmpty())
					return ItemStack.areItemsEqual(stack, invStack) && ItemStack.areTagsEqual(stack, invStack);
			}
			// All slots empty (or only this slot filled), insertion is possible.
			return true;
		}

	}

	public static class Factory implements ExtendedScreenHandlerFactory {

		public final int pouchSlot;
		public final ItemStack stack;

		public Factory(PlayerEntity player) {
			this.pouchSlot = player.inventory.selectedSlot;
			this.stack = player.getMainHandStack();
		}

		@Override
		public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
			return new PouchScreenHandler(syncId, inv, this.pouchSlot, this.stack);
		}

		@Override
		public Text getDisplayName() {
			return this.stack.getName();
		}

		@Override
		public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
			buf.writeByte(this.pouchSlot);
		}

	}

}
