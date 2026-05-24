package com.steven.frontend.features;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.steven.frontend.Main;

public final class TradeFeature {
    private TradeFeature() {}

    public static boolean buyMenuOpen = false;
    public static boolean sellMenuOpen = false;

    public static void handleTradeInput(Main main) {
        if (main.playerFatigue < 100f) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.J) && isNearSellingPlace(main)) {
                sellMenuOpen = !sellMenuOpen;
                if (sellMenuOpen) buyMenuOpen = false;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.H) && isNearSellingPlace(main)) {
                buyMenuOpen = !buyMenuOpen;
                if (buyMenuOpen) sellMenuOpen = false;
            }

            if (buyMenuOpen && isNearSellingPlace(main)) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                    if (main.inventory.coins >= Main.SEED_BIT_PRICE) {
                        main.inventory.coins -= Main.SEED_BIT_PRICE;
                        main.inventory.addBit();
                        main.markProgressDirty();
                        main.markInventoryDirty();
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                    if (main.inventory.coins >= Main.SEED_WHEAT_PRICE) {
                        main.inventory.coins -= Main.SEED_WHEAT_PRICE;
                        main.inventory.addWheat();
                        main.markProgressDirty();
                        main.markInventoryDirty();
                    }
                }
            }

            if (sellMenuOpen && isNearSellingPlace(main)) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                    int sold = main.inventory.sellWheatItems();
                    if (sold > 0) {
                        main.totalCoinsEarned += sold * 40;
                        main.soldCount += sold;
                        main.markProgressDirty();
                        main.markInventoryDirty();
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                    int sold = main.inventory.sellBitItems();
                    if (sold > 0) {
                        main.totalCoinsEarned += sold * 30;
                        main.soldCount += sold;
                        main.markProgressDirty();
                        main.markInventoryDirty();
                    }
                }
            }
        }
    }

    public static void sellAtPlayer(Main main) {
        if (!isNearSellingPlace(main)) {
            return;
        }

        int coinsBefore = main.inventory.coins;
        String mapped = null;
        if (main.hotbarSelected >= 0 && main.hotbarSelected < main.hotbarMapping.length) {
            mapped = main.hotbarMapping[main.hotbarSelected];
        }
        if ("harvest_bit".equals(mapped)) {
            int s = main.inventory.sellBitItems();
            main.soldCount += s;
        } else if ("harvest_wheat".equals(mapped)) {
            int s = main.inventory.sellWheatItems();
            main.soldCount += s;
        } else {
            int sold = main.inventory.harvestedBit + main.inventory.harvestedWheat;
            main.inventory.sellHarvested();
            main.soldCount += sold;
        }
        int earned = main.inventory.coins - coinsBefore;
        if (earned > 0) main.totalCoinsEarned += earned;
        main.markProgressDirty();
        main.markInventoryDirty();
    }

    public static boolean isNearSellingPlace(Main main) {
        int px = (int)Math.floor(main.x);
        int py = (int)Math.floor(main.y);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (main.hasTile(main.sellingPlaceLayer, px + dx, py + dy)) {
                    return true;
                }
            }
        }
        return false;
    }
}
