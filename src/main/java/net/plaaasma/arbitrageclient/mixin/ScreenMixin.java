package net.plaaasma.arbitrageclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Colors;
import net.plaaasma.arbitrageclient.ArbitrageClientClient;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("all")
@Mixin(Screen.class)
public class ScreenMixin {
	private static final MinecraftClient client = MinecraftClient.getInstance();

	private void doIngotText(DrawContext matrixStack, int x, int y) {
		if (ArbitrageClientClient.coal_profitable) {
			matrixStack.drawText(client.textRenderer, "Coal | " + ArbitrageClientClient.coal_margin, x, y, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Coal | " + ArbitrageClientClient.coal_margin, x, y, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.iron_profitable) {
			matrixStack.drawText(client.textRenderer, "Iron | " + ArbitrageClientClient.iron_margin, x, y - 10, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Iron | " + ArbitrageClientClient.iron_margin, x, y - 10, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.gold_profitable) {
			matrixStack.drawText(client.textRenderer, "Gold | " + ArbitrageClientClient.gold_margin, x, y - 20, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Gold | " + ArbitrageClientClient.gold_margin, x, y - 20, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.lapis_profitable) {
			matrixStack.drawText(client.textRenderer, "Lapis | " + ArbitrageClientClient.lapis_margin, x, y - 30, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Lapis | " + ArbitrageClientClient.lapis_margin, x, y - 30, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.diamond_profitable) {
			matrixStack.drawText(client.textRenderer, "Diamond | " + ArbitrageClientClient.diamond_margin, x, y - 40, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Diamond | " + ArbitrageClientClient.diamond_margin, x, y - 40, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.emerald_profitable) {
			matrixStack.drawText(client.textRenderer, "Emerald | " + ArbitrageClientClient.emerald_margin, x, y - 50, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Emerald | " + ArbitrageClientClient.emerald_margin, x, y - 50, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.netherite_profitable) {
			matrixStack.drawText(client.textRenderer, "Netherite | " + ArbitrageClientClient.netherite_margin, x, y - 60, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Netherite | " + ArbitrageClientClient.netherite_margin, x, y - 60, Colors.GRAY, true);
		}
	}

	private void doBlockText(DrawContext matrixStack, int x, int y) {
		if (ArbitrageClientClient.coal_block_profitable) {
			matrixStack.drawText(client.textRenderer, "Coal | " + ArbitrageClientClient.coal_block_margin, x, y, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Coal | " + ArbitrageClientClient.coal_block_margin, x, y, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.iron_block_profitable) {
			matrixStack.drawText(client.textRenderer, "Iron | " + ArbitrageClientClient.iron_block_margin, x, y - 10, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Iron | " + ArbitrageClientClient.iron_block_margin, x, y - 10, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.gold_block_profitable) {
			matrixStack.drawText(client.textRenderer, "Gold | " + ArbitrageClientClient.gold_block_margin, x, y - 20, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Gold | " + ArbitrageClientClient.gold_block_margin, x, y - 20, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.lapis_block_profitable) {
			matrixStack.drawText(client.textRenderer, "Lapis | " + ArbitrageClientClient.lapis_block_margin, x, y - 30, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Lapis | " + ArbitrageClientClient.lapis_block_margin, x, y - 30, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.diamond_block_profitable) {
			matrixStack.drawText(client.textRenderer, "Diamond | " + ArbitrageClientClient.diamond_block_margin, x, y - 40, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Diamond | " + ArbitrageClientClient.diamond_block_margin, x, y - 40, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.emerald_block_profitable) {
			matrixStack.drawText(client.textRenderer, "Emerald | " + ArbitrageClientClient.emerald_block_margin, x, y - 50, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Emerald | " + ArbitrageClientClient.emerald_block_margin, x, y - 50, Colors.GRAY, true);
		}

		if (ArbitrageClientClient.netherite_block_profitable) {
			matrixStack.drawText(client.textRenderer, "Netherite | " + ArbitrageClientClient.netherite_block_margin, x, y - 60, Colors.LIGHT_RED, true);
		} else {
			matrixStack.drawText(client.textRenderer, "Netherite | " + ArbitrageClientClient.netherite_block_margin, x, y - 60, Colors.GRAY, true);
		}
	}

	@Inject(at = @At("TAIL"), method = "render")
	public void render(DrawContext matrixStack, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (client.currentScreen.getTitle().getString().contains("Ores")) {
			int x = client.getWindow().getScaledWidth() - 300; // 100 pixels from the right edge of the screen
			int y = client.getWindow().getScaledHeight() / 2; // Vertically centered

			// Draw the text
			doIngotText(matrixStack, x, y);
			doBlockText(matrixStack, x - 450, y);
		}
	}
}