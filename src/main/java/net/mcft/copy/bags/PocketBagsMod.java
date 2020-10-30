package net.mcft.copy.bags;

import net.fabricmc.api.ModInitializer;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class PocketBagsMod implements ModInitializer {

	public static final String MOD_ID = "pocketbags";
	public static final Identifier POUCH_ID = new Identifier(MOD_ID, "pouch");
	public static final Item POUCH = new ItemPouch();

	@Override
	public void onInitialize() {
		Registry.register(Registry.ITEM, POUCH_ID, POUCH);
	}

}
