/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.game;

/** Controls whether difficulty selectors expose ten numbers or five named bands. */
public enum DifficultyDisplayMode {
    NUMBERED,
    NAMED;

    public static DifficultyDisplayMode parse(String value) {
        try {
            return valueOf(value);
        } catch(IllegalArgumentException | NullPointerException ignored) {
            return NUMBERED;
        }
    }
}
