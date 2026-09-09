/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.ui.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.GameController;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;

import java.util.List;

/**
 * Explanation-first hint dialog with a persistent Apply action on the left and paged
 * Prev/Next navigation on the right. Tapping outside dismisses the explanation.
 */
public final class HumanHintDialog {

    public interface Listener {
        void onHintOpened();
        void onHintApplied();
        default void onHintDismissed() {}
    }

    private final Context context;
    private final GameController gameController;
    private final GameHint hint;
    private final Listener listener;
    private final Runnable reviewContinue;
    private final Runnable reviewDismiss;
    private final int reviewContinueLabel;
    private AlertDialog dialog;
    private int detailIndex = -1;

    public HumanHintDialog(Context context, GameController gameController, GameHint hint) {
        this(context, gameController, hint, null);
    }

    public HumanHintDialog(Context context, GameController gameController, GameHint hint,
                           Listener listener) {
        this(context, gameController, hint, listener, 0, null, null);
    }

    private HumanHintDialog(Context context, GameController gameController, GameHint hint,
                            Listener listener, int reviewContinueLabel,
                            Runnable reviewContinue, Runnable reviewDismiss) {
        this.context = context;
        this.gameController = gameController;
        this.hint = hint;
        this.listener = listener;
        this.reviewContinueLabel = reviewContinueLabel;
        this.reviewContinue = reviewContinue;
        this.reviewDismiss = reviewDismiss;
    }

    public static HumanHintDialog forReview(Context context, GameController controller,
                                           GameHint hint, int continueLabel,
                                           Runnable onContinue, Runnable onDismiss) {
        return new HumanHintDialog(context, controller, hint, null, continueLabel,
                onContinue, onDismiss);
    }

    public void show() {
        show(-1);
    }

    /** Reopen an explanation at its saved page; -1 is the summary. */
    public void show(int initialDetailIndex) {
        dialog = new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setTitle(hint.getTitle())
                .setMessage(hint.getSummary())
                .setPositiveButton(R.string.hint_next, null)
                .setNeutralButton(R.string.hint_apply, null)
                .setNegativeButton(R.string.hint_prev, null)
                .create();

        dialog.setCanceledOnTouchOutside(true);
        configureWindow();
        dialog.setOnDismissListener(ignored -> {
            gameController.endHint();
            if(listener != null) listener.onHintDismissed();
            if(reviewDismiss != null) reviewDismiss.run();
        });
        dialog.setOnShowListener(ignored -> {
            gameController.beginHint(hint, reviewContinue == null);
            if(listener != null) listener.onHintOpened();
            configureWindow();
            if(initialDetailIndex >= 0 && initialDetailIndex < hint.getDetails().size()) {
                showDetail(initialDetailIndex);
            } else {
                showSummary();
            }
        });
        dialog.show();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public int getDetailIndex() {
        return detailIndex;
    }

    /** Release the window and board overlay when its owning activity is destroyed. */
    public void dismiss() {
        if(dialog != null) dialog.dismiss();
    }

    private void configureWindow() {
        Window window = dialog.getWindow();
        if(window == null) {
            return;
        }
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = 0.35f;
        attributes.gravity = Gravity.BOTTOM;
        window.setAttributes(attributes);
    }

    private void showSummary() {
        detailIndex = -1;
        gameController.showHintFrame(-1);
        dialog.setTitle(hint.getTitle());
        dialog.setMessage(hint.getSummary());

        configurePrimaryAction();

        Button previous = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        previous.setVisibility(View.GONE);

        Button next = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if(hint.getDetails().isEmpty()) {
            next.setVisibility(View.GONE);
        } else {
            next.setVisibility(View.VISIBLE);
            next.setText(R.string.hint_next);
            next.setOnClickListener(view -> showDetail(0));
        }
    }

    private void showDetail(int index) {
        List<String> details = hint.getDetails();
        detailIndex = index;
        gameController.showHintFrame(detailIndex);
        dialog.setTitle(context.getString(R.string.hint_detail_title,
                hint.getTitle(), detailIndex + 1, details.size()));
        dialog.setMessage(details.get(detailIndex));

        configurePrimaryAction();

        Button next = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        next.setText(R.string.hint_next);
        if(detailIndex < details.size() - 1) {
            next.setVisibility(View.VISIBLE);
            next.setOnClickListener(view -> showDetail(detailIndex + 1));
        } else {
            next.setVisibility(View.GONE);
        }

        Button previous = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        previous.setVisibility(View.VISIBLE);
        previous.setText(R.string.hint_prev);
        previous.setOnClickListener(view -> {
            if(detailIndex == 0) {
                showSummary();
            } else {
                showDetail(detailIndex - 1);
            }
        });
    }

    private void configurePrimaryAction() {
        Button primary = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        primary.setVisibility(View.VISIBLE);
        primary.setText(reviewContinue == null ? R.string.hint_apply : reviewContinueLabel);
        primary.setOnClickListener(view -> {
            if(reviewContinue == null) {
                applyHint();
            } else {
                dialog.dismiss();
                reviewContinue.run();
            }
        });
    }

    private void applyHint() {
        gameController.applyHint(hint);
        if(listener != null) listener.onHintApplied();
        dialog.dismiss();
    }
}
