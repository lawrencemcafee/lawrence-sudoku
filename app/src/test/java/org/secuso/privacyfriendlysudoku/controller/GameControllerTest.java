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
package org.secuso.privacyfriendlysudoku.controller;

import org.junit.Before;
import org.junit.Test;

import org.secuso.privacyfriendlysudoku.controller.helper.GameInfoContainer;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.game.GameDifficulty;
import org.secuso.privacyfriendlysudoku.game.GameType;

import static org.junit.Assert.*;

/**
 * Created by Chris on 10.11.2015.
 */
public class GameControllerTest {

    GameController controller;
    GameController controller2;

    @Before
    public void init() {
        controller = new GameController();
        controller.loadLevel(new GameInfoContainer(3, GameDifficulty.Easy, GameType.Default_9x9,
                new int[]{5, 0, 1, 9, 0, 0, 0, 0, 0,
                        2, 0, 0, 0, 0, 4, 9, 5, 0,
                        3, 9, 0, 7, 0, 0, 0, 2, 6,
                        0, 3, 0, 0, 0, 1, 0, 7, 2,
                        0, 0, 6, 0, 5, 7, 0, 0, 0,
                        0, 7, 2, 0, 0, 9, 0, 4, 1,
                        0, 0, 0, 0, 7, 0, 4, 0, 9,
                        6, 4, 0, 0, 0, 0, 0, 0, 0,
                        7, 0, 0, 0, 1, 0, 3, 0, 5}
                , null, null));
        controller2 = new GameController();
        controller2.loadLevel(new GameInfoContainer(2, GameDifficulty.Easy, GameType.Default_12x12,
                new int[]{0, 2, 1, 0, 0, 6, 0, 0, 0, 8, 9, 0,
                        10, 0, 12, 0, 0, 2, 1, 11, 0, 0, 0, 6,
                        6, 0, 0, 4, 0, 12, 0, 0, 0, 0, 2, 1,
                        0, 0, 0, 5, 0, 0, 0, 4, 11, 10, 0, 0,
                        0, 10, 0, 1, 0, 0, 6, 0, 0, 0, 0, 0,
                        0, 7, 0, 0, 11, 0, 0, 0, 0, 12, 8, 9,
                        2, 1, 11, 0, 0, 0, 0, 7, 0, 0, 6, 0,
                        0, 0, 0, 0, 0, 5, 0, 0, 4, 0, 10, 0,
                        0, 0, 7, 3, 9, 0, 0, 0, 1, 0, 0, 0,
                        1, 5, 0, 0, 0, 0, 4, 0, 10, 0, 0, 11,
                        9, 0, 0, 0, 1, 10, 2, 0, 0, 6, 0, 7,
                        0, 6, 10, 0, 0, 0, 8, 0, 0, 1, 12, 0}
                , null, null));

    }


    @Test
    public void setValueOfFixedCellTest() {
        assertEquals(5, controller.getValue(0, 0));
        controller.setValue(0, 0, 6);
        assertEquals(5, controller.getValue(0, 0));
    }

    @Test
    public void setValueOfFreeCellTest() {
        assertEquals(0, controller.getValue(0, 1));
        controller.setValue(0, 1, 6);
        assertEquals(6, controller.getValue(0, 1));
    }

    @Test
    public void solveTest1() {
        controller.setValue(0, 1, 8);        controller.setValue(0, 4, 2);
        controller.setValue(0, 5, 6);        controller.setValue(0, 6, 7);
        controller.setValue(0, 7, 3);        controller.setValue(0, 8, 4);
        controller.setValue(1, 1, 6);        controller.setValue(1, 2, 7);
        controller.setValue(1, 3, 1);        controller.setValue(1, 4, 3);
        controller.setValue(1, 8, 8);        controller.setValue(2, 2, 4);
        controller.setValue(2, 4, 8);        controller.setValue(2, 5, 5);
        controller.setValue(2, 6, 1);        controller.setValue(3, 0, 9);
        controller.setValue(3, 2, 5);        controller.setValue(3, 3, 8);
        controller.setValue(3, 4, 4);        controller.setValue(3, 6, 6);
        controller.setValue(4, 0, 4);        controller.setValue(4, 1, 1);
        controller.setValue(4, 3, 2);        controller.setValue(4, 6, 8);
        controller.setValue(4, 7, 9);        controller.setValue(4, 8, 3);
        controller.setValue(5, 0, 8);        controller.setValue(5, 3, 3);
        controller.setValue(5, 4, 6);        controller.setValue(5, 6, 5);
        controller.setValue(6, 0, 1);        controller.setValue(6, 1, 5);
        controller.setValue(6, 2, 3);        controller.setValue(6, 3, 6);
        controller.setValue(6, 5, 2);        controller.setValue(6, 7, 8);
        controller.setValue(7, 2, 8);        controller.setValue(7, 3, 5);
        controller.setValue(7, 4, 9);        controller.setValue(7, 5, 3);
        controller.setValue(7, 6, 2);        controller.setValue(7, 7, 1);
        controller.setValue(7, 8, 7);        controller.setValue(8, 1, 2);
        controller.setValue(8, 2, 9);        controller.setValue(8, 3, 4);
        controller.setValue(8, 5, 8);        controller.setValue(8, 7, 6);

        assertTrue(controller.isSolved());
        assertEquals(0, controller.getErrorList().size());
    }

    @Test
    public void solveTest2() {
        controller.setValue(0, 1, 8);        controller.setValue(0, 4, 2);
        controller.setValue(0, 5, 6);        controller.setValue(0, 6, 7);
        controller.setValue(0, 7, 3);        controller.setValue(0, 8, 4);
        controller.setValue(1, 1, 6);        controller.setValue(1, 2, 7);
        controller.setValue(1, 3, 1);        controller.setValue(1, 4, 3);
        controller.setValue(1, 8, 8);        controller.setValue(2, 2, 4);
        controller.setValue(2, 4, 8);        controller.setValue(2, 5, 5);
        controller.setValue(2, 6, 1);        controller.setValue(3, 0, 9);
        controller.setValue(3, 2, 5);        controller.setValue(3, 3, 8);
        controller.setValue(3, 4, 4);        controller.setValue(3, 6, 6);
        controller.setValue(4, 0, 4);        controller.setValue(4, 1, 1);
        controller.setValue(4, 3, 2);        controller.setValue(4, 6, 8);
        controller.setValue(4, 7, 9);        controller.setValue(4, 8, 3);
        controller.setValue(5, 0, 8);        controller.setValue(5, 3, 3);
        controller.setValue(5, 4, 6);        controller.setValue(5, 6, 5);
        controller.setValue(6, 0, 1);        controller.setValue(6, 1, 5);
        controller.setValue(6, 2, 3);        controller.setValue(6, 3, 1);
        controller.setValue(6, 5, 2);        controller.setValue(6, 7, 8);
        controller.setValue(7, 2, 8);        controller.setValue(7, 3, 5);
        controller.setValue(7, 4, 9);        controller.setValue(7, 5, 3);
        controller.setValue(7, 6, 2);        controller.setValue(7, 7, 1);
        controller.setValue(7, 8, 7);        controller.setValue(8, 1, 2);
        controller.setValue(8, 2, 9);        controller.setValue(8, 3, 4);
        controller.setValue(8, 5, 8);        controller.setValue(8, 7, 6);

        String result = "[List [Conflict [1 (1|3)] [1 (6|3)]], [Conflict [1 (6|0)] [1 (6|3)]], [Conflict [1 (6|3)] [1 (8|4)]]]";

        assertFalse(controller.isSolved());
        assertEquals(3, controller.getErrorList().size());
        assertEquals(result, controller.getErrorList().toString());
    }

    @Test
    public void solveTest3() {
        controller.setValue(0, 4, 2);        controller.setValue(0, 5, 6);
        controller.setValue(0, 6, 7);        controller.setValue(0, 7, 3);
        controller.setValue(0, 8, 4);        controller.setValue(1, 1, 6);
        controller.setValue(1, 2, 7);        controller.setValue(1, 3, 1);
        controller.setValue(1, 4, 3);        controller.setValue(1, 8, 8);
        controller.setValue(2, 2, 4);        controller.setValue(2, 4, 8);
        controller.setValue(2, 5, 5);        controller.setValue(2, 6, 1);
        controller.setValue(3, 0, 9);        controller.setValue(3, 2, 5);
        controller.setValue(3, 3, 8);        controller.setValue(3, 4, 4);
        controller.setValue(3, 6, 6);        controller.setValue(4, 0, 4);
        controller.setValue(4, 1, 1);        controller.setValue(4, 3, 2);
        controller.setValue(4, 6, 8);        controller.setValue(4, 7, 9);
        controller.setValue(4, 8, 3);        controller.setValue(5, 0, 8);
        controller.setValue(5, 3, 3);        controller.setValue(5, 4, 6);
        controller.setValue(5, 6, 5);        controller.setValue(6, 0, 1);
        controller.setValue(6, 1, 5);        controller.setValue(6, 2, 3);
        controller.setValue(6, 5, 2);        controller.setValue(6, 7, 8);
        controller.setValue(7, 2, 8);        controller.setValue(7, 3, 5);
        controller.setValue(7, 4, 9);        controller.setValue(7, 5, 3);
        controller.setValue(7, 6, 2);        controller.setValue(7, 7, 1);
        controller.setValue(7, 8, 7);        controller.setValue(8, 1, 2);
        controller.setValue(8, 2, 9);        controller.setValue(8, 3, 4);
        controller.setValue(8, 5, 8);        controller.setValue(8, 7, 6);

        assertFalse(controller.isSolved());
        assertEquals(0, controller.getErrorList().size());
    }

    @Test
    public void solveTest4() {
        controller.setValue(1, 2, 5);   // Produces 2 conflicts

        String result = "[List [Conflict [5 (0|0)] [5 (1|2)]], [Conflict [5 (1|2)] [5 (1|7)]]]";

        assertFalse(controller.isSolved());
        assertEquals(2, controller.getErrorList().size());
        assertEquals(result, controller.getErrorList().toString());
    }

    @Test
    public void deleteTest() {
        controller.setValue(1, 2, 5);
        assertEquals(5, controller.getValue(1, 2));
        controller.deleteValue(1, 2);
        assertEquals(0, controller.getValue(1, 2));
    }

    @Test
    public void createNoteTest() {
        controller.setNote(1, 2, 5);
        controller.setNote(1, 2, 9);

        boolean[] result = {false, false, false, false, true, false, false, false, true};

        assertArrayEquals(result, controller.getNotes(1, 2));
    }

    @Test
    public void deleteNoteTest() {
        controller.setNote(1, 2, 5);
        controller.setNote(1, 2, 9);
        controller.deleteNote(1, 2, 5);

        boolean[] result = {false, false, false, false, false, false, false, false, true};

        assertArrayEquals(result, controller.getNotes(1, 2));
    }

    @Test
    public void toggleNoteTest() {
        controller.toggleNote(1,2,5);
        controller.toggleNote(1, 2, 9);
        controller.toggleNote(1, 2, 5);
        controller.toggleNote(1, 2, 4);

        boolean[] result = {false, false, false, true, false, false, false, false, true};

        assertArrayEquals(result, controller.getNotes(1, 2));
    }

    @Test
    public void getValidCandidatesTest() {
        boolean[] expected9x9 = {false, false, false, false, false, true, false, true, false};
        boolean[] expected12x12 = {false, false, true, false, true, false,
                true, false, false, false, true, false};

        assertArrayEquals(expected9x9, controller.getValidCandidates(0, 1));
        assertArrayEquals(expected12x12, controller2.getValidCandidates(0, 0));
        assertArrayEquals(new boolean[9], controller.getValidCandidates(0, 0));
    }

    @Test
    public void fillValidCandidatesIsUndoableTest() {
        boolean[] expected = {false, false, false, false, false, true, false, true, false};
        controller.setNote(0, 1, 1);

        controller.fillValidCandidates();

        assertArrayEquals(expected, controller.getNotes(0, 1));
        assertTrue(controller.isUndoAvailable());

        controller.UnDo();
        assertArrayEquals(new boolean[9], controller.getNotes(0, 1));

        controller.ReDo();
        assertArrayEquals(expected, controller.getNotes(0, 1));
    }

    @Test
    public void explainedHintIsSolutionCorrectAndUndoableTest() {
        GameHint hint = controller.getNextHint();

        assertNotNull(hint);
        assertEquals(GameHint.Action.PLACE_VALUE, hint.getAction());
        assertFalse(hint.getSummary().isEmpty());
        assertFalse(hint.getDetails().isEmpty());
        assertEquals(controller.solve()[hint.getRow() * controller.getSize() + hint.getCol()],
                hint.getValue());

        int hintsBefore = controller.getUsedHints();
        controller.beginHint(hint);
        assertEquals(hintsBefore + 1, controller.getUsedHints());
        assertEquals(hint.getRow(), controller.getSelectedRow());
        assertEquals(hint.getCol(), controller.getSelectedCol());

        controller.applyHint(hint);
        assertEquals(hint.getValue(), controller.getValue(hint.getRow(), hint.getCol()));
        assertTrue(controller.isUndoAvailable());

        controller.UnDo();
        assertEquals(0, controller.getValue(hint.getRow(), hint.getCol()));
    }

    @Test
    public void explainedHintClearsIncorrectEntryBeforeSuggestingMoveTest() {
        controller.setValue(0, 1, 6);

        GameHint hint = controller.getNextHint();

        assertNotNull(hint);
        assertEquals("Mistake Found", hint.getTitle());
        assertEquals(GameHint.Action.CLEAR_VALUE, hint.getAction());
        assertFalse(hint.shouldApplyCandidatePreview());
        assertEquals(0, hint.getRow());
        assertEquals(1, hint.getCol());

        controller.applyHint(hint);
        assertEquals(0, controller.getValue(0, 1));
    }

    @Test
    public void hardPuzzleCanBeExplainedWithoutTrialAndEliminationTest() {
        int[] hardPuzzle = codeToPuzzle(
                "000800400008004093009003060000700000000400000060002900091000670200000100403006802");
        GameController hardController = new GameController();
        hardController.loadLevel(new GameInfoContainer(4, GameDifficulty.Hard,
                GameType.Default_9x9, hardPuzzle, null, null));

        boolean usedAdvancedTechnique = false;
        for(int move = 0; move < 2000 && !hardController.checkIfBoardIsFilled(); move++) {
            GameHint hint = hardController.getNextHint();
            assertNotNull(hint);
            assertNotEquals("Trial and Elimination", hint.getTitle());
            assertFalse(hint.getSummary().contains("only leads to a valid completion"));
            for(GameHint.Candidate elimination : hint.getEliminations()) {
                assertNotEquals(hardController.solve()[elimination.getRow() * hardController.getSize()
                        + elimination.getCol()], elimination.getValue());
            }
            if(hint.getTitle().equals("Pointing Candidates")
                    || hint.getTitle().equals("Claiming Candidates")
                    || hint.getTitle().equals("Naked Pair")
                    || hint.getTitle().equals("Hidden Pair")) {
                usedAdvancedTechnique = true;
            }
            hardController.applyHint(hint);
        }

        assertTrue(usedAdvancedTechnique);
        assertTrue(hardController.isSolved());
    }

    @Test
    public void candidateHintCancelApplyUndoAndRedoPreserveCandidateStateTest() {
        int[] hardPuzzle = codeToPuzzle(
                "000800400008004093009003060000700000000400000060002900091000670200000100403006802");
        GameController hardController = new GameController();
        hardController.loadLevel(new GameInfoContainer(5, GameDifficulty.Hard,
                GameType.Default_9x9, hardPuzzle, null, null));

        GameHint eliminationHint = null;
        for(int move = 0; move < 200 && eliminationHint == null; move++) {
            GameHint hint = hardController.getNextHint();
            assertNotNull(hint);
            if(hint.getAction() == GameHint.Action.REMOVE_CANDIDATES) {
                eliminationHint = hint;
            } else {
                hardController.applyHint(hint);
            }
        }
        assertNotNull(eliminationHint);
        GameHint.Candidate target = eliminationHint.getEliminations().get(0);
        int expected = hardController.solve()[target.getRow() * hardController.getSize()
                + target.getCol()];

        boolean[] before = hardController.getNotes(target.getRow(), target.getCol());
        hardController.beginHint(eliminationHint);
        hardController.showHintFrame(0);
        hardController.endHint();
        assertArrayEquals(before, hardController.getNotes(target.getRow(), target.getCol()));

        hardController.applyHint(eliminationHint);
        assertFalse(hardController.getNotes(target.getRow(), target.getCol())[target.getValue() - 1]);
        assertTrue(hardController.getNotes(target.getRow(), target.getCol())[expected - 1]);

        hardController.UnDo();
        assertArrayEquals(before, hardController.getNotes(target.getRow(), target.getCol()));
        hardController.ReDo();
        assertFalse(hardController.getNotes(target.getRow(), target.getCol())[target.getValue() - 1]);
        assertTrue(hardController.getNotes(target.getRow(), target.getCol())[expected - 1]);
    }

    @Test
    public void repairedCandidateNotesAreDisclosedAndAppliedAtomicallyTest() {
        controller.selectCell(0, 1);
        controller.toggleSelectedCellsNote(5);
        boolean[] original = controller.getNotes(0, 1);

        GameHint hint = controller.getNextHint();

        assertNotNull(hint);
        assertTrue(hint.getRepairedCandidateCount() >= 2);
        assertTrue(hint.getDetails().get(0).contains("reconciled"));
        controller.beginHint(hint);
        controller.endHint();
        assertArrayEquals(original, controller.getNotes(0, 1));

        controller.applyHint(hint);
        if(controller.getValue(0, 1) == 0) {
            assertFalse(controller.getNotes(0, 1)[4]);
            assertTrue(controller.getNotes(0, 1)[7]);
        }
        controller.UnDo();
        assertArrayEquals(original, controller.getNotes(0, 1));
    }

    private int[] codeToPuzzle(String code) {
        int[] puzzle = new int[code.length()];
        for(int i = 0; i < code.length(); i++) {
            puzzle[i] = code.charAt(i) - '0';
        }
        return puzzle;
    }

    @Test
    public void selectCellTest() {

        controller.selectCell(0, 1);
        assertEquals(1, controller.getSelectedCol());
        assertEquals(0, controller.getSelectedRow());
    }
}
