package com.steven.frontend.features;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.steven.frontend.Main;

public final class ChestFeature {
    private ChestFeature() {}

    public enum ChestKind { WORLD, WP1, WP2 }

    public static class ChestSpot {
        public ChestKind kind;
        public int x;
        public int y;
        public boolean requiresKey;
        public int animRow;
        public Texture layoutTexture;
        public boolean claimed = false;

        public ChestSpot(ChestKind kind, int x, int y, boolean requiresKey, int animRow, Texture layoutTexture) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.requiresKey = requiresKey;
            this.animRow = animRow;
            this.layoutTexture = layoutTexture;
        }
    }

    public static boolean chestAnimActive = false;
    public static boolean chestRewardOpen = false;
    public static boolean chestRewardClaimed = false;
    public static int activeChestX = -1, activeChestY = -1;
    public static ChestKind activeChestKind = null;
    public static ChestSpot activeChest = null;
    public static java.util.List<ChestSpot> chestSpots = new java.util.ArrayList<ChestSpot>();
    public static int worldChestX = -1, worldChestY = -1;
    public static int worldPusatChest1X = -1, worldPusatChest1Y = -1;
    public static int worldPusatChest2X = -1, worldPusatChest2Y = -1;
    public static boolean worldChestClaimed = false;
    public static boolean worldPusatChest1Claimed = false;
    public static boolean worldPusatChest2Claimed = false;
    public static final int CHEST_REWARD_COIN = 1000;

    public static void clearChestCell(Main main, int tx, int ty) {
        if (main.chestLayer == null) return;
        if (tx < 0 || ty < 0 || tx >= main.chestLayer.getWidth() || ty >= main.chestLayer.getHeight()) return;
        main.chestLayer.setCell(tx, ty, null);
    }

    public static void clearChestCellForKind(Main main, ChestKind kind) {
        if (kind == null) return;
        if (kind == ChestKind.WORLD) {
            clearChestCell(main, worldChestX, worldChestY);
        } else if (kind == ChestKind.WP1) {
            TiledMapTileLayer chest1 = (TiledMapTileLayer) (main.map != null ? main.map.getLayers().get("chest1") : null);
            if (chest1 != null && worldPusatChest1X >= 0 && worldPusatChest1Y >= 0 && worldPusatChest1X < chest1.getWidth() && worldPusatChest1Y < chest1.getHeight()) {
                chest1.setCell(worldPusatChest1X, worldPusatChest1Y, null);
            }
        } else if (kind == ChestKind.WP2) {
            TiledMapTileLayer chest2 = (TiledMapTileLayer) (main.map != null ? main.map.getLayers().get("chest2") : null);
            if (chest2 != null && worldPusatChest2X >= 0 && worldPusatChest2Y >= 0 && worldPusatChest2X < chest2.getWidth() && worldPusatChest2Y < chest2.getHeight()) {
                chest2.setCell(worldPusatChest2X, worldPusatChest2Y, null);
            }
        }
    }

    public static boolean isChestClaimed(ChestKind kind) {
        if (kind == ChestKind.WORLD) return worldChestClaimed;
        if (kind == ChestKind.WP1) return worldPusatChest1Claimed;
        if (kind == ChestKind.WP2) return worldPusatChest2Claimed;
        return false;
    }

    public static void setChestClaimed(Main main, ChestKind kind, boolean claimed) {
        if (kind == ChestKind.WORLD) worldChestClaimed = claimed;
        else if (kind == ChestKind.WP1) worldPusatChest1Claimed = claimed;
        else if (kind == ChestKind.WP2) worldPusatChest2Claimed = claimed;
        if (claimed) com.steven.frontend.features.CompletionFeature.checkForGameCompletion(main);
    }

    public static void rebuildChestSpotsForCurrentMap(Main main) {
        chestSpots.clear();

        worldChestX = -1;
        worldChestY = -1;
        worldPusatChest1X = -1;
        worldPusatChest1Y = -1;
        worldPusatChest2X = -1;
        worldPusatChest2Y = -1;

        if ("world/world.tmx".equals(main.currentMapPath)) {
            if (main.chestLayer != null) {
                boolean found = false;
                for (int tx = 0; tx < main.chestLayer.getWidth() && !found; tx++) {
                    for (int ty = 0; ty < main.chestLayer.getHeight() && !found; ty++) {
                        if (main.hasTile(main.chestLayer, tx, ty)) {
                            worldChestX = tx;
                            worldChestY = ty;
                            ChestSpot spot = new ChestSpot(ChestKind.WORLD, tx, ty, false, 0, main.chestRewardLayoutTexture);
                            spot.claimed = worldChestClaimed;
                            chestSpots.add(spot);
                            found = true;
                        }
                    }
                }
            }
        } else if ("world/world_pusat.tmx".equals(main.currentMapPath)) {
            TiledMapTileLayer chest1 = (TiledMapTileLayer) (main.map != null ? main.map.getLayers().get("chest1") : null);
            TiledMapTileLayer chest2 = (TiledMapTileLayer) (main.map != null ? main.map.getLayers().get("chest2") : null);
            if (chest1 != null) {
                boolean found1 = false;
                for (int tx = 0; tx < chest1.getWidth() && !found1; tx++) {
                    for (int ty = 0; ty < chest1.getHeight() && !found1; ty++) {
                        if (main.hasTile(chest1, tx, ty)) {
                            worldPusatChest1X = tx;
                            worldPusatChest1Y = ty;
                            ChestSpot spot1 = new ChestSpot(ChestKind.WP1, tx, ty, true, 0, main.chestRewardLayoutWp1Texture != null ? main.chestRewardLayoutWp1Texture : main.chestRewardLayoutTexture);
                            spot1.claimed = worldPusatChest1Claimed;
                            chestSpots.add(spot1);
                            found1 = true;
                        }
                    }
                }
            }
            if (chest2 != null) {
                boolean found2 = false;
                for (int tx = 0; tx < chest2.getWidth() && !found2; tx++) {
                    for (int ty = 0; ty < chest2.getHeight() && !found2; ty++) {
                        if (main.hasTile(chest2, tx, ty)) {
                            worldPusatChest2X = tx;
                            worldPusatChest2Y = ty;
                            ChestSpot spot2 = new ChestSpot(ChestKind.WP2, tx, ty, true, 1, main.chestRewardLayoutWp2Texture != null ? main.chestRewardLayoutWp2Texture : main.chestRewardLayoutTexture);
                            spot2.claimed = worldPusatChest2Claimed;
                            chestSpots.add(spot2);
                            found2 = true;
                        }
                    }
                }
            }
        }

        for (ChestSpot spot : chestSpots) {
            if (spot.claimed) clearChestCellForKind(main, spot.kind);
        }
    }

    public static void updateChestLayerVisibility(Main main) {
        if ("world/world_pusat.tmx".equals(main.currentMapPath)) {
            TiledMapTileLayer chest1 = (TiledMapTileLayer) (main.map != null ? main.map.getLayers().get("chest1") : null);
            TiledMapTileLayer chest2 = (TiledMapTileLayer) (main.map != null ? main.map.getLayers().get("chest2") : null);
            boolean chest1Visible = false;
            boolean chest2Visible = false;
            if (chest1 != null) {
                chest1Visible = !worldPusatChest1Claimed;
            }
            if (chest2 != null) {
                chest2Visible = !worldPusatChest2Claimed;
            }
            if (chest1 != null) chest1.setVisible(chest1Visible);
            if (chest2 != null) chest2.setVisible(chest2Visible);
        } else {
            if (main.chestLayer != null) {
                main.chestLayer.setVisible(!worldChestClaimed);
            }
        }
    }

    public static ChestSpot findNearbyChestSpot(Main main) {
        if (chestSpots.isEmpty()) return null;
        int px = (int)Math.floor(main.x);
        int py = (int)Math.floor(main.y);
        ChestSpot best = null;
        int bestDist = Integer.MAX_VALUE;
        for (ChestSpot spot : chestSpots) {
            if (spot == null || spot.claimed) continue;
            int dx = Math.abs(spot.x - px);
            int dy = Math.abs(spot.y - py);
            if (dx <= 1 && dy <= 1) {
                int dist = dx * dx + dy * dy;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = spot;
                }
            }
        }
        return best;
    }

    public static boolean isNearChest(Main main) {
        return findNearbyChestSpot(main) != null;
    }

    public static Animation<TextureRegion> getChestAnimationForSpot(Main main, ChestSpot spot) {
        if (spot == null) return main.chestAnimationFront;
        if (spot.kind == ChestKind.WP2) return main.chestAnimationRight != null ? main.chestAnimationRight : main.chestAnimationFront;
        return main.chestAnimationFront;
    }

    public static boolean startChestRewardSequence(Main main) {
        if (chestAnimActive || chestRewardOpen) return false;
        ChestSpot spot = findNearbyChestSpot(main);
        if (spot == null) return false;
        if (spot.requiresKey && main.inventory.keys <= 0) return false;

        if (spot.requiresKey) {
            main.inventory.keys--;
            main.markInventoryDirty();
        }

        activeChest = spot;
        activeChestKind = spot.kind;
        activeChestX = spot.x;
        activeChestY = spot.y;
        chestAnimActive = true;
        chestRewardOpen = false;
        main.chestAnimTime = 0f;
        chestRewardTimerReset(main);
        updateChestLayerVisibility(main);
        return true;
    }

    public static void chestRewardTimerReset(Main main) {
        main.chestPromptTimer = 0f;
        main.chestPromptTileX = -1;
        main.chestPromptTileY = -1;
        main.chestWasNear = false;
    }

    public static void updateChestRewardSequence(Main main, float dt) {
        if (chestAnimActive && (activeChest == null || getChestAnimationForSpot(main, activeChest) == null)) {
            chestAnimActive = false;
            chestRewardOpen = true;
            main.chestAnimTime = 0f;
            updateChestLayerVisibility(main);
            return;
        }
        if (chestAnimActive) {
            main.chestAnimTime += dt;
            Animation<TextureRegion> anim = (activeChest != null) ? getChestAnimationForSpot(main, activeChest) : main.chestAnimationFront;
            if (anim != null && main.chestAnimTime >= anim.getAnimationDuration()) {
                chestAnimActive = false;
                chestRewardOpen = true;
                main.chestAnimTime = anim.getAnimationDuration();
                updateChestLayerVisibility(main);
            }
        }
    }
}
