package net.mcft.copy.bags.client;

import net.mcft.copy.bags.FlowerPouchItem;
import net.mcft.copy.bags.PocketBagsMod;
import net.mcft.copy.bags.PouchItem;

import net.minecraft.item.DyeableItem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

@Environment(EnvType.CLIENT)
public class PocketBagsModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ScreenRegistry.register(PouchItem.SCREEN_HANDLER, ItemScreen::new);
		ScreenRegistry.register(FlowerPouchItem.SCREEN_HANDLER, ItemScreen::new);

		ColorProviderRegistry.ITEM.register((stack, tintIndex) -> ((DyeableItem) stack.getItem()).getColor(stack),
				PocketBagsMod.POUCH);
	}

}
