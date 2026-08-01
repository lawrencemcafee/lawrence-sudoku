/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.game;

import android.content.SharedPreferences;

import java.util.List;

/** One source of truth for the game type currently selected on the main screen. */
public final class GameTypePreferences {
    public static final String PREF_CURRENT_GAME_TYPE = "lastChosenGameType";

    private static final GameType DEFAULT = GameType.Default_9x9;

    private final SharedPreferences preferences;

    public GameTypePreferences(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public GameType getCurrentGameType() {
        String saved = preferences.getString(PREF_CURRENT_GAME_TYPE, DEFAULT.name());
        if(saved != null) {
            try {
                GameType result = GameType.valueOf(saved);
                if(GameType.getValidGameTypes().contains(result)) return result;
            } catch(IllegalArgumentException ignored) {
                // Repair removed or corrupt selections to the supported default below.
            }
        }
        setCurrentGameType(DEFAULT);
        return DEFAULT;
    }

    public int getCurrentGameTypeIndex() {
        List<GameType> validTypes = GameType.getValidGameTypes();
        return validTypes.indexOf(getCurrentGameType());
    }

    public void setCurrentGameType(GameType gameType) {
        if(!GameType.getValidGameTypes().contains(gameType)) {
            throw new IllegalArgumentException("Unsupported game type: " + gameType);
        }
        preferences.edit().putString(PREF_CURRENT_GAME_TYPE, gameType.name()).apply();
    }
}
