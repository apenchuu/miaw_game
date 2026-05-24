package com.steven.frontend.patterns.command;

import com.steven.frontend.Main;

// Command untuk memperbaiki jembatan rusak pertama di world pusat.
public class RepairPusatBridge1Command implements ICommand {
    private final Main game;

    public RepairPusatBridge1Command(Main game) {
        this.game = game;
    }

    @Override
    public void execute() {
        com.steven.frontend.features.RepairFeature.repairWorldPusatBridge1FromCommand(game);
    }
}
