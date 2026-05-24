package com.steven.frontend.features;

import com.steven.frontend.Main;
import com.steven.frontend.patterns.command.RepairBridgeCommand;
import com.steven.frontend.patterns.command.RepairPusatBridge1Command;
import com.steven.frontend.patterns.command.RepairPusatBridge2Command;

public final class RepairFeature {
    private RepairFeature() {}

    public static final int WORLD_BRIDGE_COST = 2000;
    public static final int WORLD_PUSAT_BRIDGE1_COST = 3000;
    public static final int WORLD_PUSAT_BRIDGE2_COST = 4000;
    public static final int HOUSE_FINAL_COST = 5000;

    public static void repairWorldBridgeFromCommand(Main main) {
        main.inventory.coins -= WORLD_BRIDGE_COST;
        main.worldBridgeRepaired = true;
        main.markProgressDirty();
        com.steven.frontend.features.MapFeature.applyPersistentLayerVisibility(main);
        com.steven.frontend.features.CompletionFeature.checkForGameCompletion(main);
    }

    public static void repairWorldPusatBridge1FromCommand(Main main) {
        main.inventory.coins -= WORLD_PUSAT_BRIDGE1_COST;
        main.worldPusatBridge1Repaired = true;
        main.markProgressDirty();
        com.steven.frontend.features.MapFeature.applyPersistentLayerVisibility(main);
        com.steven.frontend.features.CompletionFeature.checkForGameCompletion(main);
    }

    public static void repairWorldPusatBridge2FromCommand(Main main) {
        main.inventory.coins -= WORLD_PUSAT_BRIDGE2_COST;
        main.worldPusatBridge2Repaired = true;
        main.markProgressDirty();
        com.steven.frontend.features.MapFeature.applyPersistentLayerVisibility(main);
        com.steven.frontend.features.CompletionFeature.checkForGameCompletion(main);
    }

    public static boolean handleBridgeRepair(Main main) {
        int px = (int)Math.floor(main.x);
        int py = (int)Math.floor(main.y);
        
        if (!main.worldBridgeRepaired && main.bridgeLayer != null) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (main.hasTile(main.bridgeLayer, px + dx, py + dy)) {
                        if (main.inventory.coins >= WORLD_BRIDGE_COST) {
                            main.invoker.invoke(new RepairBridgeCommand(main));
                            return true;
                        }
                    }
                }
            }
        }
        
        if (!main.worldPusatBridge1Repaired && main.bridgeRusak1Layer != null) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (main.hasTile(main.bridgeRusak1Layer, px + dx, py + dy)) {
                        if (main.inventory.coins >= WORLD_PUSAT_BRIDGE1_COST) {
                            main.invoker.invoke(new RepairPusatBridge1Command(main));
                            return true;
                        }
                    }
                }
            }
        }
        
        if (!main.worldPusatBridge2Repaired && main.bridgeRusak2Layer != null) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (main.hasTile(main.bridgeRusak2Layer, px + dx, py + dy)) {
                        if (main.inventory.coins >= WORLD_PUSAT_BRIDGE2_COST) {
                            main.invoker.invoke(new RepairPusatBridge2Command(main));
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean handleHouseFinalRepair(Main main) {
        if (main.houseFinalRepaired) return false;
        int px = (int)Math.floor(main.x);
        int py = (int)Math.floor(main.y);
        if (main.houseFinalLayer != null || main.houseFinalFixLayer != null) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (main.hasTile(main.houseFinalLayer, px + dx, py + dy) || main.hasTile(main.houseFinalFixLayer, px + dx, py + dy)) {
                        if (main.inventory.coins >= HOUSE_FINAL_COST) {
                            main.inventory.coins -= HOUSE_FINAL_COST;
                            main.houseFinalRepaired = true;
                            main.markProgressDirty();
                            com.steven.frontend.features.MapFeature.applyPersistentLayerVisibility(main);
                            com.steven.frontend.features.CompletionFeature.checkForGameCompletion(main);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
