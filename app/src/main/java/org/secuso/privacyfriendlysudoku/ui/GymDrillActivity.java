/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.ui;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.GameController;
import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.controller.helper.GameInfoContainer;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanHintEngine;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingCorpus;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingCorpusProvider;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingMode;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingPosition;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingSession;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStats;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStatsRepository;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingTarget;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameType;
import org.secuso.privacyfriendlysudoku.ui.view.HumanHintDialog;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuFieldLayout;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuKeyboardLayout;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Endless isolated practice for one human Sudoku technique. */
public class GymDrillActivity extends BaseActivity {
    public static final String EXTRA_TECHNIQUE = "gymTechnique";
    public static final String EXTRA_MODE = "gymMode";

    private static final String STATE_SEED = "gymSeed";
    private static final String STATE_SEQUENCE = "gymSequence";
    private static final String STATE_ATTEMPT = "gymAttempt";
    private static final String STATE_ELIGIBLE = "gymEligible";
    private static final String STATE_REVEAL = "gymReveal";
    private static final String STATE_INTERACTION = "gymInteraction";
    private static final String STATE_ROW = "row";
    private static final String STATE_COL = "col";
    private static final String STATE_VALUE = "value";
    private static final String STATE_HINT_OPEN = "hintOpen";
    private static final String STATE_HINT_PAGE = "hintPage";
    private static final long NEXT_DELAY_MS = 600L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Set<TrainingTarget> validTargets = new HashSet<>();

    private SharedPreferences settings;
    private TrainingStatsRepository statsRepository;
    private HumanTechnique technique;
    private TrainingMode mode;
    private TrainingSession session;
    private long sessionSeed;
    private long sequence;
    private PreparedDrill current;
    private PreparedDrill prefetched;
    private long prefetchRequested = -1;
    private boolean attemptStarted;
    private boolean eligible = true;
    private boolean revealRecorded;
    private boolean completionPending;
    private boolean destroyed;
    private boolean keyboardReady;
    private boolean resumed;
    private Bundle pendingInteraction;
    private HumanHintDialog hintDialog;

    private GameController gameController;
    private SudokuFieldLayout field;
    private SudokuKeyboardLayout keyboard;
    private TextView actionView;
    private TextView statsView;
    private ImageButton hintButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        if(settings.getBoolean("pref_keep_screen_on", true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        try {
            technique = HumanTechnique.valueOf(getIntent().getStringExtra(EXTRA_TECHNIQUE));
            mode = TrainingMode.valueOf(getIntent().getStringExtra(EXTRA_MODE));
        } catch(IllegalArgumentException | NullPointerException failure) {
            finish();
            return;
        }

        if(savedInstanceState == null) {
            sessionSeed = new Random().nextLong();
        } else {
            sessionSeed = savedInstanceState.getLong(STATE_SEED);
            sequence = savedInstanceState.getLong(STATE_SEQUENCE);
            attemptStarted = savedInstanceState.getBoolean(STATE_ATTEMPT);
            eligible = savedInstanceState.getBoolean(STATE_ELIGIBLE, true);
            revealRecorded = savedInstanceState.getBoolean(STATE_REVEAL);
            pendingInteraction = savedInstanceState.getBundle(STATE_INTERACTION);
        }

        setContentView(R.layout.activity_gym_drill);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(technique.getTitle());
            actionBar.setSubtitle(mode == TrainingMode.FOCUSED
                    ? R.string.gym_mode_focused : R.string.gym_mode_full);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        field = findViewById(R.id.sudokuLayout);
        keyboard = findViewById(R.id.sudokuKeyboardLayout);
        actionView = findViewById(R.id.gymDrillAction);
        statsView = findViewById(R.id.gymDrillStats);
        hintButton = findViewById(R.id.gymHintButton);
        hintButton.setEnabled(false);
        hintButton.setOnClickListener(view -> showHint());
        statsRepository = new TrainingStatsRepository(this);
        updateStats();
        showLoading();
        requestPosition(sequence);
    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
        restoreInteraction();
    }

    @Override
    protected void onPause() {
        resumed = false;
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putLong(STATE_SEED, sessionSeed);
        if(completionPending) {
            state.putLong(STATE_SEQUENCE, sequence + 1);
            state.putBoolean(STATE_ELIGIBLE, true);
            return;
        }
        state.putLong(STATE_SEQUENCE, sequence);
        state.putBoolean(STATE_ATTEMPT, attemptStarted);
        state.putBoolean(STATE_ELIGIBLE, eligible);
        state.putBoolean(STATE_REVEAL, revealRecorded);
        state.putBundle(STATE_INTERACTION, captureInteraction());
    }

    private Bundle captureInteraction() {
        // A second recreation may happen before the first asynchronous restore finishes.
        if(pendingInteraction != null) return new Bundle(pendingInteraction);
        Bundle state = new Bundle();
        if(current != null) {
            state.putInt(STATE_ROW, gameController.getSelectedRow());
            state.putInt(STATE_COL, gameController.getSelectedCol());
            state.putInt(STATE_VALUE, gameController.getSelectedValue());
        }
        if(hintDialog != null && hintDialog.isShowing()) {
            state.putBoolean(STATE_HINT_OPEN, true);
            state.putInt(STATE_HINT_PAGE, hintDialog.getDetailIndex());
        }
        return state;
    }

    private void restoreInteraction() {
        if(current == null || pendingInteraction == null) return;
        gameController.resetSelects();
        int value = pendingInteraction.getInt(STATE_VALUE);
        int row = pendingInteraction.getInt(STATE_ROW, -1);
        int col = pendingInteraction.getInt(STATE_COL, -1);
        if(value > 0 && value <= TrainingPosition.SIZE) {
            gameController.selectValue(value);
        } else if(row >= 0 && row < TrainingPosition.SIZE
                && col >= 0 && col < TrainingPosition.SIZE) {
            gameController.selectCell(row, col);
        }
        if(pendingInteraction.getBoolean(STATE_HINT_OPEN)) {
            // Preparation can finish while stopped; wait until the window is active.
            if(!resumed) return;
            int page = pendingInteraction.getInt(STATE_HINT_PAGE, -1);
            pendingInteraction = null;
            showHint(page);
        } else {
            pendingInteraction = null;
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if(hintDialog != null) hintDialog.dismiss();
        mHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        if(gameController != null) gameController.deleteTimer();
        super.onDestroy();
    }

    private void requestPosition(long requestedSequence) {
        prefetchRequested = requestedSequence;
        executor.execute(() -> {
            try {
                if(session == null) {
                    TrainingCorpus corpus = TrainingCorpusProvider.get(getApplicationContext());
                    session = new TrainingSession(corpus.get(technique), sessionSeed);
                }
                PreparedDrill prepared = prepare(requestedSequence);
                runOnUiThread(() -> acceptPrepared(prepared));
            } catch(IOException | RuntimeException failure) {
                runOnUiThread(this::showCorpusError);
            }
        });
    }

    private PreparedDrill prepare(long requestedSequence) {
        TrainingPosition position = session.get(requestedSequence);
        Symbol symbols = selectedSymbols();
        GameHint hint = HumanHintEngine.findTechnique(GameType.Default_9x9,
                position.getValues(), position.getCandidateMasks(), position.getSolution(),
                symbols, technique);
        if(hint == null || hint.getAction() != position.getAction()) {
            throw new IllegalStateException("Bundled Gym position no longer matches its rule.");
        }
        return new PreparedDrill(requestedSequence, position, hint, symbols);
    }

    private void acceptPrepared(PreparedDrill prepared) {
        if(destroyed) return;
        if(prepared.sequence == sequence && current == null) {
            bind(prepared);
        } else if(prepared.sequence == sequence + 1 && current != null) {
            prefetched = prepared;
        }
    }

    private void bind(PreparedDrill prepared) {
        current = prepared;
        prefetched = null;
        completionPending = false;
        validTargets.clear();
        validTargets.addAll(prepared.position.getTargets());

        boolean[][] notes = toNotes(prepared.position.displayMasks(mode));
        GameInfoContainer game = new GameInfoContainer(0,
                DifficultyLevel.of(technique.getBaseLevel()), GameType.Default_9x9,
                prepared.position.getValues(), null, notes);
        if(gameController == null) {
            gameController = new GameController(settings, getApplicationContext());
            gameController.deleteTimer();
            gameController.loadLevel(game);
            field.setSettingsAndGame(settings, gameController);
            field.setOnCellClickListener(this::selectCell);
            keyboard.removeAllViews();
            keyboard.setGameController(gameController);
            Point display = new Point();
            getWindowManager().getDefaultDisplay().getSize(display);
            int orientation = getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT
                    ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL;
            keyboard.setKeyBoard(9, display.x, display.y, orientation);
            keyboard.setOnValueClickListener(this::submitValue);
            keyboardReady = true;
        } else {
            gameController.loadLevel(game);
            field.refreshGame();
        }
        field.setSymbols(prepared.symbols);
        keyboard.setSymbols(prepared.symbols);

        gameController.resetSelects();
        gameController.notifyHighlightChangedListeners();
        keyboard.setButtonsEnabled(true);
        hintButton.setEnabled(true);
        actionView.setText(prepared.position.getAction() == GameHint.Action.PLACE_VALUE
                ? R.string.gym_action_placement : R.string.gym_action_elimination);
        updateStats();
        restoreInteraction();
        requestPrefetch(sequence + 1);
    }

    private void requestPrefetch(long nextSequence) {
        if(prefetchRequested == nextSequence) return;
        requestPosition(nextSequence);
    }

    private void submitValue(int value) {
        if(current == null || completionPending) return;
        if(!gameController.isValidCellSelected()) {
            gameController.selectValue(value);
            return;
        }

        submitAnswer(gameController.getSelectedRow(), gameController.getSelectedCol(), value);
    }

    private void selectCell(int row, int col) {
        if(current == null || completionPending) return;
        int selectedValue = gameController.getSelectedValue();
        if(selectedValue == 0) {
            gameController.selectCell(row, col);
            return;
        }
        if(gameController.getGameCell(row, col).isFixed()) return;
        submitAnswer(row, col, selectedValue);
    }

    private void submitAnswer(int row, int col, int value) {
        TrainingTarget answer = new TrainingTarget(row, col, value);
        beginAttempt();
        if(!validTargets.contains(answer)) {
            markFailure();
            field.showCandidateFeedback(row, col, value, false);
            actionView.setText(R.string.gym_incorrect);
            return;
        }

        if(current.position.getAction() == GameHint.Action.PLACE_VALUE) {
            gameController.setValue(row, col, value);
        } else {
            gameController.deleteNote(row, col, value);
            field.showCandidateFeedback(row, col, value, true);
        }
        gameController.notifyHighlightChangedListeners();
        completeCurrent();
    }

    private void beginAttempt() {
        if(attemptStarted) return;
        attemptStarted = true;
        statsRepository.recordAttempt(technique, mode);
        updateStats();
    }

    private void markFailure() {
        if(!eligible) return;
        eligible = false;
        statsRepository.recordFailure(technique, mode);
        updateStats();
    }

    private void showHint() {
        showHint(-1);
    }

    private void showHint(int initialPage) {
        if(!resumed || destroyed || current == null || completionPending
                || (hintDialog != null && hintDialog.isShowing())) return;
        hintDialog = new HumanHintDialog(this, gameController, current.hint,
                new HumanHintDialog.Listener() {
            @Override
            public void onHintOpened() {
                beginAttempt();
                if(!revealRecorded) {
                    revealRecorded = true;
                    statsRepository.recordReveal(technique, mode);
                }
                markFailure();
                updateStats();
            }

            @Override
            public void onHintApplied() {
                gameController.notifyHighlightChangedListeners();
                completeCurrent();
            }
        });
        hintDialog.show(initialPage);
    }

    private void completeCurrent() {
        if(completionPending) return;
        completionPending = true;
        if(eligible) statsRepository.recordCorrect(technique, mode);
        updateStats();
        actionView.setText(R.string.gym_correct);
        keyboard.setButtonsEnabled(false);
        hintButton.setEnabled(false);
        mHandler.postDelayed(this::advance, NEXT_DELAY_MS);
    }

    private void advance() {
        sequence++;
        current = null;
        attemptStarted = false;
        eligible = true;
        revealRecorded = false;
        completionPending = false;
        validTargets.clear();
        if(prefetched != null && prefetched.sequence == sequence) {
            PreparedDrill ready = prefetched;
            prefetched = null;
            bind(ready);
        } else {
            showLoading();
            if(prefetchRequested != sequence) requestPosition(sequence);
        }
    }

    private void showLoading() {
        actionView.setText(R.string.gym_loading);
        hintButton.setEnabled(false);
        if(keyboardReady) keyboard.setButtonsEnabled(false);
    }

    private void showCorpusError() {
        if(destroyed) return;
        Toast.makeText(this, R.string.gym_corpus_error, Toast.LENGTH_LONG).show();
        finish();
    }

    private void updateStats() {
        TrainingStats stats = statsRepository.get(technique, mode);
        statsView.setText(getString(R.string.gym_drill_stats_format,
                stats.getAccuracyPercent(), stats.getCurrentStreak(), stats.getBestStreak()));
    }

    private Symbol selectedSymbols() {
        try {
            return Symbol.valueOf(settings.getString("pref_symbols", Symbol.Default.name()));
        } catch(IllegalArgumentException failure) {
            return Symbol.Default;
        }
    }

    private static boolean[][] toNotes(int[] masks) {
        boolean[][] notes = new boolean[TrainingPosition.CELL_COUNT][TrainingPosition.SIZE];
        for(int cell = 0; cell < masks.length; cell++) {
            for(int value = 1; value <= TrainingPosition.SIZE; value++) {
                notes[cell][value - 1] = (masks[cell] & (1 << (value - 1))) != 0;
            }
        }
        return notes;
    }

    private static final class PreparedDrill {
        final long sequence;
        final TrainingPosition position;
        final GameHint hint;
        final Symbol symbols;

        PreparedDrill(long sequence, TrainingPosition position, GameHint hint, Symbol symbols) {
            this.sequence = sequence;
            this.position = position;
            this.hint = hint;
            this.symbols = symbols;
        }
    }
}
