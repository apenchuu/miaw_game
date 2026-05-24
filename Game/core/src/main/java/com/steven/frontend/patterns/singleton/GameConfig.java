package com.steven.frontend.patterns.singleton;

// Menyimpan konfigurasi global untuk ukuran antarmuka game.
public final class GameConfig {
    private static final GameConfig INSTANCE = new GameConfig();

    private final int uiWidth = 640;
    private final int uiHeight = 480;

    private GameConfig() {}

    public static GameConfig get() {
        return INSTANCE;
    }

    public int getUiWidth() {
        return uiWidth;
    }

    public int getUiHeight() {
        return uiHeight;
    }
}
