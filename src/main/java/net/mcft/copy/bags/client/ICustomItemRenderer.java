package net.mcft.copy.bags.client;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

@Environment(EnvType.CLIENT)
public interface ICustomItemRenderer<T> {

	void render(T item, ItemStack stack, int x, int y, TextRenderer renderer);

	public static final class Registry {

		private Registry() {
		}

		public static final List<Pair<Class<?>, ICustomItemRenderer<Object>>> entries = new ArrayList<>();

		@SuppressWarnings("unchecked")
		public static <T> void register(Class<T> clazz, ICustomItemRenderer<T> renderer) {
			entries.add(new Pair<>(clazz, (ICustomItemRenderer<Object>) renderer));
		}

	}

}
