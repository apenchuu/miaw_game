package com.steven.frontend.features;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.steven.frontend.Main;

// Mengelola proses tidur pemain dari file terpisah.
public final class SleepFeature {
    private SleepFeature() {}

    public static final float SLEEP_DURATION = 1.5f; // Duration of sleep in seconds

    public static void processSleep(Main main, float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            int px = (int)Math.floor(main.x);
            int py = (int)Math.floor(main.y);
            TiledMapTileLayer activeBedLayer = "world/world_pusat.tmx".equals(main.currentMapPath) ? main.houseFinalBedLayer : main.bedLayer;
            boolean onBed = false;
            if (activeBedLayer != null) {
                for (int dx = -1; dx <= 1 && !onBed; dx++) {
                    for (int dy = -1; dy <= 1 && !onBed; dy++) {
                        if (main.hasTile(activeBedLayer, px + dx, py + dy)) onBed = true;
                    }
                }
            }
            if ((main.gameTime >= 21f || main.playerFatigue >= 100f) && !main.isSleeping && onBed) {
                main.isSleeping = true;
                main.sleepTimer = 0f;
                main.markProgressDirty();
            }
        }

        if (main.isSleeping) {
            main.sleepTimer += dt;
            if (main.sleepTimer >= SLEEP_DURATION) {
                main.isSleeping = false;
                main.sleepTimer = 0f;
                main.gameTime = 6f;
                main.currentDay++;
                main.playerFatigue = 0f;
                main.markProgressDirty();
            }
        }
    }
}
