package net.plaaasma.arbitrageclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.plaaasma.arbitrageclient.ArbitrageClientClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(at = @At("HEAD"), method = "sendMessage", cancellable = true)
    public void sendMessage(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (chatText.equals("--")) {
            ArbitrageClientClient.enabled = !ArbitrageClientClient.enabled;
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.of("Arbitrage is now " + (ArbitrageClientClient.enabled ? "enabled" : "disabled") + "."));
            }
            MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(chatText);
            MinecraftClient.getInstance().setScreen(null);
            cir.setReturnValue(false);
        }
    }
}