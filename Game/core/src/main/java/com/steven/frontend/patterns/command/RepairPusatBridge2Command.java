package com.steven.frontend.patterns.command;

import com.steven.frontend.Main;

// Command untuk memperbaiki jembatan rusak kedua di world pusat.
public class RepairPusatBridge2Command implements ICommand {
    private final Main game;

    public RepairPusatBridge2Command(Main game) {
        this.game = game;
    }

    @Override
    public void execute() {
        com.steven.frontend.features.RepairFeature.repairWorldPusatBridge2FromCommand(game);
    }
}
