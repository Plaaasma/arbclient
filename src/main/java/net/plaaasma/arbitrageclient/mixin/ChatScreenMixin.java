package net.plaaasma.arbitrageclient.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.plaaasma.arbitrageclient.ArbitrageClientClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(at = @At("HEAD"), method = "sendMessage", cancellable = true)
    public void sendMessage(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (chatText.equals("--arbitrage")) {
            ArbitrageClientClient.enabled = !ArbitrageClientClient.enabled;
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("[").withColor(Colors.GRAY).append("ArbClient").withColor(Colors.LIGHT_RED).append("]").withColor(Colors.GRAY).append(" Arbitrage is now ").withColor(Colors.LIGHT_GRAY).append(ArbitrageClientClient.enabled ? "enabled." : "disabled."));
            }
            MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(chatText);
            MinecraftClient.getInstance().setScreen(null);

            cir.setReturnValue(false);
        }
        else if (chatText.equals("--minecart")) {
            ArbitrageClientClient.minecart = !ArbitrageClientClient.minecart;
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("[").withColor(Colors.GRAY).append("ArbClient").withColor(Colors.LIGHT_RED).append("]").withColor(Colors.GRAY).append(" Auto minecart is now ").withColor(Colors.LIGHT_GRAY).append(ArbitrageClientClient.minecart ? "enabled." : "disabled."));
            }
            MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(chatText);
            MinecraftClient.getInstance().setScreen(null);

            cir.setReturnValue(false);
        }
        else if (chatText.equals("--crafter crash")) {
            ArbitrageClientClient.crafter = !ArbitrageClientClient.crafter;
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("[").withColor(Colors.GRAY).append("ArbClient").withColor(Colors.LIGHT_RED).append("]").withColor(Colors.GRAY).append(" Auto crash crafter is now ").withColor(Colors.LIGHT_GRAY).append(ArbitrageClientClient.crafter ? "enabled." : "disabled."));
            }
            MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(chatText);
            MinecraftClient.getInstance().setScreen(null);

            cir.setReturnValue(false);
        }
        else if (chatText.equals("--crafter gold")) {
            ArbitrageClientClient.gold_crafter = !ArbitrageClientClient.gold_crafter;
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("[").withColor(Colors.GRAY).append("ArbClient").withColor(Colors.LIGHT_RED).append("]").withColor(Colors.GRAY).append(" Auto gold crafter is now ").withColor(Colors.LIGHT_GRAY).append(ArbitrageClientClient.gold_crafter ? "enabled." : "disabled."));
            }
            MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(chatText);
            MinecraftClient.getInstance().setScreen(null);

            cir.setReturnValue(false);
        }
    }
}