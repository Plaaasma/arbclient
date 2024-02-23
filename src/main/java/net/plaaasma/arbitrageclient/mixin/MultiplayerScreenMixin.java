package net.plaaasma.arbitrageclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.DataPackContents;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.plaaasma.arbitrageclient.ArbitrageClientClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen {
    private ButtonWidget wormholeButton;
    private TextFieldWidget server1Box;
    private TextFieldWidget server2Box;

    private MultiplayerScreenMixin() {
        super(null);
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        this.server1Box = new TextFieldWidget(this.textRenderer, this.width - 285, 7, 60, 20, Text.of("fart.com"));
        this.server2Box = new TextFieldWidget(this.textRenderer, this.width - 215, 7, 60, 20, Text.of("fart.com"));

        this.addSelectableChild(this.server1Box);
        this.addSelectableChild(this.server2Box);

        this.wormholeButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.of("Wormhole"), (button) -> {
                            ServerInfo testInfo = new ServerInfo("Server1", this.server1Box.getText(), ServerInfo.ServerType.OTHER);
                            ConnectScreen.connect(this, this.client, ServerAddress.parse(testInfo.address), testInfo, false);
                            this.client.currentScreen.close();
                            ServerInfo testInfo2 = new ServerInfo("Server2", this.server2Box.getText(), ServerInfo.ServerType.OTHER);
                            ConnectScreen.connect(this, this.client, ServerAddress.parse(testInfo2.address), testInfo2, false);
                        }).width(140).position(this.width - 145, 7).build());
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.server1Box.render(context, mouseX, mouseY, delta);
        this.server2Box.render(context, mouseX, mouseY, delta);
    }

    @Inject(at = @At("TAIL"), method = "refresh")
    public void refresh(CallbackInfo ci) {
        // Create "Bypass Resource Pack" option
        this.server1Box.setX(this.width - 285);
        this.server1Box.setY(7);
        this.server2Box.setX(this.width - 215);
        this.server2Box.setY(7);

        this.wormholeButton.setX(this.width - 145);
        this.wormholeButton.setY(5);
    }
}