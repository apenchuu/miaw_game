package com.steven.frontend.features;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.steven.frontend.Main;
import com.steven.frontend.features.PlantFeature.Plant;

public class LogicFeature {
    public static void update(Main main, float dt) {
        // Update game time (24-hour cycle)
        if (!main.isSleeping) {
            main.gameTime += dt * main.TIME_SPEED;
            if (main.gameTime >= 24f) main.gameTime -= 24f;
            main.markProgressDirty();
        }

        // Update coin animation
        if (main.coinAnimation != null) {
            main.coinAnimTime += dt;
        }

        if (main.wateringActive) {
            main.wateringAnimTime += dt;
            com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> activeWaterAnim = main.getWateringAnimationForFacing(main.wateringFacing);
            if (activeWaterAnim == null || main.wateringAnimTime >= activeWaterAnim.getAnimationDuration()) {
                main.wateringActive = false;
                main.wateringAnimTime = 0f;
            }
        }

        // Update chest reward animation
        com.steven.frontend.features.ChestFeature.updateChestRewardSequence(main, dt);
        com.steven.frontend.features.ChestFeature.updateChestLayerVisibility(main);

        com.steven.frontend.features.DoorFeature.updateDoor(main, dt);
        
        // Update plants and remove wilted ones
        for (int i = PlantFeature.plants.size() - 1; i >= 0; i--) {
            Plant p = PlantFeature.plants.get(i);
            p.update(dt);
            if (p.isWilted()) {
                PlantFeature.plants.remove(i);
            }
        }

        boolean moving = false;
        float dx = 0f, dy = 0f;
        if (main.isSleeping) {
            dx = 0f;
            dy = 0f;
        }
        // Calculate effective speed based on fatigue
        float effectiveSpeed = main.speed * (1f - main.playerFatigue / 100f);
        if (main.playerFatigue >= 100f) effectiveSpeed = 0f; // Completely stopped if exhausted
        if (com.steven.frontend.features.TradeFeature.buyMenuOpen || com.steven.frontend.features.TradeFeature.sellMenuOpen || ChestFeature.chestAnimActive || ChestFeature.chestRewardOpen) effectiveSpeed = 0f;

        if (effectiveSpeed > 0f) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
                dy += effectiveSpeed * dt;
                main.lastFacing = Main.Facing.UP;
                if (main.animLeft != null) main.currentAnim = main.animLeft;
            } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
                dy -= effectiveSpeed * dt;
                main.lastFacing = Main.Facing.DOWN;
                if (main.animDown != null) main.currentAnim = main.animDown;
            }

            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
                dx -= effectiveSpeed * dt;
                main.lastFacing = Main.Facing.LEFT;
                if (main.animRight != null) main.currentAnim = main.animRight;
            } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
                dx += effectiveSpeed * dt;
                main.lastFacing = Main.Facing.RIGHT;
                if (main.animUp != null) main.currentAnim = main.animUp;
            }
        }

        // Check proposed movement against grass layer (if available)
        if (dx != 0f || dy != 0f) {
            float nx = main.x + dx;
            float ny = main.y + dy;
            if (com.steven.frontend.features.CollisionFeature.canMoveTo(main, nx, ny)) {
                main.movementStrategy.apply(dx, dy);
                moving = true;
            }
        }

        if (moving) main.stateTime += dt; else main.stateTime = 0f;

        if (main.mapTransitionCooldown > 0f) {
            main.mapTransitionCooldown -= dt;
        }

        if (!main.isSleeping && main.mapTransitionCooldown <= 0f) {
            com.steven.frontend.features.MovementFeature.checkMapTransition(main);
        }

        // Increase fatigue slightly when player is actively moving
        if (moving && !main.isSleeping) {
            main.playerFatigue = Math.min(main.playerFatigue + dt * main.MOVE_FATIGUE_RATE, main.MAX_FATIGUE);
            main.markProgressDirty();
        }
        
        // Collect coin at player's position (if any)
        int pTileX = (int)Math.floor(main.x);
        int pTileY = (int)Math.floor(main.y);
        if (main.coinLayer != null) {
            try {
                if (pTileX >= 0 && pTileY >= 0 && pTileX < main.coinLayer.getWidth() && pTileY < main.coinLayer.getHeight()) {
                    if (main.coinLayer.getCell(pTileX, pTileY) != null) {
                        main.coinLayer.setCell(pTileX, pTileY, null);
                        main.inventory.coins += 1000;
                        main.totalCoinsEarned += 1000;
                        main.worldCoinCollected = true;
                        main.markProgressDirty();
                        com.steven.frontend.features.CompletionFeature.checkForGameCompletion(main);
                    }
                }
            } catch (Exception e) {}
        }

        // Ensure completed GUI appears as soon as all conditions are met, even if no other event fires.
        com.steven.frontend.features.CompletionFeature.checkForGameCompletion(main);
    }
}
