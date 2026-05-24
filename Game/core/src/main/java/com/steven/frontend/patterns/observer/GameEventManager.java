package com.steven.frontend.patterns.observer;

import java.util.ArrayList;
import java.util.List;

// Mengelola listener dan menyebarkan event ke semua pendengar.
public class GameEventManager {
    private final List<GameEventListener> listeners = new ArrayList<>();

    public void addListener(GameEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void notifyGameCompleted() {
        for (GameEventListener listener : listeners) {
            try {
                listener.onGameCompleted();
            } catch (Exception ignored) {
            }
        }
    }
}
