package com.steven.frontend.patterns.strategy;

import com.steven.frontend.Main;

// Implementasi default untuk perpindahan karakter.
public class DefaultMovementStrategy implements MovementStrategy {
    private final Main game;

    public DefaultMovementStrategy(Main game) {
        this.game = game;
    }

    @Override
    public void apply(float dx, float dy) {
        game.moveBy(dx, dy);
    }
}
