package com.steven.frontend.features;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.steven.frontend.Main;
import com.steven.frontend.features.ChestFeature.ChestSpot;
import com.steven.frontend.features.PlantFeature.Plant;
import com.steven.frontend.features.PlantFeature.PlantSprite;

public class WorldRenderFeature {
    public static void renderWorld(Main main, float dt) {
        // Camera follow (x,y are in tile units) but clamp to map bounds
        float halfW = main.camera.viewportWidth * 0.5f;
        float halfH = main.camera.viewportHeight * 0.5f;
        float camX = MathUtils.clamp(main.x, halfW, main.mapWidth - halfW);
        float camY = MathUtils.clamp(main.y, halfH, main.mapHeight - halfH);
        main.camera.position.set(camX, camY, 0);
        main.camera.update();

        // clear screen
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Render map and player
        main.mapRenderer.setView(main.camera);
        main.mapRenderer.render();

        // Draw animated coin layer tiles
        if (main.coinLayer != null && main.coinAnimation != null) {
            TextureRegion coinFrame = main.coinAnimation.getKeyFrame(main.coinAnimTime, true);
            main.batch.setProjectionMatrix(main.camera.combined);
            main.batch.begin();
            for (int x = 0; x < main.coinLayer.getWidth(); x++) {
                for (int y = 0; y < main.coinLayer.getHeight(); y++) {
                    TiledMapTileLayer.Cell cell = main.coinLayer.getCell(x, y);
                    if (cell != null) {
                        main.batch.draw(coinFrame, x, y, 1f, 1f);
                    }
                }
            }
            main.batch.end();
        }

        // Restore original plant_area cells first
        for (int i = 0; i < 6; i++) {
            if (main.plantAreaLayers[i] != null && main.plantAreaCellsBackup[i] != null) {
                for (int x = 0; x < main.plantAreaLayers[i].getWidth(); x++) {
                    for (int y = 0; y < main.plantAreaLayers[i].getHeight(); y++) {
                        main.plantAreaLayers[i].setCell(x, y, main.plantAreaCellsBackup[i][x][y]);
                    }
                }
            }
        }
        
        // Render plants by overlaying plant tiles on plant_area layers
        for (Plant p : PlantFeature.plants) {
            int stage = p.getStage();
            int areaIdx = p.areaIdx;
            int cellIdx = p.cellIdx;
            
            if (areaIdx < 0 || areaIdx >= 6 || main.plantAreaLayers[areaIdx] == null) continue;
            
            int cellX = cellIdx % 3;
            int cellY = cellIdx / 3;
            
            int areaWidth = main.plantAreaWidthTiles[areaIdx];
            int areaHeight = main.plantAreaHeightTiles[areaIdx];
            int tilesPerCellX = Math.max(1, areaWidth / 3);
            int tilesPerCellY = Math.max(1, areaHeight / 3);
            
            TiledMapTileLayer sourceLayer = (p.type == Plant.Type.BIT) ? main.bitStageLayers[stage] : main.wheatStageLayers[stage];
            
            if (sourceLayer != null && sourceLayer.getWidth() > 0 && sourceLayer.getHeight() > 0) {
                TiledMapTileLayer.Cell plantCell = null;
                for (int sx = 0; sx < sourceLayer.getWidth() && plantCell == null; sx++) {
                    for (int sy = 0; sy < sourceLayer.getHeight() && plantCell == null; sy++) {
                        plantCell = sourceLayer.getCell(sx, sy);
                    }
                }
                
                if (plantCell != null && plantCell.getTile() != null) {
                    TextureRegion region = plantCell.getTile().getTextureRegion();
                    if (region != null) {
                        float worldX = main.plantAreaMinX[areaIdx] + 0.5f + (cellX * tilesPerCellX) + (tilesPerCellX - 1) * 0.5f;
                        float worldY = main.plantAreaMinY[areaIdx] + 0.5f + (cellY * tilesPerCellY) + (tilesPerCellY - 1) * 0.5f;
                        PlantFeature.plantSpritesToRender.add(new PlantSprite(region, worldX, worldY));
                    }
                }
            }
        }

        main.batch.setProjectionMatrix(main.camera.combined);
        main.batch.begin();
        
        for (PlantSprite ps : PlantFeature.plantSpritesToRender) {
            main.batch.draw(ps.region, ps.x - 0.5f, ps.y - 0.5f, 1f, 1f);
        }
        PlantFeature.plantSpritesToRender.clear();

        if (!ChestFeature.chestSpots.isEmpty()) {
            for (ChestSpot spot : ChestFeature.chestSpots) {
                if (spot == null || spot.claimed) continue;
                if (ChestFeature.chestAnimActive && ChestFeature.activeChest != null && ChestFeature.activeChest.kind == spot.kind) {
                    Animation<TextureRegion> anim = ChestFeature.getChestAnimationForSpot(main, spot);
                    if (anim != null) {
                        main.batch.draw(anim.getKeyFrame(main.chestAnimTime, false), spot.x, spot.y, 1f, 1f);
                    }
                }
            }
        }
        
        if (main.wateringActive) {
            Animation<TextureRegion> activeWaterAnim = main.getWateringAnimationForFacing(main.wateringFacing);
            if (activeWaterAnim != null) {
                TextureRegion frame = activeWaterAnim.getKeyFrame(main.wateringAnimTime, false);
                main.batch.draw(frame, main.x - 0.5f, main.y - 0.5f, 1f, 1f);
            } else if (main.currentAnim != null) {
                TextureRegion frame = main.currentAnim.getKeyFrame(main.stateTime, true);
                main.batch.draw(frame, main.x - 0.5f, main.y - 0.5f, 1f, 1f);
            }
        } else if (main.currentAnim != null) {
            TextureRegion frame = main.currentAnim.getKeyFrame(main.stateTime, true);
            main.batch.draw(frame, main.x - 0.5f, main.y - 0.5f, 1f, 1f);
        }
        
        main.batch.setProjectionMatrix(main.camera.combined);
        main.batch.end();
    }
}
