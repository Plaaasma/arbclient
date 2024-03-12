package net.plaaasma.arbitrageclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class UtilStuff {
    public static boolean hasItem(Item item, Integer amount, PlayerInventory inventoryHandler) {
        for (int index = 0; index < inventoryHandler.size(); index++) {
            ItemStack slotStack = inventoryHandler.getStack(index);
            Item slotItem = slotStack.getItem();
            if (slotItem == item) {
                if (slotStack.getCount() >= amount) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean menuHasItem(Item item, Integer amount, ScreenHandler inventoryHandler, PlayerInventory playerInventory) {
        int playerInvSize = playerInventory.size();
        int menuSize = inventoryHandler.slots.size() - playerInvSize;

        for (int slotIndex = 0; slotIndex < menuSize; slotIndex++) {
            ItemStack slotStack = inventoryHandler.getSlot(slotIndex).getStack();
            Item slotItem = slotStack.getItem();
            if (slotItem == item) {
                if (slotStack.getCount() >= amount) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void craftItemLarge(Item item, MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        RecipeManager recipeManager = client.world.getRecipeManager();

        if (recipeManager != null) {
            List<RecipeEntry<CraftingRecipe>> craftingRecipes = recipeManager.listAllOfType(RecipeType.CRAFTING);

            for (RecipeEntry<CraftingRecipe> recipeEntry: craftingRecipes) {
                String trimmedRecipeName = recipeEntry.toString().substring(recipeEntry.toString().indexOf(":") + 1);
                if (trimmedRecipeName.equals(item.toString()) || (trimmedRecipeName.equals("iron_ingot_from_nuggets") && item == Items.IRON_INGOT) || (trimmedRecipeName.equals("gold_ingot_from_nuggets") && item == Items.GOLD_INGOT)) {
                    List<Ingredient> ingredients = recipeEntry.value().getIngredients();
                    HashMap<Item, Integer> combinedIngredientsMap = new HashMap<>();
                    for (Ingredient ingredient : ingredients) {
                        List<ItemStack> ingredientStacks = Arrays.asList(ingredient.getMatchingStacks());
                        for (ItemStack ingredientStack : ingredientStacks) {
                            if (hasItem(ingredientStack.getItem(), ingredientStack.getCount(), player.getInventory())) {
                                if (combinedIngredientsMap.containsKey(ingredientStack.getItem())) {
                                    combinedIngredientsMap.put(ingredientStack.getItem(), combinedIngredientsMap.get(ingredientStack.getItem()) + ingredientStack.getCount());
                                }
                                else {
                                    combinedIngredientsMap.put(ingredientStack.getItem(), ingredientStack.getCount());
                                }
                                break;
                            }
                        }
                    }
                    int neededIngredientAmount = combinedIngredientsMap.size();
                    int ingredientAmount = 0;
                    for (Item ingredientItem : combinedIngredientsMap.keySet()) {
                        ItemStack ingredientStack = new ItemStack(ingredientItem, combinedIngredientsMap.get(ingredientItem));
                        if (hasItem(ingredientStack.getItem(), ingredientStack.getCount(), player.getInventory())) {
                            ingredientAmount += 1;
                        }
                    }

                    if (ingredientAmount == neededIngredientAmount) {
                        client.interactionManager.clickRecipe(client.player.currentScreenHandler.syncId, recipeEntry, false);
                        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, player);
//                        client.setScreen(null);

                        return;
                    }
                    break;
                }
            }
            ArbitrageClientClient.crafter = false;
        }
        else {
            System.out.println("Recipe manager null");
        }
    }
}
