/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

import org.junit.Assume;
import org.junit.Test;
import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.controller.qqwing.QQWing;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingCanonicalizer;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingCorpus;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingCorpusCodec;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingPosition;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingTarget;
import org.secuso.privacyfriendlysudoku.game.GameBoard;
import org.secuso.privacyfriendlysudoku.game.GameDifficulty;
import org.secuso.privacyfriendlysudoku.game.GameType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Explicit offline corpus builder. Skipped during ordinary unit-test runs. */
public class TrainingCorpusGeneratorTest {
    private static final long SEED = 0x4c55444f4b55L;
    private static final int DEFAULT_MAX_PUZZLES = 250000;

    @Test
    public void generateTrainingCorpus() throws Exception {
        String outputPath = System.getProperty("trainingCorpusOutput", "");
        Assume.assumeTrue("Run with -PtrainingCorpusOutput=<path>", !outputPath.isEmpty());
        String inputPath = System.getProperty("trainingCorpusInput", "");
        if(!inputPath.isEmpty()) {
            write(outputPath, enrich(inputPath));
            return;
        }
        String configuredMax = System.getProperty("trainingCorpusMaxPuzzles", "");
        int maximumPuzzles = configuredMax.isEmpty()
                ? DEFAULT_MAX_PUZZLES : Integer.parseInt(configuredMax);

        Map<HumanTechnique, List<TrainingPosition>> corpus = new EnumMap<>(HumanTechnique.class);
        Map<HumanTechnique, Set<String>> fingerprints = new EnumMap<>(HumanTechnique.class);
        for(HumanTechnique technique : HumanTechnique.values()) {
            corpus.put(technique, new ArrayList<>());
            fingerprints.put(technique, new HashSet<>());
        }

        Random random = new Random(SEED);
        int generated = 0;
        while(!complete(corpus) && generated < maximumPuzzles) {
            collectPuzzle(corpus, fingerprints, random.nextInt());
            generated++;
            if(generated % 250 == 0) System.out.println(progress(generated, corpus));
        }

        for(HumanTechnique technique : HumanTechnique.values()) {
            assertEquals("Incomplete corpus for " + technique + " after " + generated
                            + " puzzles", TrainingCorpusCodec.POSITIONS_PER_TECHNIQUE,
                    corpus.get(technique).size());
        }

        write(outputPath, corpus);
    }

    private Map<HumanTechnique, List<TrainingPosition>> enrich(String inputPath)
            throws Exception {
        TrainingCorpus source;
        try(FileInputStream input = new FileInputStream(inputPath)) {
            source = TrainingCorpus.load(input);
        }
        Map<HumanTechnique, List<TrainingPosition>> result =
                new EnumMap<>(HumanTechnique.class);
        for(HumanTechnique technique : HumanTechnique.values()) {
            List<TrainingPosition> positions = new ArrayList<>();
            for(TrainingPosition old : source.get(technique)) {
                CandidateState state = CandidateState.fromSnapshot(GameType.Default_9x9,
                        old.getValues(), old.getCandidateMasks());
                GameHint hint = HumanHintEngine.findTechnique(
                        state, old.getSolution(), Symbol.Default, technique);
                assertNotNull(old.getId(), hint);
                positions.add(position(old.getId(), technique, state, old.getSolution(), hint));
            }
            result.put(technique, positions);
            System.out.println("Enriched " + technique + "=" + positions.size());
        }
        return result;
    }

    private void write(String outputPath,
                       Map<HumanTechnique, List<TrainingPosition>> corpus) throws Exception {
        File output = new File(outputPath);
        File parent = output.getParentFile();
        if(parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        try(FileOutputStream stream = new FileOutputStream(output)) {
            TrainingCorpusCodec.write(stream, corpus);
        }
        System.out.println("Wrote " + output.length() + " bytes to " + output);
    }

    private void collectPuzzle(Map<HumanTechnique, List<TrainingPosition>> corpus,
                               Map<HumanTechnique, Set<String>> fingerprints, int seed) {
        GameType type = GameType.Default_9x9;
        QQWing generator = new QQWing(type, GameDifficulty.Unspecified);
        generator.setRandom(seed);
        if(!generator.generatePuzzle()) return;
        int[] puzzle = generator.getPuzzle();
        if(!generator.setPuzzle(puzzle) || !generator.hasUniqueSolution()) return;
        generator.setPuzzle(puzzle);
        if(!generator.solve()) return;
        int[] solution = generator.getSolution();

        GameBoard board = new GameBoard(type);
        board.initCells(puzzle);
        CandidateState state = CandidateState.fromBoard(board, solution);
        int steps = 0;
        while(!state.isComplete() && steps++ < 400) {
            for(HumanTechnique technique : HumanTechnique.values()) {
                if(technique == HumanTechnique.FORCING_CHAIN
                        || corpus.get(technique).size()
                        >= TrainingCorpusCodec.POSITIONS_PER_TECHNIQUE) continue;
                GameHint targeted = HumanHintEngine.findTechnique(
                        state, solution, Symbol.Default, technique);
                if(targeted != null && targeted.getAction() != GameHint.Action.CLEAR_VALUE) {
                    add(corpus, fingerprints, technique, state, solution, targeted);
                }
            }

            GameHint next = HumanHintEngine.findNextHint(state, solution, Symbol.Default, false);
            if(next == null) {
                GameHint forcing = HumanHintEngine.findForcingHint(state, solution, Symbol.Default);
                if(forcing == null) return;
                if(corpus.get(HumanTechnique.FORCING_CHAIN).size()
                        < TrainingCorpusCodec.POSITIONS_PER_TECHNIQUE) {
                    add(corpus, fingerprints, HumanTechnique.FORCING_CHAIN,
                            state, solution, forcing);
                }
                state.apply(forcing);
            } else {
                state.apply(next);
            }
        }
    }

    private void add(Map<HumanTechnique, List<TrainingPosition>> corpus,
                     Map<HumanTechnique, Set<String>> fingerprints,
                     HumanTechnique technique, CandidateState state, int[] solution,
                     GameHint hint) {
        List<TrainingPosition> positions = corpus.get(technique);
        TrainingPosition position = position(technique, positions.size(), state, solution, hint);
        String fingerprint = TrainingCanonicalizer.fingerprint(position);
        if(fingerprints.get(technique).add(fingerprint)) positions.add(position);
    }

    private TrainingPosition position(HumanTechnique technique, int index, CandidateState state,
                                      int[] solution, GameHint hint) {
        return position(technique.name() + "_" + String.format("%03d", index + 1),
                technique, state, solution, hint);
    }

    private TrainingPosition position(String id, HumanTechnique technique, CandidateState state,
                                      int[] solution, GameHint hint) {
        List<TrainingTarget> targets = new ArrayList<>();
        int focusedMask = 0;
        for(GameHint.Candidate candidate : HumanHintEngine.findTechniqueTargets(
                state, solution, Symbol.Default, technique)) {
            targets.add(new TrainingTarget(candidate.getRow(), candidate.getCol(),
                    candidate.getValue()));
            focusedMask |= CandidateState.bit(candidate.getValue());
        }
        for(GameHint.HintFrame frame : hint.getFrames()) {
            for(GameHint.CandidateMark mark : frame.getCandidateMarks()) {
                focusedMask |= CandidateState.bit(mark.getCandidate().getValue());
            }
        }
        assertNotNull(hint.getAction());
        return new TrainingPosition(id, technique, state.copyValues(), state.copyMasks(), solution,
                hint.getAction(),
                targets, focusedMask);
    }

    private boolean complete(Map<HumanTechnique, List<TrainingPosition>> corpus) {
        for(List<TrainingPosition> positions : corpus.values()) {
            if(positions.size() < TrainingCorpusCodec.POSITIONS_PER_TECHNIQUE) return false;
        }
        return true;
    }

    private String progress(int generated,
                            Map<HumanTechnique, List<TrainingPosition>> corpus) {
        StringBuilder result = new StringBuilder("Puzzles ").append(generated).append(':');
        for(HumanTechnique technique : HumanTechnique.values()) {
            result.append(' ').append(technique.name()).append('=')
                    .append(corpus.get(technique).size());
        }
        return result.toString();
    }
}
