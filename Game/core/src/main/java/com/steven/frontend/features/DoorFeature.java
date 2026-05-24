package com.steven.frontend.features;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.steven.frontend.Main;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public final class DoorFeature {
    private DoorFeature() {}

    public enum DoorState { CLOSED, OPENING_HALF, OPENING_OPEN, OPEN, CLOSING_HALF, CLOSING }
    public static DoorState doorState = DoorState.CLOSED;
    public static float doorAnimTimer = 0f;
    public static final float DOOR_HALF_TIME = 0.12f;
    public static final float DOOR_OPEN_TIME = 0.12f;

    public static void updateDoor(Main main, float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            boolean canToggleDoor = true;
            if ("world/world_pusat.tmx".equals(main.currentMapPath)) {
                canToggleDoor = main.houseFinalRepaired;
            }
            if (canToggleDoor) {
                if (doorState == DoorState.CLOSED && isNearClosedDoor(main)) {
                    doorState = DoorState.OPENING_HALF;
                    doorAnimTimer = 0f;
                    setDoorLayersVisible(main, false, true, false);
                } else if (doorState == DoorState.OPEN && isNearDoor(main)) {
                    doorState = DoorState.CLOSING_HALF;
                    doorAnimTimer = 0f;
                    setDoorLayersVisible(main, false, true, false);
                }
            }
        }

        if (doorState == DoorState.OPENING_HALF) {
            doorAnimTimer += dt;
            if (doorAnimTimer >= DOOR_HALF_TIME) {
                doorState = DoorState.OPENING_OPEN;
                doorAnimTimer = 0f;
                setDoorLayersVisible(main, false, false, true);
            }
        } else if (doorState == DoorState.OPENING_OPEN) {
            doorAnimTimer += dt;
            if (doorAnimTimer >= DOOR_OPEN_TIME) {
                doorState = DoorState.OPEN;
                doorAnimTimer = 0f;
            }
        } else if (doorState == DoorState.CLOSING_HALF) {
            doorAnimTimer += dt;
            if (doorAnimTimer >= DOOR_HALF_TIME) {
                doorState = DoorState.CLOSING;
                doorAnimTimer = 0f;
                setDoorLayersVisible(main, true, false, false);
            }
        } else if (doorState == DoorState.CLOSING) {
            doorAnimTimer += dt;
            if (doorAnimTimer >= DOOR_OPEN_TIME) {
                doorState = DoorState.CLOSED;
                doorAnimTimer = 0f;
            }
        }
    }

    public static void setDoorLayersVisible(Main main, boolean closeVisible, boolean halfVisible, boolean openVisible) {
        if ("world/world_pusat.tmx".equals(main.currentMapPath)) {
            // Handle house_final doors
            if (main.houseFinalDoorCloseLayer != null) main.houseFinalDoorCloseLayer.setVisible(closeVisible);
            if (main.houseFinalDoorHalfLayer != null) main.houseFinalDoorHalfLayer.setVisible(halfVisible);
            if (main.houseFinalDoorOpenLayer != null) main.houseFinalDoorOpenLayer.setVisible(openVisible);
        } else {
            // Handle regular world doors
            if (main.doorsCloseLayer != null) main.doorsCloseLayer.setVisible(closeVisible);
            if (main.doorsHalfLayer != null) main.doorsHalfLayer.setVisible(halfVisible);
            if (main.doorsOpenLayer != null) main.doorsOpenLayer.setVisible(openVisible);
        }
    }

    public static boolean isNearClosedDoor(Main main) {
        if ("world/world_pusat.tmx".equals(main.currentMapPath)) {
            return isNearLayer(main, main.houseFinalDoorCloseLayer);
        }
        return isNearLayer(main, main.doorsCloseLayer);
    }

    public static boolean isNearDoor(Main main) {
        if ("world/world_pusat.tmx".equals(main.currentMapPath)) {
            return isNearLayer(main, main.houseFinalDoorCloseLayer) || 
                   isNearLayer(main, main.houseFinalDoorHalfLayer) || 
                   isNearLayer(main, main.houseFinalDoorOpenLayer);
        }
        return isNearLayer(main, main.doorsCloseLayer) || 
               isNearLayer(main, main.doorsHalfLayer) || 
               isNearLayer(main, main.doorsOpenLayer);
    }

    private static boolean isNearLayer(Main main, TiledMapTileLayer layer) {
        if (layer == null) return false;
        int px = (int)Math.floor(main.x);
        int py = (int)Math.floor(main.y);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (main.hasTile(layer, px + dx, py + dy)) {
                    return true;
                }
            }
        }
        return false;
    }
}
