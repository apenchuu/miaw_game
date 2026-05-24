package com.steven.frontend.features;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.steven.frontend.Main;
import com.steven.frontend.features.ChestFeature;

public class InputFeature {
    public static void handleInput(Main main, float dt) {
        // Seed selection (press E to switch)
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            main.selectedSeed = 1 - main.selectedSeed;
        }

        // Rebuild dynamic hotbar mapping based on current inventory
        main.rebuildHotbarMapping();

        // Hotbar selection via number keys 1-9
        int[] hotbarKeys = new int[] { Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5, Input.Keys.NUM_6, Input.Keys.NUM_7, Input.Keys.NUM_8, Input.Keys.NUM_9 };
        for (int i = 0; i < hotbarKeys.length; i++) {
            if (Gdx.input.isKeyJustPressed(hotbarKeys[i])) {
                main.hotbarSelected = i;
            }
        }

        // Plant/Harvest/Water or chest interaction (right-click at player position) - only if not too tired
        if (main.playerFatigue < 100f && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            if (com.steven.frontend.features.ChestFeature.startChestRewardSequence(main)) {
                // chest sequence started; skip farming actions
            } else if (!ChestFeature.chestAnimActive && !ChestFeature.chestRewardOpen) {
                String selItem = null;
                if (main.hotbarSelected >= 0 && main.hotbarSelected < main.hotbarMapping.length) selItem = main.hotbarMapping[main.hotbarSelected];
                if ("tool_water".equals(selItem)) {
                    com.steven.frontend.features.PlantFeature.waterAtPlayer(main);
                } else {
                    if (!com.steven.frontend.features.PlantFeature.harvestAtPlayer(main)) {
                        com.steven.frontend.features.PlantFeature.plantAtPlayerHotbar(main);
                    }
                }
            }
        }
        
        // Sleep (press X when time >= 21:00 or fatigue >= 100) - only if on bed tile
        com.steven.frontend.features.SleepFeature.processSleep(main, dt);

        // Buy/sell menus: J = open/close sell menu, H = open/close buy menu
        // ESC closes any open menu (global)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            com.steven.frontend.features.TradeFeature.buyMenuOpen = false;
            com.steven.frontend.features.TradeFeature.sellMenuOpen = false;
            ChestFeature.chestRewardOpen = false;
        }

        if (ChestFeature.chestRewardOpen && Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            int coinsBefore = main.inventory.coins;
            main.inventory.coins += ChestFeature.CHEST_REWARD_COIN;
            main.inventory.addKey();
            main.totalCoinsEarned += main.inventory.coins - coinsBefore;
            main.markProgressDirty();
            main.markInventoryDirty();
            ChestFeature.chestRewardOpen = false;
            ChestFeature.chestRewardClaimed = true;
            if (ChestFeature.activeChest != null) {
                ChestFeature.activeChest.claimed = true;
                ChestFeature.setChestClaimed(main, ChestFeature.activeChest.kind, true);
                ChestFeature.clearChestCellForKind(main, ChestFeature.activeChest.kind);
            }
            ChestFeature.activeChest = null;
            ChestFeature.activeChestKind = null;
            ChestFeature.activeChestX = -1;
            ChestFeature.activeChestY = -1;
            ChestFeature.chestAnimActive = false;
            main.chestAnimTime = 0f;
            ChestFeature.updateChestLayerVisibility(main);
        }

        // Consume harvested item: when active hotbar item is harvested bit/wheat, press C to consume
        if (!ChestFeature.chestRewardOpen && Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            com.steven.frontend.features.ConsumeFeature.consumeSelectedHarvestItem(main);
        }

        com.steven.frontend.features.TradeFeature.handleTradeInput(main);

        // Bridge repair (press G to repair nearby bridge)
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            if (!com.steven.frontend.features.RepairFeature.handleBridgeRepair(main)) {
                com.steven.frontend.features.RepairFeature.handleHouseFinalRepair(main);
            }
        }
    }
}
