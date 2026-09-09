/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStats;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingQuiz;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStatsRepository;
import org.secuso.privacyfriendlysudoku.ui.view.TechniqueHelpDialog;

import java.util.ArrayList;
import java.util.List;

/** Catalog for isolated human-technique drills. */
public class GymActivity extends BaseActivity {
    private TrainingStatsRepository statsRepository;
    private SkillAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(R.string.gym);
            actionBar.setSubtitle(getString(R.string.gym_quiz_length, TrainingQuiz.LENGTH));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        statsRepository = new TrainingStatsRepository(this);

        adapter = new SkillAdapter();
        ListView list = findViewById(R.id.gymSkillList);
        list.setAdapter(adapter);
    }

    private void startQuiz(HumanTechnique technique) {
        Intent intent = new Intent(this, GymDrillActivity.class);
        intent.putExtra(GymDrillActivity.EXTRA_TECHNIQUE, technique.name());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapter != null) {
            statsRepository = new TrainingStatsRepository(this);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gym, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if(item.getItemId() == R.id.action_reset_gym) {
            new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                    .setMessage(R.string.gym_reset_confirmation)
                    .setPositiveButton(R.string.reset_confirmation_confirm, (dialog, which) -> {
                        statsRepository.reset();
                        adapter.notifyDataSetChanged();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final class SkillAdapter extends BaseAdapter {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_SKILL = 1;
        private final List<CatalogEntry> entries = new ArrayList<>();
        private final LayoutInflater inflater = LayoutInflater.from(GymActivity.this);

        SkillAdapter() {
            int previousLevel = -1;
            for(HumanTechnique technique : HumanTechnique.values()) {
                if(technique.getBaseLevel() != previousLevel) {
                    previousLevel = technique.getBaseLevel();
                    entries.add(CatalogEntry.header(previousLevel));
                }
                entries.add(CatalogEntry.skill(technique));
            }
        }

        @Override public int getCount() { return entries.size(); }
        @Override public Object getItem(int position) { return entries.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int position) {
            return entries.get(position).technique == null ? TYPE_HEADER : TYPE_SKILL;
        }
        @Override public boolean isEnabled(int position) {
            return entries.get(position).technique != null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CatalogEntry entry = entries.get(position);
            if(entry.technique == null) {
                TextView header = (TextView) (convertView == null
                        ? inflater.inflate(R.layout.gym_level_header, parent, false) : convertView);
                header.setText(getString(R.string.gym_level_format, entry.level));
                return header;
            }

            View row = convertView == null
                    ? inflater.inflate(R.layout.gym_skill_row, parent, false) : convertView;
            row.setOnClickListener(view -> startQuiz(entry.technique));
            TextView title = row.findViewById(R.id.gymSkillTitle);
            TextView stats = row.findViewById(R.id.gymSkillStats);
            title.setText(entry.technique.getTitle());
            View help = row.findViewById(R.id.gymSkillHelp);
            help.setContentDescription(getString(R.string.gym_skill_help_description,
                    entry.technique.getTitle()));
            help.setOnClickListener(view -> TechniqueHelpDialog.show(
                    getSupportFragmentManager(), entry.technique));
            TrainingStats record = statsRepository.get(entry.technique);
            stats.setText(getString(R.string.gym_stats_row_format, record.getAttempts(),
                    record.getAccuracyPercent(), record.getReveals(), record.getCurrentStreak(),
                    record.getBestStreak()));
            return row;
        }
    }

    private static final class CatalogEntry {
        final int level;
        final HumanTechnique technique;

        private CatalogEntry(int level, HumanTechnique technique) {
            this.level = level;
            this.technique = technique;
        }

        static CatalogEntry header(int level) { return new CatalogEntry(level, null); }
        static CatalogEntry skill(HumanTechnique technique) {
            return new CatalogEntry(technique.getBaseLevel(), technique);
        }
    }
}
