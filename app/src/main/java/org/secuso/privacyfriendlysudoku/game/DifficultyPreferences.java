/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.game;

import android.content.Context;
import android.content.SharedPreferences;

import org.secuso.privacyfriendlysudoku.R;

/** One migration-aware source of truth for difficulty selection and display. */
public final class DifficultyPreferences {
    public static final String PREF_DISPLAY_MODE = "pref_difficulty_display";
    public static final String PREF_CURRENT_LEVEL = "current_difficulty_level";
    public static final String PREF_CURRENT_CATEGORY = "current_difficulty_category";
    private static final String PREF_APPLIED_MODE = "applied_difficulty_display";
    private static final String LEGACY_DIFFICULTY = "lastChosenDifficulty";

    private final SharedPreferences preferences;

    public DifficultyPreferences(SharedPreferences preferences) {
        this.preferences = preferences;
        ensureMigrated();
        applyModeChange();
    }

    public DifficultyDisplayMode getMode() {
        return DifficultyDisplayMode.parse(preferences.getString(
                PREF_DISPLAY_MODE, DifficultyDisplayMode.NUMBERED.name()));
    }

    public DifficultyLevel getCurrentLevel() {
        int value = preferences.getInt(PREF_CURRENT_LEVEL, DifficultyLevel.DEFAULT.getValue());
        try {
            return DifficultyLevel.of(value);
        } catch(IllegalArgumentException ignored) {
            setCurrentLevel(DifficultyLevel.DEFAULT);
            return DifficultyLevel.DEFAULT;
        }
    }

    public DifficultyCategory getCurrentCategory() {
        String saved = preferences.getString(PREF_CURRENT_CATEGORY, null);
        if(saved != null) {
            try {
                return DifficultyCategory.valueOf(saved);
            } catch(IllegalArgumentException ignored) {
                // Fall through to the exact level, which is always canonical.
            }
        }
        DifficultyCategory result = getCurrentLevel().getCategory();
        preferences.edit().putString(PREF_CURRENT_CATEGORY, result.name()).apply();
        return result;
    }

    public void setCurrentLevel(DifficultyLevel level) {
        preferences.edit()
                .putInt(PREF_CURRENT_LEVEL, level.getValue())
                .putString(PREF_CURRENT_CATEGORY, level.getCategory().name())
                .apply();
    }

    public void setCurrentCategory(DifficultyCategory category) {
        preferences.edit().putString(PREF_CURRENT_CATEGORY, category.name()).apply();
    }

    /** Apply a settings-screen mode change exactly once. */
    public void applyModeChange() {
        DifficultyDisplayMode desired = getMode();
        DifficultyDisplayMode applied = DifficultyDisplayMode.parse(preferences.getString(
                PREF_APPLIED_MODE, DifficultyDisplayMode.NUMBERED.name()));
        if(applied == desired) return;

        SharedPreferences.Editor editor = preferences.edit();
        if(desired == DifficultyDisplayMode.NAMED) {
            editor.putString(PREF_CURRENT_CATEGORY, getCurrentLevel().getCategory().name());
        } else {
            // The named-to-numbered rule is deliberately independent of the last puzzle served.
            editor.putInt(PREF_CURRENT_LEVEL, getCurrentCategory().getLowerLevel().getValue());
        }
        editor.putString(PREF_APPLIED_MODE, desired.name()).apply();
    }

    public int getSelectionCount() {
        return getMode() == DifficultyDisplayMode.NUMBERED
                ? DifficultyLevel.MAX_VALUE : DifficultyCategory.values().length;
    }

    public int getSelectionIndex() {
        return getMode() == DifficultyDisplayMode.NUMBERED
                ? getCurrentLevel().getValue() - 1 : getCurrentCategory().ordinal();
    }

    public void setSelectionIndex(int index) {
        if(getMode() == DifficultyDisplayMode.NUMBERED) {
            setCurrentLevel(DifficultyLevel.of(index + 1));
        } else {
            DifficultyCategory[] values = DifficultyCategory.values();
            if(index < 0 || index >= values.length) throw new IllegalArgumentException("Invalid category index.");
            setCurrentCategory(values[index]);
        }
    }

    public String format(Context context, DifficultyLevel level) {
        return getMode() == DifficultyDisplayMode.NUMBERED
                ? context.getString(R.string.difficulty_level_format, level.getValue())
                : context.getString(level.getCategory().getStringResId());
    }

    public String formatCurrentSelection(Context context) {
        return getMode() == DifficultyDisplayMode.NUMBERED
                ? format(context, getCurrentLevel())
                : context.getString(getCurrentCategory().getStringResId());
    }

    private void ensureMigrated() {
        if(preferences.contains(PREF_CURRENT_LEVEL)) return;
        String legacy = preferences.getString(LEGACY_DIFFICULTY, null);
        DifficultyLevel level;
        if("Easy".equals(legacy)) level = DifficultyLevel.of(3);
        else if("Moderate".equals(legacy)) level = DifficultyLevel.of(5);
        else if("Hard".equals(legacy)) level = DifficultyLevel.of(7);
        else if("Challenge".equals(legacy)) level = DifficultyLevel.of(9);
        else level = DifficultyLevel.DEFAULT;
        preferences.edit()
                .putInt(PREF_CURRENT_LEVEL, level.getValue())
                .putString(PREF_CURRENT_CATEGORY, level.getCategory().name())
                .putString(PREF_DISPLAY_MODE, DifficultyDisplayMode.NUMBERED.name())
                .putString(PREF_APPLIED_MODE, DifficultyDisplayMode.NUMBERED.name())
                .apply();
    }
}
