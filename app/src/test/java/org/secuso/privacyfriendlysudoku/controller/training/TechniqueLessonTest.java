/* Licensed under the GNU General Public License, version 3 or later. */
package org.secuso.privacyfriendlysudoku.controller.training;

import org.junit.Test;
import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanHintEngine;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;
import org.secuso.privacyfriendlysudoku.game.GameType;

import static org.junit.Assert.*;

/** Check instructional claims against both the named technique and an independent Sudoku search. */
public class TechniqueLessonTest {
    @Test(timeout = 30000)
    public void everyIllustratedConclusionHasAValidTechniqueProof() {
        for(HumanTechnique technique : HumanTechnique.values()) {
            TechniqueLesson lesson = TechniqueLesson.forTechnique(technique);
            int[] values = lesson.getValues(), masks = lesson.getCandidateMasks();
            int[] solution = solve(values.clone(), masks);
            assertNotNull(technique + " must describe a satisfiable candidate state", solution);
            boolean conclusion = false;
            for(TechniqueLesson.Cell cell : lesson.cells) {
                assertTrue(technique.toString(), cell.row < lesson.rows && cell.col < lesson.columns);
                if(cell.placed != 0) {
                    conclusion = true;
                    GameHint proof = HumanHintEngine.findTechnique(GameType.Default_9x9, values, masks,
                            solution, Symbol.Default, technique, new GameHint.Candidate(cell.row, cell.col, cell.placed));
                    assertNotNull(technique + " placement must follow from the named technique", proof);
                    int[] alternatives = masks.clone();
                    alternatives[cell.row * 9 + cell.col] &= ~(1 << (cell.placed - 1));
                    assertNull(technique + " placement must be forced", solve(values.clone(), alternatives));
                } else for(int digit = 1; digit <= 9; digit++) {
                    if((cell.removed & (1 << (digit - 1))) == 0) continue;
                    conclusion = true;
                    GameHint proof = HumanHintEngine.findTechnique(GameType.Default_9x9, values, masks,
                            solution, Symbol.Default, technique, new GameHint.Candidate(cell.row, cell.col, digit));
                    assertNotNull(technique + " must justify removing " + digit + " at " + cell.row + "," + cell.col, proof);
                    int[] forced = values.clone();
                    forced[cell.row * 9 + cell.col] = digit;
                    assertNull(technique + " removed candidate must be impossible", solve(forced, masks));
                }
            }
            assertTrue(technique + " needs an illustrated conclusion", conclusion);
        }
    }

    @Test
    public void everyDrawnLinkHasTheAdvertisedCandidateRelationship() {
        for(HumanTechnique technique : HumanTechnique.values()) {
            TechniqueLesson lesson = TechniqueLesson.forTechnique(technique);
            int[] masks = lesson.getCandidateMasks();
            for(GameHint.Link link : lesson.links) {
                GameHint.Candidate a = link.getFrom(), b = link.getTo();
                int bit = 1 << (a.getValue() - 1);
                assertEquals(a.getValue(), b.getValue());
                assertTrue((masks[a.getRow() * 9 + a.getCol()] & bit) != 0);
                assertTrue((masks[b.getRow() * 9 + b.getCol()] & bit) != 0);
                boolean shared = false, conjugate = false;
                for(int unit = 0; unit < 27; unit++) {
                    boolean containsA = false, containsB = false;
                    int count = 0;
                    for(int pos = 0; pos < 9; pos++) {
                        int cell = cellInUnit(unit, pos);
                        containsA |= cell == a.getRow() * 9 + a.getCol();
                        containsB |= cell == b.getRow() * 9 + b.getCol();
                        if((masks[cell] & bit) != 0) count++;
                    }
                    if(containsA && containsB) {
                        shared = true;
                        conjugate |= count == 2;
                    }
                }
                assertTrue(technique + " weak links must share a unit", shared);
                if(link.isStrong()) assertTrue(technique + " strong link must have exactly two positions", conjugate);
            }
        }
    }

    private static int cellInUnit(int unit, int pos) {
        if(unit < 9) return unit * 9 + pos;
        if(unit < 18) return pos * 9 + unit - 9;
        int box = unit - 18;
        return (box / 3 * 3 + pos / 3) * 9 + box % 3 * 3 + pos % 3;
    }

    /** Backtracking with naked/hidden-single propagation; independent of the human hint engine. */
    private static int[] solve(int[] values, int[] initialMasks) {
        int[] masks = initialMasks.clone();
        boolean changed;
        do {
            changed = false;
            for(int unit = 0; unit < 27; unit++) {
                int used = 0;
                for(int pos = 0; pos < 9; pos++) {
                    int value = values[cellInUnit(unit, pos)];
                    if(value == 0) continue;
                    int bit = 1 << (value - 1);
                    if((used & bit) != 0) return null;
                    used |= bit;
                }
                for(int pos = 0; pos < 9; pos++) {
                    int cell = cellInUnit(unit, pos);
                    if(values[cell] != 0) continue;
                    masks[cell] &= ~used;
                    if(masks[cell] == 0) return null;
                }
                for(int digit = 1; digit <= 9; digit++) {
                    int bit = 1 << (digit - 1);
                    if((used & bit) != 0) continue;
                    int target = -1, count = 0;
                    for(int pos = 0; pos < 9; pos++) {
                        int cell = cellInUnit(unit, pos);
                        if(values[cell] == 0 && (masks[cell] & bit) != 0) { target = cell; count++; }
                    }
                    if(count == 0) return null;
                    if(count == 1) { values[target] = digit; changed = true; }
                }
            }
            for(int cell = 0; cell < 81; cell++) {
                if(values[cell] == 0 && Integer.bitCount(masks[cell]) == 1) {
                    values[cell] = Integer.numberOfTrailingZeros(masks[cell]) + 1;
                    changed = true;
                }
            }
        } while(changed);
        int target = -1, count = 10;
        for(int cell = 0; cell < 81; cell++) {
            if(values[cell] == 0 && Integer.bitCount(masks[cell]) < count) {
                target = cell; count = Integer.bitCount(masks[cell]);
            }
        }
        if(target == -1) return values;
        for(int digit = 1; digit <= 9; digit++) {
            if((masks[target] & (1 << (digit - 1))) == 0) continue;
            int[] branch = values.clone();
            branch[target] = digit;
            int[] solution = solve(branch, masks);
            if(solution != null) return solution;
        }
        return null;
    }
}
