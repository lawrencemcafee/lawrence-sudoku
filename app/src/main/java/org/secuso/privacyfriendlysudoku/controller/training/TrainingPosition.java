/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable 9x9 candidate-state snapshot for one isolated technique drill. */
public final class TrainingPosition {
    public static final int SIZE = 9;
    public static final int CELL_COUNT = SIZE * SIZE;

    private final String id;
    private final HumanTechnique technique;
    private final int[] values;
    private final int[] candidateMasks;
    private final int[] solution;
    private final GameHint.Action action;
    private final List<TrainingTarget> targets;

    public TrainingPosition(String id, HumanTechnique technique, int[] values,
                            int[] candidateMasks, int[] solution, GameHint.Action action,
                            List<TrainingTarget> targets) {
        if(id == null || id.isEmpty() || technique == null || action == null) {
            throw new IllegalArgumentException("Training position identity is incomplete.");
        }
        if(values == null || values.length != CELL_COUNT || candidateMasks == null
                || candidateMasks.length != CELL_COUNT || solution == null
                || solution.length != CELL_COUNT) {
            throw new IllegalArgumentException("Training positions must contain 81 cells.");
        }
        if(action == GameHint.Action.CLEAR_VALUE || targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("Training positions require a placement or elimination.");
        }
        this.id = id;
        this.technique = technique;
        this.values = Arrays.copyOf(values, values.length);
        this.candidateMasks = Arrays.copyOf(candidateMasks, candidateMasks.length);
        this.solution = Arrays.copyOf(solution, solution.length);
        this.action = action;
        List<TrainingTarget> ordered = new ArrayList<>(targets);
        Collections.sort(ordered);
        this.targets = Collections.unmodifiableList(ordered);
    }

    public String getId() { return id; }
    public HumanTechnique getTechnique() { return technique; }
    public int[] getValues() { return Arrays.copyOf(values, values.length); }
    public int[] getCandidateMasks() { return Arrays.copyOf(candidateMasks, candidateMasks.length); }
    public int[] getSolution() { return Arrays.copyOf(solution, solution.length); }
    public GameHint.Action getAction() { return action; }
    public List<TrainingTarget> getTargets() { return targets; }
}
