package net.mcft.copy.bags;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.mcft.copy.bags.client.ICustomDurabilityBar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvironmentInterface;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.ItemTags;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

@EnvironmentInterface(value = EnvType.CLIENT, itf = ICustomDurabilityBar.class)
public class ItemFlowerPouch extends Item implements IItemPickupSink, ICustomDurabilityBar {

	public static final int SLOTS = 18;
	public static final int MAX_COUNT_PER_STACK = 32;
	public static final Identifier IDENTIFIER = new Identifier(PocketBagsMod.MOD_ID, "flower_pouch");

	public static final ScreenHandlerType<ItemFlowerPouch.ScreenHandler> SCREEN_HANDLER = ScreenHandlerRegistry
			.registerExtended(IDENTIFIER, ItemFlowerPouch.ScreenHandler::new);

	public ItemFlowerPouch() {
		super(new Item.Settings().group(ItemGroup.MISC).maxCount(1));
	}

	public void collect(ServerPlayerEntity player, ItemStack pouch, ItemStack pickup) {
		if (!ItemTags.FLOWERS.contains(pickup.getItem()))
			return;
		ItemFlowerPouch.Inventory inventory = new ItemFlowerPouch.Inventory(pouch);
		// addStack creates a copy of the input stack, so we use setCount.
		pickup.setCount(inventory.addStack(pickup).getCount());
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient && user.isSneaking() && (hand == Hand.MAIN_HAND))
			user.openHandledScreen(new ItemScreenHandler.Factory<>(user, ItemFlowerPouch.ScreenHandler.class));
		return super.use(world, user, hand);
	}

	@Override
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		ArrayList<ItemStack> stacked = new ArrayList<>();
		for (ItemStack item : new ItemFlowerPouch.Inventory(stack)) {
			Optional<ItemStack> found = stacked.stream()
					.filter(i -> ItemStack.areItemsEqual(i, item) && ItemStack.areTagsEqual(i, item)).findAny();
			if (found.isPresent())
				found.get().increment(item.getCount());
			else
				stacked.add(item);
		}
		if (stacked.isEmpty())
			return;
		stacked.sort((a, b) -> b.getCount() - a.getCount());
		for (ItemStack item : stacked) {
			tooltip.add(new LiteralText(item.getCount() + "x ").append(item.getName())
					.setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
		}
	}

	@Environment(EnvType.CLIENT)
	public float getCustomDurability(ItemStack stack) {
		int maxCount = SLOTS * MAX_COUNT_PER_STACK;
		int count = 0;
		// TODO: This could be made cheaper by just iterating the tags.
		for (ItemStack item : new ItemFlowerPouch.Inventory(stack))
			count += item.getCount();
		return (count > 0) ? (float) count / maxCount : Float.NaN;
	}

	public static class Inventory extends ItemInventory {

		public Inventory(ItemStack stack) {
			super(stack, ItemFlowerPouch.SLOTS);
		}

		@Override
		public int getMaxCountPerStack() {
			return MAX_COUNT_PER_STACK;
		}

		@Override
		protected void readFromTag(Tag tag) {
			@SuppressWarnings("unchecked") // Because Minecraft NBT tags are stupid.
			List<CompoundTag> list = (List<CompoundTag>) (Object) tag;
			for (CompoundTag item : list) {
				int slot = item.getByte("Slot");
				this.setStack(slot, ItemStack.fromTag(item));
			}
		}

		@Override
		protected Tag writeToTag() {
			ListTag list = new ListTag();
			for (int i = 0; i < this.size(); i++) {
				ItemStack item = this.getStack(i);
				if (item.isEmpty())
					continue;
				CompoundTag itemTag = new CompoundTag();
				itemTag.putByte("Slot", (byte) i);
				item.toTag(itemTag);
				list.add(itemTag);
			}
			if (list.isEmpty())
				return null;
			else
				return list;
		}

	}

	public static class ScreenHandler extends ItemScreenHandler {

		public ScreenHandler(int syncId, PlayerInventory playerInventory, int protectedSlot, ItemStack stack) {
			super(ItemFlowerPouch.SCREEN_HANDLER, syncId, playerInventory, new ItemFlowerPouch.Inventory(stack),
					protectedSlot);
		}

		public ScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
			super(ItemFlowerPouch.SCREEN_HANDLER, syncId, playerInventory,
					new ItemFlowerPouch.Inventory(ItemStack.EMPTY), buf.readByte());
		}

		@Override
		protected void buildInventorySlots(int x, int y) {
			for (int i = 0; i < this.inventory.size(); ++i) {
				int xx = x + (i % 9) * 18;
				int yy = y + (i / 9) * 18;
				this.addSlot(new Slot(this.inventory, i, xx, yy) {
					@Override
					public boolean canInsert(ItemStack stack) {
						return ItemTags.FLOWERS.contains(stack.getItem());
					}
				});
			}
		}

	}

}
