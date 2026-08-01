/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

import org.secuso.privacyfriendlysudoku.PFSudoku;
import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.TargetedPuzzleGenerator.GeneratedPuzzle;
import org.secuso.privacyfriendlysudoku.controller.database.DatabaseHelper;
import org.secuso.privacyfriendlysudoku.controller.database.model.Level;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameType;
import org.secuso.privacyfriendlysudoku.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Serial background scheduler for exact, bounded puzzle pools. */
public final class GeneratorService extends JobIntentService {
    private static final String TAG = GeneratorService.class.getSimpleName();
    private static final Object GENERATION_LOCK = new Object();
    private static final int MAX_ATTEMPTS_PER_POOL = 4;
    private static final int MAX_POOLS_PER_RUN = 2;

    public static final String ACTION_GENERATE = TAG + ".GENERATE";
    public static final String ACTION_GENERATION_RESULT = TAG + ".GENERATION_RESULT";
    public static final String ACTION_STOP = TAG + ".STOP";
    public static final String EXTRA_GAMETYPE = TAG + ".GAMETYPE";
    public static final String EXTRA_DIFFICULTY_LEVEL = TAG + ".DIFFICULTY_LEVEL";
    public static final String EXTRA_ACCEPTABLE_LEVELS = TAG + ".ACCEPTABLE_LEVELS";
    public static final String EXTRA_REQUEST_ID = TAG + ".REQUEST_ID";
    public static final String EXTRA_GENERATION_SUCCEEDED = TAG + ".GENERATION_SUCCEEDED";
    public static final String EXTRA_AVAILABLE_LEVEL = TAG + ".AVAILABLE_LEVEL";

    static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeneratorService.class, 1000, intent);
    }

    @Override
    protected void onHandleWork(@Nullable Intent intent) {
        if(intent == null) return;
        synchronized(GENERATION_LOCK) {
            handleWorkSerially(intent);
        }
    }

    private void handleWorkSerially(Intent intent) {
        if(ACTION_STOP.equals(intent.getAction())) {
            stopForeground(true);
            return;
        }
        if(!ACTION_GENERATE.equals(intent.getAction())) return;

        DatabaseHelper database = new DatabaseHelper(this);
        try {
            List<Pair<GameType, DifficultyLevel>> work = buildWork(database, intent);
            Random random = new Random();
            for(Pair<GameType, DifficultyLevel> item : work) {
                if(Thread.currentThread().isInterrupted()) break;
                generatePool(database, item.first, item.second, random);
            }
        } catch(RuntimeException failure) {
            Log.e(TAG, "Puzzle generation request failed", failure);
        } finally {
            try {
                sendGenerationResult(database, intent);
            } catch(RuntimeException failure) {
                Log.e(TAG, "Could not deliver puzzle generation result", failure);
            } finally {
                stopForeground(true);
            }
        }
    }

    private List<Pair<GameType, DifficultyLevel>> buildWork(DatabaseHelper database,
                                                             Intent intent) {
        List<Pair<GameType, DifficultyLevel>> work = new ArrayList<>();
        String requestedType = intent.getStringExtra(EXTRA_GAMETYPE);
        int[] acceptableLevels = intent.getIntArrayExtra(EXTRA_ACCEPTABLE_LEVELS);
        if(requestedType != null && acceptableLevels != null) {
            GameType gameType = GameType.valueOf(requestedType);
            for(int value : acceptableLevels) {
                if(work.size() >= MAX_POOLS_PER_RUN) break;
                if(value >= DifficultyLevel.MIN_VALUE && value <= DifficultyLevel.MAX_VALUE) {
                    addDeficit(work, database, gameType, DifficultyLevel.of(value));
                }
            }
            // A user waiting to play should not be held up by unrelated pool work.
            if(intent.hasExtra(EXTRA_REQUEST_ID)) return work;
        }
        int requestedLevel = intent.getIntExtra(EXTRA_DIFFICULTY_LEVEL, 0);
        if(requestedType != null && requestedLevel >= DifficultyLevel.MIN_VALUE
                && requestedLevel <= DifficultyLevel.MAX_VALUE) {
            addDeficit(work, database, GameType.valueOf(requestedType),
                    DifficultyLevel.of(requestedLevel));
        }
        for(GameType type : GameType.getValidGameTypes()) {
            for(DifficultyLevel level : DifficultyLevel.all()) {
                if(work.size() >= MAX_POOLS_PER_RUN) return work;
                if(database.countLevels(level, type) < NewLevelManager.LOW_WATER_MARK) {
                    addDeficit(work, database, type, level);
                }
            }
        }
        return work;
    }

    private void sendGenerationResult(DatabaseHelper database, Intent request) {
        String requestId = request.getStringExtra(EXTRA_REQUEST_ID);
        String requestedType = request.getStringExtra(EXTRA_GAMETYPE);
        int[] acceptableLevels = request.getIntArrayExtra(EXTRA_ACCEPTABLE_LEVELS);
        if(requestId == null || requestedType == null || acceptableLevels == null) return;

        int availableLevel = 0;
        try {
            GameType type = GameType.valueOf(requestedType);
            for(int value : acceptableLevels) {
                if(value >= DifficultyLevel.MIN_VALUE && value <= DifficultyLevel.MAX_VALUE
                        && database.countLevels(DifficultyLevel.of(value), type) > 0) {
                    availableLevel = value;
                    break;
                }
            }
        } catch(RuntimeException failure) {
            Log.e(TAG, "Could not resolve a play-generation request", failure);
        }

        Intent result = new Intent(ACTION_GENERATION_RESULT);
        result.setPackage(getPackageName());
        result.putExtra(EXTRA_REQUEST_ID, requestId);
        result.putExtra(EXTRA_GAMETYPE, requestedType);
        result.putExtra(EXTRA_GENERATION_SUCCEEDED, availableLevel != 0);
        result.putExtra(EXTRA_AVAILABLE_LEVEL, availableLevel);
        sendBroadcast(result);
    }

    private void addDeficit(List<Pair<GameType, DifficultyLevel>> work,
                            DatabaseHelper database, GameType type, DifficultyLevel level) {
        Pair<GameType, DifficultyLevel> pair = new Pair<>(type, level);
        if(database.countLevels(level, type) < NewLevelManager.TARGET_READY
                && !work.contains(pair)) work.add(pair);
    }

    private void generatePool(DatabaseHelper database, GameType type,
                              DifficultyLevel target, Random random) {
        database.trimLevelPool(target, type, NewLevelManager.HARD_CAP);
        int attempts = 0;
        while(database.countLevels(target, type) < NewLevelManager.TARGET_READY
                && attempts++ < MAX_ATTEMPTS_PER_POOL
                && !Thread.currentThread().isInterrupted()) {
            showNotification(type, target);
            try {
                GeneratedPuzzle generated = TargetedPuzzleGenerator.generate(type, target, random);
                DifficultyLevel actual = generated.getRating().getLevel();
                if(database.countLevels(actual, type) < NewLevelManager.TARGET_READY) {
                    Level level = new Level();
                    level.setGameType(type);
                    level.setDifficulty(actual);
                    level.setPuzzle(generated.getPuzzle());
                    long id = database.addLevelBounded(level, NewLevelManager.HARD_CAP);
                    Log.d(TAG, "Generated " + type.name() + " level " + actual
                            + " for requested level " + target + " (row " + id + ")");
                }
            } catch(RuntimeException failure) {
                Log.e(TAG, "Could not generate " + type.name() + " level " + target, failure);
            }
        }
    }

    private void showNotification(GameType type, DifficultyLevel level) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, PFSudoku.CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.generating))
                .setSubText(getString(type.getStringResID()) + ", "
                        + getString(R.string.difficulty_level_format, level.getValue()))
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setWhen(0)
                .setSmallIcon(R.drawable.splash_icon);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(this, 50, builder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(50, builder.build());
        }
    }
}
