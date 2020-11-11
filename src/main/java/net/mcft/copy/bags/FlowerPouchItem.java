package net.mcft.copy.bags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import net.mcft.copy.bags.client.ICustomDurabilityBar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvironmentInterface;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;

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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.ItemTags;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@EnvironmentInterface(value = EnvType.CLIENT, itf = ICustomDurabilityBar.class)
public class FlowerPouchItem extends Item implements IItemPickupSink, ICustomDurabilityBar {

	public static final int SLOTS = 18;
	public static final int MAX_COUNT_PER_STACK = 32;
	public static final int MAX_TOOLTIP_ITEMS = 5; // Maximum items shown in tooltip before some are hidden.
	public static final Identifier IDENTIFIER = new Identifier(PocketBagsMod.MOD_ID, "flower_pouch");

	public static final ScreenHandlerType<FlowerPouchItem.ScreenHandler> SCREEN_HANDLER = ScreenHandlerRegistry
			.registerExtended(IDENTIFIER, FlowerPouchItem.ScreenHandler::new);

	public FlowerPouchItem() {
		super(new Item.Settings().group(ItemGroup.MISC).maxCount(1));
	}

	public void collect(ServerPlayerEntity player, ItemStack pouch, ItemStack pickup) {
		if (!ItemTags.FLOWERS.contains(pickup.getItem()))
			return;
		FlowerPouchItem.Inventory inventory = new FlowerPouchItem.Inventory(pouch);
		// addStack creates a copy of the input stack, so we use setCount.
		pickup.setCount(inventory.addStack(pickup).getCount());
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient && user.isSneaking() && (hand == Hand.MAIN_HAND))
			user.openHandledScreen(new ItemScreenHandler.Factory<>(user, FlowerPouchItem.ScreenHandler.class));
		return super.use(world, user, hand);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		if (player.world.isClient)
			return ActionResult.FAIL;

		ItemStack pouch = context.getStack();
		Inventory inventory = new FlowerPouchItem.Inventory(pouch);
		List<ItemStack> stacks = inventory.stream().collect(Collectors.toList());
		if (stacks.isEmpty())
			return ActionResult.FAIL;

		BlockPos clickedPos = context.getBlockPos();
		Random rnd = player.world.random;
		ActionResult result = ActionResult.FAIL;

		try {
			boolean isFirst = true;
			// TODO: Emulating bone meal code here. Could be more straight-forward.
			outerLoop: for (int i = 0; i < 16; i++) {
				BlockPos pos = clickedPos;
				for (int j = 0; j <= i / 2; j++) {
					if (!isFirst)
						pos = pos.add(rnd.nextInt(3) - 1, rnd.nextInt(3) - 1, rnd.nextInt(3) - 1);

					if (!(isFirst
							|| player.world.getBlockState(pos).isSideSolidFullSquare(player.world, pos, Direction.UP))
							|| !player.world.getBlockState(pos.up()).getFluidState().isEmpty()) {
						if (isFirst)
							break outerLoop;
						else
							continue;
					}

					int index = rnd.nextInt(stacks.size());
					ItemStack stack = stacks.get(index);
					PocketBagsUtil.setStackInHand(player, context.getHand(), stack);

					BlockHitResult hit = new BlockHitResult(Vec3d.ZERO, Direction.UP, pos, false);
					ItemUsageContext newContext = new ItemUsageContext(player, context.getHand(), hit);
					if (stack.getItem().useOnBlock(newContext).isAccepted()) {
						result = ActionResult.SUCCESS;

						// FIXME: Account for the possibility of the item being changed?
						stack.setCount(player.getStackInHand(context.getHand()).getCount());
						if (stack.isEmpty()) {
							stacks.remove(index);
							if (stacks.isEmpty())
								break outerLoop;
						}

						if (isFirst)
							player.world.playSound(null, pos, SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.PLAYERS,
									1.0F, 1.0F);
					} else if (isFirst)
						break outerLoop;

					if (isFirst) {
						isFirst = false;
						if (player.isSneaking())
							break outerLoop;
					}
				}
			}
		} finally {
			// Write changes to the item stack and reset held item back to the pouch.
			inventory.markDirty();
			PocketBagsUtil.setStackInHand(player, context.getHand(), pouch);
		}

		return result;
	}

	@Override
	public ActionResult useOnEntity(ItemStack pouch, PlayerEntity player, LivingEntity origEntity, Hand hand) {
		Inventory inventory = new FlowerPouchItem.Inventory(pouch);
		List<ItemStack> stacks = inventory.stream().collect(Collectors.toList());
		if (stacks.isEmpty())
			return ActionResult.PASS;
		Random rnd = player.world.random;
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

		try {
			// Use contents on found entities.
			for (LivingEntity entity : entities) {
				int index = rnd.nextInt(stacks.size());
				ItemStack stack = stacks.get(index);
				PocketBagsUtil.setStackInHand(player, hand, stack);

				if (entity.interact(player, hand).isAccepted()
						|| stack.getItem().useOnEntity(stack, player, entity, hand).isAccepted()) {
					result = ActionResult.SUCCESS;

					stack.setCount(player.getStackInHand(hand).getCount());
					if (stack.isEmpty()) {
						stacks.remove(index);
						if (stacks.isEmpty())
							break;
					}
				}
			}
		} finally {
			// Write changes to the item stack and reset held item back to the pouch.
			inventory.markDirty();
			PocketBagsUtil.setStackInHand(player, hand, pouch);
		}

		return result;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		List<ItemStack> contents = getContentsStackedAndSorted(stack);
		if (contents.isEmpty())
			return;
		if (contents.size() <= MAX_TOOLTIP_ITEMS)
			for (ItemStack item : contents)
				appendTooltipSingle(item, tooltip);
		else {
			for (int i = 0; i < MAX_TOOLTIP_ITEMS / 2; i++)
				appendTooltipSingle(contents.get(i), tooltip);
			tooltip.add(new LiteralText("and " + contents.size() + " more...") // TODO: Localization.
					.setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)));
			for (int i = contents.size() - MAX_TOOLTIP_ITEMS / 2; i < contents.size(); i++)
				appendTooltipSingle(contents.get(i), tooltip);
		}
	}

	private static void appendTooltipSingle(ItemStack stack, List<Text> tooltip) {
		tooltip.add(new LiteralText(stack.getCount() + "x ").append(stack.getName())
				.setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
	}

	public static List<ItemStack> getContentsStackedAndSorted(ItemStack stack) {
		ArrayList<ItemStack> stacked = new ArrayList<>();
		for (ItemStack item : new FlowerPouchItem.Inventory(stack)) {
			Optional<ItemStack> found = stacked.stream()
					.filter(i -> ItemStack.areItemsEqual(i, item) && ItemStack.areTagsEqual(i, item)).findAny();
			if (found.isPresent())
				found.get().increment(item.getCount());
			else
				stacked.add(item);
		}
		stacked.sort((a, b) -> b.getCount() - a.getCount());
		return stacked;
	}

	@Environment(EnvType.CLIENT)
	public float getCustomDurability(ItemStack stack) {
		int maxCount = SLOTS * MAX_COUNT_PER_STACK;
		int count = 0;
		// TODO: This could be made cheaper by just iterating the tags.
		for (ItemStack item : new FlowerPouchItem.Inventory(stack))
			count += item.getCount();
		return (count > 0) ? (float) count / maxCount : Float.NaN;
	}

	@Environment(EnvType.CLIENT)
	public int getCustomDurabilityColor(float fraction) {
		return MathHelper.packRgb(0.4F, 0.4F, 1.0F);
	}

	public static class Inventory extends ItemInventory {

		public Inventory(ItemStack stack) {
			super(stack, FlowerPouchItem.SLOTS);
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
			super(FlowerPouchItem.SCREEN_HANDLER, syncId, playerInventory, new FlowerPouchItem.Inventory(stack),
					protectedSlot);
		}

		public ScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
			super(FlowerPouchItem.SCREEN_HANDLER, syncId, playerInventory,
					new FlowerPouchItem.Inventory(ItemStack.EMPTY), buf.readByte());
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
