/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

import org.junit.Test;
import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.game.GameBoard;
import org.secuso.privacyfriendlysudoku.game.GameType;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HumanHintEngineTest {

    @Test
    public void topologyIsGenericForEverySupportedSize() {
        assertTopology(6, 2, 3);
        assertTopology(9, 3, 3);
        assertTopology(12, 3, 4);
        assertTopology(16, 4, 4);
    }

    @Test
    public void rulesUseTheSameCandidateModelAtEverySupportedSize() {
        assertNakedPairOn(new BoardTopology(6, 2, 3));
        assertNakedPairOn(new BoardTopology(9, 3, 3));
        assertNakedPairOn(new BoardTopology(12, 3, 4));
        assertNakedPairOn(new BoardTopology(16, 4, 4));
    }

    @Test
    public void normalizationRepairsImpossibleAndMissingSolutionCandidates() {
        GameBoard board = new GameBoard(GameType.Default_6x6);
        int[] clues = new int[36];
        clues[0] = 1;
        board.initCells(clues);
        board.getCell(0, 1).setNote(1);
        int[] solution = new int[36];
        Arrays.fill(solution, 2);

        CandidateState state = CandidateState.fromBoard(board, solution);

        assertFalse(state.hasCandidate(0, 1, 1));
        assertTrue(state.hasCandidate(0, 1, 2));
        assertEquals(2, state.getRepairedCandidateCount());
    }

    @Test
    public void findsLockedCandidatesAndSubsets() {
        assertEliminates("Pointing Candidates", state6(
                c(0, 0, 1), c(0, 1, 1), c(0, 4, 1)), 0, 4, 1);
        assertEliminates("Claiming Candidates", state6(
                c(0, 0, 1), c(0, 1, 1), c(1, 2, 1)), 1, 2, 1);

        assertEliminates("Naked Pair", state6(
                c(0, 0, 1, 2), c(0, 1, 1, 2), c(0, 2, 1, 2, 3)), 0, 2, 1);
        assertEliminates("Naked Triple", state6(
                c(0, 0, 1, 2), c(0, 1, 1, 3), c(0, 2, 2, 3),
                c(0, 3, 1, 4)), 0, 3, 1);
        assertEliminates("Naked Quad", state6(
                c(0, 0, 1, 2), c(0, 1, 1, 3), c(0, 2, 2, 4),
                c(0, 3, 3, 4), c(0, 4, 1, 5)), 0, 4, 1);

        assertEliminates("Hidden Pair", state6(
                c(0, 0, 1, 2, 3), c(0, 1, 1, 2, 4),
                c(0, 2, 3, 4), c(0, 3, 3, 4)), 0, 0, 3);
        assertEliminates("Hidden Triple", state6(
                c(0, 0, 1, 2, 4), c(0, 1, 1, 3, 5), c(0, 2, 2, 3, 6),
                c(0, 3, 4, 5, 6)), 0, 0, 4);
        assertEliminates("Hidden Quad", state6(
                c(0, 0, 1, 2, 5), c(0, 1, 1, 3, 6),
                c(0, 2, 2, 4, 5), c(0, 3, 3, 4, 6),
                c(0, 4, 5, 6)), 0, 0, 5);
    }

    @Test
    public void findsFishOnGenericBoards() {
        assertEliminates("X-Wing", state6(
                c(0, 1, 1), c(0, 4, 1), c(3, 1, 1), c(3, 4, 1),
                c(2, 1, 1)), 2, 1, 1);
        assertEliminates("Swordfish", state6(
                c(0, 0, 1), c(0, 2, 1), c(2, 2, 1), c(2, 5, 1),
                c(4, 0, 1), c(4, 5, 1), c(1, 0, 1)), 1, 0, 1);
        assertEliminates("Jellyfish", state9(
                c(0, 0, 1), c(0, 2, 1), c(2, 2, 1), c(2, 4, 1),
                c(4, 4, 1), c(4, 6, 1), c(6, 0, 1), c(6, 6, 1),
                c(8, 0, 1)), 8, 0, 1);
    }

    @Test
    public void findsWingsAndShortChains() {
        assertEliminates("Skyscraper", state6(
                c(0, 1, 1), c(0, 4, 1), c(3, 1, 1), c(3, 5, 1),
                c(1, 5, 1)), 1, 5, 1);
        assertEliminates("2-String Kite", state6(
                c(0, 0, 1), c(0, 4, 1), c(1, 1, 1), c(4, 1, 1),
                c(4, 4, 1)), 4, 4, 1);
        assertEliminates("XY-Wing", state6(
                c(1, 1, 1, 2), c(1, 4, 1, 3), c(4, 1, 2, 3),
                c(4, 4, 3)), 4, 4, 3);
        assertEliminates("XYZ-Wing", state6(
                c(1, 1, 1, 2, 3), c(1, 2, 1, 3), c(0, 0, 2, 3),
                c(1, 0, 3)), 1, 0, 3);
        assertEliminates("W-Wing", state6(
                c(0, 0, 1, 2), c(1, 4, 1, 2),
                c(0, 2, 1), c(1, 2, 1), c(1, 0, 2)), 1, 0, 2);
        assertEliminates("Simple Coloring", state6(
                c(0, 0, 1), c(0, 1, 1), c(1, 1, 1)), 0, 0, 1);
        assertEliminates("X-Chain", state9(
                c(0, 0, 1), c(1, 1, 1), c(1, 4, 1), c(1, 7, 1),
                c(0, 3, 1), c(0, 7, 1)), 0, 7, 1);
        assertEliminates("XY-Chain", state6(
                c(0, 0, 1, 2), c(0, 4, 2, 3), c(3, 4, 1, 3),
                c(3, 0, 1)), 3, 0, 1);
    }

    @Test
    public void forcingFallbackRetainsTheExactContradiction() {
        CandidateState state = state6(c(0, 0, 1, 2), c(0, 1, 1));
        int[] solution = solution(6);
        solution[0] = 2;

        GameHint hint = HumanHintEngine.findForcingHint(state, solution, Symbol.Default);

        assertNotNull(hint);
        assertEquals("Forcing Chain", hint.getTitle());
        assertTrue(hint.eliminates(0, 0, 1));
        assertTrue(hint.getDetails().stream().anyMatch(message -> message.startsWith("Contradiction:")));
        assertFalse(hint.getDetails().stream().anyMatch(message -> message.contains("remaining puzzle")));
    }

    @Test
    public void findsEveryValidTargetForOneTechnique() {
        CandidateState singles = state6(c(0, 0, 1), c(0, 1, 2));
        List<GameHint.Candidate> placements = HumanHintEngine.findTechniqueTargets(
                singles, solution(6), Symbol.Default, HumanTechnique.NAKED_SINGLE);
        assertEquals(2, placements.size());
        assertTrue(placements.contains(new GameHint.Candidate(0, 0, 1)));
        assertTrue(placements.contains(new GameHint.Candidate(0, 1, 2)));

        CandidateState pointing = state6(
                c(0, 0, 1), c(0, 1, 1), c(0, 4, 1), c(0, 5, 1), c(1, 3, 1));
        List<GameHint.Candidate> eliminations = HumanHintEngine.findTechniqueTargets(
                pointing, solution(6), Symbol.Default, HumanTechnique.POINTING_CANDIDATES);
        assertEquals(2, eliminations.size());
        assertTrue(eliminations.contains(new GameHint.Candidate(0, 4, 1)));
        assertTrue(eliminations.contains(new GameHint.Candidate(0, 5, 1)));
    }

    @Test
    public void explainsTheSubmittedAlternativeInsteadOfTheFirstMatchingMove() {
        CandidateState singles = state6(c(0, 0, 1), c(0, 1, 2));
        GameHint second = HumanHintEngine.findTechnique(singles, solution(6), Symbol.Default,
                HumanTechnique.NAKED_SINGLE, new GameHint.Candidate(0, 1, 2));
        assertNotNull(second);
        assertEquals(1, second.getCol());
        assertEquals(2, second.getValue());
        assertNull(HumanHintEngine.findTechnique(singles, solution(6), Symbol.Default,
                HumanTechnique.NAKED_SINGLE, new GameHint.Candidate(0, 1, 3)));

        CandidateState pointing = state6(
                c(0, 0, 1), c(0, 1, 1), c(0, 4, 1), c(0, 5, 1), c(1, 3, 1));
        GameHint elimination = HumanHintEngine.findTechnique(pointing, solution(6), Symbol.Default,
                HumanTechnique.POINTING_CANDIDATES, new GameHint.Candidate(0, 5, 1));
        assertNotNull(elimination);
        assertTrue(elimination.eliminates(0, 5, 1));
        assertEquals(5, elimination.getCol());
        assertFalse(elimination.getFrames().isEmpty());
    }

    private void assertTopology(int size, int blockHeight, int blockWidth) {
        BoardTopology topology = new BoardTopology(size, blockHeight, blockWidth);
        assertEquals(size * 3, topology.getUnits().size());
        assertEquals(size - 1 + size - 1 + size - 1
                        - (blockWidth - 1) - (blockHeight - 1),
                topology.peers(topology.cell(0, 0)).size());
        assertEquals(size, topology.block(0).getCells().size());
    }

    private void assertNakedPairOn(BoardTopology topology) {
        CandidateState state = state(topology,
                c(0, 0, 1, 2), c(0, 1, 1, 2), c(0, 2, 1, 2, 3));
        assertEliminates("Naked Pair", state, 0, 2, 1);
    }

    private void assertEliminates(String technique, CandidateState state,
                                  int row, int col, int value) {
        GameHint hint = HumanHintEngine.findTechnique(state, solution(state.getSize()),
                Symbol.Default, technique);
        assertNotNull(technique, hint);
        assertEquals(GameHint.Action.REMOVE_CANDIDATES, hint.getAction());
        assertTrue(technique + " should eliminate the expected candidate",
                hint.eliminates(row, col, value));
        assertFalse(hint.getFrames().isEmpty());
    }

    private CandidateState state6(CellCandidates... candidates) {
        return state(new BoardTopology(6, 2, 3), candidates);
    }

    private CandidateState state9(CellCandidates... candidates) {
        return state(new BoardTopology(9, 3, 3), candidates);
    }

    private CandidateState state(BoardTopology topology, CellCandidates... candidates) {
        int[] values = new int[topology.getSize() * topology.getSize()];
        int[] masks = new int[values.length];
        for(CellCandidates entry : candidates) {
            int mask = 0;
            for(int value : entry.values) mask |= CandidateState.bit(value);
            masks[topology.index(entry.row, entry.col)] = mask;
        }
        return CandidateState.fromMasks(topology, values, masks);
    }

    private static CellCandidates c(int row, int col, int... values) {
        return new CellCandidates(row, col, values);
    }

    private int[] solution(int size) {
        int[] solution = new int[size * size];
        for(int index = 0; index < solution.length; index++) solution[index] = index % size + 1;
        return solution;
    }

    private static final class CellCandidates {
        private final int row;
        private final int col;
        private final int[] values;

        private CellCandidates(int row, int col, int[] values) {
            this.row = row;
            this.col = col;
            this.values = values;
        }
    }
}
