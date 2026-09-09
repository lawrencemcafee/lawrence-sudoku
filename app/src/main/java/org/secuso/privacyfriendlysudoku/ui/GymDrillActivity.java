/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.ui;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SwitchCompat;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.GameController;
import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.controller.helper.GameInfoContainer;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanHintEngine;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingCorpus;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingCorpusProvider;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingPosition;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingQuiz;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingSession;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStatsRepository;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingTarget;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameType;
import org.secuso.privacyfriendlysudoku.ui.view.HumanHintDialog;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuFieldLayout;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuKeyboardLayout;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuSpecialButtonLayout;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuButtonType;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Ten-question quizzes with optional review of each human-technique deduction. */
public class GymDrillActivity extends BaseActivity {
    public static final String EXTRA_TECHNIQUE = "gymTechnique";
    public static final String PREF_REVIEW = "reviewAnswers";

    private static final String TAG = "GymQuiz";
    private static final String STATE_SEED = "gymSeed";
    private static final String STATE_SEQUENCE = "gymSequence";
    private static final String STATE_ATTEMPT = "gymAttempt";
    private static final String STATE_ELIGIBLE = "gymEligible";
    private static final String STATE_REVEAL = "gymReveal";
    private static final String STATE_INTERACTION = "gymInteraction";
    private static final String STATE_ROW = "row";
    private static final String STATE_COL = "col";
    private static final String STATE_VALUE = "value";
    private static final String STATE_NOTES = "notes";
    private static final String STATE_HINT_OPEN = "hintOpen";
    private static final String STATE_HINT_PAGE = "hintPage";
    private static final String STATE_QUIZ_START = "quizStart";
    private static final String STATE_QUIZ_RESULTS = "quizResults";
    private static final String STATE_RESULTS_VISIBLE = "resultsVisible";
    private static final String STATE_RECAP_REVIEW = "recapReview";
    private static final String STATE_REVIEW_REQUESTED = "reviewRequested";
    private static final long NEXT_DELAY_MS = 600L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Set<TrainingTarget> validTargets = new HashSet<>();

    private SharedPreferences settings;
    private TrainingStatsRepository statsRepository;
    private HumanTechnique technique;
    private TrainingSession session;
    private TrainingQuiz quiz;
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
    private GameHint reviewHint;
    private PreparedDrill reviewProofRequested;
    private boolean resultsVisible;
    private boolean recapReview;
    private boolean reviewRequested;
    private int reviewPage = -1;
    private final Runnable advanceRunnable = this::advance;

    private Toolbar toolbar;
    private SwitchCompat reviewSwitch;
    private Button nextQuestion;
    private GameController gameController;
    private SudokuFieldLayout field;
    private SudokuKeyboardLayout keyboard;
    private SudokuSpecialButtonLayout specialButtons;
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
        } catch(IllegalArgumentException | NullPointerException failure) {
            finish();
            return;
        }

        if(savedInstanceState == null) {
            sessionSeed = new Random().nextLong();
            quiz = new TrainingQuiz(0);
        } else {
            sessionSeed = savedInstanceState.getLong(STATE_SEED);
            sequence = savedInstanceState.getLong(STATE_SEQUENCE);
            attemptStarted = savedInstanceState.getBoolean(STATE_ATTEMPT);
            eligible = savedInstanceState.getBoolean(STATE_ELIGIBLE, true);
            revealRecorded = savedInstanceState.getBoolean(STATE_REVEAL);
            pendingInteraction = savedInstanceState.getBundle(STATE_INTERACTION);
            quiz = TrainingQuiz.restore(savedInstanceState.getLong(STATE_QUIZ_START, sequence),
                    savedInstanceState.getIntArray(STATE_QUIZ_RESULTS));
            resultsVisible = savedInstanceState.getBoolean(STATE_RESULTS_VISIBLE);
            recapReview = savedInstanceState.getBoolean(STATE_RECAP_REVIEW);
            reviewRequested = savedInstanceState.getBoolean(STATE_REVIEW_REQUESTED);
        }

        setContentView(R.layout.activity_gym_drill);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(technique.getTitle());
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences gymSettings = getSharedPreferences("gym", MODE_PRIVATE);
        reviewSwitch = findViewById(R.id.gymReviewSwitch);
        reviewSwitch.setChecked(gymSettings.getBoolean(PREF_REVIEW, false));
        updateReviewDescription();
        reviewSwitch.setOnCheckedChangeListener((button, review) -> {
            gymSettings.edit().putBoolean(PREF_REVIEW, review).apply();
            updateReviewDescription();
            if(completionPending && !recapReview) {
                reviewRequested = review;
                updateCompletionBehavior();
            }
        });
        nextQuestion = findViewById(R.id.gymNextQuestion);
        nextQuestion.setOnClickListener(view -> advance());
        findViewById(R.id.gymAnotherQuiz).setOnClickListener(view -> startAnotherQuiz());
        findViewById(R.id.gymChooseSkill).setOnClickListener(view -> finish());

        field = findViewById(R.id.sudokuLayout);
        keyboard = findViewById(R.id.sudokuKeyboardLayout);
        specialButtons = findViewById(R.id.sudokuSpecialLayout);
        actionView = findViewById(R.id.gymDrillAction);
        statsView = findViewById(R.id.gymDrillStats);
        statsRepository = new TrainingStatsRepository(this);
        updateQuizScore();
        if(resultsVisible) {
            showResults();
        } else {
            showLoading();
            requestPosition(sequence);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
        restoreInteraction();
        updateCompletionBehavior();
    }

    @Override
    protected void onPause() {
        resumed = false;
        mHandler.removeCallbacks(advanceRunnable);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            if(recapReview) showResults();
            else finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(recapReview) showResults();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if(quiz == null) return;
        state.putLong(STATE_SEED, sessionSeed);
        state.putLong(STATE_SEQUENCE, sequence);
        state.putLong(STATE_QUIZ_START, quiz.getStartSequence());
        state.putIntArray(STATE_QUIZ_RESULTS, quiz.saveResults());
        state.putBoolean(STATE_RESULTS_VISIBLE, resultsVisible);
        state.putBoolean(STATE_RECAP_REVIEW, recapReview);
        state.putBoolean(STATE_REVIEW_REQUESTED, reviewRequested);
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
            state.putBoolean(STATE_NOTES, gameController.getNoteStatus());
        }
        if(hintDialog != null && hintDialog.isShowing()) {
            state.putBoolean(STATE_HINT_OPEN, true);
            state.putInt(STATE_HINT_PAGE, hintDialog.getDetailIndex());
        } else if(completionPending && reviewRequested) {
            state.putBoolean(STATE_HINT_OPEN, true);
            state.putInt(STATE_HINT_PAGE, reviewPage);
        }
        return state;
    }

    private void restoreInteraction() {
        if(current == null || pendingInteraction == null) return;
        if(completionPending) {
            reviewRequested = pendingInteraction.getBoolean(STATE_HINT_OPEN, reviewRequested);
            reviewPage = pendingInteraction.getInt(STATE_HINT_PAGE, -1);
            pendingInteraction = null;
            return;
        }
        gameController.setNoteStatus(pendingInteraction.getBoolean(STATE_NOTES,
                gameController.getNoteStatus()));
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
                Log.e(TAG, "Could not prepare Gym position " + requestedSequence, failure);
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
        if(destroyed || resultsVisible) return;
        if(prepared.sequence == sequence && current == null) {
            bind(prepared);
        } else if(prepared.sequence == sequence + 1 && current != null) {
            prefetched = prepared;
        }
    }

    private void bind(PreparedDrill prepared) {
        current = prepared;
        prefetched = null;
        completionPending = quiz.hasResult(sequence);
        reviewHint = null;
        reviewProofRequested = null;
        validTargets.clear();
        validTargets.addAll(prepared.position.getTargets());

        boolean[][] notes = toNotes(prepared.position.getCandidateMasks());
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
            specialButtons.setButtons(display.x, gameController, keyboard, orientation, this,
                    Arrays.asList(SudokuButtonType.Hint, SudokuButtonType.NoteToggle));
            specialButtons.setOnButtonClickListener(type -> {
                if(current == null) return true;
                if(type == SudokuButtonType.Hint) {
                    if(completionPending) {
                        reviewRequested = true;
                        reviewPage = -1;
                        showReview();
                    } else showHint();
                } else if(!completionPending && type == SudokuButtonType.NoteToggle) {
                    gameController.setNoteStatus(!gameController.getNoteStatus());
                    gameController.notifyHighlightChangedListeners();
                }
                return true;
            });
            hintButton = specialButtons.getButton(SudokuButtonType.Hint);
            hintButton.setContentDescription(getString(R.string.gym_hint_description));
            keyboardReady = true;
        } else {
            gameController.loadLevel(game);
            field.refreshGame();
        }
        field.setSymbols(prepared.symbols);
        keyboard.setSymbols(prepared.symbols);

        gameController.setNoteStatus(prepared.position.getAction() == GameHint.Action.REMOVE_CANDIDATES);
        gameController.resetSelects();
        gameController.notifyHighlightChangedListeners();
        keyboard.setButtonsEnabled(true);
        specialButtons.setButtonsEnabled(true);
        hintButton.setContentDescription(getString(R.string.gym_hint_description));
        nextQuestion.setVisibility(View.GONE);
        toolbar.setSubtitle(getString(R.string.gym_question_progress,
                sequence - quiz.getStartSequence() + 1, TrainingQuiz.LENGTH));
        actionView.setText(prepared.position.getAction() == GameHint.Action.PLACE_VALUE
                ? R.string.gym_action_placement : R.string.gym_action_elimination);
        updateQuizScore();
        if(completionPending) {
            TrainingQuiz.Result result = quiz.getResult(sequence);
            if(result.wasHintApplied()) {
                gameController.applyHint(prepared.hint);
                reviewHint = prepared.hint;
            } else applyAnswer(result.getAnswer());
        }
        restoreInteraction();
        if(completionPending) updateCompletionBehavior();
        if(!recapReview && sequence + 1 < quiz.getStartSequence() + TrainingQuiz.LENGTH) {
            requestPrefetch(sequence + 1);
        }
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
        GameHint.Action inputAction = gameController.getNoteStatus()
                ? GameHint.Action.REMOVE_CANDIDATES : GameHint.Action.PLACE_VALUE;
        if(inputAction != current.position.getAction()) {
            markFailure();
            field.showCandidateFeedback(row, col, value, false);
            actionView.setText(current.position.getAction() == GameHint.Action.REMOVE_CANDIDATES
                    ? R.string.gym_use_notes : R.string.gym_use_entry);
            return;
        }
        if(!validTargets.contains(answer)) {
            markFailure();
            field.showCandidateFeedback(row, col, value, false);
            actionView.setText(R.string.gym_incorrect);
            return;
        }

        applyAnswer(answer);
        completeCurrent(answer, false);
    }

    private void applyAnswer(TrainingTarget answer) {
        int row = answer.getRow(), col = answer.getCol(), value = answer.getValue();
        if(current.position.getAction() == GameHint.Action.PLACE_VALUE) {
            gameController.setValue(row, col, value);
        } else {
            gameController.deleteNote(row, col, value);
            field.showCandidateFeedback(row, col, value, true);
        }
        gameController.notifyHighlightChangedListeners();
    }

    private void beginAttempt() {
        if(attemptStarted) return;
        attemptStarted = true;
        statsRepository.recordAttempt(technique);
        updateQuizScore();
    }

    private void markFailure() {
        if(!eligible) return;
        eligible = false;
        statsRepository.recordFailure(technique);
        updateQuizScore();
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
                    statsRepository.recordReveal(technique);
                }
                markFailure();
                updateQuizScore();
            }

            @Override
            public void onHintApplied() {
                gameController.notifyHighlightChangedListeners();
                reviewHint = current.hint;
                completeCurrent(new TrainingTarget(current.hint.getRow(),
                        current.hint.getCol(), current.hint.getValue()), true);
            }

            @Override
            public void onHintDismissed() {
                // Start review after the old dialog has cleared its overlay.
                if(completionPending) updateCompletionBehavior();
            }
        });
        hintDialog.show(initialPage);
    }

    private void completeCurrent(TrainingTarget answer, boolean hintApplied) {
        if(completionPending) return;
        completionPending = true;
        TrainingQuiz.Outcome outcome = revealRecorded ? TrainingQuiz.Outcome.ASSISTED
                : eligible ? TrainingQuiz.Outcome.FIRST_TRY : TrainingQuiz.Outcome.AFTER_ERRORS;
        quiz.record(sequence, new TrainingQuiz.Result(outcome, answer, hintApplied));
        if(eligible) statsRepository.recordCorrect(technique);
        updateQuizScore();
        reviewRequested = reviewSwitch.isChecked();
        reviewPage = -1;
        updateCompletionBehavior();
    }

    private void updateReviewDescription() {
        reviewSwitch.setText(reviewSwitch.isChecked()
                ? R.string.gym_advance_review : R.string.gym_advance_auto);
        reviewSwitch.setContentDescription(getString(reviewSwitch.isChecked()
                ? R.string.gym_review_description : R.string.gym_auto_description));
    }

    private void updateCompletionBehavior() {
        mHandler.removeCallbacks(advanceRunnable);
        if(current == null || !completionPending || destroyed || resultsVisible) return;
        boolean review = recapReview || reviewSwitch.isChecked();
        actionView.setText(outcomeLabel(quiz.getResult(sequence).getOutcome()));
        keyboard.setButtonsEnabled(false);
        specialButtons.setButtonsEnabled(false);
        hintButton.setEnabled(review);
        hintButton.setContentDescription(getString(R.string.gym_review_answer));
        nextQuestion.setText(continueLabel());
        nextQuestion.setVisibility(review ? View.VISIBLE : View.GONE);
        if(review) {
            if(reviewRequested && resumed) showReview();
        } else {
            dismissHint();
            if(resumed) mHandler.postDelayed(advanceRunnable, NEXT_DELAY_MS);
        }
    }

    private void advance() {
        if(!completionPending || destroyed || resultsVisible) return;
        mHandler.removeCallbacks(advanceRunnable);
        dismissHint();
        if(recapReview || quiz.isComplete()) {
            showResults();
            return;
        }
        sequence = quiz.getNextSequence();
        current = null;
        attemptStarted = false;
        eligible = true;
        revealRecorded = false;
        completionPending = false;
        reviewRequested = false;
        reviewPage = -1;
        reviewHint = null;
        pendingInteraction = null;
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
        updateQuizScore();
        findViewById(R.id.gymQuizResults).setVisibility(View.GONE);
        findViewById(R.id.gymGameContent).setVisibility(View.VISIBLE);
        findViewById(R.id.gymDrillHeader).setVisibility(View.VISIBLE);
        nextQuestion.setVisibility(View.GONE);
        actionView.setText(R.string.gym_loading);
        specialButtons.setButtonsEnabled(false);
        if(keyboardReady) keyboard.setButtonsEnabled(false);
    }

    private int continueLabel() {
        if(recapReview) return R.string.gym_back_to_results;
        return quiz.isComplete() ? R.string.gym_show_results : R.string.gym_next_question;
    }

    private void showReview() {
        if(!resumed || destroyed || current == null || !completionPending || resultsVisible
                || !reviewRequested || (hintDialog != null && hintDialog.isShowing())) return;
        if(reviewHint == null) {
            if(reviewProofRequested == current) return;
            PreparedDrill source = current;
            reviewProofRequested = source;
            TrainingTarget answer = quiz.getResult(sequence).getAnswer();
            actionView.setText(R.string.gym_explanation_loading);
            executor.execute(() -> {
                try {
                    TrainingPosition position = source.position;
                    GameHint explanation = HumanHintEngine.findTechnique(GameType.Default_9x9,
                            position.getValues(), position.getCandidateMasks(), position.getSolution(),
                            source.symbols, technique,
                            new GameHint.Candidate(answer.getRow(), answer.getCol(), answer.getValue()));
                    if(explanation == null) {
                        throw new IllegalStateException("Accepted Gym answer has no technique proof.");
                    }
                    runOnUiThread(() -> {
                        if(destroyed || current != source || !completionPending) return;
                        reviewProofRequested = null;
                        reviewHint = explanation;
                        if(recapReview || reviewSwitch.isChecked()) updateCompletionBehavior();
                    });
                } catch(RuntimeException failure) {
                    Log.e(TAG, "Could not explain Gym answer", failure);
                    runOnUiThread(() -> {
                        if(destroyed || current != source) return;
                        reviewProofRequested = null;
                        reviewRequested = false;
                        actionView.setText(R.string.gym_explanation_error);
                    });
                }
            });
            return;
        }
        hintDialog = HumanHintDialog.forReview(this, gameController, reviewHint, continueLabel(),
                this::advance, () -> {
                    reviewRequested = false;
                    reviewPage = -1;
                });
        hintDialog.show(reviewPage);
    }

    private void dismissHint() {
        if(hintDialog != null) {
            hintDialog.dismiss();
            hintDialog = null;
        }
    }

    private void showResults() {
        mHandler.removeCallbacks(advanceRunnable);
        dismissHint();
        resultsVisible = true;
        recapReview = false;
        completionPending = false;
        reviewRequested = false;
        pendingInteraction = null;
        current = null;
        prefetched = null;
        reviewHint = null;
        validTargets.clear();
        findViewById(R.id.gymGameContent).setVisibility(View.GONE);
        findViewById(R.id.gymDrillHeader).setVisibility(View.GONE);
        findViewById(R.id.gymQuizResults).setVisibility(View.VISIBLE);
        toolbar.setSubtitle(R.string.gym_quiz_complete);
        ((TextView) findViewById(R.id.gymQuizScore)).setText(getString(R.string.gym_quiz_score,
                quiz.count(TrainingQuiz.Outcome.FIRST_TRY), TrainingQuiz.LENGTH));
        ((TextView) findViewById(R.id.gymQuizBreakdown)).setText(getString(R.string.gym_quiz_breakdown,
                quiz.count(TrainingQuiz.Outcome.FIRST_TRY), quiz.count(TrainingQuiz.Outcome.AFTER_ERRORS),
                quiz.count(TrainingQuiz.Outcome.ASSISTED)));
        LinearLayout questions = findViewById(R.id.gymQuizQuestions);
        questions.removeAllViews();
        for(int index = 0; index < quiz.getCompletedCount(); index++) {
            long questionSequence = quiz.getStartSequence() + index;
            Button question = new Button(this);
            question.setText(getString(R.string.gym_question_result, index + 1,
                    getString(outcomeLabel(quiz.getResult(questionSequence).getOutcome()))));
            question.setOnClickListener(view -> reviewQuestion(questionSequence));
            questions.addView(question, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private void reviewQuestion(long questionSequence) {
        resultsVisible = false;
        recapReview = true;
        sequence = questionSequence;
        current = null;
        prefetched = null;
        pendingInteraction = null;
        reviewRequested = true;
        reviewPage = -1;
        showLoading();
        requestPosition(sequence);
    }

    private void startAnotherQuiz() {
        quiz = new TrainingQuiz(quiz.getStartSequence() + TrainingQuiz.LENGTH);
        sequence = quiz.getStartSequence();
        resultsVisible = false;
        recapReview = false;
        completionPending = false;
        attemptStarted = false;
        eligible = true;
        revealRecorded = false;
        reviewRequested = false;
        pendingInteraction = null;
        showLoading();
        requestPosition(sequence);
    }

    private static int outcomeLabel(TrainingQuiz.Outcome outcome) {
        switch(outcome) {
            case FIRST_TRY: return R.string.gym_result_first_try;
            case AFTER_ERRORS: return R.string.gym_result_after_errors;
            case ASSISTED: return R.string.gym_result_assisted;
            default: throw new IllegalArgumentException("Unknown quiz outcome.");
        }
    }

    private void showCorpusError() {
        if(destroyed) return;
        Toast.makeText(this, R.string.gym_corpus_error, Toast.LENGTH_LONG).show();
        finish();
    }

    private void updateQuizScore() {
        int attempted = quiz.getCompletedCount();
        // A wrong answer or hint scores the active question once, even before completion.
        if(attemptStarted && !quiz.hasResult(sequence)) attempted++;
        if(attempted == 0) {
            statsView.setText(R.string.gym_quiz_stats_empty);
            return;
        }
        int correct = quiz.count(TrainingQuiz.Outcome.FIRST_TRY);
        statsView.setText(getString(R.string.gym_quiz_stats_format,
                Math.round(correct * 100f / attempted), correct, attempted));
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
