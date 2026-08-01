/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.game;

import androidx.annotation.StringRes;

import org.secuso.privacyfriendlysudoku.R;

/** The five user-facing bands derived from the canonical ten-level scale. */
public enum DifficultyCategory {
    Beginner(1, R.string.difficulty_beginner),
    Easy(3, R.string.difficulty_easy),
    Moderate(5, R.string.difficulty_moderate),
    Hard(7, R.string.difficulty_hard),
    Challenge(9, R.string.difficulty_challenge);

    private final int lowerLevel;
    private final int stringResId;

    DifficultyCategory(int lowerLevel, @StringRes int stringResId) {
        this.lowerLevel = lowerLevel;
        this.stringResId = stringResId;
    }

    public DifficultyLevel getLowerLevel() {
        return DifficultyLevel.of(lowerLevel);
    }

    public DifficultyLevel getUpperLevel() {
        return DifficultyLevel.of(lowerLevel + 1);
    }

    @StringRes
    public int getStringResId() {
        return stringResId;
    }

    public static DifficultyCategory fromLevel(DifficultyLevel level) {
        return values()[(level.getValue() - 1) / 2];
    }
}
