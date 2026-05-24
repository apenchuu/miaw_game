package com.steven.frontend.features;
import com.steven.frontend.Main;
import com.steven.frontend.Main.Facing;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public final class AssetFeature {
    private AssetFeature() {}
    
    public static Animation<TextureRegion> createAnimationFromSheet(Main main, TextureRegion[][] tiles, int rowIndex, int[] colIndexes) {
        Array<TextureRegion> frames = new Array<>();
        for (int col : colIndexes) {
            frames.add(tiles[rowIndex][col]);
        }
        return new Animation<>(0.2f, frames, Animation.PlayMode.LOOP);
    }
    
    public static Animation<TextureRegion> getWateringAnimationForFacing(Main main, Facing facing) {
        switch (facing) {
            case LEFT: return main.waterAnimLeft;
            case RIGHT: return main.waterAnimRight;
            case UP: return main.waterAnimRight; // use right for up
            case DOWN: return main.waterAnimDown;
            default: return main.waterAnimDown;
        }
    }
    
    public static void startWateringAnimation(Main main) {
        main.wateringAnimTime = 0f;
        main.wateringFacing = main.lastFacing;
        main.wateringActive = true;
        Animation<TextureRegion> anim = getWateringAnimationForFacing(main, main.wateringFacing);
        if (anim != null) {
            anim.setPlayMode(Animation.PlayMode.NORMAL);
        }
    }

    public static void loadAllAssets(Main main) {

        int frameW = 16;
        int frameH = 16;
        main.sheetTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("character/character.png"));
        com.badlogic.gdx.graphics.g2d.TextureRegion[][] tiles = com.badlogic.gdx.graphics.g2d.TextureRegion.split(main.sheetTexture, frameW, frameH);
        int[] frameCols = new int[]{1, 4, 7, 10};

        main.animDown = createAnimationFromSheet(main, tiles, 1, frameCols);
        main.animLeft = createAnimationFromSheet(main, tiles, 4, frameCols);
        main.animRight = createAnimationFromSheet(main, tiles, 7, frameCols);
        main.animUp = createAnimationFromSheet(main, tiles, 10, frameCols);

        com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> fallbackAnim = main.animDown;
        if (main.animLeft == null) main.animLeft = fallbackAnim;
        if (main.animRight == null) main.animRight = fallbackAnim;
        if (main.animUp == null) main.animUp = fallbackAnim;

        try {
            main.waterTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("character/watering.png"));
            com.badlogic.gdx.graphics.g2d.TextureRegion[][] waterTiles = com.badlogic.gdx.graphics.g2d.TextureRegion.split(main.waterTexture, frameW, frameH);
            main.waterAnimLeft = createAnimationFromSheet(main, waterTiles, 10, frameCols); 
            main.waterAnimRight = main.waterAnimLeft;
            main.waterAnimDown = main.waterAnimLeft;
            main.waterAnimUp = main.waterAnimLeft;
        } catch (Exception e) {
            main.waterTexture = null;
            main.waterAnimDown = null;
            main.waterAnimLeft = null;
            main.waterAnimRight = null;
            main.waterAnimUp = null;
        }

        main.currentAnim = main.animDown != null ? main.animDown : (main.animLeft != null ? main.animLeft : (main.animRight != null ? main.animRight : main.animUp));
        main.stateTime = 0f;

        try {
            main.seedTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("tools/seed.png"));
            com.badlogic.gdx.graphics.g2d.TextureRegion[][] seedTiles = com.badlogic.gdx.graphics.g2d.TextureRegion.split(main.seedTexture, 16, 16);
            if (seedTiles.length > 0 && seedTiles[0].length > 0) {
                main.seedWheatRegion = seedTiles[0][0];
            }
            if (seedTiles.length > 1 && seedTiles[1].length > 0) {
                main.seedBitRegion = seedTiles[1][0];
            }
            if (main.seedBitRegion == null && seedTiles.length > 0 && seedTiles[0].length > 0) main.seedBitRegion = seedTiles[0][0];
            if (main.seedWheatRegion == null && seedTiles.length > 0 && seedTiles[0].length > 0) main.seedWheatRegion = seedTiles[0][0];
        } catch (Exception e) {
            main.seedTexture = null;
            main.seedWheatRegion = null;
            main.seedBitRegion = null;
        }

        try {
            main.toolsTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("tools/tools.png"));
            com.badlogic.gdx.graphics.g2d.TextureRegion[][] toolTiles = com.badlogic.gdx.graphics.g2d.TextureRegion.split(main.toolsTexture, 16, 16);
            if (toolTiles.length > 0 && toolTiles[0].length > 0) {
                main.toolBox3Region = toolTiles[0][0];
            }
        } catch (Exception e) {
            main.toolsTexture = null;
            main.toolBox3Region = null;
        }

        try {
            main.harvestedBitTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("tools/bit.png"));
            main.harvestedBitRegion = new com.badlogic.gdx.graphics.g2d.TextureRegion(main.harvestedBitTexture);
        } catch (Exception e) {
            main.harvestedBitTexture = null;
            main.harvestedBitRegion = null;
        }

        try {
            main.harvestedWheatTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("tools/wheat.png"));
            main.harvestedWheatRegion = new com.badlogic.gdx.graphics.g2d.TextureRegion(main.harvestedWheatTexture);
        } catch (Exception e) {
            main.harvestedWheatTexture = null;
            main.harvestedWheatRegion = null;
        }

        try {
            main.coinTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("tools/coin.png"));
            com.badlogic.gdx.graphics.g2d.TextureRegion[][] coinTiles = com.badlogic.gdx.graphics.g2d.TextureRegion.split(main.coinTexture, 16, 16);
            com.badlogic.gdx.graphics.g2d.TextureRegion[] coinFrames = new com.badlogic.gdx.graphics.g2d.TextureRegion[coinTiles.length * coinTiles[0].length];
            int idx = 0;
            for (int r = 0; r < coinTiles.length; r++) {
                for (int c = 0; c < coinTiles[r].length; c++) {
                    coinFrames[idx++] = coinTiles[r][c];
                }
            }
            if (idx > 0) {
                com.badlogic.gdx.graphics.g2d.TextureRegion[] trimmedFrames = new com.badlogic.gdx.graphics.g2d.TextureRegion[idx];
                System.arraycopy(coinFrames, 0, trimmedFrames, 0, idx);
                main.coinAnimation = new com.badlogic.gdx.graphics.g2d.Animation<>(0.1f, trimmedFrames);
                main.coinAnimation.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            }
        } catch (Exception e) {
            main.coinTexture = null;
            main.coinAnimation = null;
        }

        try { main.buyLayoutTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("layout/buy_layout.png")); } catch (Exception e) { main.buyLayoutTexture = null; }
        try { main.sellLayoutTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("layout/sell_layout.png")); } catch (Exception e) { main.sellLayoutTexture = null; }
        try { main.chestRewardLayoutTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("layout/chest_reward.png")); } catch (Exception e) { main.chestRewardLayoutTexture = null; }
        try { main.chestRewardLayoutWp1Texture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("layout/chest_reward_wp1.png")); } catch (Exception e) { main.chestRewardLayoutWp1Texture = null; }
        try { main.chestRewardLayoutWp2Texture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("layout/chest_reward_wp2.png")); } catch (Exception e) { main.chestRewardLayoutWp2Texture = null; }
        
        try { main.loginFormTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("login/form_login.png")); } catch (Exception e) { main.loginFormTexture = null; }
        try { main.usernameFormTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("login/username_form.png")); } catch (Exception e) { main.usernameFormTexture = null; }
        try { main.passwordFormTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("login/password_form.png")); } catch (Exception e) { main.passwordFormTexture = null; }
        try { main.loginButtonTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("login/login_button.png")); } catch (Exception e) { main.loginButtonTexture = null; }
        try { main.registerButtonTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("login/register_button.png")); } catch (Exception e) { main.registerButtonTexture = null; }
        
        main.loginButtonBounds = new com.badlogic.gdx.math.Rectangle(main.loginButtonX, main.loginButtonY, 
            main.loginButtonTexture != null ? main.loginButtonTexture.getWidth() : 120, 
            main.loginButtonTexture != null ? main.loginButtonTexture.getHeight() : 40);
        main.registerButtonBounds = new com.badlogic.gdx.math.Rectangle(main.registerButtonX, main.registerButtonY, 
            main.registerButtonTexture != null ? main.registerButtonTexture.getWidth() : 120, 
            main.registerButtonTexture != null ? main.registerButtonTexture.getHeight() : 40);

        try {
            main.frontChestTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("tools/front_chest.png"));
            com.badlogic.gdx.graphics.g2d.TextureRegion[][] frontChestTiles = com.badlogic.gdx.graphics.g2d.TextureRegion.split(main.frontChestTexture, 16, 16);
            main.chestAnimationFront = createAnimationFromSheet(main, frontChestTiles, 0, new int[]{0, 1, 2, 3});
            if (main.chestAnimationFront != null) main.chestAnimationFront.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);

            main.rightChestTexture = new com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.internal("tools/right_chest.png"));
            com.badlogic.gdx.graphics.g2d.TextureRegion[][] rightChestTiles = com.badlogic.gdx.graphics.g2d.TextureRegion.split(main.rightChestTexture, 16, 16);
            main.chestAnimationRight = createAnimationFromSheet(main, rightChestTiles, 0, new int[]{0, 1, 2, 3});
            if (main.chestAnimationRight != null) main.chestAnimationRight.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
        } catch (Exception e) {
            main.frontChestTexture = null;
            main.rightChestTexture = null;
            main.chestAnimationFront = null;
            main.chestAnimationRight = null;
        }

        try {
            com.badlogic.gdx.graphics.Pixmap sourcePixmap = new com.badlogic.gdx.graphics.Pixmap(com.badlogic.gdx.Gdx.files.internal("tools/key.png"));
            com.badlogic.gdx.graphics.Pixmap resizedPixmap = new com.badlogic.gdx.graphics.Pixmap(16, 16, sourcePixmap.getFormat());
            resizedPixmap.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None);
            resizedPixmap.drawPixmap(
                sourcePixmap,
                0, 0, sourcePixmap.getWidth(), sourcePixmap.getHeight(),
                0, 0, 16, 16
            );
            main.keyTexture = new com.badlogic.gdx.graphics.Texture(resizedPixmap);
            main.keyRegion = new com.badlogic.gdx.graphics.g2d.TextureRegion(main.keyTexture);
            sourcePixmap.dispose();
            resizedPixmap.dispose();
        } catch (Exception e) {
            main.keyTexture = null;
            main.keyRegion = null;
        }

        try {
            main.backsoundMusic = com.badlogic.gdx.Gdx.audio.newMusic(com.badlogic.gdx.Gdx.files.internal("sound/backsound.mp3"));
            main.backsoundMusic.setLooping(true);
            main.backsoundMusic.play();
        } catch (Exception e) {
            main.backsoundMusic = null;
        }

    }
}
