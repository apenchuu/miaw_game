package com.steven.frontend.patterns.factory;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

// Membuat komponen UI dengan pola yang seragam.
public class UIFactory {
    public Label createLabel(Skin skin, String text) {
        return new Label(text, skin);
    }

    public Label createLabel(Skin skin, String text, String style) {
        if (style == null || style.isEmpty()) {
            return new Label(text, skin);
        }
        return new Label(text, skin, style);
    }
}
