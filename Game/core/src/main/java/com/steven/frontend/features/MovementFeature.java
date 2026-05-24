package com.steven.frontend.features;
import com.steven.frontend.Main;
public final class MovementFeature {
    private MovementFeature() {}
    public static void moveBy(Main main, float dx, float dy) {
        main.x += dx;
        main.y += dy;
    }

    public static void checkMapTransition(Main main) {
        MapFeature.checkMapTransition(main);
    }

}
