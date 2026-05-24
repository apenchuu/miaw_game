package com.steven.frontend.patterns.command;

import com.steven.frontend.Main;

// Command untuk memperbaiki jembatan utama.
public class RepairBridgeCommand implements ICommand {
    private final Main game;

    public RepairBridgeCommand(Main game) {
        this.game = game;
    }

    @Override
    public void execute() {
        com.steven.frontend.features.RepairFeature.repairWorldBridgeFromCommand(game);
    }
}
