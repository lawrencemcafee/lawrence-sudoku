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

package org.secuso.privacyfriendlysudoku.controller.database.columns;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.controller.database.model.*;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameType;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LevelColumns implements BaseColumns {

    public static final String TABLE_NAME = "levels";

    /** Legacy v2 column retained only for the v2-to-v3 daily-history migration. */
    public static final String LEGACY_DIFFICULTY = "level_difficulty";
    public static final String DIFFICULTY_LEVEL = "difficulty_level";
    public static final String GAMETYPE = "level_gametype";
    public static final String PUZZLE = "level_puzzle";
    public static final String PUZZLE_HASH = "puzzle_hash";
    public static final String CREATED_AT = "created_at";

    private static final String TEXT_TYPE = " TEXT ";
    private static final String INTEGER_TYPE = " INTEGER ";
    private static final String COMMA_SEP = ",";

    public static final String[] PROJECTION = {
            _ID,
            DIFFICULTY_LEVEL,
            GAMETYPE,
            PUZZLE,
            PUZZLE_HASH,
            CREATED_AT
    };

    public static String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID         + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                    DIFFICULTY_LEVEL  + INTEGER_TYPE + " NOT NULL CHECK (" +
                    DIFFICULTY_LEVEL + " BETWEEN 1 AND 10)" + COMMA_SEP +
                    GAMETYPE  + TEXT_TYPE + " NOT NULL" + COMMA_SEP +
                    PUZZLE     + TEXT_TYPE + " NOT NULL" + COMMA_SEP +
                    PUZZLE_HASH + TEXT_TYPE + " NOT NULL" + COMMA_SEP +
                    CREATED_AT + INTEGER_TYPE + " NOT NULL" + COMMA_SEP +
                    "UNIQUE (" + GAMETYPE + COMMA_SEP + PUZZLE_HASH + ") )";
    public static String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static Level getLevel(Cursor c) {
        Level level = new Level();

        // *** ID ***
        level.setId(c.getInt(c.getColumnIndexOrThrow(LevelColumns._ID)));

        // *** GAME TYPE ***
        String gameTypeString = c.getString(c.getColumnIndexOrThrow(LevelColumns.GAMETYPE));
        GameType gameType = GameType.valueOf(gameTypeString);
        level.setGameType(gameType);

        // *** DIFFICULTY ***
        level.setDifficulty(DifficultyLevel.of(
                c.getInt(c.getColumnIndexOrThrow(LevelColumns.DIFFICULTY_LEVEL))));

        // *** PUZZLE ***
        String puzzleString = c.getString(c.getColumnIndexOrThrow(LevelColumns.PUZZLE));
        int[] puzzle = new int[gameType.getSize()*gameType.getSize()];

        if(puzzle.length != puzzleString.length()) {
            throw new IllegalArgumentException("Saved level does not have the correct size.");
        }

        for(int i = 0; i < puzzleString.length(); i++) {
            puzzle[i] = Symbol.getValue(Symbol.SaveFormat, String.valueOf(puzzleString.charAt(i)))+1;
        }
        level.setPuzzle(puzzle);

        return level;
    }

    public static ContentValues getValues(Level record) {
        ContentValues values = new ContentValues();
        if(record.getId() != -1) {
            values.put(LevelColumns._ID, record.getId());
        }
        values.put(LevelColumns.GAMETYPE, record.getGameType().name());
        values.put(LevelColumns.DIFFICULTY_LEVEL, record.getDifficulty().getValue());

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < record.getPuzzle().length; i++) {
            if (record.getPuzzle()[i] == 0) {
                sb.append(0);
            } else {
                sb.append(Symbol.getSymbol(Symbol.SaveFormat, record.getPuzzle()[i]-1));
            }
        }
        values.put(LevelColumns.PUZZLE, sb.toString());
        values.put(LevelColumns.PUZZLE_HASH, hash(record.getGameType().name() + ":" + sb));
        values.put(LevelColumns.CREATED_AT, System.currentTimeMillis());
        return values;
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(Charset.forName("UTF-8")));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for(byte part : digest) result.append(String.format("%02x", part & 0xff));
            return result.toString();
        } catch(NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Every Android runtime must provide SHA-256.", impossible);
        }
    }
}
