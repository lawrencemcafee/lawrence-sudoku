/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class TrainingCorpus {
    private final Map<HumanTechnique, List<TrainingPosition>> positions;

    private TrainingCorpus(Map<HumanTechnique, List<TrainingPosition>> positions) {
        this.positions = Collections.unmodifiableMap(new EnumMap<>(positions));
    }

    public static TrainingCorpus load(InputStream input) throws IOException {
        return new TrainingCorpus(TrainingCorpusCodec.read(input));
    }

    public List<TrainingPosition> get(HumanTechnique technique) {
        List<TrainingPosition> result = positions.get(technique);
        if(result == null) throw new IllegalArgumentException("No Gym positions for " + technique);
        return result;
    }
}
