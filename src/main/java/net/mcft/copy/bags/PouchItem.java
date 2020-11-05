package net.mcft.copy.bags;

import java.util.Arrays;
import java.util.List;

import net.mcft.copy.bags.client.ICustomDurabilityBar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvironmentInterface;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fabricmc.fabric.api.tag.TagRegistry;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

@EnvironmentInterface(value = EnvType.CLIENT, itf = ICustomDurabilityBar.class)
public class PouchItem extends Item implements IItemPickupSink, ICustomDurabilityBar {

	public static final int SLOTS = 9;
	public static final Identifier IDENTIFIER = new Identifier(PocketBagsMod.MOD_ID, "pouch");
	public static final Tag<Item> POUCHABLE_TAG = TagRegistry.item(new Identifier(PocketBagsMod.MOD_ID, "pouchable"));

	public static final ScreenHandlerType<PouchItem.ScreenHandler> SCREEN_HANDLER = ScreenHandlerRegistry
			.registerExtended(IDENTIFIER, PouchItem.ScreenHandler::new);

	public PouchItem() {
		super(new Item.Settings().group(ItemGroup.MISC).maxCount(1));
	}

	public void collect(ServerPlayerEntity player, ItemStack pouch, ItemStack pickup) {
		if (!PouchItem.isPouchableItem(pickup))
			return;
		ItemStack contents = PouchItem.getContents(pouch);
		if (ItemStack.areItemsEqual(contents, pickup) && ItemStack.areTagsEqual(contents, pickup)) {
			int maxCount = pickup.getMaxCount() * SLOTS;
			if (contents.getCount() < maxCount) {
				int newCount = Math.min(maxCount, contents.getCount() + pickup.getCount());
				int diff = newCount - contents.getCount();
				contents.setCount(newCount);
				pickup.decrement(diff);
				PouchItem.setContents(pouch, contents);
			}
		}
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient && user.isSneaking() && (hand == Hand.MAIN_HAND))
			user.openHandledScreen(new ItemScreenHandler.Factory<>(user, PouchItem.ScreenHandler.class));
		return super.use(world, user, hand);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		ItemStack pouch = context.getStack();
		ItemStack contents = PouchItem.getContents(pouch);
		if (contents.isEmpty())
			return ActionResult.PASS;
		ActionResult result = ActionResult.PASS;

		// Generate the order of block offsets to be used on.
		Vec3i[] order;
		if (player.isSneaking()) {
			// When sneaking, only place a single block.
			order = new Vec3i[] { Vec3i.ZERO };
		} else {
			order = new Vec3i[9];
			Direction facing = player.getHorizontalFacing();
			order[0] = Vec3i.ZERO;
			order[1] = facing.rotateYClockwise().getVector();
			order[2] = facing.rotateYCounterclockwise().getVector();
			order[3] = order[0].offset(facing, 1);
			order[4] = order[1].offset(facing, 1);
			order[5] = order[2].offset(facing, 1);
			order[6] = order[0].offset(facing, -1);
			order[7] = order[1].offset(facing, -1);
			order[8] = order[2].offset(facing, -1);
		}

		// Set currently held item to the pouch contents.
		PouchItem.setStackInHand(player, context.getHand(), contents);

		try {
			// Use contents stack on all blocks in order.
			for (int i = 0; i < order.length; i++) {
				BlockPos pos = context.getBlockPos().add(order[i]);
				Vec3d hitPos = context.getHitPos().add(Vec3d.of(order[i]));
				BlockHitResult hit = new BlockHitResult(hitPos, context.getSide(), pos, context.hitsInsideBlock());
				ItemUsageContext newContext = new ItemUsageContext(player, context.getHand(), hit);
				if (contents.getItem().useOnBlock(newContext).isAccepted())
					result = ActionResult.SUCCESS;
				if (player.getStackInHand(context.getHand()).isEmpty())
					break;
			}
		} finally {
			// Update pouch contents and reset the held item to the pouch.
			PouchItem.setContents(pouch, player.getStackInHand(context.getHand()));
			PouchItem.setStackInHand(player, context.getHand(), pouch);
		}

		return result;
	}

	@Override
	public ActionResult useOnEntity(ItemStack pouch, PlayerEntity player, LivingEntity origEntity, Hand hand) {
		ItemStack contents = PouchItem.getContents(pouch);
		if (contents.isEmpty())
			return ActionResult.PASS;
		ActionResult result = ActionResult.PASS;

		List<LivingEntity> entities;
		if (player.isSneaking()) {
			// When sneaking, only interact with the clicked entity.
			entities = Arrays.asList(origEntity);
		} else {
			// Find all entities of the same type (and adult state) in range.
			Box box = new Box(origEntity.getPos().subtract(1.5, 1.0, 1.5), origEntity.getPos().add(1.5, 1.0, 1.5));
			@SuppressWarnings("unchecked")
			EntityType<LivingEntity> type = (EntityType<LivingEntity>) origEntity.getType();
			entities = player.world.getEntitiesByType(type, box, e -> (e.isBaby() == origEntity.isBaby()));
		}

		// Set currently held item to the pouch contents.
		PouchItem.setStackInHand(player, hand, contents);

		try {
			// Use contents on found entities.
			for (LivingEntity entity : entities) {
				ItemStack stack = player.getStackInHand(hand);
				if (entity.interact(player, hand).isAccepted()
						|| stack.getItem().useOnEntity(stack, player, entity, hand).isAccepted())
					result = ActionResult.SUCCESS;
				if (player.getStackInHand(hand).isEmpty())
					break;
			}
		} finally {
			// Update pouch contents and reset the held item to the pouch.
			PouchItem.setContents(pouch, player.getStackInHand(hand));
			PouchItem.setStackInHand(player, hand, pouch);
		}

		return result;
	}

	@Override
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		ItemStack contents = PouchItem.getContents(stack);
		if (contents.isEmpty())
			return;
		tooltip.add(new LiteralText(contents.getCount() + "x ").append(contents.getName())
				.setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
	}

	public static boolean isPouchableItem(ItemStack stack) {
		return isPouchableItem(stack.getItem());
	}

	public static boolean isPouchableItem(Item item) {
		return POUCHABLE_TAG.contains(item);
	}

	public static ItemStack getContents(ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof PouchItem))
			return ItemStack.EMPTY;
		CompoundTag tag = stack.getSubTag("Contents");
		if (tag == null)
			return ItemStack.EMPTY;
		ItemStack contents = ItemStack.fromTag(tag);
		// fromTag reads a byte but we want an int to allow Count > 127.
		contents.setCount(tag.getInt("Count"));
		return contents;
	}

	public static void setContents(ItemStack stack, ItemStack contents) {
		if (stack.isEmpty() || !(stack.getItem() instanceof PouchItem))
			throw new IllegalArgumentException("Specified stack is not an ItemPouch");
		if (contents.isEmpty())
			stack.removeSubTag("Contents");
		else {
			CompoundTag tag = stack.getOrCreateSubTag("Contents");
			contents.toTag(tag);
			// Again, toTag writes a byte, but we want it to be an int.
			tag.putInt("Count", contents.getCount());
		}
	}

	private static void setStackInHand(PlayerEntity player, Hand hand, ItemStack stack) {
		if (hand == Hand.MAIN_HAND)
			player.inventory.setStack(player.inventory.selectedSlot, stack);
		else
			player.inventory.offHand.set(0, stack);
	}

	@Environment(EnvType.CLIENT)
	public float getCustomDurability(ItemStack stack) {
		ItemStack contents = PouchItem.getContents(stack);
		if (contents.isEmpty())
			return Float.NaN;

		float count = contents.getCount();
		float maxCount = contents.getMaxCount() * 9;
		return count / maxCount;
	}

	public static class Inventory extends ItemInventory {

		public Inventory(ItemStack stack) {
			super(stack, PouchItem.SLOTS);
		}

		@Override
		protected void readFromTag(net.minecraft.nbt.Tag tag) {
			CompoundTag compound = (CompoundTag) tag;
			ItemStack contents = ItemStack.fromTag(compound);
			// fromTag reads a byte but we want an int to allow Count > 127.
			contents.setCount(compound.getInt("Count"));

			int maxStackCount = Math.min(contents.getMaxCount(), this.getMaxCountPerStack());
			for (int i = 0; i < this.size() && !contents.isEmpty(); i++)
				this.setStack(i, contents.split(maxStackCount));
		}

		@Override
		protected net.minecraft.nbt.Tag writeToTag() {
			// Collect the items inside this inventory.
			ItemStack contents = ItemStack.EMPTY;
			for (int i = 0; i < this.size(); i++) {
				ItemStack invStack = this.getStack(i);
				if (invStack.isEmpty())
					continue;
				if (contents.isEmpty())
					contents = invStack.copy();
				else
					contents.increment(invStack.getCount());
			}

			CompoundTag compound = contents.toTag(new CompoundTag());
			// Again, toTag writes a byte, but we want it to be an int.
			compound.putInt("Count", contents.getCount());
			return compound;
		}

	}

	public static class ScreenHandler extends ItemScreenHandler {

		public ScreenHandler(int syncId, PlayerInventory playerInventory, int protectedSlot, ItemStack stack) {
			super(PouchItem.SCREEN_HANDLER, syncId, playerInventory, new PouchItem.Inventory(stack), protectedSlot);
		}

		public ScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
			super(PouchItem.SCREEN_HANDLER, syncId, playerInventory, new PouchItem.Inventory(ItemStack.EMPTY),
					buf.readByte());
		}

		@Override
		protected void buildInventorySlots(int x, int y) {
			for (int i = 0; i < this.inventory.size(); ++i) {
				int xx = x + (i % 9) * 18;
				int yy = y + (i / 9) * 18;
				this.addSlot(new PouchItem.Slot(this.inventory, i, xx, yy));
			}
		}

	}

	public static class Slot extends net.minecraft.screen.slot.Slot {

		private final int index;

		public Slot(net.minecraft.inventory.Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
			this.index = index;
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			if (!PouchItem.isPouchableItem(stack))
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

}
