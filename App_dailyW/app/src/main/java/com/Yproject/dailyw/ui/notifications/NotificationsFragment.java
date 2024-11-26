package com.Yproject.dailyw.ui.notifications;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.Yproject.dailyw.R;
import com.Yproject.dailyw.alarmBroadcastReceiver;
import com.Yproject.dailyw.databinding.FragmentNotificationsBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationsFragment extends Fragment {
    private TextView tvTime;
    private Button btnTimePicker, btnSet;
    private LinearLayout linearWeekdays;
    private List<Integer> selectedWeekdays = new ArrayList<>();
    private Calendar calendar = Calendar.getInstance();
    private SharedPreferences sharedPreferences;
    private String timeStr;

    private String[] weekdays = {"일", "월", "화", "수", "목", "금", "토"};

    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        tvTime = root.findViewById(R.id.btnTimePicker);
        btnTimePicker = root.findViewById(R.id.btnTimePicker);
        btnSet = root.findViewById(R.id.btnSet);
        linearWeekdays = root.findViewById(R.id.linearWeekdays);

        sharedPreferences = getContext().getSharedPreferences("ScheduleData", MODE_PRIVATE);

        for (int i = 0; i < weekdays.length; i++) {
            Button dayButton = getButton(i);

            linearWeekdays.addView(dayButton);
        }

        btnTimePicker.setOnClickListener(v -> showTimePicker());
        btnSet.setOnClickListener(v -> {
            saveSchedule();

            resetWeekdaysSelection();

            resetTimePickerButton();
        });

        return root;
    }

    @NonNull
    private Button getButton(int i) {
        Button dayButton = new Button(getContext());
        dayButton.setText(weekdays[i]);
        dayButton.setTag(i);
        dayButton.setTextSize(20f);
        dayButton.setTextColor(Color.WHITE);
        dayButton.setBackgroundResource(R.drawable.noset_button);
        dayButton.setOnClickListener(v -> toggleWeekday((int) v.getTag(), dayButton));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(12, 20, 12, 20);  // 여백 설정

        dayButton.setLayoutParams(params);
        return dayButton;
    }

    private void toggleWeekday(int index, Button button) {
        if (selectedWeekdays.contains(index)) {
            selectedWeekdays.remove((Integer) index);
            button.setBackgroundResource(R.drawable.noset_button);
        } else {
            selectedWeekdays.add(index);
            button.setBackgroundResource(R.drawable.set_button);
        }
    }

    private void resetWeekdaysSelection() {
        selectedWeekdays.clear();

        for (int i = 0; i < linearWeekdays.getChildCount(); i++) {
            View child = linearWeekdays.getChildAt(i);
            if (child instanceof Button) {
                ((Button) child).setBackgroundResource(R.drawable.noset_button);
            }
        }
    }

    private void resetTimePickerButton() {
        timeStr = null; // 선택된 시간 초기화
        btnTimePicker.setText("시간 선택");
    }

    private void showTimePicker() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minuteOfHour) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minuteOfHour);
                    timeStr = String.format("%02d:%02d", hourOfDay, minuteOfHour);
                    btnTimePicker.setText(timeStr);
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void saveSchedule() {
        for(int day : selectedWeekdays) {
            Set<String> timeSet = sharedPreferences.getStringSet(String.valueOf(day), new HashSet<>());
            Log.d("SharedPreferences", "Stored timeSet: " + day);

            timeSet.add(timeStr);

            sharedPreferences.edit().remove(String.valueOf(day)).apply();
            sharedPreferences.edit().putStringSet(String.valueOf(day), timeSet).apply();

            Toast.makeText(requireContext(), "Success", Toast.LENGTH_SHORT).show();

            try{
                scheduleAlarm(requireContext());
            } catch (Exception e) {
                Log.e("eeee", e.toString());
            }
        }
    }

    public void scheduleAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, alarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        long interval = 60000;
        long startTime = System.currentTimeMillis() + interval;

        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    startTime,
                    interval,
                    pendingIntent
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}