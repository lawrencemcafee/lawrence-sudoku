/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.secuso.privacyfriendlysudoku.controller.database.DatabaseHelper;
import org.secuso.privacyfriendlysudoku.controller.database.model.Level;
import org.secuso.privacyfriendlysudoku.game.DifficultyCategory;
import org.secuso.privacyfriendlysudoku.game.DifficultyDisplayMode;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.DifficultyPreferences;
import org.secuso.privacyfriendlysudoku.game.GameType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/** Owns the bounded generated-puzzle queues and named-category balancing. */
public final class NewLevelManager {
    public static final int LOW_WATER_MARK = 1;
    public static final int TARGET_READY = 2;
    public static final int HARD_CAP = 3;

    private static final double CHALLENGE_GENERATION_PROBABILITY = 0.25;
    private static final int CHALLENGE_ITERATIONS = 4;
    private static NewLevelManager instance;

    private final Context context;
    private final SharedPreferences settings;
    private final DatabaseHelper dbHelper;

    public static synchronized NewLevelManager getInstance(Context context,
                                                            SharedPreferences settings) {
        if(instance == null) {
            instance = new NewLevelManager(context.getApplicationContext(), settings);
        }
        return instance;
    }

    private NewLevelManager(Context context, SharedPreferences settings) {
        this.context = context;
        this.settings = settings;
        this.dbHelper = new DatabaseHelper(context);
    }

    public boolean isLevelLoadable(GameType type, DifficultyLevel level) {
        return getCountAvailableLevels(type, level) > 0;
    }

    public boolean isLevelLoadable(GameType type, DifficultyCategory category) {
        return getCountAvailableLevels(type, category) > 0;
    }

    public int getCountAvailableLevels(GameType type, DifficultyLevel level) {
        return dbHelper.countLevels(level, type);
    }

    public int getCountAvailableLevels(GameType type, DifficultyCategory category) {
        return getCountAvailableLevels(type, category.getLowerLevel())
                + getCountAvailableLevels(type, category.getUpperLevel());
    }

    /** Determine the exact pool used by a named selection without consuming it. */
    public DifficultyLevel chooseBalancedLevel(GameType type, DifficultyCategory category) {
        DifficultyLevel lower = category.getLowerLevel();
        DifficultyLevel upper = category.getUpperLevel();
        int previous = settings.getInt(lastServedKey(type, category), upper.getValue());
        DifficultyLevel preferred = previous == lower.getValue() ? upper : lower;
        DifficultyLevel sibling = preferred.equals(lower) ? upper : lower;
        if(isLevelLoadable(type, preferred)) return preferred;
        if(isLevelLoadable(type, sibling)) return sibling;
        return preferred;
    }

    public Level loadLevel(GameType type, DifficultyLevel level) {
        Level result = dbHelper.claimLevel(level, type);
        if(result == null) throw new IllegalStateException("No puzzle is ready for this difficulty.");
        settings.edit().putInt(lastServedKey(type, result.getDifficulty()),
                result.getDifficulty().getValue()).apply();
        return result;
    }

    private String lastServedKey(GameType type, DifficultyLevel level) {
        return lastServedKey(type, level.getCategory());
    }

    private String lastServedKey(GameType type, DifficultyCategory category) {
        return "last_served_" + type.name() + "_" + category.name();
    }

    public int[] loadDailySudoku() {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);
        String seed = "Sudoku/.PrivacyFriendly/." + format.format(new Date());
        return new QQWingController().generateFromSeed(seed.hashCode(),
                CHALLENGE_GENERATION_PROBABILITY, CHALLENGE_ITERATIONS);
    }

    public void checkAndRestock() {
        GameType type = GameType.Default_9x9;
        try {
            type = GameType.valueOf(settings.getString("lastChosenGameType", type.name()));
        } catch(IllegalArgumentException ignored) {
            // A removed or corrupt preference should not prevent background generation.
        }
        DifficultyPreferences preferences = new DifficultyPreferences(settings);
        if(preferences.getMode() == DifficultyDisplayMode.NAMED) {
            requestRestock(type, preferences.getCurrentCategory());
        } else {
            requestRestock(type, preferences.getCurrentLevel());
        }
    }

    public void requestRestock(GameType type, DifficultyLevel level) {
        Intent intent = new Intent(context, GeneratorService.class);
        intent.setAction(GeneratorService.ACTION_GENERATE);
        intent.putExtra(GeneratorService.EXTRA_GAMETYPE, type.name());
        intent.putExtra(GeneratorService.EXTRA_DIFFICULTY_LEVEL, level.getValue());
        GeneratorService.enqueueWork(context, intent);
    }

    public void requestRestock(GameType type, DifficultyCategory category) {
        DifficultyLevel primary = chooseBalancedLevel(type, category);
        requestRestock(type, primary);
        DifficultyLevel sibling = primary.equals(category.getLowerLevel())
                ? category.getUpperLevel() : category.getLowerLevel();
        if(getCountAvailableLevels(type, sibling) < LOW_WATER_MARK) requestRestock(type, sibling);
    }

    /** Queue an interactive request and return the token used for its completion result. */
    public String requestLevelForPlay(GameType type, DifficultyLevel level) {
        return requestLevelsForPlay(type, new DifficultyLevel[]{level});
    }

    /** Queue both exact levels in a named category, preferring the balanced next level. */
    public String requestLevelForPlay(GameType type, DifficultyCategory category) {
        DifficultyLevel primary = chooseBalancedLevel(type, category);
        DifficultyLevel sibling = primary.equals(category.getLowerLevel())
                ? category.getUpperLevel() : category.getLowerLevel();
        return requestLevelsForPlay(type, new DifficultyLevel[]{primary, sibling});
    }

    private String requestLevelsForPlay(GameType type, DifficultyLevel[] levels) {
        String requestId = UUID.randomUUID().toString();
        int[] values = new int[levels.length];
        for(int index = 0; index < levels.length; index++) {
            values[index] = levels[index].getValue();
        }
        Intent intent = new Intent(context, GeneratorService.class);
        intent.setAction(GeneratorService.ACTION_GENERATE);
        intent.putExtra(GeneratorService.EXTRA_GAMETYPE, type.name());
        intent.putExtra(GeneratorService.EXTRA_ACCEPTABLE_LEVELS, values);
        intent.putExtra(GeneratorService.EXTRA_REQUEST_ID, requestId);
        GeneratorService.enqueueWork(context, intent);
        return requestId;
    }
}
