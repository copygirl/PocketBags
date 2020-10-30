package net.mcft.copy.bags;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

public class PouchInventory extends SimpleInventory {

	public final ItemStack stack;

	public PouchInventory(ItemStack stack, int size) {
		super(size);
		this.stack = stack;

		ItemStack contents = ItemPouch.getContents(stack);
		int maxStackCount = Math.min(contents.getMaxCount(), this.getMaxCountPerStack());
		for (int i = 0; i < size() && !contents.isEmpty(); i++)
			this.setStack(i, contents.split(maxStackCount));
	}

	@Override
	public void markDirty() {
		// Collect the items inside this inventory.
		ItemStack contents = ItemStack.EMPTY;
		for (int i = 0; i < size(); i++) {
			ItemStack invStack = this.getStack(i);
			if (invStack.isEmpty())
				continue;
			if (contents.isEmpty())
				contents = invStack.copy();
			else
				contents.increment(invStack.getCount());
		}

		ItemPouch.setContents(this.stack, contents);
		super.markDirty();
	}

}
