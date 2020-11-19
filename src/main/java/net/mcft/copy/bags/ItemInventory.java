package net.mcft.copy.bags;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.inventory.SimpleInventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.Tag;

public abstract class ItemInventory extends SimpleInventory implements Iterable<ItemStack> {

	public final ItemStack stack;

	public ItemInventory(ItemStack stack, int size) {
		super(size);
		this.stack = stack;
		if (stack.hasTag()) {
			Tag tag;
			if (((tag = stack.getTag().get("Contents")) != null)
					// BACKWARDS COMPATIBILITY: 1.0.0
					|| ((tag = stack.getTag().get("pocketbags:pouch")) != null))
				this.readFromTag(tag);
		}
	}

	protected abstract void readFromTag(Tag tag);

	protected abstract Tag writeToTag();

	@Override
	public void markDirty() {
		Tag tag = this.writeToTag();
		if (tag == null)
			this.stack.removeSubTag("Contents");
		else
			this.stack.putSubTag("Contents", tag);

		super.markDirty();
	}

	@Override
	public ItemStack addStack(ItemStack stack) {
		ItemStack itemStack = stack.copy();
		this.mergeIntoExistingSlots(itemStack);
		if (!itemStack.isEmpty())
			this.mergeIntoNewSlots(itemStack);
		return itemStack;
	}

	private void mergeIntoNewSlots(ItemStack stack) {
		int maxCount = Math.min(stack.getMaxCount(), this.getMaxCountPerStack());
		for (int slot = 0; slot < this.size(); slot++) {
			ItemStack invStack = this.getStack(slot);
			if (!invStack.isEmpty())
				continue;
			this.setStack(slot, stack.split(maxCount));
			if (stack.isEmpty())
				return;
		}
	}

	private void mergeIntoExistingSlots(ItemStack stack) {
		int maxCount = Math.min(stack.getMaxCount(), this.getMaxCountPerStack());
		for (int slot = 0; slot < this.size(); slot++) {
			ItemStack invStack = this.getStack(slot);
			if (ItemStack.areItemsEqual(invStack, stack) && ItemStack.areTagsEqual(invStack, stack)) {
				int diff = Math.min(stack.getCount(), maxCount - invStack.getCount());
				if (diff > 0) {
					invStack.increment(diff);
					stack.decrement(diff);
					this.markDirty();
				}
				if (stack.isEmpty())
					return;
			}
		}
	}

	@Override
	public Iterator<ItemStack> iterator() {
		return IntStream.range(0, this.size()).mapToObj(this::getStack).filter(s -> !s.isEmpty()).iterator();
	}

	public Stream<ItemStack> stream() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this.iterator(), Spliterator.NONNULL), false);
	}
}
