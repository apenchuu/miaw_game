package com.steven.frontend.features;

import com.steven.frontend.Main;

public final class ConsumeFeature {
    private ConsumeFeature() {}

    public static boolean consumeSelectedHarvestItem(Main main) {
        if (main.hotbarSelected < 0 || main.hotbarSelected >= main.hotbarMapping.length) return false;
        String sel = main.hotbarMapping[main.hotbarSelected];
        if ("harvest_bit".equals(sel) && main.inventory.harvestedBit > 0) {
            main.inventory.harvestedBit--;
            main.playerFatigue = 0f;
            main.consumedCount++;
            main.markProgressDirty();
            main.markInventoryDirty();
            return true;
        }
        if ("harvest_wheat".equals(sel) && main.inventory.harvestedWheat > 0) {
            main.inventory.harvestedWheat--;
            main.playerFatigue = 0f;
            main.consumedCount++;
            main.markProgressDirty();
            main.markInventoryDirty();
            return true;
        }
        return false;
    }
}
