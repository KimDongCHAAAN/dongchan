package com.Yproject.dailyw.ui.dashboard;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.Yproject.dailyw.R;
import com.Yproject.dailyw.databinding.FragmentDashboardBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private LinearLayout linearWeekdays;
    private GridLayout timeGrid;
    private SharedPreferences sharedPreferences;
    private String selectedWeekday = "";
    private String[] weekdays = {"일", "월", "화", "수", "목", "금", "토"};

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        linearWeekdays = root.findViewById(R.id.linearWeekdays);

        timeGrid = root.findViewById(R.id.timegrid);

        sharedPreferences = getContext().getSharedPreferences("ScheduleData", MODE_PRIVATE);

        selectedWeekday = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1);

        for (int i = 0; i < weekdays.length; i++) {
            Button dayButton = new Button(getContext());
            dayButton.setText(weekdays[i]);
            dayButton.setTag(i);
            dayButton.setTextSize(20f);
            dayButton.setTextColor(Color.WHITE);
            dayButton.setBackgroundResource(R.drawable.noset_button);
            dayButton.setOnClickListener(v -> toggleWeekday((int) v.getTag(), dayButton));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(12, 20, 12, 150);
            dayButton.setLayoutParams(params);

            if (String.valueOf(i).equals(selectedWeekday)) {
                dayButton.setBackgroundResource(R.drawable.set_button);
                showTimes(String.valueOf(i));
            }

            linearWeekdays.addView(dayButton);
        }

        return root;
    }

    private void toggleWeekday(int index, Button button) {
        if (!String.valueOf(index).equals(selectedWeekday)) {
            Button previousButton = (Button) linearWeekdays.getChildAt(Integer.parseInt(selectedWeekday));
            previousButton.setBackgroundResource(R.drawable.noset_button);

            button.setBackgroundResource(R.drawable.set_button);
            selectedWeekday = String.valueOf(index);

            showTimes(selectedWeekday);
        }
    }

    private void showTimes(String dayIndex) {
        timeGrid.removeAllViews();

        Set<String> timeSet = sharedPreferences.getStringSet(dayIndex, new HashSet<>());

        List<String> timeList = new ArrayList<>(timeSet);
        timeList.sort(String::compareTo);

        for (String time : timeList) {
            View timeItemView = LayoutInflater.from(getContext()).inflate(R.layout.time_item, null);
            TextView timeTextView = timeItemView.findViewById(R.id.timeText);
            ImageButton deleteButton = timeItemView.findViewById(R.id.deleteButton);

            timeTextView.setText(time);
            timeTextView.setTextSize(40f);

            deleteButton.setOnClickListener(v -> {
                Set<String> updatedTimeSet = sharedPreferences.getStringSet(dayIndex, new HashSet<>());

                if (updatedTimeSet.contains(time)) {
                    updatedTimeSet.remove(time);

                    sharedPreferences.edit().remove(dayIndex).apply();
                    sharedPreferences.edit().putStringSet(dayIndex, updatedTimeSet).apply();

                    timeGrid.removeView(timeItemView);
                }
            });

            timeGrid.addView(timeItemView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}