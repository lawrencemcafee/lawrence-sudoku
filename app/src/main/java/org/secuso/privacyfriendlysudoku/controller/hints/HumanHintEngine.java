/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.game.GameBoard;
import org.secuso.privacyfriendlysudoku.game.GameCell;
import org.secuso.privacyfriendlysudoku.game.GameType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Offline, size-independent human hint engine. Each rule returns one directly applicable
 * deduction with structured visual evidence. If named techniques are exhausted, a complete
 * contradiction search retains the actual assumptions and consequences used by its proof.
 */
public final class HumanHintEngine {

    private final GameBoard board;
    private final CandidateState state;
    private final BoardTopology topology;
    private final int[] solution;
    private final Symbol symbols;
    private final int size;
    private GameHint.Candidate requiredTarget;

    private HumanHintEngine(GameBoard board, CandidateState state, int[] solution, Symbol symbols) {
        this.board = board;
        this.state = state;
        this.topology = state.getTopology();
        this.solution = solution;
        this.symbols = symbols == null ? Symbol.Default : symbols;
        this.size = topology.getSize();
    }

    public static GameHint findHint(GameBoard board, int[] solution) {
        return findHint(board, solution, Symbol.Default);
    }

    public static GameHint findHint(GameBoard board, int[] solution, Symbol symbols) {
        if(board == null || !validSolution(board, solution)) return null;
        CandidateState state = CandidateState.fromBoard(board, solution);
        HumanHintEngine engine = new HumanHintEngine(board, state, solution, symbols);
        Deduction mistake = engine.findMistake();
        if(mistake != null) return engine.toHint(mistake);
        return engine.findLogicalHint();
    }

    /** Package-visible test hook that runs one named rule against a handcrafted candidate state. */
    static GameHint findTechnique(CandidateState state, int[] solution, Symbol symbols,
                                  String technique) {
        return findTechnique(state, solution, symbols, HumanTechnique.fromTitle(technique));
    }

    /** Run one selected rule against a prevalidated candidate-state snapshot. */
    public static GameHint findTechnique(CandidateState state, int[] solution, Symbol symbols,
                                         HumanTechnique technique) {
        return findTechnique(state, solution, symbols, technique, null);
    }

    /** Explain a particular consequence, including alternatives to the first matching move. */
    public static GameHint findTechnique(CandidateState state, int[] solution, Symbol symbols,
                                         HumanTechnique technique, GameHint.Candidate target) {
        if(state == null || technique == null || solution == null
                || solution.length != state.getSize() * state.getSize()) return null;
        if(target != null && (target.getRow() < 0 || target.getRow() >= state.getSize()
                || target.getCol() < 0 || target.getCol() >= state.getSize()
                || target.getValue() < 1 || target.getValue() > state.getSize())) return null;
        HumanHintEngine engine = new HumanHintEngine(null, state, solution, symbols);
        engine.requiredTarget = target;
        Deduction deduction = engine.findDeduction(technique);
        return deduction == null ? null : engine.toHint(deduction);
    }

    /** Convenience API for immutable Gym records. */
    public static GameHint findTechnique(GameType gameType, int[] values, int[] masks,
                                         int[] solution, Symbol symbols,
                                         HumanTechnique technique) {
        return findTechnique(CandidateState.fromSnapshot(gameType, values, masks), solution,
                symbols, technique);
    }

    public static GameHint findTechnique(GameType gameType, int[] values, int[] masks,
                                         int[] solution, Symbol symbols,
                                         HumanTechnique technique, GameHint.Candidate target) {
        return findTechnique(CandidateState.fromSnapshot(gameType, values, masks), solution,
                symbols, technique, target);
    }

    /** Return every cell/value consequence that can be justified by one selected rule. */
    public static List<GameHint.Candidate> findTechniqueTargets(
            GameType gameType, int[] values, int[] masks, int[] solution, Symbol symbols,
            HumanTechnique technique) {
        return findTechniqueTargets(CandidateState.fromSnapshot(gameType, values, masks),
                solution, symbols, technique);
    }

    static List<GameHint.Candidate> findTechniqueTargets(
            CandidateState state, int[] solution, Symbol symbols, HumanTechnique technique) {
        if(state == null || technique == null || solution == null
                || solution.length != state.getSize() * state.getSize()) {
            return Collections.emptyList();
        }
        HumanHintEngine engine = new HumanHintEngine(null, state, solution, symbols);
        Set<GameHint.Candidate> targets = new LinkedHashSet<>();
        boolean placement = technique == HumanTechnique.LAST_DIGIT
                || technique == HumanTechnique.NAKED_SINGLE
                || technique == HumanTechnique.HIDDEN_SINGLE;
        for(int index = 0; index < state.getSize() * state.getSize(); index++) {
            if(state.getValue(index) != 0) continue;
            int row = index / state.getSize();
            int col = index % state.getSize();
            if(placement) {
                engine.addIfValidTarget(targets,
                        new GameHint.Candidate(row, col, solution[index]), technique);
            } else {
                for(int value : state.candidates(index)) {
                    if(value != solution[index]) {
                        engine.addIfValidTarget(targets,
                                new GameHint.Candidate(row, col, value), technique);
                    }
                }
            }
        }
        engine.requiredTarget = null;
        return new ArrayList<>(targets);
    }

    private void addIfValidTarget(Set<GameHint.Candidate> targets,
                                  GameHint.Candidate target, HumanTechnique technique) {
        requiredTarget = target;
        Deduction deduction = findDeduction(technique);
        if(deduction != null) targets.add(target);
    }

    private boolean accepts(BoardTopology.Cell cell, int value) {
        return requiredTarget == null || requiredTarget.equals(candidate(cell, value));
    }

    private boolean accepts(List<GameHint.Candidate> candidates) {
        return requiredTarget == null || candidates.contains(requiredTarget);
    }

    /** Package-visible entry point used by the complete difficulty grader. */
    static GameHint findNextHint(CandidateState state, int[] solution, Symbol symbols,
                                 boolean includeForcing) {
        HumanHintEngine engine = new HumanHintEngine(null, state, solution, symbols);
        return engine.findLogicalHint(includeForcing);
    }

    static GameHint findForcingHint(CandidateState state, int[] solution, Symbol symbols) {
        HumanHintEngine engine = new HumanHintEngine(null, state, solution, symbols);
        Deduction deduction = engine.findForcingProof();
        return deduction == null ? null : engine.toHint(deduction);
    }

    private static boolean validSolution(GameBoard board, int[] solution) {
        if(solution == null || solution.length != board.getSize() * board.getSize()) return false;
        for(int value : solution) {
            if(value <= 0 || value > board.getSize()) return false;
        }
        return true;
    }

    private GameHint findLogicalHint() {
        return findLogicalHint(true);
    }

    private GameHint findLogicalHint(boolean includeForcing) {
        if(isComplete()) return null;
        for(HumanTechnique technique : HumanTechnique.values()) {
            if(!includeForcing && technique == HumanTechnique.FORCING_CHAIN) break;
            Deduction deduction = findDeduction(technique);
            if(deduction != null) return toHint(deduction);
        }
        return null;
    }

    private Deduction findDeduction(HumanTechnique technique) {
        switch(technique) {
            case LAST_DIGIT: return findLastDigit();
            case NAKED_SINGLE: return findNakedSingle();
            case HIDDEN_SINGLE: return findHiddenSingle();
            case POINTING_CANDIDATES: return findPointing();
            case CLAIMING_CANDIDATES: return findClaiming();
            case NAKED_PAIR: return findNakedSubset(2);
            case HIDDEN_PAIR: return findHiddenSubset(2);
            case NAKED_TRIPLE: return findNakedSubset(3);
            case HIDDEN_TRIPLE: return findHiddenSubset(3);
            case NAKED_QUAD: return findNakedSubset(4);
            case HIDDEN_QUAD: return findHiddenSubset(4);
            case X_WING: return findFish(2);
            case SKYSCRAPER: return findSkyscraper();
            case TWO_STRING_KITE: return findTwoStringKite();
            case SWORDFISH: return findFish(3);
            case XY_WING: return findXYWing();
            case XYZ_WING: return findXYZWing();
            case W_WING: return findWWing();
            case SIMPLE_COLORING: return findSimpleColoring();
            case JELLYFISH: return findFish(4);
            case X_CHAIN: return findXChain();
            case XY_CHAIN: return findXYChain();
            case FORCING_CHAIN: return findForcingProof();
            default: throw new IllegalArgumentException("Unsupported technique: " + technique);
        }
    }

    private Deduction findMistake() {
        if(board == null) return null;
        for(int index = 0; index < size * size; index++) {
            GameCell cell = board.getCell(index / size, index % size);
            if(!cell.isFixed() && cell.hasValue() && cell.getValue() != solution[index]) {
                BoardTopology.Cell position = topology.cell(index);
                String current = symbol(cell.getValue());
                List<GameHint.HintFrame> frames = new ArrayList<>();
                frames.add(frame(coordinate(position) + " currently contains " + current + ".",
                        cells(position, GameHint.Mark.ASSUMPTION), candidates(), units(), links()));
                frames.add(frame("That entry disagrees with the unique solution determined by the fixed clues.",
                        cells(position, GameHint.Mark.CONTRADICTION), candidates(), units(), links()));
                frames.add(frame("Clear " + current + " from " + coordinate(position)
                                + ", then ask for the next deduction.",
                        cells(position, GameHint.Mark.ELIMINATE), candidates(), units(), links()));
                return new Deduction("Mistake Found",
                        "The " + current + " at " + coordinate(position)
                                + " cannot be part of this puzzle's solution.",
                        frames, position.getRow(), position.getCol(), cell.getValue(),
                        GameHint.Action.CLEAR_VALUE, Collections.emptyList());
            }
        }
        return null;
    }

    private Deduction findLastDigit() {
        int full = (1 << size) - 1;
        for(BoardTopology.Unit unit : topology.getUnits()) {
            BoardTopology.Cell empty = null;
            int used = 0;
            int emptyCount = 0;
            for(BoardTopology.Cell cell : unit.getCells()) {
                int value = state.getValue(cell.getIndex());
                if(value == 0) {
                    empty = cell;
                    emptyCount++;
                } else {
                    used |= CandidateState.bit(value);
                }
            }
            if(emptyCount == 1 && Integer.bitCount(full & ~used) == 1) {
                int value = Integer.numberOfTrailingZeros(full & ~used) + 1;
                if(!accepts(empty, value)) continue;
                List<GameHint.HintFrame> frames = Arrays.asList(
                        frame("Every cell but one is filled in " + unit.label() + ".",
                                cells(empty, GameHint.Mark.FOCUS), candidates(),
                                units(unit, GameHint.Mark.SUPPORT), links()),
                        frame("The missing value in this " + unit.getType().name().toLowerCase(Locale.ROOT)
                                        + " is " + symbol(value) + ".",
                                cells(empty, GameHint.Mark.FOCUS),
                                candidates(candidate(empty, value), GameHint.Mark.SUPPORT),
                                units(unit, GameHint.Mark.SUPPORT), links()),
                        placementConclusion(empty, value));
                return placement("Last Digit", "Only one cell is empty in " + unit.label()
                        + "; its missing value is " + symbol(value) + ".", frames, empty, value);
            }
        }
        return null;
    }

    private Deduction findNakedSingle() {
        for(int index = 0; index < size * size; index++) {
            if(state.getValue(index) == 0 && state.candidateCount(index) == 1) {
                BoardTopology.Cell cell = topology.cell(index);
                int value = firstValue(state.getMask(index));
                if(!accepts(cell, value)) continue;
                List<GameHint.CandidateMark> ruledOut = new ArrayList<>();
                for(int other = 1; other <= size; other++) {
                    if(other != value) ruledOut.add(mark(candidate(cell, other), GameHint.Mark.ELIMINATE));
                }
                List<GameHint.HintFrame> frames = Arrays.asList(
                        frame("Check the candidates at " + coordinate(cell) + ".",
                                cells(cell, GameHint.Mark.FOCUS),
                                candidates(candidate(cell, value), GameHint.Mark.SUPPORT), units(), links()),
                        frame("Values already fixed in its row, column, and block rule out every alternative.",
                                cells(cell, GameHint.Mark.FOCUS), ruledOut,
                                cellUnits(cell, GameHint.Mark.SUPPORT), links()),
                        placementConclusion(cell, value));
                return placement("Naked Single", "Only " + symbol(value) + " can be placed at "
                        + coordinate(cell) + ".", frames, cell, value);
            }
        }
        return null;
    }

    private Deduction findHiddenSingle() {
        for(BoardTopology.Unit unit : topology.getUnits()) {
            for(int value = 1; value <= size; value++) {
                if(unitContainsValue(unit, value)) continue;
                List<BoardTopology.Cell> positions = state.candidateCells(unit, value);
                if(positions.size() == 1) {
                    BoardTopology.Cell cell = positions.get(0);
                    if(!accepts(cell, value)) continue;
                    List<GameHint.HintFrame> frames = Arrays.asList(
                            frame("Consider every place for " + symbol(value) + " in " + unit.label() + ".",
                                    cells(cell, GameHint.Mark.FOCUS),
                                    candidates(candidate(cell, value), GameHint.Mark.SUPPORT),
                                    units(unit, GameHint.Mark.SUPPORT), links()),
                            frame("All other cells in this " + unit.getType().name().toLowerCase(Locale.ROOT)
                                            + " are blocked by an existing " + symbol(value) + ".",
                                    cells(cell, GameHint.Mark.FOCUS),
                                    candidates(candidate(cell, value), GameHint.Mark.SUPPORT),
                                    units(unit, GameHint.Mark.SUPPORT), links()),
                            placementConclusion(cell, value));
                    return placement("Hidden Single", "Within " + unit.label() + ", only "
                            + coordinate(cell) + " can contain " + symbol(value) + ".",
                            frames, cell, value);
                }
            }
        }
        return null;
    }

    private Deduction findPointing() {
        for(int blockIndex = 0; blockIndex < size; blockIndex++) {
            BoardTopology.Unit block = topology.block(blockIndex);
            for(int value = 1; value <= size; value++) {
                List<BoardTopology.Cell> positions = state.candidateCells(block, value);
                if(positions.size() < 2) continue;
                boolean sameRow = allSameRow(positions);
                boolean sameColumn = allSameColumn(positions);
                if(sameRow) {
                    List<GameHint.Candidate> removals = candidatesInUnitOutside(
                            topology.row(positions.get(0).getRow()), block, value);
                    if(!removals.isEmpty() && accepts(removals)) return lockedCandidates("Pointing Candidates", block,
                            topology.row(positions.get(0).getRow()), positions, value, removals);
                }
                if(sameColumn) {
                    List<GameHint.Candidate> removals = candidatesInUnitOutside(
                            topology.column(positions.get(0).getCol()), block, value);
                    if(!removals.isEmpty() && accepts(removals)) return lockedCandidates("Pointing Candidates", block,
                            topology.column(positions.get(0).getCol()), positions, value, removals);
                }
            }
        }
        return null;
    }

    private Deduction findClaiming() {
        for(int lineIndex = 0; lineIndex < size * 2; lineIndex++) {
            BoardTopology.Unit line = lineIndex < size ? topology.row(lineIndex)
                    : topology.column(lineIndex - size);
            for(int value = 1; value <= size; value++) {
                List<BoardTopology.Cell> positions = state.candidateCells(line, value);
                if(positions.size() < 2 || !allSameBlock(positions)) continue;
                BoardTopology.Unit block = topology.block(topology.blockIndex(
                        positions.get(0).getRow(), positions.get(0).getCol()));
                List<GameHint.Candidate> removals = candidatesInUnitOutside(block, line, value);
                if(!removals.isEmpty() && accepts(removals)) return lockedCandidates("Claiming Candidates", line,
                        block, positions, value, removals);
            }
        }
        return null;
    }

    private Deduction lockedCandidates(String title, BoardTopology.Unit source,
                                       BoardTopology.Unit target,
                                       List<BoardTopology.Cell> support, int value,
                                       List<GameHint.Candidate> removals) {
        List<GameHint.CandidateMark> supportMarks = markCandidates(support, value, GameHint.Mark.SUPPORT);
        List<GameHint.CandidateMark> removalMarks = markCandidates(removals, GameHint.Mark.ELIMINATE);
        List<GameHint.CandidateMark> all = concat(supportMarks, removalMarks);
        String verb = title.startsWith("Pointing") ? "lie" : "are confined";
        List<GameHint.HintFrame> frames = Arrays.asList(
                frame("In " + source.label() + ", every candidate " + symbol(value) + " " + verb
                                + " in " + target.label() + ".",
                        markCells(support, GameHint.Mark.SUPPORT), supportMarks,
                        units(source, GameHint.Mark.SUPPORT), links()),
                frame("One of those highlighted candidates must be " + symbol(value)
                                + ", so the rest of " + target.label() + " cannot contain it.",
                        markCells(support, GameHint.Mark.SUPPORT), all,
                        units(target, GameHint.Mark.FOCUS), links()),
                eliminationConclusion(removals));
        return elimination(title, "The " + symbol(value) + " candidates in " + source.label()
                + " are locked into " + target.label() + ".", frames, removals);
    }

    private Deduction findNakedSubset(int subsetSize) {
        for(BoardTopology.Unit unit : topology.getUnits()) {
            List<BoardTopology.Cell> eligible = new ArrayList<>();
            for(BoardTopology.Cell cell : unit.getCells()) {
                int count = state.candidateCount(cell.getIndex());
                if(state.getValue(cell.getIndex()) == 0 && count >= 2 && count <= subsetSize) {
                    eligible.add(cell);
                }
            }
            for(List<BoardTopology.Cell> subset : combinations(eligible, subsetSize)) {
                int union = 0;
                for(BoardTopology.Cell cell : subset) union |= state.getMask(cell.getIndex());
                if(Integer.bitCount(union) != subsetSize) continue;
                List<GameHint.Candidate> removals = new ArrayList<>();
                for(BoardTopology.Cell cell : unit.getCells()) {
                    if(subset.contains(cell)) continue;
                    int removable = state.getMask(cell.getIndex()) & union;
                    addCandidates(removals, cell, removable);
                }
                if(!removals.isEmpty() && accepts(removals)) {
                    String title = "Naked " + subsetName(subsetSize);
                    List<GameHint.CandidateMark> subsetMarks = markMaskCandidates(
                            subset, union, GameHint.Mark.SUPPORT);
                    List<GameHint.HintFrame> frames = Arrays.asList(
                            frame(coordinates(subset) + " contain only " + values(union) + " in "
                                            + unit.label() + ".",
                                    markCells(subset, GameHint.Mark.SUPPORT), subsetMarks,
                                    units(unit, GameHint.Mark.SUPPORT), links()),
                            frame("Those " + subsetSize + " values must occupy those " + subsetSize
                                            + " cells in some order.",
                                    markCells(subset, GameHint.Mark.SUPPORT), subsetMarks,
                                    units(unit, GameHint.Mark.SUPPORT), links()),
                            eliminationConclusion(removals));
                    return elimination(title, coordinates(subset) + " form a naked "
                            + subsetName(subsetSize).toLowerCase(Locale.ROOT) + " in " + unit.label() + ".",
                            frames, removals);
                }
            }
        }
        return null;
    }

    private Deduction findHiddenSubset(int subsetSize) {
        List<Integer> allValues = new ArrayList<>();
        for(int value = 1; value <= size; value++) allValues.add(value);
        for(BoardTopology.Unit unit : topology.getUnits()) {
            for(List<Integer> selectedValues : combinations(allValues, subsetSize)) {
                int valueMask = 0;
                Set<BoardTopology.Cell> positions = new LinkedHashSet<>();
                boolean valid = true;
                for(int value : selectedValues) {
                    List<BoardTopology.Cell> valuePositions = state.candidateCells(unit, value);
                    if(valuePositions.isEmpty() || valuePositions.size() > subsetSize) {
                        valid = false;
                        break;
                    }
                    positions.addAll(valuePositions);
                    valueMask |= CandidateState.bit(value);
                }
                if(!valid || positions.size() != subsetSize) continue;
                List<GameHint.Candidate> removals = new ArrayList<>();
                for(BoardTopology.Cell cell : positions) {
                    addCandidates(removals, cell, state.getMask(cell.getIndex()) & ~valueMask);
                }
                if(!removals.isEmpty() && accepts(removals)) {
                    List<BoardTopology.Cell> cells = new ArrayList<>(positions);
                    String title = "Hidden " + subsetName(subsetSize);
                    List<GameHint.CandidateMark> support = markMaskCandidates(cells, valueMask,
                            GameHint.Mark.SUPPORT);
                    List<GameHint.HintFrame> frames = Arrays.asList(
                            frame("In " + unit.label() + ", " + values(valueMask) + " can appear only at "
                                            + coordinates(cells) + ".",
                                    markCells(cells, GameHint.Mark.SUPPORT), support,
                                    units(unit, GameHint.Mark.SUPPORT), links()),
                            frame("Those values must fill those cells, so their other candidates are impossible.",
                                    markCells(cells, GameHint.Mark.SUPPORT),
                                    concat(support, markCandidates(removals, GameHint.Mark.ELIMINATE)),
                                    units(unit, GameHint.Mark.SUPPORT), links()),
                            eliminationConclusion(removals));
                    return elimination(title, values(valueMask) + " form a hidden "
                            + subsetName(subsetSize).toLowerCase(Locale.ROOT) + " in " + unit.label() + ".",
                            frames, removals);
                }
            }
        }
        return null;
    }

    private Deduction findFish(int fishSize) {
        Deduction rowFish = findFish(fishSize, true);
        return rowFish != null ? rowFish : findFish(fishSize, false);
    }

    private Deduction findFish(int fishSize, boolean rowsAreBases) {
        for(int value = 1; value <= size; value++) {
            List<Integer> eligibleBases = new ArrayList<>();
            for(int base = 0; base < size; base++) {
                BoardTopology.Unit unit = rowsAreBases ? topology.row(base) : topology.column(base);
                int count = state.candidateCells(unit, value).size();
                if(count >= 2 && count <= fishSize) eligibleBases.add(base);
            }
            for(List<Integer> bases : combinations(eligibleBases, fishSize)) {
                Set<Integer> covers = new LinkedHashSet<>();
                List<BoardTopology.Cell> supportCells = new ArrayList<>();
                for(int base : bases) {
                    BoardTopology.Unit unit = rowsAreBases ? topology.row(base) : topology.column(base);
                    for(BoardTopology.Cell cell : state.candidateCells(unit, value)) {
                        covers.add(rowsAreBases ? cell.getCol() : cell.getRow());
                        supportCells.add(cell);
                    }
                }
                if(covers.size() != fishSize) continue;
                List<GameHint.Candidate> removals = new ArrayList<>();
                for(int cover : covers) {
                    BoardTopology.Unit unit = rowsAreBases ? topology.column(cover) : topology.row(cover);
                    for(BoardTopology.Cell cell : state.candidateCells(unit, value)) {
                        int base = rowsAreBases ? cell.getRow() : cell.getCol();
                        if(!bases.contains(base)) removals.add(candidate(cell, value));
                    }
                }
                if(!removals.isEmpty() && accepts(removals)) {
                    String title = fishName(fishSize);
                    List<GameHint.UnitMark> unitMarks = new ArrayList<>();
                    for(int base : bases) unitMarks.add(new GameHint.UnitMark(
                            rowsAreBases ? GameHint.UnitType.ROW : GameHint.UnitType.COLUMN,
                            base, GameHint.Mark.SUPPORT));
                    List<GameHint.CandidateMark> support = markCandidates(supportCells, value,
                            GameHint.Mark.SUPPORT);
                    List<GameHint.HintFrame> frames = Arrays.asList(
                            frame("For " + symbol(value) + ", the highlighted "
                                            + (rowsAreBases ? "rows" : "columns") + " use only "
                                            + fishSize + " opposite lines.",
                                    markCells(supportCells, GameHint.Mark.SUPPORT), support,
                                    unitMarks, fishLinks(supportCells, value, rowsAreBases)),
                            frame("Each base line must place " + symbol(value) + " on one of those crossings.",
                                    markCells(supportCells, GameHint.Mark.SUPPORT),
                                    concat(support, markCandidates(removals, GameHint.Mark.ELIMINATE)),
                                    unitMarks, fishLinks(supportCells, value, rowsAreBases)),
                            eliminationConclusion(removals));
                    return elimination(title, "A " + title + " on " + symbol(value)
                            + " removes it from the other cells in the crossing lines.", frames, removals);
                }
            }
        }
        return null;
    }

    private Deduction findSkyscraper() {
        Deduction rows = findSkyscraper(true);
        return rows != null ? rows : findSkyscraper(false);
    }

    private Deduction findSkyscraper(boolean rowBased) {
        for(int value = 1; value <= size; value++) {
            for(int first = 0; first < size; first++) {
                List<BoardTopology.Cell> firstPair = state.candidateCells(
                        rowBased ? topology.row(first) : topology.column(first), value);
                if(firstPair.size() != 2) continue;
                for(int second = first + 1; second < size; second++) {
                    List<BoardTopology.Cell> secondPair = state.candidateCells(
                            rowBased ? topology.row(second) : topology.column(second), value);
                    if(secondPair.size() != 2) continue;
                    for(BoardTopology.Cell baseA : firstPair) {
                        for(BoardTopology.Cell baseB : secondPair) {
                            boolean aligned = rowBased ? baseA.getCol() == baseB.getCol()
                                    : baseA.getRow() == baseB.getRow();
                            if(!aligned) continue;
                            BoardTopology.Cell roofA = other(firstPair, baseA);
                            BoardTopology.Cell roofB = other(secondPair, baseB);
                            List<GameHint.Candidate> removals = commonPeerCandidates(
                                    Arrays.asList(roofA, roofB), value,
                                    Arrays.asList(baseA, baseB, roofA, roofB));
                            if(!removals.isEmpty() && accepts(removals)) {
                                List<BoardTopology.Cell> supportCells = Arrays.asList(
                                        baseA, roofA, baseB, roofB);
                                List<GameHint.Link> links = Arrays.asList(
                                        link(baseA, roofA, value, true),
                                        link(baseB, roofB, value, true),
                                        link(baseA, baseB, value, false));
                                List<GameHint.CandidateMark> support = markCandidates(
                                        supportCells, value, GameHint.Mark.SUPPORT);
                                List<GameHint.HintFrame> frames = Arrays.asList(
                                        frame("Each highlighted line has exactly two places for "
                                                        + symbol(value) + ".", markCells(supportCells,
                                                GameHint.Mark.SUPPORT), support, units(), links),
                                        frame("The aligned base candidates cannot both be false, so at least one roof must be "
                                                        + symbol(value) + ".", markCells(Arrays.asList(roofA, roofB),
                                                GameHint.Mark.FOCUS), concat(support,
                                                markCandidates(removals, GameHint.Mark.ELIMINATE)),
                                                units(), links),
                                        eliminationConclusion(removals));
                                return elimination("Skyscraper", "The two strong links on "
                                        + symbol(value) + " form a Skyscraper.", frames, removals);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private Deduction findTwoStringKite() {
        for(int value = 1; value <= size; value++) {
            for(int row = 0; row < size; row++) {
                List<BoardTopology.Cell> rowPair = state.candidateCells(topology.row(row), value);
                if(rowPair.size() != 2) continue;
                for(int col = 0; col < size; col++) {
                    List<BoardTopology.Cell> colPair = state.candidateCells(topology.column(col), value);
                    if(colPair.size() != 2) continue;
                    for(BoardTopology.Cell rowBase : rowPair) {
                        for(BoardTopology.Cell colBase : colPair) {
                            if(rowBase.equals(colBase) || topology.blockIndex(rowBase.getRow(), rowBase.getCol())
                                    != topology.blockIndex(colBase.getRow(), colBase.getCol())) continue;
                            BoardTopology.Cell rowRoof = other(rowPair, rowBase);
                            BoardTopology.Cell colRoof = other(colPair, colBase);
                            BoardTopology.Cell target = topology.cell(colRoof.getRow(), rowRoof.getCol());
                            if(target.equals(rowRoof) || target.equals(colRoof)
                                    || !state.hasCandidate(target.getIndex(), value)) continue;
                            List<GameHint.Candidate> removals = Collections.singletonList(candidate(target, value));
                            if(!accepts(removals)) continue;
                            List<BoardTopology.Cell> supportCells = Arrays.asList(
                                    rowRoof, rowBase, colBase, colRoof);
                            List<GameHint.Link> links = Arrays.asList(
                                    link(rowRoof, rowBase, value, true),
                                    link(rowBase, colBase, value, false),
                                    link(colBase, colRoof, value, true));
                            List<GameHint.HintFrame> frames = Arrays.asList(
                                    frame("The row and column each have a strong link on " + symbol(value)
                                                    + ", joined inside one block.",
                                            markCells(supportCells, GameHint.Mark.SUPPORT),
                                            markCandidates(supportCells, value, GameHint.Mark.SUPPORT),
                                            units(), links),
                                    frame("At least one outer endpoint must be " + symbol(value)
                                                    + ", and both see " + coordinate(target) + ".",
                                            markCells(Arrays.asList(rowRoof, colRoof), GameHint.Mark.FOCUS),
                                            concat(markCandidates(supportCells, value, GameHint.Mark.SUPPORT),
                                                    markCandidates(removals, GameHint.Mark.ELIMINATE)),
                                            units(), links), eliminationConclusion(removals));
                            return elimination("2-String Kite", "A row and column strong link form a 2-String Kite on "
                                    + symbol(value) + ".", frames, removals);
                        }
                    }
                }
            }
        }
        return null;
    }

    private Deduction findXYWing() {
        List<BoardTopology.Cell> bivalue = cellsWithCandidateCount(2);
        for(BoardTopology.Cell pivot : bivalue) {
            int pivotMask = state.getMask(pivot.getIndex());
            for(BoardTopology.Cell firstWing : bivalue) {
                if(!topology.sees(pivot, firstWing)) continue;
                int firstMask = state.getMask(firstWing.getIndex());
                if(Integer.bitCount(firstMask & pivotMask) != 1
                        || Integer.bitCount(firstMask | pivotMask) != 3) continue;
                for(BoardTopology.Cell secondWing : bivalue) {
                    if(secondWing.getIndex() <= firstWing.getIndex()
                            || !topology.sees(pivot, secondWing)) continue;
                    int secondMask = state.getMask(secondWing.getIndex());
                    if(Integer.bitCount(secondMask & pivotMask) != 1
                            || Integer.bitCount(secondMask | pivotMask) != 3) continue;
                    int zMask = firstMask & secondMask & ~pivotMask;
                    if(Integer.bitCount(zMask) != 1 || (firstMask | secondMask) != (pivotMask | zMask)) continue;
                    int value = firstValue(zMask);
                    List<GameHint.Candidate> removals = commonPeerCandidates(
                            Arrays.asList(firstWing, secondWing), value,
                            Arrays.asList(pivot, firstWing, secondWing));
                    if(!removals.isEmpty() && accepts(removals)) return wing("XY-Wing", pivot,
                            Arrays.asList(firstWing, secondWing), value, removals);
                }
            }
        }
        return null;
    }

    private Deduction findXYZWing() {
        List<BoardTopology.Cell> bivalue = cellsWithCandidateCount(2);
        for(int index = 0; index < size * size; index++) {
            if(state.candidateCount(index) != 3) continue;
            BoardTopology.Cell pivot = topology.cell(index);
            int pivotMask = state.getMask(index);
            for(BoardTopology.Cell firstWing : bivalue) {
                int firstMask = state.getMask(firstWing.getIndex());
                if(!topology.sees(pivot, firstWing) || (firstMask & ~pivotMask) != 0) continue;
                for(BoardTopology.Cell secondWing : bivalue) {
                    if(secondWing.getIndex() <= firstWing.getIndex() || !topology.sees(pivot, secondWing)) continue;
                    int secondMask = state.getMask(secondWing.getIndex());
                    int common = firstMask & secondMask;
                    if((secondMask & ~pivotMask) != 0 || (firstMask | secondMask) != pivotMask
                            || Integer.bitCount(common) != 1) continue;
                    int value = firstValue(common);
                    List<GameHint.Candidate> removals = commonPeerCandidates(
                            Arrays.asList(pivot, firstWing, secondWing), value,
                            Arrays.asList(pivot, firstWing, secondWing));
                    if(!removals.isEmpty() && accepts(removals)) return wing("XYZ-Wing", pivot,
                            Arrays.asList(firstWing, secondWing), value, removals);
                }
            }
        }
        return null;
    }

    private Deduction wing(String title, BoardTopology.Cell pivot,
                           List<BoardTopology.Cell> wings, int value,
                           List<GameHint.Candidate> removals) {
        List<BoardTopology.Cell> supportCells = new ArrayList<>();
        supportCells.add(pivot);
        supportCells.addAll(wings);
        List<GameHint.CandidateMark> supportMarks = new ArrayList<>();
        for(BoardTopology.Cell cell : supportCells) {
            addCandidateMarks(supportMarks, cell, state.getMask(cell.getIndex()), GameHint.Mark.SUPPORT);
        }
        List<GameHint.Link> links = new ArrayList<>();
        for(BoardTopology.Cell wing : wings) {
            int shared = state.getMask(pivot.getIndex()) & state.getMask(wing.getIndex());
            links.add(link(pivot, wing, firstValue(shared), false));
        }
        List<GameHint.HintFrame> frames = Arrays.asList(
                frame(coordinate(pivot) + " is the pivot; the two highlighted cells are its wings.",
                        cells(pivot, GameHint.Mark.FOCUS), supportMarks, units(), links),
                frame("Whichever pivot candidate is true, one wing must contain " + symbol(value) + ".",
                        markCells(wings, GameHint.Mark.SUPPORT),
                        concat(supportMarks, markCandidates(removals, GameHint.Mark.ELIMINATE)),
                        units(), links), eliminationConclusion(removals));
        return elimination(title, "The pivot and wings force " + symbol(value)
                + " into one of the highlighted wing cells.", frames, removals);
    }

    private Deduction findWWing() {
        List<BoardTopology.Cell> bivalue = cellsWithCandidateCount(2);
        for(int first = 0; first < bivalue.size(); first++) {
            BoardTopology.Cell wingA = bivalue.get(first);
            int pairMask = state.getMask(wingA.getIndex());
            for(int second = first + 1; second < bivalue.size(); second++) {
                BoardTopology.Cell wingB = bivalue.get(second);
                if(state.getMask(wingB.getIndex()) != pairMask) continue;
                for(int linkValue : maskValues(pairMask)) {
                    int eliminateValue = firstValue(pairMask & ~CandidateState.bit(linkValue));
                    for(BoardTopology.Unit unit : topology.getUnits()) {
                        List<BoardTopology.Cell> conjugates = state.candidateCells(unit, linkValue);
                        if(conjugates.size() != 2) continue;
                        BoardTopology.Cell linkA = conjugates.get(0);
                        BoardTopology.Cell linkB = conjugates.get(1);
                        BoardTopology.Cell wingALink;
                        BoardTopology.Cell wingBLink;
                        if(topology.sees(wingA, linkA) && topology.sees(wingB, linkB)) {
                            wingALink = linkA;
                            wingBLink = linkB;
                        } else if(topology.sees(wingA, linkB) && topology.sees(wingB, linkA)) {
                            wingALink = linkB;
                            wingBLink = linkA;
                        } else {
                            continue;
                        }
                        List<GameHint.Candidate> removals = commonPeerCandidates(
                                Arrays.asList(wingA, wingB), eliminateValue,
                                Arrays.asList(wingA, wingB));
                        if(removals.isEmpty() || !accepts(removals)) continue;
                        List<GameHint.Link> links = Arrays.asList(
                                link(wingA, wingALink, linkValue, false),
                                link(wingALink, wingBLink, linkValue, true),
                                link(wingBLink, wingB, linkValue, false));
                        List<BoardTopology.Cell> supportCells = Arrays.asList(
                                wingA, wingALink, wingBLink, wingB);
                        List<GameHint.HintFrame> frames = Arrays.asList(
                                frame(coordinate(wingA) + " and " + coordinate(wingB) + " share the pair "
                                                + values(pairMask) + ".",
                                        markCells(Arrays.asList(wingA, wingB), GameHint.Mark.SUPPORT),
                                        markMaskCandidates(Arrays.asList(wingA, wingB), pairMask,
                                                GameHint.Mark.SUPPORT), units(), links),
                                frame("The strong link on " + symbol(linkValue) + " forces one wing to be "
                                                + symbol(eliminateValue) + ".",
                                        markCells(supportCells, GameHint.Mark.SUPPORT),
                                        concat(markCandidates(supportCells, linkValue, GameHint.Mark.SUPPORT),
                                                markCandidates(removals, GameHint.Mark.ELIMINATE)),
                                        units(unit, GameHint.Mark.SUPPORT), links),
                                eliminationConclusion(removals));
                        return elimination("W-Wing", "A strong link connects two cells with the same pair "
                                + values(pairMask) + ".", frames, removals);
                    }
                }
            }
        }
        return null;
    }

    private Deduction findSimpleColoring() {
        for(int value = 1; value <= size; value++) {
            Map<Integer, Set<Integer>> graph = new LinkedHashMap<>();
            for(BoardTopology.Unit unit : topology.getUnits()) {
                List<BoardTopology.Cell> pair = state.candidateCells(unit, value);
                if(pair.size() == 2) {
                    addGraphEdge(graph, pair.get(0).getIndex(), pair.get(1).getIndex());
                    addGraphEdge(graph, pair.get(1).getIndex(), pair.get(0).getIndex());
                }
            }
            Set<Integer> visited = new HashSet<>();
            for(int start : graph.keySet()) {
                if(visited.contains(start)) continue;
                Map<Integer, Integer> colors = new LinkedHashMap<>();
                Deque<Integer> queue = new ArrayDeque<>();
                queue.add(start);
                colors.put(start, 0);
                visited.add(start);
                while(!queue.isEmpty()) {
                    int current = queue.removeFirst();
                    Set<Integer> neighbors = graph.get(current);
                    if(neighbors == null) neighbors = Collections.emptySet();
                    for(int neighbor : neighbors) {
                        if(!colors.containsKey(neighbor)) {
                            colors.put(neighbor, 1 - colors.get(current));
                            visited.add(neighbor);
                            queue.addLast(neighbor);
                        }
                    }
                }
                if(colors.size() < 3) continue;

                for(int color = 0; color <= 1; color++) {
                    List<BoardTopology.Cell> sameColor = colorCells(colors, color);
                    boolean collision = false;
                    for(int first = 0; first < sameColor.size() && !collision; first++) {
                        for(int second = first + 1; second < sameColor.size(); second++) {
                            if(topology.sees(sameColor.get(first), sameColor.get(second))) {
                                collision = true;
                                break;
                            }
                        }
                    }
                    if(collision) {
                        List<GameHint.Candidate> removals = new ArrayList<>();
                        for(BoardTopology.Cell cell : sameColor) removals.add(candidate(cell, value));
                        if(!accepts(removals)) continue;
                        return coloring(value, colors, removals,
                                "Two candidates with the same color see each other, so that color is false.");
                    }
                }

                List<BoardTopology.Cell> firstColor = colorCells(colors, 0);
                List<BoardTopology.Cell> secondColor = colorCells(colors, 1);
                List<GameHint.Candidate> removals = new ArrayList<>();
                for(int index = 0; index < size * size; index++) {
                    if(colors.containsKey(index) || !state.hasCandidate(index, value)) continue;
                    BoardTopology.Cell cell = topology.cell(index);
                    if(seesAny(cell, firstColor) && seesAny(cell, secondColor)) {
                        removals.add(candidate(cell, value));
                    }
                }
                if(!removals.isEmpty() && accepts(removals)) {
                    return coloring(value, colors, removals,
                            "Each uncolored target sees both colors, so it sees whichever color is true.");
                }
            }
        }
        return null;
    }

    private Deduction coloring(int value, Map<Integer, Integer> colors,
                               List<GameHint.Candidate> removals, String reason) {
        List<GameHint.CandidateMark> coloredMarks = new ArrayList<>();
        List<BoardTopology.Cell> coloredCells = new ArrayList<>();
        for(Map.Entry<Integer, Integer> entry : colors.entrySet()) {
            BoardTopology.Cell cell = topology.cell(entry.getKey());
            coloredCells.add(cell);
            coloredMarks.add(mark(candidate(cell, value), entry.getValue() == 0
                    ? GameHint.Mark.SUPPORT : GameHint.Mark.ASSUMPTION));
        }
        List<GameHint.Link> graphLinks = new ArrayList<>();
        for(BoardTopology.Unit unit : topology.getUnits()) {
            List<BoardTopology.Cell> pair = state.candidateCells(unit, value);
            if(pair.size() == 2 && colors.containsKey(pair.get(0).getIndex())
                    && colors.containsKey(pair.get(1).getIndex())) {
                graphLinks.add(link(pair.get(0), pair.get(1), value, true));
            }
        }
        List<GameHint.HintFrame> frames = Arrays.asList(
                frame("Strong links on " + symbol(value) + " alternate between the two colors.",
                        markCells(coloredCells, GameHint.Mark.SUPPORT), coloredMarks,
                        units(), graphLinks),
                frame(reason, markCells(coloredCells, GameHint.Mark.SUPPORT),
                        concat(coloredMarks, markCandidates(removals, GameHint.Mark.ELIMINATE)),
                        units(), graphLinks), eliminationConclusion(removals));
        return elimination("Simple Coloring", "The conjugate chain on " + symbol(value)
                + " proves the highlighted candidate" + plural(removals.size()) + " impossible.",
                frames, removals);
    }

    private Deduction findXChain() {
        int maxDepth = Math.min(12, size * 2);
        for(int value = 1; value <= size; value++) {
            List<BoardTopology.Cell> candidateCells = new ArrayList<>();
            for(int index = 0; index < size * size; index++) {
                if(state.hasCandidate(index, value)) candidateCells.add(topology.cell(index));
            }
            for(BoardTopology.Cell start : candidateCells) {
                List<BoardTopology.Cell> path = new ArrayList<>();
                path.add(start);
                XChainResult result = searchXChain(value, path, candidateCells, true, maxDepth);
                if(result != null) return xChain(result);
            }
        }
        return null;
    }

    private XChainResult searchXChain(int value, List<BoardTopology.Cell> path,
                                      List<BoardTopology.Cell> candidates,
                                      boolean requireStrong, int maxDepth) {
        if(Thread.currentThread().isInterrupted() || path.size() >= maxDepth) return null;
        BoardTopology.Cell current = path.get(path.size() - 1);
        for(BoardTopology.Cell next : candidates) {
            if(path.contains(next) || !topology.sees(current, next)) continue;
            boolean strong = isStrongLink(current, next, value);
            if(requireStrong && !strong) continue;
            path.add(next);
            if(requireStrong && path.size() >= 4) {
                List<GameHint.Candidate> removals = commonPeerCandidates(
                        Arrays.asList(path.get(0), next), value, path);
                if(!removals.isEmpty() && accepts(removals)) {
                    return new XChainResult(new ArrayList<>(path), value, removals);
                }
            }
            XChainResult deeper = searchXChain(value, path, candidates,
                    !requireStrong, maxDepth);
            if(deeper != null) return deeper;
            path.remove(path.size() - 1);
        }
        return null;
    }

    private boolean isStrongLink(BoardTopology.Cell first, BoardTopology.Cell second, int value) {
        for(BoardTopology.Unit unit : topology.units(first)) {
            if(unit.getCells().contains(second)) {
                List<BoardTopology.Cell> positions = state.candidateCells(unit, value);
                if(positions.size() == 2 && positions.contains(first) && positions.contains(second)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Deduction xChain(XChainResult result) {
        List<GameHint.Link> chainLinks = new ArrayList<>();
        for(int index = 0; index + 1 < result.path.size(); index++) {
            chainLinks.add(link(result.path.get(index), result.path.get(index + 1),
                    result.value, index % 2 == 0));
        }
        List<GameHint.CandidateMark> support = markCandidates(result.path, result.value,
                GameHint.Mark.SUPPORT);
        BoardTopology.Cell first = result.path.get(0);
        BoardTopology.Cell last = result.path.get(result.path.size() - 1);
        List<GameHint.HintFrame> frames = Arrays.asList(
                frame("Strong and weak links on " + symbol(result.value)
                                + " alternate along the highlighted chain.",
                        markCells(result.path, GameHint.Mark.SUPPORT), support, units(), chainLinks),
                frame("The chain begins and ends with a strong link, so at least one endpoint must be "
                                + symbol(result.value) + ".",
                        cells(first, GameHint.Mark.FOCUS, last, GameHint.Mark.FOCUS),
                        concat(support, markCandidates(result.removals, GameHint.Mark.ELIMINATE)),
                        units(), chainLinks), eliminationConclusion(result.removals));
        return elimination("X-Chain", "One endpoint of this X-Chain must contain "
                + symbol(result.value) + ".", frames, result.removals);
    }

    private Deduction findXYChain() {
        List<BoardTopology.Cell> bivalue = cellsWithCandidateCount(2);
        int maxDepth = Math.min(12, size * 2);
        for(BoardTopology.Cell start : bivalue) {
            for(int outer : maskValues(state.getMask(start.getIndex()))) {
                int linkValue = firstValue(state.getMask(start.getIndex())
                        & ~CandidateState.bit(outer));
                List<BoardTopology.Cell> path = new ArrayList<>();
                path.add(start);
                XYChainResult result = searchXYChain(start, outer, linkValue, path,
                        new ArrayList<>(), bivalue, maxDepth);
                if(result != null) return xyChain(result);
            }
        }
        return null;
    }

    private XYChainResult searchXYChain(BoardTopology.Cell start, int outer, int linkValue,
                                        List<BoardTopology.Cell> path, List<Integer> sharedValues,
                                        List<BoardTopology.Cell> bivalue, int maxDepth) {
        if(Thread.currentThread().isInterrupted() || path.size() >= maxDepth) return null;
        BoardTopology.Cell current = path.get(path.size() - 1);
        for(BoardTopology.Cell next : bivalue) {
            if(path.contains(next) || !topology.sees(current, next)
                    || !state.hasCandidate(next.getIndex(), linkValue)) continue;
            int nextOther = firstValue(state.getMask(next.getIndex())
                    & ~CandidateState.bit(linkValue));
            path.add(next);
            sharedValues.add(linkValue);
            if(nextOther == outer && path.size() >= 3) {
                List<GameHint.Candidate> removals = commonPeerCandidates(
                        Arrays.asList(start, next), outer, path);
                if(!removals.isEmpty() && accepts(removals)) {
                    return new XYChainResult(new ArrayList<>(path),
                            new ArrayList<>(sharedValues), outer, removals);
                }
            }
            XYChainResult deeper = searchXYChain(start, outer, nextOther, path,
                    sharedValues, bivalue, maxDepth);
            if(deeper != null) return deeper;
            path.remove(path.size() - 1);
            sharedValues.remove(sharedValues.size() - 1);
        }
        return null;
    }

    private Deduction xyChain(XYChainResult result) {
        List<GameHint.CandidateMark> support = new ArrayList<>();
        for(BoardTopology.Cell cell : result.path) {
            addCandidateMarks(support, cell, state.getMask(cell.getIndex()), GameHint.Mark.SUPPORT);
        }
        List<GameHint.Link> chainLinks = new ArrayList<>();
        for(int index = 0; index < result.path.size(); index++) {
            BoardTopology.Cell cell = result.path.get(index);
            List<Integer> pair = maskValues(state.getMask(cell.getIndex()));
            chainLinks.add(new GameHint.Link(candidate(cell, pair.get(0)),
                    candidate(cell, pair.get(1)), true));
            if(index + 1 < result.path.size()) {
                chainLinks.add(link(cell, result.path.get(index + 1),
                        result.sharedValues.get(index), false));
            }
        }
        BoardTopology.Cell first = result.path.get(0);
        BoardTopology.Cell last = result.path.get(result.path.size() - 1);
        List<GameHint.HintFrame> frames = Arrays.asList(
                frame("Each highlighted cell has two candidates; strong and weak links alternate along the chain.",
                        markCells(result.path, GameHint.Mark.SUPPORT), support, units(), chainLinks),
                frame("If " + symbol(result.value) + " is false at " + coordinate(first)
                                + ", the chain forces it true at " + coordinate(last) + ".",
                        cells(first, GameHint.Mark.ASSUMPTION, last, GameHint.Mark.FOCUS),
                        concat(support, markCandidates(result.removals, GameHint.Mark.ELIMINATE)),
                        units(), chainLinks), eliminationConclusion(result.removals));
        return elimination("XY-Chain", "One end of this XY-Chain must contain "
                + symbol(result.value) + ".", frames, result.removals);
    }

    private Deduction findForcingProof() {
        if(requiredTarget != null) return findRequiredForcingProof();
        List<BoardTopology.Cell> roots = new ArrayList<>();
        for(int index = 0; index < size * size; index++) {
            if(state.getValue(index) == 0 && state.candidateCount(index) > 1) roots.add(topology.cell(index));
        }
        Collections.sort(roots, new Comparator<BoardTopology.Cell>() {
            @Override
            public int compare(BoardTopology.Cell first, BoardTopology.Cell second) {
                return Integer.compare(state.candidateCount(first.getIndex()),
                        state.candidateCount(second.getIndex()));
            }
        });
        ProofNode best = null;
        GameHint.Candidate bestCandidate = null;
        int inspectedRoots = 0;
        for(BoardTopology.Cell root : roots) {
            if(Thread.currentThread().isInterrupted()) return null;
            for(int value : state.candidates(root.getIndex())) {
                if(solution != null && solution.length == size * size
                        && solution[root.getIndex()] == value) continue;
                ProofNode proof = prove(state, candidate(root, value), 0);
                if(proof != null && (best == null || proof.size() < best.size())) {
                    best = proof;
                    bestCandidate = candidate(root, value);
                }
            }
            inspectedRoots++;
            if(best != null && inspectedRoots >= Math.min(12, roots.size())) break;
        }
        if(best == null || bestCandidate == null) return null;

        List<GameHint.HintFrame> frames = new ArrayList<>();
        appendProofFrames(best, frames);
        List<GameHint.Candidate> removals = Collections.singletonList(bestCandidate);
        frames.add(eliminationConclusion(removals));
        return elimination("Forcing Chain", "Assuming " + symbol(bestCandidate.getValue()) + " at "
                + coordinate(topology.cell(bestCandidate.getRow(), bestCandidate.getCol()))
                + " creates a contradiction, so that candidate can be removed.", frames, removals);
    }

    private Deduction findRequiredForcingProof() {
        int index = requiredTarget.getRow() * size + requiredTarget.getCol();
        if(index < 0 || index >= size * size || state.getValue(index) != 0
                || state.candidateCount(index) <= 1
                || !state.hasCandidate(index, requiredTarget.getValue())
                || solution[index] == requiredTarget.getValue()) return null;
        ProofNode proof = prove(state, requiredTarget, 0);
        if(proof == null) return null;
        List<GameHint.HintFrame> frames = new ArrayList<>();
        appendProofFrames(proof, frames);
        List<GameHint.Candidate> removals = Collections.singletonList(requiredTarget);
        frames.add(eliminationConclusion(removals));
        BoardTopology.Cell cell = topology.cell(index);
        return elimination("Forcing Chain", "Assuming " + symbol(requiredTarget.getValue())
                + " at " + coordinate(cell)
                + " creates a contradiction, so that candidate can be removed.", frames, removals);
    }

    private ProofNode prove(CandidateState base, GameHint.Candidate assumption, int depth) {
        if(Thread.currentThread().isInterrupted()) return null;
        CandidateState working = base.copy();
        ProofNode node = new ProofNode();
        BoardTopology.Cell assumedCell = topology.cell(assumption.getRow(), assumption.getCol());
        if(!working.hasCandidate(assumedCell.getIndex(), assumption.getValue())) return null;
        working.setValue(assumedCell.getIndex(), assumption.getValue());
        node.events.add(new ProofEvent("Assume " + symbol(assumption.getValue()) + " at "
                + coordinate(assumedCell) + ".", assumption, GameHint.Mark.ASSUMPTION));

        String contradiction = propagate(working, node.events);
        if(contradiction != null) {
            node.contradiction = contradiction;
            return node;
        }
        if(complete(working)) return null;

        int branchIndex = selectBranchCell(working);
        if(branchIndex < 0) return null;
        BoardTopology.Cell branchCell = topology.cell(branchIndex);
        List<Integer> branchValues = working.candidates(branchIndex);
        node.branchMessage = "Now " + coordinate(branchCell) + " has " + joinedSymbols(branchValues)
                + ". Every remaining branch must be checked.";
        for(int value : branchValues) {
            ProofNode branch = prove(working, candidate(branchCell, value), depth + 1);
            if(branch == null) return null;
            node.branches.add(branch);
        }
        return node;
    }

    private String propagate(CandidateState working, List<ProofEvent> events) {
        boolean changed;
        do {
            if(Thread.currentThread().isInterrupted()) return null;
            changed = false;
            for(BoardTopology.Unit unit : topology.getUnits()) {
                int placedMask = 0;
                for(BoardTopology.Cell cell : unit.getCells()) {
                    int value = working.getValue(cell.getIndex());
                    if(value == 0) continue;
                    int bit = CandidateState.bit(value);
                    if((placedMask & bit) != 0) {
                        return unit.label() + " would contain two " + symbol(value) + "s.";
                    }
                    placedMask |= bit;
                }
            }
            for(int index = 0; index < size * size; index++) {
                if(working.getValue(index) != 0) continue;
                int mask = working.getMask(index);
                for(BoardTopology.Cell peer : topology.peers(topology.cell(index))) {
                    mask &= ~CandidateState.bit(working.getValue(peer.getIndex()));
                }
                if(mask == 0) return coordinate(topology.cell(index)) + " would have no candidate left.";
                if(mask != working.getMask(index)) {
                    working.setMask(index, mask);
                    changed = true;
                }
                if(Integer.bitCount(mask) == 1) {
                    int value = firstValue(mask);
                    working.setValue(index, value);
                    GameHint.Candidate forced = candidate(topology.cell(index), value);
                    events.add(new ProofEvent(coordinate(topology.cell(index)) + " is then forced to "
                            + symbol(value) + " because it has no other candidate.",
                            forced, GameHint.Mark.FOCUS));
                    changed = true;
                }
            }
            for(BoardTopology.Unit unit : topology.getUnits()) {
                for(int value = 1; value <= size; value++) {
                    boolean placed = false;
                    List<BoardTopology.Cell> positions = new ArrayList<>();
                    for(BoardTopology.Cell cell : unit.getCells()) {
                        if(working.getValue(cell.getIndex()) == value) placed = true;
                        else if(working.hasCandidate(cell.getIndex(), value)) positions.add(cell);
                    }
                    if(placed) continue;
                    if(positions.isEmpty()) return unit.label() + " would have no place for "
                            + symbol(value) + ".";
                    if(positions.size() == 1) {
                        BoardTopology.Cell cell = positions.get(0);
                        working.setValue(cell.getIndex(), value);
                        GameHint.Candidate forced = candidate(cell, value);
                        events.add(new ProofEvent(coordinate(cell) + " is then forced to "
                                + symbol(value) + " as its only position in " + unit.label() + ".",
                                forced, GameHint.Mark.FOCUS));
                        changed = true;
                    }
                }
            }
        } while(changed);
        return null;
    }

    private void appendProofFrames(ProofNode node, List<GameHint.HintFrame> frames) {
        for(ProofEvent event : node.events) {
            frames.add(frame(event.message,
                    cells(topology.cell(event.candidate.getRow(), event.candidate.getCol()), event.mark),
                    candidates(event.candidate, event.mark), units(), links()));
        }
        if(node.contradiction != null) {
            ProofEvent last = node.events.get(node.events.size() - 1);
            frames.add(frame("Contradiction: " + node.contradiction,
                    cells(topology.cell(last.candidate.getRow(), last.candidate.getCol()),
                            GameHint.Mark.CONTRADICTION),
                    candidates(last.candidate, GameHint.Mark.CONTRADICTION), units(), links()));
            return;
        }
        if(node.branchMessage != null) frames.add(GameHint.HintFrame.message(node.branchMessage));
        for(ProofNode branch : node.branches) appendProofFrames(branch, frames);
    }

    private int selectBranchCell(CandidateState working) {
        int best = -1;
        int bestCount = Integer.MAX_VALUE;
        for(int index = 0; index < size * size; index++) {
            int count = working.candidateCount(index);
            if(working.getValue(index) == 0 && count > 1 && count < bestCount) {
                best = index;
                bestCount = count;
            }
        }
        return best;
    }

    private GameHint toHint(Deduction deduction) {
        List<GameHint.HintFrame> frames = new ArrayList<>(deduction.frames);
        if(state.getRepairedCandidateCount() > 0) {
            frames.add(0, GameHint.HintFrame.message(state.getRepairedCandidateCount()
                    + " candidate note" + plural(state.getRepairedCandidateCount())
                    + " were reconciled with the fixed clues and unique solution before this deduction."));
        }
        boolean applyPreview = deduction.action != GameHint.Action.CLEAR_VALUE
                && (deduction.action == GameHint.Action.REMOVE_CANDIDATES
                || state.isCandidateModeActive() || state.getRepairedCandidateCount() > 0);
        return new GameHint(deduction.title, deduction.summary, frames,
                requiredTarget == null ? deduction.row : requiredTarget.getRow(),
                requiredTarget == null ? deduction.col : requiredTarget.getCol(),
                requiredTarget == null ? deduction.value : requiredTarget.getValue(), deduction.action,
                deduction.eliminations, state.previewMasks(), applyPreview,
                state.getRepairedCandidateCount());
    }

    private Deduction placement(String title, String summary, List<GameHint.HintFrame> frames,
                                BoardTopology.Cell cell, int value) {
        return new Deduction(title, summary, frames, cell.getRow(), cell.getCol(), value,
                GameHint.Action.PLACE_VALUE, Collections.emptyList());
    }

    private Deduction elimination(String title, String summary, List<GameHint.HintFrame> frames,
                                  List<GameHint.Candidate> removals) {
        GameHint.Candidate target = removals.get(0);
        return new Deduction(title, summary, frames, target.getRow(), target.getCol(),
                target.getValue(), GameHint.Action.REMOVE_CANDIDATES, removals);
    }

    private GameHint.HintFrame placementConclusion(BoardTopology.Cell cell, int value) {
        return frame("Therefore place " + symbol(value) + " at " + coordinate(cell) + ".",
                cells(cell, GameHint.Mark.FOCUS),
                candidates(candidate(cell, value), GameHint.Mark.FOCUS), units(), links());
    }

    private GameHint.HintFrame eliminationConclusion(List<GameHint.Candidate> removals) {
        return frame("Therefore remove " + candidateList(removals) + ".",
                cells(), markCandidates(removals, GameHint.Mark.ELIMINATE), units(), links());
    }

    private List<GameHint.Candidate> candidatesInUnitOutside(BoardTopology.Unit target,
                                                              BoardTopology.Unit source,
                                                              int value) {
        List<GameHint.Candidate> result = new ArrayList<>();
        for(BoardTopology.Cell cell : target.getCells()) {
            if(!source.getCells().contains(cell) && state.hasCandidate(cell.getIndex(), value)) {
                result.add(candidate(cell, value));
            }
        }
        return result;
    }

    private List<GameHint.Candidate> commonPeerCandidates(List<BoardTopology.Cell> sources,
                                                           int value,
                                                           List<BoardTopology.Cell> excluded) {
        List<GameHint.Candidate> result = new ArrayList<>();
        for(int index = 0; index < size * size; index++) {
            BoardTopology.Cell cell = topology.cell(index);
            if(excluded.contains(cell) || !state.hasCandidate(index, value)) continue;
            boolean seesAll = true;
            for(BoardTopology.Cell source : sources) seesAll &= topology.sees(cell, source);
            if(seesAll) result.add(candidate(cell, value));
        }
        return result;
    }

    private List<GameHint.Link> fishLinks(List<BoardTopology.Cell> support, int value,
                                          boolean rowBased) {
        List<GameHint.Link> result = new ArrayList<>();
        for(int line = 0; line < size; line++) {
            List<BoardTopology.Cell> inLine = new ArrayList<>();
            for(BoardTopology.Cell cell : support) {
                if((rowBased ? cell.getRow() : cell.getCol()) == line) inLine.add(cell);
            }
            for(int first = 0; first < inLine.size(); first++) {
                for(int second = first + 1; second < inLine.size(); second++) {
                    result.add(link(inLine.get(first), inLine.get(second), value,
                            inLine.size() == 2));
                }
            }
        }
        return result;
    }

    private List<BoardTopology.Cell> cellsWithCandidateCount(int count) {
        List<BoardTopology.Cell> result = new ArrayList<>();
        for(int index = 0; index < size * size; index++) {
            if(state.getValue(index) == 0 && state.candidateCount(index) == count) {
                result.add(topology.cell(index));
            }
        }
        return result;
    }

    private boolean seesAny(BoardTopology.Cell cell, List<BoardTopology.Cell> sources) {
        for(BoardTopology.Cell source : sources) if(topology.sees(cell, source)) return true;
        return false;
    }

    private static void addGraphEdge(Map<Integer, Set<Integer>> graph, int from, int to) {
        Set<Integer> neighbors = graph.get(from);
        if(neighbors == null) {
            neighbors = new LinkedHashSet<>();
            graph.put(from, neighbors);
        }
        neighbors.add(to);
    }

    private List<BoardTopology.Cell> colorCells(Map<Integer, Integer> colors, int color) {
        List<BoardTopology.Cell> result = new ArrayList<>();
        for(Map.Entry<Integer, Integer> entry : colors.entrySet()) {
            if(entry.getValue() == color) result.add(topology.cell(entry.getKey()));
        }
        return result;
    }

    private boolean unitContainsValue(BoardTopology.Unit unit, int value) {
        for(BoardTopology.Cell cell : unit.getCells()) {
            if(state.getValue(cell.getIndex()) == value) return true;
        }
        return false;
    }

    private boolean isComplete() {
        for(int index = 0; index < size * size; index++) if(state.getValue(index) == 0) return false;
        return true;
    }

    private boolean complete(CandidateState candidateState) {
        for(int index = 0; index < size * size; index++) {
            if(candidateState.getValue(index) == 0) return false;
        }
        return true;
    }

    private static boolean allSameRow(List<BoardTopology.Cell> cells) {
        int row = cells.get(0).getRow();
        for(BoardTopology.Cell cell : cells) if(cell.getRow() != row) return false;
        return true;
    }

    private static boolean allSameColumn(List<BoardTopology.Cell> cells) {
        int col = cells.get(0).getCol();
        for(BoardTopology.Cell cell : cells) if(cell.getCol() != col) return false;
        return true;
    }

    private boolean allSameBlock(List<BoardTopology.Cell> cells) {
        int block = topology.blockIndex(cells.get(0).getRow(), cells.get(0).getCol());
        for(BoardTopology.Cell cell : cells) {
            if(topology.blockIndex(cell.getRow(), cell.getCol()) != block) return false;
        }
        return true;
    }

    private static BoardTopology.Cell other(List<BoardTopology.Cell> pair,
                                            BoardTopology.Cell one) {
        return pair.get(0).equals(one) ? pair.get(1) : pair.get(0);
    }

    private static int firstValue(int mask) {
        return mask == 0 ? 0 : Integer.numberOfTrailingZeros(mask) + 1;
    }

    private static List<Integer> maskValues(int mask) {
        List<Integer> result = new ArrayList<>();
        for(int value = 1; value <= 31; value++) {
            if((mask & CandidateState.bit(value)) != 0) result.add(value);
        }
        return result;
    }

    private static <T> List<List<T>> combinations(List<T> source, int count) {
        List<List<T>> result = new ArrayList<>();
        combinations(source, count, 0, new ArrayList<>(), result);
        return result;
    }

    private static <T> void combinations(List<T> source, int count, int start,
                                         List<T> current, List<List<T>> result) {
        if(current.size() == count) {
            result.add(new ArrayList<>(current));
            return;
        }
        int needed = count - current.size();
        for(int index = start; index <= source.size() - needed; index++) {
            current.add(source.get(index));
            combinations(source, count, index + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private void addCandidates(List<GameHint.Candidate> target, BoardTopology.Cell cell, int mask) {
        for(int value = 1; value <= size; value++) {
            if((mask & CandidateState.bit(value)) != 0) target.add(candidate(cell, value));
        }
    }

    private GameHint.HintFrame frame(String message, List<GameHint.CellMark> cellMarks,
                                     List<GameHint.CandidateMark> candidateMarks,
                                     List<GameHint.UnitMark> unitMarks,
                                     List<GameHint.Link> frameLinks) {
        return new GameHint.HintFrame(message, cellMarks, candidateMarks, unitMarks, frameLinks);
    }

    private static List<GameHint.CellMark> cells(Object... values) {
        List<GameHint.CellMark> result = new ArrayList<>();
        for(int index = 0; index + 1 < values.length; index += 2) {
            BoardTopology.Cell cell = (BoardTopology.Cell) values[index];
            GameHint.Mark mark = (GameHint.Mark) values[index + 1];
            result.add(new GameHint.CellMark(cell.getRow(), cell.getCol(), mark));
        }
        return result;
    }

    private static List<GameHint.CellMark> markCells(List<BoardTopology.Cell> source,
                                                      GameHint.Mark mark) {
        List<GameHint.CellMark> result = new ArrayList<>();
        for(BoardTopology.Cell cell : source) {
            result.add(new GameHint.CellMark(cell.getRow(), cell.getCol(), mark));
        }
        return result;
    }

    private static List<GameHint.CandidateMark> candidates(Object... values) {
        List<GameHint.CandidateMark> result = new ArrayList<>();
        for(int index = 0; index + 1 < values.length; index += 2) {
            result.add(mark((GameHint.Candidate) values[index], (GameHint.Mark) values[index + 1]));
        }
        return result;
    }

    private static GameHint.CandidateMark mark(GameHint.Candidate candidate, GameHint.Mark mark) {
        return new GameHint.CandidateMark(candidate, mark);
    }

    private static List<GameHint.CandidateMark> markCandidates(List<GameHint.Candidate> source,
                                                                GameHint.Mark mark) {
        List<GameHint.CandidateMark> result = new ArrayList<>();
        for(GameHint.Candidate candidate : source) result.add(mark(candidate, mark));
        return result;
    }

    private static List<GameHint.CandidateMark> markCandidates(List<BoardTopology.Cell> source,
                                                                int value, GameHint.Mark mark) {
        List<GameHint.CandidateMark> result = new ArrayList<>();
        for(BoardTopology.Cell cell : source) result.add(mark(candidate(cell, value), mark));
        return result;
    }

    private List<GameHint.CandidateMark> markMaskCandidates(List<BoardTopology.Cell> source,
                                                             int mask, GameHint.Mark mark) {
        List<GameHint.CandidateMark> result = new ArrayList<>();
        for(BoardTopology.Cell cell : source) addCandidateMarks(result, cell, mask, mark);
        return result;
    }

    private void addCandidateMarks(List<GameHint.CandidateMark> target, BoardTopology.Cell cell,
                                   int mask, GameHint.Mark mark) {
        for(int value = 1; value <= size; value++) {
            if((mask & CandidateState.bit(value)) != 0) target.add(mark(candidate(cell, value), mark));
        }
    }

    @SafeVarargs
    private static List<GameHint.CandidateMark> concat(List<GameHint.CandidateMark>... sources) {
        List<GameHint.CandidateMark> result = new ArrayList<>();
        for(List<GameHint.CandidateMark> source : sources) result.addAll(source);
        return result;
    }

    private static List<GameHint.UnitMark> units(Object... values) {
        List<GameHint.UnitMark> result = new ArrayList<>();
        for(int index = 0; index + 1 < values.length; index += 2) {
            BoardTopology.Unit unit = (BoardTopology.Unit) values[index];
            result.add(new GameHint.UnitMark(unit.getType(), unit.getIndex(),
                    (GameHint.Mark) values[index + 1]));
        }
        return result;
    }

    private List<GameHint.UnitMark> cellUnits(BoardTopology.Cell cell, GameHint.Mark mark) {
        List<GameHint.UnitMark> result = new ArrayList<>();
        for(BoardTopology.Unit unit : topology.units(cell)) {
            result.add(new GameHint.UnitMark(unit.getType(), unit.getIndex(), mark));
        }
        return result;
    }

    private static List<GameHint.Link> links(GameHint.Link... source) {
        return source.length == 0 ? Collections.emptyList() : Arrays.asList(source);
    }

    private static GameHint.Link link(BoardTopology.Cell from, BoardTopology.Cell to,
                                      int value, boolean strong) {
        return new GameHint.Link(candidate(from, value), candidate(to, value), strong);
    }

    private static GameHint.Candidate candidate(BoardTopology.Cell cell, int value) {
        return new GameHint.Candidate(cell.getRow(), cell.getCol(), value);
    }

    private String symbol(int value) {
        return Symbol.getSymbol(symbols, value - 1);
    }

    private String values(int mask) {
        return joinedSymbols(maskValues(mask));
    }

    private String joinedSymbols(List<Integer> values) {
        if(values.isEmpty()) return "no candidates";
        StringBuilder result = new StringBuilder();
        for(int index = 0; index < values.size(); index++) {
            if(index > 0) result.append(index == values.size() - 1 ? " and " : ", ");
            result.append(symbol(values.get(index)));
        }
        return result.toString();
    }

    private String coordinates(List<BoardTopology.Cell> cells) {
        StringBuilder result = new StringBuilder();
        for(int index = 0; index < cells.size(); index++) {
            if(index > 0) result.append(index == cells.size() - 1 ? " and " : ", ");
            result.append(coordinate(cells.get(index)));
        }
        return result.toString();
    }

    private static String coordinate(BoardTopology.Cell cell) {
        return "row " + (cell.getRow() + 1) + ", column " + (cell.getCol() + 1);
    }

    private String candidateList(List<GameHint.Candidate> removals) {
        StringBuilder result = new StringBuilder();
        for(int index = 0; index < removals.size(); index++) {
            GameHint.Candidate candidate = removals.get(index);
            if(index > 0) result.append(index == removals.size() - 1 ? " and " : ", ");
            result.append(symbol(candidate.getValue())).append(" at ")
                    .append(coordinate(topology.cell(candidate.getRow(), candidate.getCol())));
        }
        return result.toString();
    }

    private static String subsetName(int size) {
        switch(size) {
            case 2: return "Pair";
            case 3: return "Triple";
            default: return "Quad";
        }
    }

    private static String fishName(int size) {
        switch(size) {
            case 2: return "X-Wing";
            case 3: return "Swordfish";
            default: return "Jellyfish";
        }
    }

    private static String plural(int count) { return count == 1 ? "" : "s"; }

    private static final class Deduction {
        private final String title;
        private final String summary;
        private final List<GameHint.HintFrame> frames;
        private final int row;
        private final int col;
        private final int value;
        private final GameHint.Action action;
        private final List<GameHint.Candidate> eliminations;

        private Deduction(String title, String summary, List<GameHint.HintFrame> frames,
                          int row, int col, int value, GameHint.Action action,
                          List<GameHint.Candidate> eliminations) {
            this.title = title;
            this.summary = summary;
            this.frames = frames;
            this.row = row;
            this.col = col;
            this.value = value;
            this.action = action;
            this.eliminations = eliminations;
        }
    }

    private static final class XYChainResult {
        private final List<BoardTopology.Cell> path;
        private final List<Integer> sharedValues;
        private final int value;
        private final List<GameHint.Candidate> removals;

        private XYChainResult(List<BoardTopology.Cell> path, List<Integer> sharedValues,
                              int value, List<GameHint.Candidate> removals) {
            this.path = path;
            this.sharedValues = sharedValues;
            this.value = value;
            this.removals = removals;
        }
    }

    private static final class XChainResult {
        private final List<BoardTopology.Cell> path;
        private final int value;
        private final List<GameHint.Candidate> removals;

        private XChainResult(List<BoardTopology.Cell> path, int value,
                             List<GameHint.Candidate> removals) {
            this.path = path;
            this.value = value;
            this.removals = removals;
        }
    }

    private static final class ProofEvent {
        private final String message;
        private final GameHint.Candidate candidate;
        private final GameHint.Mark mark;

        private ProofEvent(String message, GameHint.Candidate candidate, GameHint.Mark mark) {
            this.message = message;
            this.candidate = candidate;
            this.mark = mark;
        }
    }

    private static final class ProofNode {
        private final List<ProofEvent> events = new ArrayList<>();
        private final List<ProofNode> branches = new ArrayList<>();
        private String branchMessage;
        private String contradiction;

        private ProofNode() { }

        private int size() {
            int result = events.size() + (contradiction == null ? 0 : 1);
            for(ProofNode branch : branches) result += branch.size();
            return result;
        }
    }
}
