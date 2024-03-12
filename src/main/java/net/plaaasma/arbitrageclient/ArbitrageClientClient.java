package net.plaaasma.arbitrageclient;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.recipe.book.RecipeBook;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArbitrageClientClient implements ClientModInitializer {
    public static final Logger C_LOGGER = LoggerFactory.getLogger(ArbitrageClient.MOD_ID);

    public static boolean enabled = false;
    public static boolean minecart = false;
    public static boolean crafter = false;
    public static boolean gold_crafter = false;

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

    public static List<Packet<?>> packetQueue = new ArrayList<>();

    private static long lastShopTime = 0;

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
                                if (sellDictString != null && buyDictString != null) {
                                    if (JsonParser.parseString(sellDictString).isJsonObject() && JsonParser.parseString(buyDictString).isJsonObject()) {
                                        JsonObject sellJson = JsonParser.parseString(sellDictString).getAsJsonObject();
                                        JsonObject buyJson = JsonParser.parseString(buyDictString).getAsJsonObject();
                                        if (sellJson != null && buyJson != null) {
                                            if (sellJson.has("extra") && buyJson.has("extra")) {
                                                String sellText = sellJson.get("extra").getAsJsonArray().get(0).getAsJsonObject().get("text").toString();
                                                String buyText = buyJson.get("extra").getAsJsonArray().get(0).getAsJsonObject().get("text").toString();
                                                if (sellText.toLowerCase().contains("sell")) {
                                                    sellPrice = Double.parseDouble(sellText.replaceAll("[^0-9.]", ""));
                                                    buyPrice = Double.parseDouble(buyText.replaceAll("[^0-9.]", ""));
                                                }

                                            }
                                        }
                                    }
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

    public void navigateToPurchase(int slot_num, HandledScreen<?> handledScreen, ScreenHandler screenHandler, MinecraftClient client) {
        if (handledScreen.getTitle().getString().contains("Ores")) {
            ItemStack slotStack = screenHandler.getSlot(slot_num).getStack();
            Int2ObjectOpenHashMap<ItemStack> slotInt2ObjectMap = new Int2ObjectOpenHashMap<ItemStack>();
            DefaultedList<Slot> defaultedList = screenHandler.slots;
            int i = defaultedList.size();
            ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
            for (Slot slot : defaultedList) {
                list.add(slot.getStack().copy());
            }
            for (int j = 0; j < i; ++j) {
                ItemStack itemStack2;
                ItemStack itemStack = list.get(j);
                if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
                slotInt2ObjectMap.put(j, itemStack2.copy());
            }

            packetQueue.add(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), slot_num, 0, SlotActionType.PICKUP, slotStack, slotInt2ObjectMap));
        }
    }

    public void doCheckAndPurchase(HandledScreen<?> handledScreen, ScreenHandler screenHandler, MinecraftClient client) {
        if (handledScreen.getTitle().getString().contains("Ores")) {
            if (hasSellables(client)) {
                if (enabled) {
                    Item sellItem = sellableItem(client);
                    if (sellItem == Items.COAL_BLOCK) {
                        navigateToPurchase(19, handledScreen, screenHandler, client);
                    } else if (sellItem == Items.IRON_BLOCK) {
                        navigateToPurchase(20, handledScreen, screenHandler, client);
                    } else if (sellItem == Items.GOLD_BLOCK) {
                        navigateToPurchase(21, handledScreen, screenHandler, client);
                    } else if (sellItem == Items.LAPIS_BLOCK) {
                        navigateToPurchase(22, handledScreen, screenHandler, client);
                    } else if (sellItem == Items.DIAMOND_BLOCK) {
                        navigateToPurchase(23, handledScreen, screenHandler, client);
                    } else if (sellItem == Items.EMERALD_BLOCK) {
                        navigateToPurchase(24, handledScreen, screenHandler, client);
                    } else if (sellItem == Items.NETHERITE_BLOCK) {
                        navigateToPurchase(25, handledScreen, screenHandler, client);
                    }
                }
            }
            else {
                HashMap<String, List<Double>> priceMap = getPriceMap(screenHandler);

                if (priceMap.containsKey("Coal") && priceMap.containsKey("Block of Coal")) {
                    double coalBlockMakePrice = priceMap.get("Coal").get(1) * 9;
                    double coalBlockSellPrice = priceMap.get("Block of Coal").get(0);
                    coal_profitable = coalBlockSellPrice - coalBlockMakePrice > 0;
                    coal_margin = coalBlockSellPrice - coalBlockMakePrice;

                    if (enabled && coal_profitable && coal_margin >= 0.05) {
                        navigateToPurchase(10, handledScreen, screenHandler, client);
                    }

                    double coalIngotMakePrice = priceMap.get("Block of Coal").get(1) / 9;
                    double coalIngotSellPrice = priceMap.get("Coal").get(0);
                    coal_block_profitable = coalIngotSellPrice - coalIngotMakePrice > 0;
                    coal_block_margin = coalIngotSellPrice - coalIngotMakePrice;
                }
                if (priceMap.containsKey("Iron Ingot") && priceMap.containsKey("Block of Iron")) {
                    double ironBlockMakePrice = priceMap.get("Iron Ingot").get(1) * 9;
                    double ironBlockSellPrice = priceMap.get("Block of Iron").get(0);
                    iron_profitable = ironBlockSellPrice - ironBlockMakePrice > 0;
                    iron_margin = ironBlockSellPrice - ironBlockMakePrice;

                    if (enabled && iron_profitable && iron_margin >= 0.05) {
                        navigateToPurchase(11, handledScreen, screenHandler, client);
                    }

                    double ironIngotMakePrice = priceMap.get("Block of Iron").get(1) / 9;
                    double ironIngotSellPrice = priceMap.get("Iron Ingot").get(0);
                    iron_block_profitable = ironIngotSellPrice - ironIngotMakePrice > 0;
                    iron_block_margin = ironIngotSellPrice - ironIngotMakePrice;
                }
                if (priceMap.containsKey("Gold Ingot") && priceMap.containsKey("Block of Gold")) {
                    double goldBlockMakePrice = priceMap.get("Gold Ingot").get(1) * 9;
                    double goldBlockSellPrice = priceMap.get("Block of Gold").get(0);
                    gold_profitable = goldBlockSellPrice - goldBlockMakePrice > 0;
                    gold_margin = goldBlockSellPrice - goldBlockMakePrice;

                    if (enabled && gold_profitable && gold_margin >= 5) {
                        navigateToPurchase(12, handledScreen, screenHandler, client);
                    }

                    double goldIngotMakePrice = priceMap.get("Block of Gold").get(1) / 9;
                    double goldIngotSellPrice = priceMap.get("Gold Ingot").get(0);
                    gold_block_profitable = goldIngotSellPrice - goldIngotMakePrice > 0;
                    gold_block_margin = goldIngotSellPrice - goldIngotMakePrice;
                }
                if (priceMap.containsKey("Lapis Lazuli") && priceMap.containsKey("Block of Lapis Lazuli")) {
                    double lapisBlockMakePrice = priceMap.get("Lapis Lazuli").get(1) * 9;
                    double lapisBlockSellPrice = priceMap.get("Block of Lapis Lazuli").get(0);
                    lapis_profitable = lapisBlockSellPrice - lapisBlockMakePrice > 0;
                    lapis_margin = lapisBlockSellPrice - lapisBlockMakePrice;

                    if (enabled && lapis_profitable && lapis_margin >= 8) {
                        navigateToPurchase(13, handledScreen, screenHandler, client);
                    }

                    double lapisIngotMakePrice = priceMap.get("Block of Lapis Lazuli").get(1) / 9;
                    double lapisIngotSellPrice = priceMap.get("Lapis Lazuli").get(0);
                    lapis_block_profitable = lapisIngotSellPrice - lapisIngotMakePrice > 0;
                    lapis_block_margin = lapisIngotSellPrice - lapisIngotMakePrice;
                }
                if (priceMap.containsKey("Diamond") && priceMap.containsKey("Block of Diamond")) {
                    double diamondBlockMakePrice = priceMap.get("Diamond").get(1) * 9;
                    double diamondBlockSellPrice = priceMap.get("Block of Diamond").get(0);
                    diamond_profitable = diamondBlockSellPrice - diamondBlockMakePrice > 0;
                    diamond_margin = diamondBlockSellPrice - diamondBlockMakePrice;

                    if (enabled && diamond_profitable && diamond_margin >= 10) {
                        navigateToPurchase(14, handledScreen, screenHandler, client);
                    }

                    double diamondIngotMakePrice = priceMap.get("Block of Diamond").get(1) / 9;
                    double diamondIngotSellPrice = priceMap.get("Diamond").get(0);
                    diamond_block_profitable = diamondIngotSellPrice - diamondIngotMakePrice > 0;
                    diamond_block_margin = diamondIngotSellPrice - diamondIngotMakePrice;
                }
                if (priceMap.containsKey("Emerald") && priceMap.containsKey("Block of Emerald")) {
                    double emeraldBlockMakePrice = priceMap.get("Emerald").get(1) * 9;
                    double emeraldBlockSellPrice = priceMap.get("Block of Emerald").get(0);
                    emerald_profitable = emeraldBlockSellPrice - emeraldBlockMakePrice > 0;
                    emerald_margin = emeraldBlockSellPrice - emeraldBlockMakePrice;

                    if (enabled && emerald_profitable && emerald_margin >= 12) {
                        navigateToPurchase(15, handledScreen, screenHandler, client);
                    }

                    double emeraldIngotMakePrice = priceMap.get("Block of Emerald").get(1) / 9;
                    double emeraldIngotSellPrice = priceMap.get("Emerald").get(0);
                    emerald_block_profitable = emeraldIngotSellPrice - emeraldIngotMakePrice > 0;
                    emerald_block_margin = emeraldIngotSellPrice - emeraldIngotMakePrice;
                }
                if (priceMap.containsKey("Netherite Ingot") && priceMap.containsKey("Block of Netherite")) {
                    double netheriteBlockMakePrice = priceMap.get("Netherite Ingot").get(1) * 9;
                    double netheriteBlockSellPrice = priceMap.get("Block of Netherite").get(0);
                    netherite_profitable = netheriteBlockSellPrice - netheriteBlockMakePrice > 0;
                    netherite_margin = netheriteBlockSellPrice - netheriteBlockMakePrice;

                    if (enabled && netherite_profitable && netherite_margin >= 20) {
                        navigateToPurchase(16, handledScreen, screenHandler, client);
                    }

                    double netheriteIngotMakePrice = priceMap.get("Block of Netherite").get(1) / 9;
                    double netheriteIngotSellPrice = priceMap.get("Netherite Ingot").get(0);
                    netherite_block_profitable = netheriteIngotSellPrice - netheriteIngotMakePrice > 0;
                    netherite_block_margin = netheriteIngotSellPrice - netheriteIngotMakePrice;
                }
            }
        }
        else if (handledScreen.getTitle().getString().contains("Start")) {
            if (enabled) {
                ItemStack slotStack = screenHandler.getSlot(14).getStack();
                Int2ObjectOpenHashMap<ItemStack> slotInt2ObjectMap = new Int2ObjectOpenHashMap<ItemStack>();
                DefaultedList<Slot> defaultedList = screenHandler.slots;
                int i = defaultedList.size();
                ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
                for (Slot slot : defaultedList) {
                    list.add(slot.getStack().copy());
                }
                for (int j = 0; j < i; ++j) {
                    ItemStack itemStack2;
                    ItemStack itemStack = list.get(j);
                    if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
                    slotInt2ObjectMap.put(j, itemStack2.copy());
                }

                packetQueue.add(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), 14, 0, SlotActionType.PICKUP, slotStack, slotInt2ObjectMap));
            }
        }
        else if (handledScreen.getTitle().getString().contains("Trade")) {
            if (enabled && packetQueue.size() == 0) {
                Int2ObjectOpenHashMap<ItemStack> slotInt2ObjectMap = new Int2ObjectOpenHashMap<ItemStack>();
                DefaultedList<Slot> defaultedList = screenHandler.slots;
                int i = defaultedList.size();
                ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
                for (Slot slot : defaultedList) {
                    list.add(slot.getStack().copy());
                }
                for (int j = 0; j < i; ++j) {
                    ItemStack itemStack2;
                    ItemStack itemStack = list.get(j);
                    if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
                    slotInt2ObjectMap.put(j, itemStack2.copy());
                }

                if (hasSellables(client)) {
                    ItemStack slotStack = screenHandler.getSlot(8).getStack();

                    ClickSlotC2SPacket clickSlotC2SPacket = new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), 8, 0, SlotActionType.PICKUP, slotStack, slotInt2ObjectMap);
                    packetQueue.add(clickSlotC2SPacket);
                    packetQueue.add(new CloseHandledScreenC2SPacket(screenHandler.syncId));
                }
                else {
                    ItemStack slotStack = screenHandler.getSlot(16).getStack();

                    if (!slotStack.getName().getString().toLowerCase().contains("block")) {
                        ClickSlotC2SPacket clickSlotC2SPacket = new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), 16, 0, SlotActionType.PICKUP, slotStack, slotInt2ObjectMap);
                        packetQueue.add(clickSlotC2SPacket);
                        packetQueue.add(new CloseHandledScreenC2SPacket(screenHandler.syncId));
                    }
                    else {
                        packetQueue.add(new CloseHandledScreenC2SPacket(screenHandler.syncId));
                    }
                }
            }
        }
        else if (handledScreen.getTitle().getString().contains("Craft")) {
            if (enabled && packetQueue.size() == 0) {
                if (hasCraftables(client)) {
                    Int2ObjectOpenHashMap<ItemStack> slotInt2ObjectMap = new Int2ObjectOpenHashMap<ItemStack>();
                    DefaultedList<Slot> defaultedList = screenHandler.slots;
                    int i = defaultedList.size();
                    ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
                    for (Slot slot : defaultedList) {
                        list.add(slot.getStack().copy());
                    }
                    for (int j = 0; j < i; ++j) {
                        ItemStack itemStack2;
                        ItemStack itemStack = list.get(j);
                        if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
                        slotInt2ObjectMap.put(j, itemStack2.copy());
                    }

                    ItemStack slotStack = screenHandler.getSlot(0).getStack();
                    if (slotStack.getItem() != Items.AIR) {
                        packetQueue.add(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), 0, 0, SlotActionType.QUICK_MOVE, slotStack, slotInt2ObjectMap));
                    }

                    int slotToPullFrom = craftableSlot(screenHandler, client);
                    if (slotToPullFrom > 9) {
                        slotStack = screenHandler.getSlot(slotToPullFrom).getStack();

                        packetQueue.add(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), slotToPullFrom, 0, SlotActionType.PICKUP, slotStack, slotInt2ObjectMap));
                    }
                    for (int j = 1; j < 10; j++) {
                        slotStack = screenHandler.getSlot(j).getStack();

                        packetQueue.add(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), j, 1, SlotActionType.PICKUP, slotStack, slotInt2ObjectMap));
                    }
                }
                else {
                    Int2ObjectOpenHashMap<ItemStack> slotInt2ObjectMap = new Int2ObjectOpenHashMap<ItemStack>();
                    DefaultedList<Slot> defaultedList = screenHandler.slots;
                    int i = defaultedList.size();
                    ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
                    for (Slot slot : defaultedList) {
                        list.add(slot.getStack().copy());
                    }
                    for (int j = 0; j < i; ++j) {
                        ItemStack itemStack2;
                        ItemStack itemStack = list.get(j);
                        if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
                        slotInt2ObjectMap.put(j, itemStack2.copy());
                    }

                    ItemStack slotStack = screenHandler.getSlot(0).getStack();
                    if (slotStack.getItem() != Items.AIR) {
                        packetQueue.add(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), 0, 0, SlotActionType.QUICK_MOVE, slotStack, slotInt2ObjectMap));
                    }

                    packetQueue.add(new CloseHandledScreenC2SPacket(screenHandler.syncId));
                }
            }
        }
    }

    public boolean hasCraftables(MinecraftClient client) {
        boolean canCraft = false;

        ItemStack heldStack = client.player.currentScreenHandler.getCursorStack();
        for (int slot = 0; slot < client.player.getInventory().size(); slot++) {
            ItemStack slotStack = client.player.getInventory().getStack(slot);
            if ((slotStack.isOf(Items.COAL) || slotStack.isOf(Items.IRON_INGOT) || slotStack.isOf(Items.GOLD_INGOT) || slotStack.isOf(Items.LAPIS_LAZULI) || slotStack.isOf(Items.EMERALD) || slotStack.isOf(Items.DIAMOND) || slotStack.isOf(Items.NETHERITE_INGOT))) {
                int itemCount = slotStack.getCount();
                if (itemCount >= 9) {
                    canCraft = true;
                    break;
                }
            }
            else if (heldStack.isOf(Items.COAL) || heldStack.isOf(Items.IRON_INGOT) || heldStack.isOf(Items.GOLD_INGOT) || heldStack.isOf(Items.LAPIS_LAZULI) || heldStack.isOf(Items.EMERALD) || heldStack.isOf(Items.DIAMOND) || heldStack.isOf(Items.NETHERITE_INGOT)) {
                int itemCount = heldStack.getCount();
                if (itemCount >= 9) {
                    canCraft = true;
                    break;
                }
            }
        }

        return canCraft;
    }

    public int craftableSlot(ScreenHandler screenHandler, MinecraftClient client) {
        int craftSlot = -1;

        for (int slot = 0; slot < client.player.getInventory().size(); slot++) {
            ItemStack slotStack = client.player.getInventory().getStack(slot);
            if (slotStack.isOf(Items.COAL) || slotStack.isOf(Items.IRON_INGOT) || slotStack.isOf(Items.GOLD_INGOT) || slotStack.isOf(Items.LAPIS_LAZULI) || slotStack.isOf(Items.EMERALD) || slotStack.isOf(Items.DIAMOND) || slotStack.isOf(Items.NETHERITE_INGOT)) {
                if (slotStack.getCount() >= 9) {
                    craftSlot = screenHandler.getSlotIndex(client.player.getInventory(), slot).getAsInt();
                    break;
                }
            }
        }

        return craftSlot;
    }

    public boolean hasSellables(MinecraftClient client) {
        boolean canSell = false;

        for (int slot = 0; slot < client.player.getInventory().size(); slot++) {
            ItemStack slotStack = client.player.getInventory().getStack(slot);
            if (slotStack.isOf(Items.COAL_BLOCK) || slotStack.isOf(Items.IRON_BLOCK) || slotStack.isOf(Items.GOLD_BLOCK) || slotStack.isOf(Items.LAPIS_BLOCK) || slotStack.isOf(Items.EMERALD_BLOCK) || slotStack.isOf(Items.DIAMOND_BLOCK) || slotStack.isOf(Items.NETHERITE_BLOCK)) {
                canSell = true;
            }
        }

        return canSell;
    }

    public Item sellableItem(MinecraftClient client) {
        Item sellItem = Items.AIR;

        for (int slot = 0; slot < client.player.getInventory().size(); slot++) {
            ItemStack slotStack = client.player.getInventory().getStack(slot);
            if (slotStack.isOf(Items.COAL_BLOCK) || slotStack.isOf(Items.IRON_BLOCK) || slotStack.isOf(Items.GOLD_BLOCK) || slotStack.isOf(Items.LAPIS_BLOCK) || slotStack.isOf(Items.EMERALD_BLOCK) || slotStack.isOf(Items.DIAMOND_BLOCK) || slotStack.isOf(Items.NETHERITE_BLOCK)) {
                sellItem = slotStack.getItem();
                break;
            }
        }

        return sellItem;
    }

    private void doArbitrage(MinecraftClient client) {
        if (client.currentScreen instanceof HandledScreen) {
            HandledScreen<?> handledScreen = (HandledScreen<?>) client.currentScreen;
            if (client.player.getStackInHand(Hand.MAIN_HAND).isOf(Items.COMPASS) && enabled) {
                ScreenHandler screenHandler = handledScreen.getScreenHandler();
                Int2ObjectOpenHashMap<ItemStack> slotInt2ObjectMap = new Int2ObjectOpenHashMap<ItemStack>();
                DefaultedList<Slot> defaultedList = screenHandler.slots;
                int i = defaultedList.size();
                ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
                for (Slot slot : defaultedList) {
                    list.add(slot.getStack().copy());
                }
                for (int j = 0; j < i; ++j) {
                    ItemStack itemStack2;
                    ItemStack itemStack = list.get(j);
                    if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
                    slotInt2ObjectMap.put(j, itemStack2.copy());
                }

                ItemStack slotStack = screenHandler.getSlot(11).getStack();
                packetQueue.add(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.nextRevision(), 11, 0, SlotActionType.PICKUP, slotStack, slotInt2ObjectMap));

                packetQueue.add(new CloseHandledScreenC2SPacket(screenHandler.syncId));
            }
            else{
                ScreenHandler screenHandler = handledScreen.getScreenHandler();
                doCheckAndPurchase(handledScreen, screenHandler, client);
            }
        } else {
            if (enabled) {
                if (client.player.getStackInHand(Hand.MAIN_HAND).isOf(Items.COMPASS)) {
                    if (client.currentScreen == null) {
                        packetQueue.add(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));
                    }
                }
                else {
                    if (hasSellables(client)) {
                        if (client.world.getTime() < lastShopTime || client.world.getTime() > lastShopTime + (80)) {
                            client.getNetworkHandler().sendCommand("shop");
                            lastShopTime = client.world.getTime();
                        }
                    } else {
                        HitResult hitResult = client.crosshairTarget;

                        // Check if we're looking at a block
                        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                            BlockHitResult blockHitResult = (BlockHitResult) hitResult;

                            BlockState hitBlockState = client.world.getBlockState(blockHitResult.getBlockPos());

                            if (hitBlockState.getBlock() == Blocks.CRAFTING_TABLE) {
                                if (hasCraftables(client)) {
                                    packetQueue.add(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
                                } else {
                                    if (client.world.getTime() < lastShopTime || client.world.getTime() > lastShopTime + (80)) {
                                        client.getNetworkHandler().sendCommand("shop");
                                        lastShopTime = client.world.getTime();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!packetQueue.isEmpty()) {
            Packet<?> packet = packetQueue.get(0);
            packetQueue.remove(0);
            if (packet instanceof CloseHandledScreenC2SPacket) {
                if (client.currentScreen instanceof HandledScreen) {
                    HandledScreen<?> handledScreen = (HandledScreen<?>) client.currentScreen;
                    handledScreen.close();
                    packetQueue.clear();
                }
            }
            else if (packet instanceof ClickSlotC2SPacket) {
                if (client.currentScreen instanceof HandledScreen) {
                    HandledScreen<?> handledScreen = (HandledScreen<?>) client.currentScreen;
                    ScreenHandler screenHandler = handledScreen.getScreenHandler();

                    if (handledScreen.getTitle().getString().contains("Craft")) {
                        screenHandler.onSlotClick(((ClickSlotC2SPacket) packet).getSlot(), ((ClickSlotC2SPacket) packet).getButton(), ((ClickSlotC2SPacket) packet).getActionType(), client.player);
                    }
                }
            }
            client.getNetworkHandler().sendPacket(packet);
        }
    }

    private void sellFurnaceMinecart(MinecraftClient client) {
        if (client.currentScreen == null) {
            if (client.world.getTime() < lastShopTime || client.world.getTime() > lastShopTime + (80)) {
                client.getNetworkHandler().sendCommand("shop");
                lastShopTime = client.world.getTime();
            }
        }
        else if (client.currentScreen.getTitle().getString().contains("Start Page")) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, client.player.currentScreenHandler.getSlot(32).id, 0, SlotActionType.PICKUP, client.player);
        }
        else if (client.currentScreen.getTitle().getString().contains("Redstone")) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, client.player.currentScreenHandler.getSlot(22).id, 0, SlotActionType.PICKUP, client.player);
        }
        else if (client.currentScreen.getTitle().getString().contains("Trade")) {
            if (UtilStuff.menuHasItem(Items.FURNACE_MINECART, 1, client.player.currentScreenHandler, client.player.getInventory())) {
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, client.player.currentScreenHandler.getSlot(8).id, 0, SlotActionType.PICKUP, client.player);
                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                client.setScreen(null);
            }
            else {
                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                client.setScreen(null);
            }
        }
    }

    private void buyIron(MinecraftClient client) {
        if (client.currentScreen == null) {
            if (client.world.getTime() < lastShopTime || client.world.getTime() > lastShopTime + (80)) {
                client.getNetworkHandler().sendCommand("shop");
                lastShopTime = client.world.getTime();
            }
        }
        else if (client.currentScreen.getTitle().getString().contains("Start Page")) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, client.player.currentScreenHandler.getSlot(14).id, 0, SlotActionType.PICKUP, client.player);
        }
        else if (client.currentScreen.getTitle().getString().contains("Ores")) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, client.player.currentScreenHandler.getSlot(11).id, 0, SlotActionType.PICKUP, client.player);
        }
        else if (client.currentScreen.getTitle().getString().contains("Trade")) {
            if (UtilStuff.menuHasItem(Items.IRON_INGOT, 1, client.player.currentScreenHandler, client.player.getInventory())) {
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, client.player.currentScreenHandler.getSlot(17).id, 0, SlotActionType.PICKUP, client.player);
                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                client.setScreen(null);
            }
            else {
                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                client.setScreen(null);
            }
        }
    }

    private void buyCobblestone(MinecraftClient client) {
        if (client.currentScreen == null) {
            if (client.world.getTime() < lastShopTime || client.world.getTime() > lastShopTime + (80)) {
                client.getNetworkHandler().sendCommand("shop");
                lastShopTime = client.world.getTime();
            }
        }
        else if (client.currentScreen.getTitle().getString().contains("Start Page")) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, client.player.currentScreenHandler.getSlot(12).id, 0, SlotActionType.PICKUP, client.player);
        }
        else if (client.currentScreen.getTitle().getString().contains("Blocks")) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, client.player.currentScreenHandler.getSlot(44).id, 0, SlotActionType.PICKUP, client.player);
        }
        else if (client.currentScreen.getTitle().getString().contains("Trade")) {
            if (UtilStuff.menuHasItem(Items.COBBLESTONE, 1, client.player.currentScreenHandler, client.player.getInventory())) {
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, client.player.currentScreenHandler.getSlot(17).id, 0, SlotActionType.PICKUP, client.player);
                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                client.setScreen(null);
            }
            else {
                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                client.setScreen(null);
            }
        }
    }

    private void doMinecart(MinecraftClient client) {
        if ((UtilStuff.hasItem(Items.MINECART, 1, client.player.getInventory()) && UtilStuff.hasItem(Items.FURNACE, 1, client.player.getInventory())) || UtilStuff.hasItem(Items.IRON_INGOT, 5, client.player.getInventory()) || UtilStuff.hasItem(Items.COBBLESTONE, 8, client.player.getInventory())){
            HitResult hitResult = client.crosshairTarget;

            // Check if we're looking at a block
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) hitResult;

                BlockState hitBlockState = client.world.getBlockState(blockHitResult.getBlockPos());

                if (hitBlockState.getBlock() == Blocks.CRAFTING_TABLE) {
                    if (!(client.player.currentScreenHandler instanceof CraftingScreenHandler)) {
                        client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
                    }
                    else {
                        if (UtilStuff.hasItem(Items.MINECART, 1, client.player.getInventory()) && UtilStuff.hasItem(Items.FURNACE, 1, client.player.getInventory())) {
                            UtilStuff.craftItemLarge(Items.FURNACE_MINECART, client, false);
                        }
                        else if (UtilStuff.hasItem(Items.IRON_INGOT, 5, client.player.getInventory())) {
                            UtilStuff.craftItemLarge(Items.MINECART, client, false);
                        }
                        else if (UtilStuff.hasItem(Items.COBBLESTONE, 8, client.player.getInventory())) {
                            UtilStuff.craftItemLarge(Items.FURNACE, client, true);
                        }
                        else {
                            client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                            client.setScreen(null);
                        }
                    }
                }
            }
        }
        else {
            if (client.player.currentScreenHandler instanceof CraftingScreenHandler) {
                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                client.setScreen(null);
            }
            if (UtilStuff.hasItem(Items.FURNACE_MINECART, 1, client.player.getInventory())) {
                sellFurnaceMinecart(client);
            }
            else {
                if (!UtilStuff.hasItem(Items.MINECART, 1, client.player.getInventory())) {
                    buyIron(client);
                }
                else if (!UtilStuff.hasItem(Items.FURNACE, 1, client.player.getInventory())) {
                    buyCobblestone(client);
                }
            }
        }
    }

    private void doCraftLoop(MinecraftClient client) {
        if (UtilStuff.hasItem(Items.IRON_INGOT, 1, client.player.getInventory())) {
            UtilStuff.craftItemLarge(Items.IRON_NUGGET, client, false);
        }
        else if (UtilStuff.hasItem(Items.IRON_NUGGET, 9, client.player.getInventory())) {
            UtilStuff.craftItemLarge(Items.IRON_INGOT, client, false);
        }
        else {
            crafter = false;
            client.player.sendMessage(Text.literal("No iron found in inventory.").withColor(Formatting.RED.getColorValue()));
        }
    }

    private void doGoldCraftLoop(MinecraftClient client) {
        if (UtilStuff.hasItem(Items.GOLD_INGOT, 9, client.player.getInventory())) {
            UtilStuff.craftItemLarge(Items.GOLD_BLOCK, client, true);
        }
        else if (UtilStuff.hasItem(Items.GOLD_NUGGET, 9, client.player.getInventory())) {
            UtilStuff.craftItemLarge(Items.GOLD_INGOT, client, true);
        }
        else {
            crafter = false;
            client.player.sendMessage(Text.literal("No gold found in inventory.").withColor(Formatting.RED.getColorValue()));
        }
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (client.world.getTime() % 8 == 0) {
                doArbitrage(client);
            }
            if (minecart) {
                if (client.world.getTime() % 8 == 0) {
                    doMinecart(client);
                }
            }
            else if (crafter) {
                if (client.currentScreen instanceof CraftingScreen) {
//                    if (client.world.getTime() % 8 == 0) {
//                        doCraftLoop(client);
//                    }
                    doCraftLoop(client);
                }
            }
            else if (gold_crafter) {
                if (client.currentScreen instanceof CraftingScreen) {
                    if (client.world.getTime() % 8 == 0) {
                        doGoldCraftLoop(client);
                    }
                }
            }
        });
    }
}
