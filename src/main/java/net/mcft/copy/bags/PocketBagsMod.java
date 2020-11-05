package net.mcft.copy.bags;

import net.fabricmc.api.ModInitializer;

import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;

public class PocketBagsMod implements ModInitializer {

	public static final String MOD_ID = "pocketbags";

	public static final Item POUCH = new PouchItem();
	public static final Item FLOWER_POUCH = new FlowerPouchItem();

	@Override
	public void onInitialize() {
		Registry.register(Registry.ITEM, PouchItem.IDENTIFIER, POUCH);
		Registry.register(Registry.ITEM, FlowerPouchItem.IDENTIFIER, FLOWER_POUCH);
	}

}
