/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.ui.view;

import android.graphics.Color;

import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;

/** One accessible, theme-independent semantic palette shared by every hint overlay renderer. */
final class HintPalette {
    private HintPalette() { }

    static int colorFor(GameHint.Mark mark) {
        switch(mark) {
            case ELIMINATE:
            case CONTRADICTION:
                return Color.rgb(211, 47, 47);
            case ASSUMPTION:
                return Color.rgb(245, 124, 0);
            case FOCUS:
                return Color.rgb(0, 121, 107);
            case SUPPORT:
            default:
                return Color.rgb(25, 118, 210);
        }
    }
}
