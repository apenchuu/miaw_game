package com.steven.frontend.features;
import com.steven.frontend.Main;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public final class CollisionFeature {
    private CollisionFeature() {}

public static boolean canMoveTo(Main main, float nx, float ny) {
        int tx = (int)Math.floor(nx);
        int ty = (int)Math.floor(ny);
        if (tx < 0 || ty < 0 || tx >= main.mapWidth || ty >= main.mapHeight) return false;
        boolean onWater = main.hasTile(main.waterLayer, tx, ty);
        boolean onGrass = main.hasTile(main.grassLayer, tx, ty);
        boolean onHouseFloor = main.hasTile(main.houseFloorLayer, tx, ty);
        boolean onSpawn = main.hasTile(main.spawnLayer, tx, ty);
        boolean onKeset = main.hasTile(main.kesetLayer, tx, ty);
        boolean onBridge = main.hasTile(main.bridgeLayer, tx, ty);
        boolean onBridgeBener = main.hasTile(main.bridgeBenerLayer, tx, ty);
        boolean onBridgeRusak1 = main.hasTile(main.bridgeRusak1Layer, tx, ty);
        boolean onBridgeRusak2 = main.hasTile(main.bridgeRusak2Layer, tx, ty);
        boolean onBridgeBener1 = main.hasTile(main.bridgeBener1Layer, tx, ty);
        boolean onBridgeBener2 = main.hasTile(main.bridgeBener2Layer, tx, ty);
        boolean onToKota = main.hasTile(main.toKotaLayer, tx, ty);
        boolean onToHome = main.hasTile(main.toHomeLayer, tx, ty);
        boolean onSpawnKota = main.hasTile(main.spawnKotaLayer, tx, ty);
        boolean onSpawnHome = main.hasTile(main.spawnHomeLayer, tx, ty);
        boolean onDoorOpen = main.hasTile(MapFeature.getActiveDoorOpenLayer(main), tx, ty);
        boolean onDoorHalf = main.hasTile(MapFeature.getActiveDoorHalfLayer(main), tx, ty);
        boolean onDoorClose = main.hasTile(MapFeature.getActiveDoorCloseLayer(main), tx, ty);
        boolean onHouseFinal = main.hasTile(main.houseFinalLayer, tx, ty);
        boolean onHouseFinalFix = main.hasTile(main.houseFinalFixLayer, tx, ty);
        boolean onHouseFinalFurniture = main.hasTile(main.houseFinalFurnitureLayer, tx, ty);
        boolean onHouseLine = main.hasTile(main.houseLineLayer, tx, ty);
        boolean onTree = main.hasTile(main.treeLayer, tx, ty);
        boolean onRock = main.hasTile(main.rockLayer, tx, ty);

        boolean collidesHouseObject = main.hasTile(main.houseObjectLayer, tx, ty);
        boolean collidesSellingPlace = main.hasTile(main.sellingPlaceLayer, tx, ty);
        boolean collidesChest = main.chestLayer != null && main.hasTile(main.chestLayer, tx, ty);
        boolean collidesChest1 = main.hasTile((TiledMapTileLayer) (main.map != null ? main.map.getLayers().get("chest1") : null), tx, ty);
        boolean collidesChest2 = main.hasTile((TiledMapTileLayer) (main.map != null ? main.map.getLayers().get("chest2") : null), tx, ty);

        // Explicit collision: if position is on an unrepaired broken bridge, block movement
        if (main.bridgeRusak1Layer != null && main.bridgeRusak1Layer.getCell(tx, ty) != null && !main.worldPusatBridge1Repaired) return false;
        if (main.bridgeRusak2Layer != null && main.bridgeRusak2Layer.getCell(tx, ty) != null && !main.worldPusatBridge2Repaired) return false;
        if (main.bridgeLayer != null && main.bridgeLayer.getCell(tx, ty) != null && !main.worldBridgeRepaired) return false;
        if (main.bridgeBenerLayer != null && main.bridgeBenerLayer.getCell(tx, ty) != null && !main.worldBridgeRepaired) return false;
        if (main.bridgeBener1Layer != null && main.bridgeBener1Layer.getCell(tx, ty) != null && !main.worldPusatBridge1Repaired) return false;
        if (main.bridgeBener2Layer != null && main.bridgeBener2Layer.getCell(tx, ty) != null && !main.worldPusatBridge2Repaired) return false;
        if ("world/world_pusat.tmx".equals(main.currentMapPath)) {
            if (onHouseFinal || onHouseFinalFix || onHouseFinalFurniture) return false;
        }

        boolean walkableBase = onGrass || onHouseFloor || onSpawn || onKeset
            || (onBridge && main.worldBridgeRepaired) || (onBridgeBener && main.worldBridgeRepaired)
            || (onBridgeBener1 && main.worldPusatBridge1Repaired) || (onBridgeBener2 && main.worldPusatBridge2Repaired)
            || onDoorOpen || onDoorHalf || onDoorClose
            || onToKota || onToHome || onSpawnKota || onSpawnHome;
        if (!walkableBase) return false;
        if ((onWater && !((onBridge && main.worldBridgeRepaired) || (onBridgeBener && main.worldBridgeRepaired) || (onBridgeBener1 && main.worldPusatBridge1Repaired) || (onBridgeBener2 && main.worldPusatBridge2Repaired))) || onHouseLine || onTree || onRock) return false;
        if (collidesHouseObject || collidesSellingPlace || collidesChest || collidesChest1 || collidesChest2) return false;

        // Only block door entry if house is not repaired in world_pusat
        if ("world/world_pusat.tmx".equals(main.currentMapPath) && !main.houseFinalRepaired) {
            if ((onDoorClose || onDoorHalf || onDoorOpen)) return false;
        }

        if ((onDoorClose || onDoorHalf) && com.steven.frontend.features.DoorFeature.doorState != com.steven.frontend.features.DoorFeature.DoorState.OPEN) return false;

        return true;
    }
}
