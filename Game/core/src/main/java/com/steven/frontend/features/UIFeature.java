package com.steven.frontend.features;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.steven.frontend.Main;
import com.steven.frontend.features.ChestFeature.ChestSpot;
import com.steven.frontend.features.PlantFeature.Plant;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public class UIFeature {
    public static void renderHUD(Main main, float dt) {
        main.batch.setProjectionMatrix(main.uiCamera.combined);
        main.batch.begin();

        float brightness = 1f;
        if (main.isSleeping) {
            float sleepProgress = main.sleepTimer / com.steven.frontend.features.SleepFeature.SLEEP_DURATION;
            if (sleepProgress < 0.5f) {
                brightness = 1f - (sleepProgress * 2f);
            } else {
                brightness = (sleepProgress - 0.5f) * 2f;
            }
        } else if (main.gameTime < 6f) {
            brightness = 0.2f + (main.gameTime / 6f) * 0.8f;
        } else if (main.gameTime >= 20f) {
            brightness = 0.2f + ((24f - main.gameTime) / 4f) * 0.8f;
        }
        float alpha = 1f - brightness;
        if (main.overlayTexture != null && alpha > 0.01f) {
            main.batch.setColor(0f, 0f, 0f, alpha * 0.7f);
            main.batch.draw(main.overlayTexture, 0, 0, 640, 480);
            main.batch.setColor(1f, 1f, 1f, 1f);
        }

        int hour = (int)main.gameTime;
        int minute = (int)((main.gameTime - hour) * 60f);
        String timeStr = String.format("%02d:%02d", hour, minute);

        if (main.font != null) {
            String fatigueStr = String.format("Fatigue: %.0f%%", main.playerFatigue);
            if (main.playerFatigue >= 80f) {
                main.batch.setColor(1f, 0.2f, 0.2f, 1f);
            } else if (main.playerFatigue >= 50f) {
                main.batch.setColor(1f, 1f, 0.2f, 1f);
            }
            main.font.draw(main.batch, fatigueStr, 10, 460);
            if (main.playerFatigue >= 100f) {
                main.batch.setColor(1f, 0.2f, 0.2f, 1f);
                main.font.draw(main.batch, "TOO TIRED! Press X to sleep!", 10, 440);
                main.batch.setColor(1f, 1f, 1f, 1f);
            }
            main.batch.setColor(1f, 1f, 1f, 1f);

            main.font.draw(main.batch, "Day " + main.currentDay + " - Time: " + timeStr, 10, 445);
            String coinNumberStr = Integer.toString(main.inventory.coins);
            GlyphLayout coinNumberLayout = new GlyphLayout(main.font, coinNumberStr);
            float padding = 8f;
            float textX = 640f - padding - coinNumberLayout.width;
            float textY = 460f;
            if (main.coinAnimation != null) {
                TextureRegion coinFrame = main.coinAnimation.getKeyFrame(main.coinAnimTime, true);
                float coinX = textX - 4f - 16f;
                main.batch.draw(coinFrame, coinX, 450, 16, 16);
            }
            main.font.draw(main.batch, coinNumberStr, textX, textY);
            if (main.gameTime >= 21f) {
                main.font.draw(main.batch, "Press X to sleep", 10, 425);
            }
            if (main.isSleeping) {
                main.font.draw(main.batch, "Sleeping... zzz", 270, 240);
            }

            Vector3 projected = new Vector3();
            float sxScale = 640f / (float)Math.max(1, Gdx.graphics.getWidth());
            float syScale = 480f / (float)Math.max(1, Gdx.graphics.getHeight());
            for (Plant p : PlantFeature.plants) {
                if (!p.needsWater) continue;
                int areaIdx = p.areaIdx;
                int cellIdx = p.cellIdx;
                if (areaIdx < 0 || areaIdx >= 6 || main.plantAreaLayers[areaIdx] == null) continue;

                int cellX = cellIdx % 3;
                int cellY = cellIdx / 3;
                int areaWidth = main.plantAreaWidthTiles[areaIdx];
                int areaHeight = main.plantAreaHeightTiles[areaIdx];
                int tilesPerCellX = Math.max(1, areaWidth / 3);
                int tilesPerCellY = Math.max(1, areaHeight / 3);

                float worldX = main.plantAreaMinX[areaIdx] + 0.5f + (cellX * tilesPerCellX) + (tilesPerCellX - 1) * 0.5f;
                float worldY = main.plantAreaMinY[areaIdx] + 0.5f + (cellY * tilesPerCellY) + (tilesPerCellY - 1) * 0.5f;
                projected.set(worldX, worldY + 0.7f, 0f);
                main.camera.project(projected);

                float uiX = projected.x * sxScale;
                float uiY = projected.y * syScale;
                main.batch.setColor(1f, 0.25f, 0.25f, 1f);
                main.font.draw(main.batch, "Siram!", uiX - 16f, uiY + 10f);
                main.batch.setColor(1f, 1f, 1f, 1f);
            }
            
            int px = (int)Math.floor(main.x);
            int py = (int)Math.floor(main.y);

            boolean foundBridge = false;
            int foundBx = 0, foundBy = 0;
            int foundBridgeType = -1;
            if (!main.worldBridgeRepaired) {
                outer: for (int bdx = -2; bdx <= 2; bdx++) {
                    for (int bdy = -2; bdy <= 2; bdy++) {
                        if (main.hasTile(main.bridgeLayer, px + bdx, py + bdy)) {
                            foundBridge = true; foundBx = px + bdx; foundBy = py + bdy; foundBridgeType = 0; break outer;
                        }
                    }
                }
            }
            if (!foundBridge && !main.worldPusatBridge1Repaired) {
                outer2: for (int bdx = -2; bdx <= 2; bdx++) {
                    for (int bdy = -2; bdy <= 2; bdy++) {
                        if (main.hasTile(main.bridgeRusak1Layer, px + bdx, py + bdy)) {
                            foundBridge = true; foundBx = px + bdx; foundBy = py + bdy; foundBridgeType = 1; break outer2;
                        }
                    }
                }
            }
            if (!foundBridge && !main.worldPusatBridge2Repaired) {
                outer3: for (int bdx = -2; bdx <= 2; bdx++) {
                    for (int bdy = -2; bdy <= 2; bdy++) {
                        if (main.hasTile(main.bridgeRusak2Layer, px + bdx, py + bdy)) {
                            foundBridge = true; foundBx = px + bdx; foundBy = py + bdy; foundBridgeType = 2; break outer3;
                        }
                    }
                }
            }

            if (foundBridge) {
                if (!main.bridgeWasNear) {
                    main.bridgePromptTimer = 1f;
                    main.bridgePromptTileX = foundBx; main.bridgePromptTileY = foundBy;
                    main.bridgeWasNear = true;
                }
            } else {
                main.bridgeWasNear = false;
                main.bridgePromptTimer = 0f;
            }

            if (main.bridgePromptTimer > 0f) {
                projected.set(main.bridgePromptTileX + 0.5f, main.bridgePromptTileY + 0.5f, 0f);
                main.camera.project(projected);
                float uiX = projected.x * sxScale;
                float uiY = projected.y * syScale;
                int cost = RepairFeature.WORLD_BRIDGE_COST;
                if (foundBridgeType == 1) cost = RepairFeature.WORLD_PUSAT_BRIDGE1_COST;
                else if (foundBridgeType == 2) cost = RepairFeature.WORLD_PUSAT_BRIDGE2_COST;
                main.batch.setColor(0.8f, 0.8f, 0.2f, 1f);
                main.font.draw(main.batch, "Perbaiki: " + cost + " coin (Use G)", uiX - 60f, uiY + 20f);
                main.batch.setColor(1f, 1f, 1f, 1f);
                main.bridgePromptTimer -= dt;
                if (main.bridgePromptTimer <= 0f) main.bridgePromptTimer = 0f;
            }

            boolean foundHouseFinal = false;
            int foundHfX = 0, foundHfY = 0;
            if (!main.houseFinalRepaired) {
                outerHf: for (int hdx = -2; hdx <= 2; hdx++) {
                    for (int hdy = -2; hdy <= 2; hdy++) {
                        if (main.hasTile(main.houseFinalLayer, px + hdx, py + hdy) || main.hasTile(main.houseFinalFixLayer, px + hdx, py + hdy)) {
                            foundHouseFinal = true; foundHfX = px + hdx; foundHfY = py + hdy; break outerHf;
                        }
                    }
                }
            }

            if (foundHouseFinal) {
                if (!main.houseFinalWasNear) {
                    main.houseFinalPromptTimer = 1f;
                    main.houseFinalPromptTileX = foundHfX;
                    main.houseFinalPromptTileY = foundHfY;
                    main.houseFinalWasNear = true;
                }
            } else {
                main.houseFinalWasNear = false;
                main.houseFinalPromptTimer = 0f;
            }

            if (main.houseFinalPromptTimer > 0f) {
                projected.set(main.houseFinalPromptTileX + 0.5f, main.houseFinalPromptTileY + 0.5f, 0f);
                main.camera.project(projected);
                float uiX = projected.x * sxScale;
                float uiY = projected.y * syScale;
                main.batch.setColor(0.8f, 0.8f, 0.2f, 1f);
                main.font.draw(main.batch, "Perbaiki: " + RepairFeature.HOUSE_FINAL_COST + " coin (Use G)", uiX - 84f, uiY + 20f);
                main.batch.setColor(1f, 1f, 1f, 1f);
                main.houseFinalPromptTimer -= dt;
                if (main.houseFinalPromptTimer <= 0f) main.houseFinalPromptTimer = 0f;
            }

            boolean foundSell = false;
            int foundSx = 0, foundSy = 0;
            for (int sdx = -2; sdx <= 2 && !foundSell; sdx++) {
                for (int sdy = -2; sdy <= 2 && !foundSell; sdy++) {
                    if (main.hasTile(main.sellingPlaceLayer, px + sdx, py + sdy)) {
                        foundSell = true; foundSx = px + sdx; foundSy = py + sdy;
                    }
                }
            }
            if (foundSell) {
                if (!main.sellingWasNear) {
                    main.sellingPromptTimer = 1f;
                    main.sellingPromptTileX = foundSx; main.sellingPromptTileY = foundSy;
                    main.sellingWasNear = true;
                }
            } else {
                main.sellingWasNear = false;
                main.sellingPromptTimer = 0f;
            }
            if (main.sellingPromptTimer > 0f) {
                projected.set(main.sellingPromptTileX + 0.5f, main.sellingPromptTileY + 1.0f, 0f);
                main.camera.project(projected);
                float uiX = projected.x * sxScale;
                float uiY = projected.y * syScale;
                main.batch.setColor(0.9f, 0.9f, 0.95f, 1f);
                main.font.draw(main.batch, "Click J for sell and H for buy", uiX - 72f, uiY + 18f);
                main.batch.setColor(1f, 1f, 1f, 1f);
                main.sellingPromptTimer -= dt;
                if (main.sellingPromptTimer <= 0f) main.sellingPromptTimer = 0f;
            }

            boolean foundPlant = false;
            int foundPx = 0, foundPy = 0;
            for (int ai = 0; ai < main.plantAreaLayers.length && !foundPlant; ai++) {
                TiledMapTileLayer pal = main.plantAreaLayers[ai];
                if (pal == null) continue;
                for (int pdx = -1; pdx <= 1 && !foundPlant; pdx++) {
                    for (int pdy = -1; pdy <= 1 && !foundPlant; pdy++) {
                        if (main.hasTile(pal, px + pdx, py + pdy)) {
                            foundPlant = true; foundPx = px + pdx; foundPy = py + pdy;
                        }
                    }
                }
            }
            if (foundPlant) {
                if (!main.plantWasNear) {
                    main.plantPromptTimer = 1f;
                    main.plantPromptTileX = foundPx; main.plantPromptTileY = foundPy;
                    main.plantWasNear = true;
                }
            } else {
                main.plantWasNear = false;
                main.plantPromptTimer = 0f;
            }
            if (main.plantPromptTimer > 0f) {
                projected.set(main.plantPromptTileX + 0.5f, main.plantPromptTileY + 1.0f, 0f);
                main.camera.project(projected);
                float uiX = projected.x * sxScale;
                float uiY = projected.y * syScale;
                main.batch.setColor(0.9f, 0.9f, 0.95f, 1f);
                main.font.draw(main.batch, "Right click for plant", uiX - 36f, uiY + 18f);
                main.batch.setColor(1f, 1f, 1f, 1f);
                main.plantPromptTimer -= dt;
                if (main.plantPromptTimer <= 0f) main.plantPromptTimer = 0f;
            }

            ChestSpot nearbyChest = (!ChestFeature.chestAnimActive && !ChestFeature.chestRewardOpen) ? ChestFeature.findNearbyChestSpot(main) : null;
            if (nearbyChest != null) {
                if (!main.chestWasNear) {
                    main.chestPromptTimer = 1f;
                    main.chestPromptTileX = nearbyChest.x;
                    main.chestPromptTileY = nearbyChest.y;
                    main.chestWasNear = true;
                }
            } else {
                main.chestWasNear = false;
                main.chestPromptTimer = 0f;
            }
            if (main.chestPromptTimer > 0f) {
                projected.set(main.chestPromptTileX + 0.5f, main.chestPromptTileY + 0.5f, 0f);
                main.camera.project(projected);
                float uiX = projected.x * sxScale;
                float uiY = projected.y * syScale;
                main.batch.setColor(0.95f, 0.95f, 0.95f, 1f);
                main.font.draw(main.batch, "Right Click", uiX - 24f, uiY + 18f);
                main.batch.setColor(1f, 1f, 1f, 1f);
                main.chestPromptTimer -= dt;
                if (main.chestPromptTimer <= 0f) main.chestPromptTimer = 0f;
            }
        }

        if (TradeFeature.buyMenuOpen || TradeFeature.sellMenuOpen || ChestFeature.chestRewardOpen) {
            float menuW = 420f;
            float menuH = 240f;
            float mx = (640f - menuW) * 0.5f;
            float my = (480f - menuH) * 0.5f;

            if (ChestFeature.chestRewardOpen) {
                Texture rewardTexture = (ChestFeature.activeChest != null && ChestFeature.activeChest.layoutTexture != null) ? ChestFeature.activeChest.layoutTexture : main.chestRewardLayoutTexture;
                if (rewardTexture != null) {
                    float texW = rewardTexture.getWidth();
                    float texH = rewardTexture.getHeight();
                    float rx = (640f - texW) * 0.5f;
                    float ry = (480f - texH) * 0.5f;
                    main.batch.draw(rewardTexture, rx, ry, texW, texH);
                } else { main.batch.setColor(0.25f, 0.25f, 0.3f, 0.8f); main.batch.draw(main.overlayTexture, mx, my, menuW, menuH); }
            } else if (TradeFeature.buyMenuOpen) {
                if (main.buyLayoutTexture != null) main.batch.draw(main.buyLayoutTexture, mx, my, menuW, menuH);
                else { main.batch.setColor(0.25f, 0.22f, 0.18f, 0.75f); main.batch.draw(main.overlayTexture, mx, my, menuW, menuH); }
            } else if (TradeFeature.sellMenuOpen) {
                if (main.sellLayoutTexture != null) main.batch.draw(main.sellLayoutTexture, mx, my, menuW, menuH);
                else { main.batch.setColor(0.22f, 0.25f, 0.18f, 0.75f); main.batch.draw(main.overlayTexture, mx, my, menuW, menuH); }
            }

            main.batch.setColor(1f, 1f, 1f, 1f);
        }

        int slots = 9;
        float slotSize = 40f;
        float slotPad = 6f;
        float totalW = slots * slotSize + (slots - 1) * slotPad;
        float startX = (640f - totalW) * 0.5f;
        float startY = 8f;
        for (int i = 0; i < slots; i++) {
            float sx = startX + i * (slotSize + slotPad);
            float slotOffsetY = (i == main.hotbarSelected) ? 6f : 0f;
            float drawY = startY + slotOffsetY;
            if (i == main.hotbarSelected) {
                main.batch.setColor(1f, 1f, 1f, 0.24f);
                main.batch.draw(main.overlayTexture, sx - 4f, drawY - 4f, slotSize + 8f, slotSize + 8f);
            }
            main.batch.setColor(1f, 1f, 1f, 0.16f);
            main.batch.draw(main.overlayTexture, sx, drawY, slotSize, slotSize);
            main.batch.setColor(1f, 1f, 1f, 1f);
            
            float iconSize = (i == main.hotbarSelected) ? 32f : 28f;
            float iconX = sx + (slotSize - iconSize) * 0.5f;
            float iconY = drawY + (slotSize - iconSize) * 0.5f;
            String mapped = main.hotbarMapping[i];
            if (mapped != null) {
                switch (mapped) {
                    case "seed_bit":
                        if (main.seedBitRegion != null) main.batch.draw(main.seedBitRegion, iconX, iconY, iconSize, iconSize);
                        if (main.font != null) main.font.draw(main.batch, Integer.toString(main.inventory.bitSeeds), sx + slotSize - 10f, drawY + 12f);
                        break;
                    case "seed_wheat":
                        if (main.seedWheatRegion != null) main.batch.draw(main.seedWheatRegion, iconX, iconY, iconSize, iconSize);
                        if (main.font != null) main.font.draw(main.batch, Integer.toString(main.inventory.wheatSeeds), sx + slotSize - 10f, drawY + 12f);
                        break;
                    case "tool_water":
                        if (main.toolBox3Region != null) main.batch.draw(main.toolBox3Region, iconX, iconY, iconSize, iconSize);
                        break;
                    case "harvest_bit":
                        if (main.harvestedBitRegion != null) main.batch.draw(main.harvestedBitRegion, iconX, iconY, iconSize, iconSize);
                        if (main.font != null) main.font.draw(main.batch, Integer.toString(main.inventory.harvestedBit), sx + slotSize - 10f, drawY + 12f);
                        break;
                    case "harvest_wheat":
                        if (main.harvestedWheatRegion != null) main.batch.draw(main.harvestedWheatRegion, iconX, iconY, iconSize, iconSize);
                        if (main.font != null) main.font.draw(main.batch, Integer.toString(main.inventory.harvestedWheat), sx + slotSize - 10f, drawY + 12f);
                        break;
                    case "key":
                        if (main.keyRegion != null) main.batch.draw(main.keyRegion, iconX, iconY, iconSize, iconSize);
                        if (main.font != null) main.font.draw(main.batch, Integer.toString(main.inventory.keys), sx + slotSize - 10f, drawY + 12f);
                        break;
                }
            }
        }

        main.batch.end();
    }

    public static void initializeGameCompletedUI(Main main) {
        // Dummy or minimum implementation
    }

}
