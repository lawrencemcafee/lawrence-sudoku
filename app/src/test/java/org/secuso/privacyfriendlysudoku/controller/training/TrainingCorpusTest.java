/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import org.junit.BeforeClass;
import org.junit.Test;
import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanHintEngine;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;
import org.secuso.privacyfriendlysudoku.game.GameType;

import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TrainingCorpusTest {
    private static TrainingCorpus corpus;

    @BeforeClass
    public static void loadCorpus() throws Exception {
        try(FileInputStream input = new FileInputStream("src/main/res/raw/gym_corpus_v1.bin")) {
            corpus = TrainingCorpus.load(input);
        }
    }

    @Test
    public void containsExactlyOneHundredValidatedPositionsPerTechnique() {
        for(HumanTechnique technique : HumanTechnique.values()) {
            List<TrainingPosition> positions = corpus.get(technique);
            assertEquals(TrainingCorpusCodec.POSITIONS_PER_TECHNIQUE, positions.size());
            Set<String> ids = new HashSet<>();
            Set<String> fingerprints = new HashSet<>();
            for(TrainingPosition position : positions) {
                assertTrue(ids.add(position.getId()));
                assertTrue("Equivalent duplicate in " + technique + ": " + position.getId(),
                        fingerprints.add(TrainingCanonicalizer.fingerprint(position)));
                validateSolution(position);
                validateCandidates(position);
                assertEquals(targets(position), detectedTargets(position));
            }
        }
    }

    @Test
    public void candidateSnapshotsAreCompleteAndCannotBeMutatedByTheView() {
        for(HumanTechnique technique : HumanTechnique.values()) {
            TrainingPosition position = corpus.get(technique).get(0);
            int[] masks = position.getCandidateMasks();
            for(TrainingTarget target : position.getTargets()) {
                int index = target.getRow() * TrainingPosition.SIZE + target.getCol();
                assertTrue((masks[index] & (1 << (target.getValue() - 1))) != 0);
            }
            int[] altered = position.getCandidateMasks();
            java.util.Arrays.fill(altered, 0);
            assertArrayEquals(masks, position.getCandidateMasks());
        }
    }

    @Test
    public void nakedSingleCorpusRetainsAlternativeValidAnswers() {
        boolean foundAlternatives = false;
        for(TrainingPosition position : corpus.get(HumanTechnique.NAKED_SINGLE)) {
            if(position.getTargets().size() > 1) {
                foundAlternatives = true;
                break;
            }
        }
        assertTrue("Naked Single drills should preserve alternative valid answers",
                foundAlternatives);
    }

    @Test
    public void transformationsPreserveTheDetectedAction() {
        long seed = 7;
        for(HumanTechnique technique : HumanTechnique.values()) {
            for(int index = 0; index < 5; index++) {
                TrainingPosition transformed = TrainingTransformer.prepare(
                        corpus.get(technique).get(index), seed++);
                validateSolution(transformed);
                validateCandidates(transformed);
                assertEquals(targets(transformed), detectedTargets(transformed));
            }
        }
    }

    @Test
    public void sessionUsesEveryCanonicalPositionBeforeRepeating() {
        List<TrainingPosition> positions = corpus.get(HumanTechnique.SKYSCRAPER);
        TrainingSession session = new TrainingSession(positions, 1234L);
        Set<String> firstCycle = new HashSet<>();
        for(int index = 0; index < positions.size(); index++) {
            assertTrue(firstCycle.add(session.get(index).getId()));
        }
        assertFalse(session.get(99).getId().equals(session.get(100).getId()));
    }

    private static Set<TrainingTarget> targets(TrainingPosition position) {
        return new HashSet<>(position.getTargets());
    }

    private static Set<TrainingTarget> detectedTargets(TrainingPosition position) {
        GameHint hint = HumanHintEngine.findTechnique(GameType.Default_9x9,
                position.getValues(), position.getCandidateMasks(), position.getSolution(),
                Symbol.Default, position.getTechnique());
        assertNotNull(position.getId(), hint);
        assertEquals(position.getAction(), hint.getAction());
        Set<TrainingTarget> result = new HashSet<>();
        for(GameHint.Candidate candidate : HumanHintEngine.findTechniqueTargets(
                GameType.Default_9x9, position.getValues(), position.getCandidateMasks(),
                position.getSolution(), Symbol.Default, position.getTechnique())) {
            result.add(new TrainingTarget(candidate.getRow(), candidate.getCol(),
                    candidate.getValue()));
        }
        return result;
    }

    private static void validateCandidates(TrainingPosition position) {
        int[] values = position.getValues();
        int[] masks = position.getCandidateMasks();
        int[] solution = position.getSolution();
        for(int index = 0; index < values.length; index++) {
            if(values[index] != 0) {
                assertEquals(solution[index], values[index]);
                assertEquals(0, masks[index]);
            } else {
                assertTrue("Solution missing at " + position.getId() + " cell " + index,
                        (masks[index] & (1 << (solution[index] - 1))) != 0);
            }
        }
        for(TrainingTarget target : position.getTargets()) {
            int index = target.getRow() * TrainingPosition.SIZE + target.getCol();
            assertTrue((masks[index] & (1 << (target.getValue() - 1))) != 0);
            if(position.getAction() == GameHint.Action.REMOVE_CANDIDATES) {
                assertFalse(solution[index] == target.getValue());
            }
        }
    }

    private static void validateSolution(TrainingPosition position) {
        int[] solution = position.getSolution();
        for(int index = 0; index < TrainingPosition.SIZE; index++) {
            assertHouse(solution, index, true);
            assertHouse(solution, index, false);
        }
        for(int blockRow = 0; blockRow < 3; blockRow++) {
            for(int blockCol = 0; blockCol < 3; blockCol++) {
                int mask = 0;
                for(int row = blockRow * 3; row < blockRow * 3 + 3; row++) {
                    for(int col = blockCol * 3; col < blockCol * 3 + 3; col++) {
                        mask |= 1 << (solution[row * 9 + col] - 1);
                    }
                }
                assertEquals(0x1ff, mask);
            }
        }
    }

    private static void assertHouse(int[] solution, int house, boolean row) {
        int mask = 0;
        for(int offset = 0; offset < TrainingPosition.SIZE; offset++) {
            int index = row ? house * 9 + offset : offset * 9 + house;
            mask |= 1 << (solution[index] - 1);
        }
        assertEquals(0x1ff, mask);
    }
}
