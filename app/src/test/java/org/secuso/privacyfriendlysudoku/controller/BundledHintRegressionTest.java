/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller;

import org.junit.Test;
import org.secuso.privacyfriendlysudoku.controller.helper.GameInfoContainer;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.game.GameDifficulty;
import org.secuso.privacyfriendlysudoku.game.GameType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Replays the exact Hard and Challenge puzzles installed on first launch. */
public class BundledHintRegressionTest {

    private static final String[] HARD_6 = {
            "630010002000001000040020400006003040",
            "000000060130006000050603030005000041",
            "502360000040430050000030650000000000",
            "000004300050006010001003000561000000",
            "000000042000003140064030000320200000"
    };
    private static final String[] CHALLENGE_6 = {
            "004200200000003002600050300400046000",
            "003050200003502000000000640002000045",
            "004030000000003020205004400000006500",
            "001650000320100000000060000000065000",
            "500004013005004020000000042500100000"
    };
    private static final String[] HARD_9 = {
            "000800400008004093009003060000700000000400000060002900091000670200000100403006802",
            "000006040000050000600040080043000200500810000100300605209080460004500000001030000",
            "080000000040000000000071300050130700004006000003508014000000081012600405070000002",
            "030200000008000096700600050013720009006000700802009000000903082500010070000000000",
            "007500200120093700004007050000000305500800090040300018000000009000715000000400030"
    };
    private static final String[] CHALLENGE_9 = {
            "086000000000000070090000304968027030000100060200900000072006100000000296000000740",
            "450900001001400000600010004006700000500000000100024060002000030000062400040005700",
            "100000020006020091000600000030070000260040000008010000000380060700000049040100002",
            "040100800059408061700000002500000009080700000007004000000000090801009200000000685",
            "007000050300710000140000000000500406001000907000370005790030001060004000005620000"
    };
    private static final String[] HARD_12 = {
            "00600500000004000000002050004C00800A28049000050000A900C000000000B00000A00B0560000C900C708A00000B0002000000769000008000B002B0C6000017C00107400908",
            "020000B000A608070530000B9050200A03800030980010B001000B6C30900000032000CAB0000000000000067000B000000500000A000C09000081302B0100070950836000100000",
            "0A00070050B107B0060A8000005090020C0A500040001060000000A000040B00219000000107B000602C8090C000001700400A00058B00000000000620000B400090090400800000",
            "000000908A0002900080006B0000C000007005800B2C00006020300000043B090806500C090400C000300060A700040500A00400002800000CA0000900000060B000A0B0003970C6",
            "0070000000000020009B0100000A030000000C039002004800904005000B100B008070507040C20005A0000870B32000C5000810070980100000000000000A070C000A0500000090"
    };
    private static final String[] CHALLENGE_12 = {
            "2000000000000B070C00000000AC100000000020050CA00BB60002097800079001000C60400000B0070A0000A7000298005048000010004000070009A10600C00B000C00B0000020",
            "01000002000C00640A800070000A0000082020000008100B0C081090000050A0060C079009BC000A04010500097000000000000000000000904000866070B100020000000C00B009",
            "000C0B020600400900600A020800140000C000000013050000040600900300000C0500B49A0250000000B0C0002000300143C000B0060000800064000000000B10000000020A8B09",
            "08060000000100000765A0000C00400000B0605001C7000019C0A4000002A000000B0040000000000100B500000080740020100005690A000000048B706000009020000020807000",
            "B000730000000000090040000C00B00000003A07180000C00000000CB0000000400A200040B2000000A8000020006700710A06000C00000BA0200170A700603002B0043000090060"
    };

    @Test(timeout = 600000)
    public void everyBundledHardAndChallengePuzzleCanBeCompletedByHints() {
        replayAll(GameType.Default_6x6, GameDifficulty.Hard, HARD_6);
        replayAll(GameType.Default_6x6, GameDifficulty.Challenge, CHALLENGE_6);
        replayAll(GameType.Default_9x9, GameDifficulty.Hard, HARD_9);
        replayAll(GameType.Default_9x9, GameDifficulty.Challenge, CHALLENGE_9);
        replayAll(GameType.Default_12x12, GameDifficulty.Hard, HARD_12);
        replayAll(GameType.Default_12x12, GameDifficulty.Challenge, CHALLENGE_12);
    }

    private void replayAll(GameType type, GameDifficulty difficulty, String[] puzzles) {
        for(String code : puzzles) replay(type, difficulty, code);
    }

    private void replay(GameType type, GameDifficulty difficulty, String code) {
        GameController controller = new GameController(type, null, null);
        controller.loadLevel(new GameInfoContainer(99, difficulty, type,
                decode(code), null, null));
        int[] solution = controller.solve();
        int maximumMoves = type.getSize() * type.getSize() * type.getSize() * 4;

        for(int move = 0; move < maximumMoves && !controller.checkIfBoardIsFilled(); move++) {
            GameHint hint = controller.getNextHint();
            assertNotNull(type + " " + difficulty + " stopped at move " + move, hint);
            assertFalse(hint.getSummary().contains("remaining puzzle"));
            if(hint.getAction() == GameHint.Action.PLACE_VALUE) {
                assertTrue(hint.getValue() == solution[hint.getRow() * type.getSize() + hint.getCol()]);
            }
            for(GameHint.Candidate removal : hint.getEliminations()) {
                assertNotEquals(solution[removal.getRow() * type.getSize() + removal.getCol()],
                        removal.getValue());
            }
            String before = controller.getFieldAsString();
            controller.applyHint(hint);
            assertNotEquals("Hint made no progress: " + hint.getTitle(),
                    before, controller.getFieldAsString());
        }
        assertTrue(type + " " + difficulty + " was not solved", controller.isSolved());
    }

    private int[] decode(String code) {
        int[] result = new int[code.length()];
        for(int index = 0; index < code.length(); index++) {
            char symbol = code.charAt(index);
            result[index] = symbol >= 'A' ? symbol - 'A' + 10 : symbol - '0';
        }
        return result;
    }
}
