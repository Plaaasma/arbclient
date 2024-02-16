package net.plaaasma.arbitrageclient;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.recipe.book.RecipeBook;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArbitrageClientClient implements ClientModInitializer {
    public static final Logger C_LOGGER = LoggerFactory.getLogger(ArbitrageClient.MOD_ID);

    public static boolean coal_profitable = false;
    public static double coal_margin = 0;
    public static boolean iron_profitable = false;
    public static double iron_margin = 0;
    public static boolean gold_profitable = false;
    public static double gold_margin = 0;
    public static boolean lapis_profitable = false;
    public static double lapis_margin = 0;
    public static boolean diamond_profitable = false;
    public static double diamond_margin = 0;
    public static boolean emerald_profitable = false;
    public static double emerald_margin = 0;
    public static boolean netherite_profitable = false;
    public static double netherite_margin = 0;

    public static boolean coal_block_profitable = false;
    public static double coal_block_margin = 0;
    public static boolean iron_block_profitable = false;
    public static double iron_block_margin = 0;
    public static boolean gold_block_profitable = false;
    public static double gold_block_margin = 0;
    public static boolean lapis_block_profitable = false;
    public static double lapis_block_margin = 0;
    public static boolean diamond_block_profitable = false;
    public static double diamond_block_margin = 0;
    public static boolean emerald_block_profitable = false;
    public static double emerald_block_margin = 0;
    public static boolean netherite_block_profitable = false;
    public static double netherite_block_margin = 0;

    private HashMap<String, List<Double>> getPriceMap(ScreenHandler screenHandler) {
        HashMap<String, List<Double>> priceMap = new HashMap<>();

        for (int i = 0; i < screenHandler.slots.size(); i++) {
            Slot slot = screenHandler.slots.get(i);
            ItemStack slotStack = slot.getStack();
            if (!slotStack.isOf(Items.AIR)) {
                if (slotStack.hasNbt()) {
                    NbtCompound slotTag = slotStack.getNbt();
                    if (slotTag.contains("display")) {
                        NbtCompound displayTag = slotTag.getCompound("display");
                        if (displayTag.contains("Lore", NbtElement.LIST_TYPE)) {
                            NbtList loreList = displayTag.getList("Lore", NbtElement.STRING_TYPE);
                            double sellPrice = 0;
                            double buyPrice = 0;
                            if (loreList.size() > 1) {
                                String sellDictString = loreList.getString(0);
                                String buyDictString = loreList.getString(1);
                                JsonElement sellJson = JsonParser.parseString(sellDictString);
                                JsonElement buyJson = JsonParser.parseString(buyDictString);
                                String sellText = sellJson.getAsJsonObject().get("extra").getAsJsonArray().get(0).getAsJsonObject().get("text").toString();
                                String buyText = buyJson.getAsJsonObject().get("extra").getAsJsonArray().get(0).getAsJsonObject().get("text").toString();
                                if (sellText.toLowerCase().contains("sell")) {
                                    sellPrice = Double.parseDouble(sellText.replaceAll("[^0-9.]", ""));
                                    buyPrice = Double.parseDouble(buyText.replaceAll("[^0-9.]", ""));
                                }
                            }
                            List<Double> priceList = new ArrayList<>();
                            priceList.add(sellPrice);
                            priceList.add(buyPrice);

                            priceMap.put(slotStack.getName().getString(), priceList);
                        }
                    }
                }
            }
        }

        return priceMap;
    }

    public void doCheckAndPurchase(HandledScreen<?> handledScreen, ScreenHandler screenHandler, MinecraftClient client) {
        if (handledScreen.getTitle().getString().contains("Ores")) {
            HashMap<String, List<Double>> priceMap = getPriceMap(screenHandler);

            if (priceMap.containsKey("Coal") && priceMap.containsKey("Block of Coal")) {
                double coalBlockMakePrice = priceMap.get("Coal").get(1) * 9;
                double coalBlockSellPrice = priceMap.get("Block of Coal").get(0);
                coal_profitable = coalBlockMakePrice < coalBlockSellPrice;
                coal_margin = coalBlockSellPrice - coalBlockMakePrice;

                double coalIngotMakePrice = priceMap.get("Block of Coal").get(1) / 9;
                double coalIngotSellPrice = priceMap.get("Coal").get(0);
                coal_block_profitable = coalIngotMakePrice < coalIngotSellPrice;
                coal_block_margin = coalIngotSellPrice - coalIngotMakePrice;
            }
            if (priceMap.containsKey("Iron Ingot") && priceMap.containsKey("Block of Iron")) {
                double ironBlockMakePrice = priceMap.get("Iron Ingot").get(1) * 9;
                double ironBlockSellPrice = priceMap.get("Block of Iron").get(0);
                iron_profitable = ironBlockMakePrice < ironBlockSellPrice;
                iron_margin = ironBlockSellPrice - ironBlockMakePrice;

                double ironIngotMakePrice = priceMap.get("Block of Iron").get(1) / 9;
                double ironIngotSellPrice = priceMap.get("Iron Ingot").get(0);
                iron_block_profitable = ironIngotMakePrice < ironIngotSellPrice;
                iron_block_margin = ironIngotSellPrice - ironIngotMakePrice;
            }
            if (priceMap.containsKey("Gold Ingot") && priceMap.containsKey("Block of Gold")) {
                double goldBlockMakePrice = priceMap.get("Gold Ingot").get(1) * 9;
                double goldBlockSellPrice = priceMap.get("Block of Gold").get(0);
                gold_profitable = goldBlockMakePrice < goldBlockSellPrice;
                gold_margin = goldBlockSellPrice - goldBlockMakePrice;

                double goldIngotMakePrice = priceMap.get("Block of Gold").get(1) / 9;
                double goldIngotSellPrice = priceMap.get("Gold Ingot").get(0);
                gold_block_profitable = goldIngotMakePrice < goldIngotSellPrice;
                gold_block_margin = goldIngotSellPrice - goldIngotMakePrice;
            }
            if (priceMap.containsKey("Lapis Lazuli") && priceMap.containsKey("Block of Lapis Lazuli")) {
                double lapisBlockMakePrice = priceMap.get("Lapis Lazuli").get(1) * 9;
                double lapisBlockSellPrice = priceMap.get("Block of Lapis Lazuli").get(0);
                lapis_profitable = lapisBlockMakePrice < lapisBlockSellPrice;
                lapis_margin = lapisBlockSellPrice - lapisBlockMakePrice;

                double lapisIngotMakePrice = priceMap.get("Block of Lapis Lazuli").get(1) / 9;
                double lapisIngotSellPrice = priceMap.get("Lapis Lazuli").get(0);
                lapis_block_profitable = lapisIngotMakePrice < lapisIngotSellPrice;
                lapis_block_margin = lapisIngotSellPrice - lapisIngotMakePrice;
            }
            if (priceMap.containsKey("Diamond") && priceMap.containsKey("Block of Diamond")) {
                double diamondBlockMakePrice = priceMap.get("Diamond").get(1) * 9;
                double diamondBlockSellPrice = priceMap.get("Block of Diamond").get(0);
                diamond_profitable = diamondBlockMakePrice < diamondBlockSellPrice;
                diamond_margin = diamondBlockSellPrice - diamondBlockMakePrice;

                double diamondIngotMakePrice = priceMap.get("Block of Diamond").get(1) / 9;
                double diamondIngotSellPrice = priceMap.get("Diamond").get(0);
                diamond_block_profitable = diamondIngotMakePrice < diamondIngotSellPrice;
                diamond_block_margin = diamondIngotSellPrice - diamondIngotMakePrice;
            }
            if (priceMap.containsKey("Emerald") && priceMap.containsKey("Block of Emerald")) {
                double emeraldBlockMakePrice = priceMap.get("Emerald").get(1) * 9;
                double emeraldBlockSellPrice = priceMap.get("Block of Emerald").get(0);
                emerald_profitable = emeraldBlockMakePrice < emeraldBlockSellPrice;
                emerald_margin = emeraldBlockSellPrice - emeraldBlockMakePrice;

                double emeraldIngotMakePrice = priceMap.get("Block of Emerald").get(1) / 9;
                double emeraldIngotSellPrice = priceMap.get("Emerald").get(0);
                emerald_block_profitable = emeraldIngotMakePrice < emeraldIngotSellPrice;
                emerald_block_margin = emeraldIngotSellPrice - emeraldIngotMakePrice;
            }
            if (priceMap.containsKey("Netherite Ingot") && priceMap.containsKey("Block of Netherite")) {
                double netheriteBlockMakePrice = priceMap.get("Netherite Ingot").get(1) * 9;
                double netheriteBlockSellPrice = priceMap.get("Block of Netherite").get(0);
                netherite_profitable = netheriteBlockMakePrice < netheriteBlockSellPrice;
                netherite_margin = netheriteBlockSellPrice - netheriteBlockMakePrice;

                double netheriteIngotMakePrice = priceMap.get("Block of Netherite").get(1) / 9;
                double netheriteIngotSellPrice = priceMap.get("Netherite Ingot").get(0);
                netherite_block_profitable = netheriteIngotMakePrice < netheriteIngotSellPrice;
                netherite_block_margin = netheriteIngotSellPrice - netheriteIngotMakePrice;
            }
        }
//        else if (handledScreen.getTitle().getString().contains("Start")) {
//            ItemStack slotStack = screenHandler.getSlot(14).getStack();
//            Int2ObjectOpenHashMap<ItemStack> slotInt2ObjectMap = new Int2ObjectOpenHashMap<ItemStack>();
//            DefaultedList<Slot> defaultedList = screenHandler.slots;
//            int i = defaultedList.size();
//            ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
//            for (Slot slot : defaultedList) {
//                list.add(slot.getStack().copy());
//            }
//            for (int j = 0; j < i; ++j) {
//                ItemStack itemStack2;
//                ItemStack itemStack = list.get(j);
//                if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
//                slotInt2ObjectMap.put(j, itemStack2.copy());
//            }
//
//            client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), 14, 0, SlotActionType.PICKUP, slotStack, slotInt2ObjectMap));
//        }
//        else if (handledScreen.getTitle().getString().contains("Trade")) {
//            ItemStack slotStack = screenHandler.getSlot(17).getStack();
//            Int2ObjectOpenHashMap<ItemStack> slotInt2ObjectMap = new Int2ObjectOpenHashMap<ItemStack>();
//            DefaultedList<Slot> defaultedList = screenHandler.slots;
//            int i = defaultedList.size();
//            ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
//            for (Slot slot : defaultedList) {
//                list.add(slot.getStack().copy());
//            }
//            for (int j = 0; j < i; ++j) {
//                ItemStack itemStack2;
//                ItemStack itemStack = list.get(j);
//                if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
//                slotInt2ObjectMap.put(j, itemStack2.copy());
//            }
//
//            client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), 17, 0, SlotActionType.PICKUP, slotStack, slotInt2ObjectMap));
//            client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(screenHandler.syncId));
//        }
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (client.currentScreen instanceof HandledScreen) {
                HandledScreen<?> handledScreen = (HandledScreen<?>) client.currentScreen;
                ScreenHandler screenHandler = handledScreen.getScreenHandler();

                doCheckAndPurchase(handledScreen, screenHandler, client);
            }
        });
    }
}
