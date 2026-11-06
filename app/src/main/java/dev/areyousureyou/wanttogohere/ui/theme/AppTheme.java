package com.auwimo.areyousure.ui.theme;

import android.graphics.Color;

import java.util.Collections;
import java.util.List;

public final class AppTheme {
    private static final AppTheme FIELD_KIT = new AppTheme(
            "field-kit", "Field Kit",
            Color.rgb(10, 11, 9),
            Color.rgb(20, 22, 19),
            Color.rgb(75, 82, 58),
            Color.rgb(238, 239, 230),
            Color.rgb(158, 163, 147),
            Color.rgb(179, 190, 126),
            Color.rgb(16, 18, 11),
            0, 0, 1, 0.04f, 0.08f);

    public final String id;
    public final String name;
    public final int background;
    public final int surface;
    public final int edge;
    public final int primaryText;
    public final int secondaryText;
    public final int accent;
    public final int accentText;
    public final int panelCornerDp;
    public final int controlCornerDp;
    public final int borderWidthDp;
    public final float profileNameLetterSpacing;
    public final float buttonLetterSpacing;

    private AppTheme(
            String id,
            String name,
            int background,
            int surface,
            int edge,
            int primaryText,
            int secondaryText,
            int accent,
            int accentText,
            int panelCornerDp,
            int controlCornerDp,
            int borderWidthDp,
            float profileNameLetterSpacing,
            float buttonLetterSpacing) {
        this.id = id;
        this.name = name;
        this.background = background;
        this.surface = surface;
        this.edge = edge;
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
        this.accent = accent;
        this.accentText = accentText;
        this.panelCornerDp = panelCornerDp;
        this.controlCornerDp = controlCornerDp;
        this.borderWidthDp = borderWidthDp;
        this.profileNameLetterSpacing = profileNameLetterSpacing;
        this.buttonLetterSpacing = buttonLetterSpacing;
    }

    public static AppTheme defaultTheme() {
        return FIELD_KIT;
    }

    public static List<AppTheme> availableThemes() {
        return Collections.singletonList(FIELD_KIT);
    }

    public static AppTheme findById(String id) {
        return FIELD_KIT.id.equals(id) ? FIELD_KIT : null;
    }
}
