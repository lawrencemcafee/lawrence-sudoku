/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import android.content.Context;

import org.secuso.privacyfriendlysudoku.R;

import java.io.IOException;
import java.io.InputStream;

/** Process-wide cache for the immutable bundled Gym corpus. */
public final class TrainingCorpusProvider {
    private static volatile TrainingCorpus corpus;

    private TrainingCorpusProvider() {}

    public static TrainingCorpus get(Context context) throws IOException {
        TrainingCorpus existing = corpus;
        if(existing != null) return existing;
        synchronized(TrainingCorpusProvider.class) {
            if(corpus == null) {
                try(InputStream input = context.getResources()
                        .openRawResource(R.raw.gym_corpus_v1)) {
                    corpus = TrainingCorpus.load(input);
                }
            }
            return corpus;
        }
    }
}
