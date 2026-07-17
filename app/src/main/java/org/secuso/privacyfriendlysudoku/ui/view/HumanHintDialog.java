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

    private final Context context;
    private final GameController gameController;
    private final GameHint hint;
    private AlertDialog dialog;
    private int detailIndex = -1;

    public HumanHintDialog(Context context, GameController gameController, GameHint hint) {
        this.context = context;
        this.gameController = gameController;
        this.hint = hint;
    }

    public void show() {
        dialog = new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setTitle(hint.getTitle())
                .setMessage(hint.getSummary())
                .setPositiveButton(R.string.hint_next, null)
                .setNeutralButton(R.string.hint_apply, null)
                .setNegativeButton(R.string.hint_prev, null)
                .create();

        dialog.setCanceledOnTouchOutside(true);
        configureWindow();
        dialog.setOnDismissListener(ignored -> gameController.endHint());
        dialog.setOnShowListener(ignored -> {
            gameController.beginHint(hint);
            configureWindow();
            showSummary();
        });
        dialog.show();
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

        Button apply = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        apply.setVisibility(View.VISIBLE);
        apply.setText(R.string.hint_apply);
        apply.setOnClickListener(view -> applyHint());

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

        Button apply = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        apply.setVisibility(View.VISIBLE);
        apply.setText(R.string.hint_apply);
        apply.setOnClickListener(view -> applyHint());

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

    private void applyHint() {
        gameController.applyHint(hint);
        dialog.dismiss();
    }
}
