package com.steven.frontend.features;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.steven.frontend.Main;

public final class CompletionFeature {
    private CompletionFeature() {}

    public static void initializeGameCompletedUI(Main main) {
        if (main.gameCompleteStage != null) return;
        main.gameCompleteStage = new Stage(new ScreenViewport());

        // Reuse main.authSkin if exists, otherwise create minimal skin
        if (main.authSkin != null) {
            main.gameCompleteSkin = main.authSkin;
        } else {
            main.gameCompleteSkin = new Skin();
            com.badlogic.gdx.graphics.g2d.BitmapFont f = new com.badlogic.gdx.graphics.g2d.BitmapFont();
            main.gameCompleteSkin.add("default-font", f);
            Label.LabelStyle ls = new Label.LabelStyle(); ls.font = f; ls.fontColor = Color.WHITE;
            main.gameCompleteSkin.add("default", ls);
            Label.LabelStyle ts = new Label.LabelStyle(); ts.font = f; ts.fontColor = Color.WHITE;
            main.gameCompleteSkin.add("title", ts);
        }

        
        Table mainTable = new Table(main.gameCompleteSkin);
        mainTable.setFillParent(true);
        mainTable.center();
        mainTable.pad(20f);
        mainTable.defaults().expandX().center();
        // background image like login form if available
        if (main.loginFormTexture != null) {
            mainTable.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable(new com.badlogic.gdx.graphics.g2d.Sprite(main.loginFormTexture)));
        }

        Label title = main.uiFactory.createLabel(main.gameCompleteSkin, "Game Completed", "title");
        title.setFontScale(1.6f);
        title.setAlignment(com.badlogic.gdx.utils.Align.center);
        mainTable.add(title).padBottom(14f).expandX().row();

        Label msg = main.uiFactory.createLabel(main.gameCompleteSkin, "Congratulations! You have completed the game.");
        msg.setWrap(true);
        msg.setFontScale(1.0f);
        msg.setAlignment(com.badlogic.gdx.utils.Align.center);
        mainTable.add(msg).width(Math.min(420f, com.steven.frontend.patterns.singleton.GameConfig.get().getUiWidth() - 80f)).padBottom(18f).row();

        // Untuk menampilkan ringkasan statistik permainan secara terpusat.
        Label statDays = main.uiFactory.createLabel(main.gameCompleteSkin, "Days played: " + main.currentDay);
        statDays.setAlignment(com.badlogic.gdx.utils.Align.center);
        mainTable.add(statDays).padBottom(6f).row();
        Label statHarvest = main.uiFactory.createLabel(main.gameCompleteSkin, "Harvested crops: " + (main.inventory.harvestedBit + main.inventory.harvestedWheat));
        statHarvest.setAlignment(com.badlogic.gdx.utils.Align.center);
        mainTable.add(statHarvest).padBottom(6f).row();
        Label statCoins = main.uiFactory.createLabel(main.gameCompleteSkin, "Total coins earned: " + main.totalCoinsEarned);
        statCoins.setAlignment(com.badlogic.gdx.utils.Align.center);
        mainTable.add(statCoins).padBottom(6f).row();
        Label statConsumed = main.uiFactory.createLabel(main.gameCompleteSkin, "Consumed items: " + main.consumedCount);
        statConsumed.setAlignment(com.badlogic.gdx.utils.Align.center);
        mainTable.add(statConsumed).padBottom(6f).row();
        Label statSold = main.uiFactory.createLabel(main.gameCompleteSkin, "Sold items: " + main.soldCount);
        statSold.setAlignment(com.badlogic.gdx.utils.Align.center);
        mainTable.add(statSold).padBottom(6f).row();

        main.gameCompleteStage.addActor(mainTable);
    }



    public static void checkForGameCompletion(Main main) {
        // Untuk mengecek kondisi kemenangan dan memicu layar selesai permainan lewat observer.
        boolean all = main.worldBridgeRepaired && main.worldPusatBridge1Repaired && main.worldPusatBridge2Repaired
            && main.houseFinalRepaired && ChestFeature.worldChestClaimed && ChestFeature.worldPusatChest1Claimed && ChestFeature.worldPusatChest2Claimed
            && main.worldCoinCollected;

        // Debug logging to trace completion condition
        System.out.println("[completion] worldBridgeRepaired=" + main.worldBridgeRepaired
            + " worldPusatBridge1=" + main.worldPusatBridge1Repaired
            + " worldPusatBridge2=" + main.worldPusatBridge2Repaired
            + " houseFinalRepaired=" + main.houseFinalRepaired
            + " worldChestClaimed=" + ChestFeature.worldChestClaimed
            + " wp1Chest=" + ChestFeature.worldPusatChest1Claimed
            + " wp2Chest=" + ChestFeature.worldPusatChest2Claimed
            + " worldCoinCollected=" + main.worldCoinCollected
            + " => all=" + all + " gameCompletedShown=" + main.gameCompletedShown);

        if (all && !main.gameCompletedShown) {
            System.out.println("[completion] conditions met: notifying listeners");
            main.gameCompletedShown = true;
            main.gameEventManager.notifyGameCompleted();
        }
    }


}
