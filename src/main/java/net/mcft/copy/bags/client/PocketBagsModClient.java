package net.mcft.copy.bags.client;

import net.mcft.copy.bags.ItemFlowerPouch;
import net.mcft.copy.bags.ItemPouch;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

@Environment(EnvType.CLIENT)
public class PocketBagsModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ScreenRegistry.register(ItemPouch.SCREEN_HANDLER, ItemScreen::new);
		ScreenRegistry.register(ItemFlowerPouch.SCREEN_HANDLER, ItemScreen::new);
	}

}
