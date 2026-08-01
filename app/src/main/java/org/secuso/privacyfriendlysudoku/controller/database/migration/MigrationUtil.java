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
package org.secuso.privacyfriendlysudoku.controller.database.migration;

import android.database.sqlite.SQLiteDatabase;

import java.util.Arrays;
import java.util.List;

import static org.secuso.privacyfriendlysudoku.controller.database.columns.LevelColumns.DIFFICULTY_LEVEL;
import static org.secuso.privacyfriendlysudoku.controller.database.columns.LevelColumns.LEGACY_DIFFICULTY;

/**
 * @author Christopher Beckmann
 */
public class MigrationUtil {

    public static List<Migration> migrations = Arrays.asList(
            new Migration(1,2) {
                @Override
                public void migrate(SQLiteDatabase db) {
                    db.execSQL("CREATE TABLE ds_levels ("
                            + "_id INTEGER PRIMARY KEY,"
                            + LEGACY_DIFFICULTY + " TEXT,"
                            + "level_gametype TEXT,"
                            + "level_puzzle TEXT,"
                            + "ds_hints_used INTEGER,"
                            + "ds_time_needed TIME (0))");
                }
            },
            new Migration(2,3) {
                @Override
                public void migrate(SQLiteDatabase db) {
                    // Generated levels are only a cache. Rebuild it instead of inventing exact
                    // levels for rows that were graded by the old four-band classifier.
                    db.execSQL(org.secuso.privacyfriendlysudoku.controller.database.columns.LevelColumns.SQL_DELETE_ENTRIES);
                    db.execSQL(org.secuso.privacyfriendlysudoku.controller.database.columns.LevelColumns.SQL_CREATE_ENTRIES);

                    // Daily history is user data, so preserve it and map each legacy band to the
                    // lower exact level in the corresponding new category.
                    db.execSQL("ALTER TABLE "
                            + org.secuso.privacyfriendlysudoku.controller.database.columns.DailySudokuColumns.TABLE_NAME
                            + " ADD COLUMN " + DIFFICULTY_LEVEL + " INTEGER");
                    db.execSQL("UPDATE "
                            + org.secuso.privacyfriendlysudoku.controller.database.columns.DailySudokuColumns.TABLE_NAME
                            + " SET " + DIFFICULTY_LEVEL + " = CASE " + LEGACY_DIFFICULTY
                            + " WHEN 'Easy' THEN 3 WHEN 'Moderate' THEN 5"
                            + " WHEN 'Hard' THEN 7 WHEN 'Challenge' THEN 9 ELSE 5 END");
                }
            }
    );

    public static boolean executeMigration(SQLiteDatabase db, int from, int to) {
        int current = from;
        while(current < to) {
            Migration next = null;
            for(Migration migration : migrations) {
                if(migration.from == current && migration.to == current + 1) {
                    next = migration;
                    break;
                }
            }
            if(next == null) return false;
            next.migrate(db);
            current = next.to;
        }
        return current == to;
    }

}
