/* Licensed under the GNU General Public License, version 3 or later. */
package org.secuso.privacyfriendlysudoku.ui.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;
import org.secuso.privacyfriendlysudoku.controller.training.TechniqueLesson;

/** Catalog help has no connection to quiz sessions or performance statistics. */
public final class TechniqueHelpDialog extends DialogFragment {
    public static final String TAG = "gym-technique-help";
    private static final String TECHNIQUE = "technique";

    public static void show(FragmentManager manager, HumanTechnique technique) {
        if(manager.findFragmentByTag(TAG) != null) return;
        TechniqueHelpDialog dialog = new TechniqueHelpDialog();
        Bundle arguments = new Bundle();
        arguments.putString(TECHNIQUE, technique.name());
        dialog.setArguments(arguments);
        dialog.show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        HumanTechnique technique = HumanTechnique.valueOf(requireArguments().getString(TECHNIQUE));
        TechniqueLesson lesson = TechniqueLesson.forTechnique(technique);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AppTheme_Dialog);
        View content = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.gym_technique_help, null);
        ((TechniqueDiagramView) content.findViewById(R.id.gymHelpDiagram)).setLesson(lesson);
        ((TextView) content.findViewById(R.id.gymHelpCaption)).setText(lesson.caption);
        ((TextView) content.findViewById(R.id.gymHelpLegend)).setText(lesson.links.isEmpty()
                ? R.string.gym_help_legend : R.string.gym_help_link_legend);
        LinearLayout steps = content.findViewById(R.id.gymHelpSteps);
        for(String step : getResources().getStringArray(lesson.steps)) {
            LinearLayout line = new LinearLayout(builder.getContext());
            line.setOrientation(LinearLayout.HORIZONTAL);
            TextView bullet = new TextView(builder.getContext());
            bullet.setText("•");
            bullet.setTextSize(16);
            line.addView(bullet, new LinearLayout.LayoutParams(dp(20), -2));
            TextView text = new TextView(builder.getContext());
            text.setText(step);
            text.setTextSize(16);
            text.setLineSpacing(dp(2), 1);
            text.setPadding(0, 0, 0, dp(10));
            line.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
            steps.addView(line);
        }
        return builder.setTitle(technique.getTitle()).setView(content)
                .setPositiveButton(R.string.gym_help_close, null).create();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
