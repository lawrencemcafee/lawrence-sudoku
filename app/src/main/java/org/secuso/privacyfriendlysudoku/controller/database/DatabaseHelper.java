/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly Sudoku is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly Sudoku. If not, see <http://www.gnu.org/licenses/>.
 */
package org.secuso.privacyfriendlysudoku.controller.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.secuso.privacyfriendlysudoku.controller.database.columns.DailySudokuColumns;
import org.secuso.privacyfriendlysudoku.controller.database.columns.LevelColumns;
import org.secuso.privacyfriendlysudoku.controller.database.migration.MigrationUtil;
import org.secuso.privacyfriendlysudoku.controller.database.model.DailySudoku;
import org.secuso.privacyfriendlysudoku.controller.database.model.Level;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "Database.db";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(LevelColumns.SQL_CREATE_ENTRIES);
        db.execSQL(DailySudokuColumns.SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(!MigrationUtil.executeMigration(db, oldVersion, newVersion)) {
            throw new IllegalStateException("No safe database migration from "
                    + oldVersion + " to " + newVersion + ".");
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Database downgrades are not supported because they "
                + "could destroy saved games or daily history.");
    }

    public synchronized List<Level> getLevels(DifficultyLevel difficulty, GameType gameType) {
        if(difficulty == null || gameType == null) {
            throw new IllegalArgumentException("Arguments may not be null");
        }

        List<Level> levelList = new LinkedList<Level>();

        SQLiteDatabase database = getWritableDatabase();

        String selection = LevelColumns.DIFFICULTY_LEVEL + " = ? AND " + LevelColumns.GAMETYPE + " = ?";
        String[] selectionArgs = {String.valueOf(difficulty.getValue()), gameType.name()};

        // How you want the results sorted in the resulting Cursor
        Cursor c = database.query(
                LevelColumns.TABLE_NAME,         // The table to query
                LevelColumns.PROJECTION,                // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                LevelColumns._ID + " ASC"              // The sort order
        );

        if (c != null) {
            while(c.moveToNext()) {
                levelList.add(LevelColumns.getLevel(c));
            }
        }

        c.close();
        return levelList;
    }

    public synchronized int countLevels(DifficultyLevel difficulty, GameType gameType) {
        if(difficulty == null || gameType == null) {
            throw new IllegalArgumentException("Arguments may not be null");
        }
        String selection = LevelColumns.DIFFICULTY_LEVEL + " = ? AND "
                + LevelColumns.GAMETYPE + " = ?";
        String[] selectionArgs = {String.valueOf(difficulty.getValue()), gameType.name()};
        return (int) DatabaseUtils.queryNumEntries(getReadableDatabase(),
                LevelColumns.TABLE_NAME, selection, selectionArgs);
    }

    /** Atomically remove and return the oldest queued puzzle in an exact pool. */
    public synchronized Level claimLevel(DifficultyLevel difficulty, GameType gameType) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        Cursor cursor = null;
        try {
            String selection = LevelColumns.DIFFICULTY_LEVEL + " = ? AND "
                    + LevelColumns.GAMETYPE + " = ?";
            String[] args = {String.valueOf(difficulty.getValue()), gameType.name()};
            cursor = database.query(LevelColumns.TABLE_NAME, LevelColumns.PROJECTION,
                    selection, args, null, null, LevelColumns._ID + " ASC", "1");
            if(!cursor.moveToFirst()) return null;
            Level level = LevelColumns.getLevel(cursor);
            database.delete(LevelColumns.TABLE_NAME, LevelColumns._ID + " = ?",
                    new String[]{String.valueOf(level.getId())});
            database.setTransactionSuccessful();
            return level;
        } finally {
            if(cursor != null) cursor.close();
            database.endTransaction();
        }
    }

    /**
     * Returns a list of all the daily sudokus that have been solved and thus saved to the database
     * @return a list of all the daily sudokus that have been solved so far
     */
    public synchronized List<DailySudoku> getDailySudokus() {
        List<DailySudoku> dailySudokuList = new LinkedList<>();
        SQLiteDatabase database = getWritableDatabase();

        // order results from most to least recent
        String order = DailySudokuColumns._ID + " DESC";

        // How you want the results sorted in the resulting Cursor
        Cursor c = database.query(
                DailySudokuColumns.TABLE_NAME,         // The table to query
                DailySudokuColumns.PROJECTION,                // The columns to return
                null,                                // select all rows
                null,                            // select all rows
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                order                                    // The sort order
        );

        if (c != null) {
            while(c.moveToNext()) {
                dailySudokuList.add(DailySudokuColumns.getLevel(c));
            }
        }

        c.close();
        return dailySudokuList;

    }

    public synchronized void deleteLevel(int id) {
        SQLiteDatabase database = getWritableDatabase();

        String selection = LevelColumns._ID + " = ?";
        String[] selectionArgs = {id+""};

        database.delete(LevelColumns.TABLE_NAME, selection, selectionArgs);
    }

    public synchronized long addLevel(Level level) {
        SQLiteDatabase database = getWritableDatabase();
        return database.insertWithOnConflict(LevelColumns.TABLE_NAME, null,
                LevelColumns.getValues(level), SQLiteDatabase.CONFLICT_IGNORE);
    }

    /** Insert only while the exact pool is below its hard cap. */
    public synchronized long addLevelBounded(Level level, int hardCap) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            String selection = LevelColumns.DIFFICULTY_LEVEL + " = ? AND "
                    + LevelColumns.GAMETYPE + " = ?";
            String[] args = {String.valueOf(level.getDifficulty().getValue()),
                    level.getGameType().name()};
            long count = DatabaseUtils.queryNumEntries(database, LevelColumns.TABLE_NAME,
                    selection, args);
            if(count >= hardCap) return -1;
            long result = database.insertWithOnConflict(LevelColumns.TABLE_NAME, null,
                    LevelColumns.getValues(level), SQLiteDatabase.CONFLICT_IGNORE);
            database.setTransactionSuccessful();
            return result;
        } finally {
            database.endTransaction();
        }
    }

    public synchronized void trimLevelPool(DifficultyLevel difficulty, GameType gameType,
                                           int hardCap) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        Cursor cursor = null;
        try {
            cursor = database.query(LevelColumns.TABLE_NAME,
                    new String[]{LevelColumns._ID},
                    LevelColumns.DIFFICULTY_LEVEL + " = ? AND " + LevelColumns.GAMETYPE + " = ?",
                    new String[]{String.valueOf(difficulty.getValue()), gameType.name()},
                    null, null, LevelColumns._ID + " ASC");
            List<Integer> surplusIds = new ArrayList<>();
            int index = 0;
            while(cursor.moveToNext()) {
                if(index++ >= hardCap) surplusIds.add(cursor.getInt(0));
            }
            cursor.close();
            cursor = null;
            for(int id : surplusIds) {
                database.delete(LevelColumns.TABLE_NAME, LevelColumns._ID + " = ?",
                        new String[]{String.valueOf(id)});
            }
            database.setTransactionSuccessful();
        } finally {
            if(cursor != null) cursor.close();
            database.endTransaction();
        }
    }

    /**
     * Adds a new daily sudoku to the database
     * @param ds the daily sudoku which is to be added to the database
     * @return the row id of the newly inserted sudoku (or -1 if an error occurred)
     */
    public synchronized long addDailySudoku(DailySudoku ds) {
        SQLiteDatabase database = getWritableDatabase();
        return database.insert(DailySudokuColumns.TABLE_NAME, null, DailySudokuColumns.getValues(ds));
    }
}
