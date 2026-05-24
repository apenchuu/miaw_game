package com.steven.frontend.features;
import com.steven.frontend.Main;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;

public final class MapFeature {
    private MapFeature() {}
    
    public static boolean layerHasAnyTile(Main main, TiledMapTileLayer layer) {
        if (layer == null) return false;
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                if (layer.getCell(x, y) != null) return true;
            }
        }
        return false;
    }
    
    public static TiledMapTileLayer getActiveDoorCloseLayer(Main main) {
        if ("world/house_final.tmx".equals(main.currentMapPath)) return main.houseFinalDoorCloseLayer;
        return main.doorsCloseLayer;
    }
    
    public static TiledMapTileLayer getActiveDoorHalfLayer(Main main) {
        if ("world/house_final.tmx".equals(main.currentMapPath)) return main.houseFinalDoorHalfLayer;
        return main.doorsHalfLayer;
    }
    
    public static TiledMapTileLayer getActiveDoorOpenLayer(Main main) {
        if ("world/house_final.tmx".equals(main.currentMapPath)) return main.houseFinalDoorOpenLayer;
        return main.doorsOpenLayer;
    }
    
    public static boolean hasTile(Main main, TiledMapTileLayer layer, int tx, int ty) {
        if (layer == null) return false;
        if (tx < 0 || ty < 0 || tx >= layer.getWidth() || ty >= layer.getHeight()) return false;
        Cell cell = layer.getCell(tx, ty);
        return cell != null && cell.getTile() != null;
    }

    public static void applyPersistentLayerVisibility(Main main) {
        if (main.bridgeLayer != null) main.bridgeLayer.setVisible(!main.worldBridgeRepaired);
        if (main.bridgeBenerLayer != null) main.bridgeBenerLayer.setVisible(main.worldBridgeRepaired);
        if (main.bridgeRusak1Layer != null) main.bridgeRusak1Layer.setVisible(!main.worldPusatBridge1Repaired);
        if (main.bridgeBener1Layer != null) main.bridgeBener1Layer.setVisible(main.worldPusatBridge1Repaired);
        if (main.bridgeRusak2Layer != null) main.bridgeRusak2Layer.setVisible(!main.worldPusatBridge2Repaired);
        if (main.bridgeBener2Layer != null) main.bridgeBener2Layer.setVisible(main.worldPusatBridge2Repaired);
        if (main.houseFinalLayer != null) main.houseFinalLayer.setVisible(!main.houseFinalRepaired);
        if (main.houseFinalFixLayer != null) main.houseFinalFixLayer.setVisible(main.houseFinalRepaired);
        if (main.houseFinalFurnitureLayer != null) main.houseFinalFurnitureLayer.setVisible(main.houseFinalRepaired);
        if (main.houseFinalBedLayer != null) main.houseFinalBedLayer.setVisible(main.houseFinalRepaired);
    }
    
    public static void checkMapTransition(Main main) {
        int px = (int)java.lang.Math.floor(main.x);
        int py = (int)java.lang.Math.floor(main.y);

        boolean isWorld = "world/world.tmx".equals(main.currentMapPath) || "world.tmx".equals(main.currentMapPath);
        boolean isWorldPusat = "world/world_pusat.tmx".equals(main.currentMapPath) || "world_pusat.tmx".equals(main.currentMapPath);

        if (isWorld && hasTile(main, main.toKotaLayer, px, py)) {
            main.switchMap("world/world_pusat.tmx", "spawn_home");
            return;
        }

        if (isWorldPusat && hasTile(main, main.toHomeLayer, px, py)) {
            main.switchMap("world/world.tmx", "spawn_kota");
        }
    }


    public static void updateMapDimensionsAndViewport(Main main) {
        if (main.map == null) return;
        com.badlogic.gdx.maps.MapProperties props = main.map.getProperties();
        main.mapWidth = props.get("width", Integer.class);
        main.mapHeight = props.get("height", Integer.class);
        if (main.camera == null) {
            main.camera = new com.badlogic.gdx.graphics.OrthographicCamera();
            main.camera.setToOrtho(false, 40, 22.5f);
        }
    }

}
