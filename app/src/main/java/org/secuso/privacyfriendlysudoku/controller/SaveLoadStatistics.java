/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller;

import android.content.Context;
import android.util.Log;

import org.secuso.privacyfriendlysudoku.controller.helper.HighscoreInfoContainer;
import org.secuso.privacyfriendlysudoku.game.DifficultyCategory;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameDifficulty;
import org.secuso.privacyfriendlysudoku.game.GameType;
import org.secuso.privacyfriendlysudoku.game.listener.IHintListener;
import org.secuso.privacyfriendlysudoku.game.listener.ITimerListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Versioned file statistics keyed by board type and exact difficulty. */
public class SaveLoadStatistics implements ITimerListener, IHintListener {
    private static final String FILE_EXTENSION = ".txt";
    private static final String SAVE_PREFIX = "stat";
    private static final String SAVES_DIR = "stats";

    private final Context context;
    private GameController gameController;

    public SaveLoadStatistics(Context context) {
        this.context = context;
    }

    public void setGameController(GameController controller) {
        gameController = controller;
        controller.registerTimerListener(this);
        controller.registerHintListener(this);
    }

    public HighscoreInfoContainer loadStats(GameType type, DifficultyLevel level) {
        HighscoreInfoContainer result = new HighscoreInfoContainer(type, level);
        readInto(result, statsFile(type, level));
        return result;
    }

    private void readInto(HighscoreInfoContainer result, File file) {
        if(!file.isFile()) return;
        byte[] bytes = new byte[(int) file.length()];
        try(FileInputStream input = new FileInputStream(file)) {
            input.read(bytes);
            result.setInfosFromFile(new String(bytes));
        } catch(IOException | IllegalArgumentException failure) {
            Log.e("Statistics", "Could not read " + file.getName(), failure);
        }
    }

    public HighscoreInfoContainer loadStats(GameType type, DifficultyCategory category) {
        HighscoreInfoContainer result = new HighscoreInfoContainer(type, category.getLowerLevel());
        GameDifficulty legacy = legacyDifficulty(category);
        if(legacy != null) {
            readInto(result, new File(context.getDir(SAVES_DIR, 0),
                    SAVE_PREFIX + type.name() + "_" + legacy.name() + FILE_EXTENSION));
        }
        result.merge(loadStats(type, category.getLowerLevel()));
        result.merge(loadStats(type, category.getUpperLevel()));
        return result;
    }

    public List<HighscoreInfoContainer> loadStats(GameType type) {
        List<HighscoreInfoContainer> result = new ArrayList<>();
        for(DifficultyLevel level : DifficultyLevel.all()) result.add(loadStats(type, level));
        return result;
    }

    public static void resetStats(Context context) {
        File directory = context.getDir(SAVES_DIR, 0);
        for(GameType type : GameType.getValidGameTypes()) {
            for(DifficultyLevel level : DifficultyLevel.all()) {
                new File(directory, fileName(type, level)).delete();
            }
            // A reset also removes preserved legacy four-band records.
            for(GameDifficulty legacy : GameDifficulty.getValidDifficultyList()) {
                new File(directory, SAVE_PREFIX + type.name() + "_" + legacy.name()
                        + FILE_EXTENSION).delete();
            }
        }
    }

    public void saveGameStats() {
        if(gameController == null || gameController.gameIsCustom()) return;
        HighscoreInfoContainer stats = loadStats(gameController.getGameType(),
                gameController.getDifficulty());
        stats.add(gameController);
        save(stats, gameController.getGameType(), gameController.getDifficulty());
    }

    @Override
    public void onTick(int time) {
        if(gameController == null || gameController.gameIsCustom()) return;
        HighscoreInfoContainer stats = loadStats(gameController.getGameType(),
                gameController.getDifficulty());
        stats.incTime();
        save(stats, gameController.getGameType(), gameController.getDifficulty());
    }

    @Override
    public void onHintUsed() {
        if(gameController == null || gameController.gameIsCustom()) return;
        HighscoreInfoContainer stats = loadStats(gameController.getGameType(),
                gameController.getDifficulty());
        stats.incHints();
        save(stats, gameController.getGameType(), gameController.getDifficulty());
    }

    private void save(HighscoreInfoContainer stats, GameType type, DifficultyLevel level) {
        File file = statsFile(type, level);
        try(FileOutputStream output = new FileOutputStream(file)) {
            output.write(stats.getActualStats().getBytes());
        } catch(IOException failure) {
            Log.e("Statistics", "Could not write " + file.getName(), failure);
        }
    }

    private File statsFile(GameType type, DifficultyLevel level) {
        return new File(context.getDir(SAVES_DIR, 0), fileName(type, level));
    }

    private static String fileName(GameType type, DifficultyLevel level) {
        return SAVE_PREFIX + type.name() + "_L" + level.getValue() + FILE_EXTENSION;
    }

    private static GameDifficulty legacyDifficulty(DifficultyCategory category) {
        switch(category) {
            case Easy: return GameDifficulty.Easy;
            case Moderate: return GameDifficulty.Moderate;
            case Hard: return GameDifficulty.Hard;
            case Challenge: return GameDifficulty.Challenge;
            case Beginner:
            default: return null;
        }
    }
}
