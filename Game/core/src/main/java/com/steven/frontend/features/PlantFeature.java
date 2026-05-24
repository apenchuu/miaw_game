package com.steven.frontend.features;

import com.steven.frontend.Main;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

// Mengelola fitur tanam, panen, dan siram dari file terpisah.
public final class PlantFeature {
    private PlantFeature() {}

    // Layer/state containers
    public static com.badlogic.gdx.maps.tiled.TiledMapTileLayer[] plantAreaLayers = new com.badlogic.gdx.maps.tiled.TiledMapTileLayer[6];
    public static com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell[][][] plantAreaCellsBackup = new com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell[6][][];
    public static com.badlogic.gdx.maps.tiled.TiledMapTileLayer[] bitStageLayers = new com.badlogic.gdx.maps.tiled.TiledMapTileLayer[5];
    public static com.badlogic.gdx.maps.tiled.TiledMapTileLayer[] wheatStageLayers = new com.badlogic.gdx.maps.tiled.TiledMapTileLayer[5];
    public static int[] plantAreaMinX = new int[6], plantAreaMaxX = new int[6];
    public static int[] plantAreaMinY = new int[6], plantAreaMaxY = new int[6];
    public static int[] plantAreaWidthTiles = new int[6], plantAreaHeightTiles = new int[6];

    // Plants and sprites
    public static java.util.List<Plant> plants = new java.util.ArrayList<Plant>();
    public static java.util.List<PlantSprite> plantSpritesToRender = new java.util.ArrayList<PlantSprite>();

    // Initialize plant feature state from the main map. Safe to call after map and layers are loaded.
    public static void init(Main main) {
        if (main == null || main.map == null) return;

        for (int i = 0; i < 6; i++) {
            try {
                plantAreaLayers[i] = (com.badlogic.gdx.maps.tiled.TiledMapTileLayer) main.map.getLayers().get("plant_area" + (i + 1));
            } catch (Exception e) {
                plantAreaLayers[i] = null;
            }
            if (plantAreaLayers[i] != null) {
                int width = plantAreaLayers[i].getWidth();
                int height = plantAreaLayers[i].getHeight();
                plantAreaCellsBackup[i] = new com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell[width][height];
                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        try {
                            plantAreaCellsBackup[i][x][y] = plantAreaLayers[i].getCell(x, y);
                        } catch (Exception ignored) {}
                        if (plantAreaCellsBackup[i][x][y] != null) {
                            minX = Math.min(minX, x);
                            minY = Math.min(minY, y);
                            maxX = Math.max(maxX, x);
                            maxY = Math.max(maxY, y);
                        }
                    }
                }
                if (minX == Integer.MAX_VALUE) {
                    plantAreaMinX[i] = 0; plantAreaMinY[i] = 0; plantAreaMaxX[i] = 0; plantAreaMaxY[i] = 0;
                    plantAreaWidthTiles[i] = width; plantAreaHeightTiles[i] = height;
                } else {
                    plantAreaMinX[i] = minX; plantAreaMinY[i] = minY; plantAreaMaxX[i] = maxX; plantAreaMaxY[i] = maxY;
                    plantAreaWidthTiles[i] = maxX - minX + 1; plantAreaHeightTiles[i] = maxY - minY + 1;
                }
            } else {
                plantAreaCellsBackup[i] = null;
                plantAreaMinX[i] = plantAreaMinY[i] = plantAreaMaxX[i] = plantAreaMaxY[i] = 0;
                plantAreaWidthTiles[i] = plantAreaHeightTiles[i] = 0;
            }
        }

        // Load global plant stage layers (bit/gandum)
        String[] bitNames = {"bit1", "bit2", "bit3", "bit4", "bitdone"};
        String[] wheatNames = {"gandum1", "gandum2", "gandum3", "gandum4", "gandumdone"};
        for (int i = 0; i < 5; i++) {
            try {
                bitStageLayers[i] = (com.badlogic.gdx.maps.tiled.TiledMapTileLayer) main.map.getLayers().get(bitNames[i]);
            } catch (Exception e) {
                bitStageLayers[i] = null;
            }
            try {
                wheatStageLayers[i] = (com.badlogic.gdx.maps.tiled.TiledMapTileLayer) main.map.getLayers().get(wheatNames[i]);
            } catch (Exception e) {
                wheatStageLayers[i] = null;
            }
        }
    }

    public static class Plant {
        public enum Type { BIT, WHEAT }
        private static final float WILT_DELAY = 2f;
        public Type type;
        public int areaIdx;
        public int cellIdx;
        public float growTime, maxGrowTime;
        public float waterTimer;
        public float nextWaterTime;
        public boolean needsWater;
        public float needWaterElapsed;
        public boolean wilted;

        public Plant(Type type, int areaIdx, int cellIdx, float maxGrowTime) {
            this.type = type;
            this.areaIdx = areaIdx;
            this.cellIdx = cellIdx;
            this.growTime = 0f;
            this.maxGrowTime = maxGrowTime;
            this.waterTimer = 0f;
            this.nextWaterTime = MathUtils.random(1f, 10f);
            this.needsWater = false;
            this.needWaterElapsed = 0f;
            this.wilted = false;
        }

        public void update(float dt) {
            if (wilted) return;
            if (growTime >= maxGrowTime) {
                needsWater = false;
                return;
            }
            waterTimer += dt;
            if (!needsWater && waterTimer >= nextWaterTime) {
                needsWater = true;
                needWaterElapsed = 0f;
            }
            if (needsWater) {
                needWaterElapsed += dt;
                if (needWaterElapsed >= WILT_DELAY) {
                    wilted = true;
                }
            } else {
                growTime = Math.min(growTime + dt, maxGrowTime);
            }
        }

        public void water() {
            if (wilted) return;
            needsWater = false;
            waterTimer = 0f;
            nextWaterTime = MathUtils.random(1f, 10f);
            needWaterElapsed = 0f;
        }

        public boolean isWilted() {
            return wilted;
        }

        public int getStage() {
            if (growTime >= maxGrowTime) return 4;
            float ratio = growTime / maxGrowTime;
            if (ratio < 0.25f) return 0;
            if (ratio < 0.5f) return 1;
            if (ratio < 0.75f) return 2;
            return 3;
        }

        public String getTileName() {
            int stage = getStage();
            if (type == Type.BIT) {
                if (stage == 4) return "bitdone";
                return "bit" + (stage + 1);
            } else {
                if (stage == 4) return "gandumdone";
                return "gandum" + (stage + 1);
            }
        }

        public boolean isDone() {
            return growTime >= maxGrowTime;
        }
    }

    public static class PlantSprite {
        public TextureRegion region;
        public float x, y;

        public PlantSprite(TextureRegion region, float x, float y) {
            this.region = region;
            this.x = x;
            this.y = y;
        }
    }

    private static int getPlantCellIndex(int areaIdx, int px, int py) {
        if (areaIdx < 0 || areaIdx >= 6) return -1;
        if (plantAreaLayers[areaIdx] == null) return -1;
        
        int minX = plantAreaMinX[areaIdx];
        int maxX = plantAreaMaxX[areaIdx];
        int minY = plantAreaMinY[areaIdx];
        int maxY = plantAreaMaxY[areaIdx];
        
        if (px < minX || px > maxX || py < minY || py > maxY) return -1;
        
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        
        int cellX = (int)((px - minX) * 3f / width);
        int cellY = (int)((py - minY) * 3f / height);
        
        cellX = Math.max(0, Math.min(2, cellX));
        cellY = Math.max(0, Math.min(2, cellY));
        
        return cellY * 3 + cellX;
    }

    public static void plantAtPlayer(Main main) {
        int px = (int)Math.floor(main.x);
        int py = (int)Math.floor(main.y);

        int areaIdx = -1;
        for (int i = 0; i < 6; i++) {
            if (plantAreaLayers[i] != null && main.hasTile(plantAreaLayers[i], px, py)) {
                areaIdx = i;
                break;
            }
        }

        if (areaIdx == -1) return;

        int cellIdx = getPlantCellIndex(areaIdx, px, py);
        if (cellIdx == -1) return;

        Plant.Type type = main.selectedSeed == 0 ? Plant.Type.BIT : Plant.Type.WHEAT;
        float maxGrowTime = main.selectedSeed == 0 ? 30f : 45f;

        if (main.selectedSeed == 0) {
            if (!main.inventory.plantBit()) return;
        } else {
            if (!main.inventory.plantWheat()) return;
        }

        Plant p = new Plant(type, areaIdx, cellIdx, maxGrowTime);
        plants.add(p);
        
        main.playerFatigue = Math.min(main.playerFatigue + 2f, 100f);
        main.markProgressDirty();
        main.markInventoryDirty();
    }

    public static void plantAtPlayerHotbar(Main main) {
        if (main.hotbarSelected < 0 || main.hotbarSelected >= main.hotbarMapping.length) {
            plantAtPlayer(main);
            return;
        }
        String item = main.hotbarMapping[main.hotbarSelected];
        if (item == null) {
            plantAtPlayer(main);
            return;
        }
        if (item.equals("seed_bit")) {
            if (!main.inventory.plantBit()) return;
            int px = (int)Math.floor(main.x);
            int py = (int)Math.floor(main.y);
            int areaIdx = -1;
            for (int i = 0; i < 6; i++) {
                if (plantAreaLayers[i] != null && main.hasTile(plantAreaLayers[i], px, py)) {
                    areaIdx = i; break;
                }
            }
            if (areaIdx == -1) return;
            int cellIdx = getPlantCellIndex(areaIdx, px, py);
            if (cellIdx == -1) return;
            plants.add(new Plant(Plant.Type.BIT, areaIdx, cellIdx, 30f));
            main.playerFatigue = Math.min(main.playerFatigue + 2f, 100f);
            main.markProgressDirty();
            main.markInventoryDirty();
            return;
        } else if (item.equals("seed_wheat")) {
            if (!main.inventory.plantWheat()) return;
            int px = (int)Math.floor(main.x);
            int py = (int)Math.floor(main.y);
            int areaIdx = -1;
            for (int i = 0; i < 6; i++) {
                if (plantAreaLayers[i] != null && main.hasTile(plantAreaLayers[i], px, py)) {
                    areaIdx = i; break;
                }
            }
            if (areaIdx == -1) return;
            int cellIdx = getPlantCellIndex(areaIdx, px, py);
            if (cellIdx == -1) return;
            plants.add(new Plant(Plant.Type.WHEAT, areaIdx, cellIdx, 45f));
            main.playerFatigue = Math.min(main.playerFatigue + 2f, 100f);
            main.markProgressDirty();
            main.markInventoryDirty();
            return;
        }
        plantAtPlayer(main);
    }

    public static boolean harvestAtPlayer(Main main) {
        int px = (int)Math.floor(main.x);
        int py = (int)Math.floor(main.y);

        int areaIdx = -1;
        for (int i = 0; i < 6; i++) {
            if (plantAreaLayers[i] != null && main.hasTile(plantAreaLayers[i], px, py)) {
                areaIdx = i;
                break;
            }
        }

        if (areaIdx == -1) return false;

        int cellIdx = getPlantCellIndex(areaIdx, px, py);
        if (cellIdx == -1) return false;

        boolean harvested = false;
        for (int i = plants.size() - 1; i >= 0; i--) {
            Plant p = plants.get(i);
            if (p.areaIdx == areaIdx && p.cellIdx == cellIdx && p.isDone()) {
                int amount = MathUtils.random(1, 5);
                boolean giveSeeds = MathUtils.randomBoolean();
                if (p.type == Plant.Type.BIT) {
                    if (giveSeeds) {
                        main.inventory.bitSeeds += amount;
                    } else {
                        main.inventory.harvestedBit += amount;
                    }
                } else {
                    if (giveSeeds) {
                        main.inventory.wheatSeeds += amount;
                    } else {
                        main.inventory.harvestedWheat += amount;
                    }
                }
                plants.remove(i);
                harvested = true;
                main.playerFatigue = Math.min(main.playerFatigue + 1f, 100f);
                main.markProgressDirty();
                main.markInventoryDirty();
            }
        }

        return harvested;
    }

    public static boolean waterAtPlayer(Main main) {
        int px = (int)Math.floor(main.x);
        int py = (int)Math.floor(main.y);

        int areaIdx = -1;
        for (int i = 0; i < 6; i++) {
            if (plantAreaLayers[i] != null && main.hasTile(plantAreaLayers[i], px, py)) {
                areaIdx = i;
                break;
            }
        }

        if (areaIdx == -1) return false;

        int cellIdx = getPlantCellIndex(areaIdx, px, py);
        if (cellIdx == -1) return false;

        boolean watered = false;
        for (Plant p : plants) {
            if (p.areaIdx == areaIdx && p.cellIdx == cellIdx && p.needsWater) {
                p.water();
                watered = true;
            }
        }

        if (watered) {
            main.playerFatigue = Math.min(main.playerFatigue + 1f, 100f);
            main.startWateringAnimation();
            main.markProgressDirty();
        }
        return watered;
    }
}