package com.steven.frontend.patterns.strategy;

// Strategi dasar untuk memindahkan karakter.
public interface MovementStrategy {
    void apply(float dx, float dy);
}
