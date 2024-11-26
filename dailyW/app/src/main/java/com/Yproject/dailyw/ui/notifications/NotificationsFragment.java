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

// 알람을 설정하는 화면
public class NotificationsFragment extends Fragment {
    private TextView tvTime;
    private Button btnTimePicker, btnSet;
    private LinearLayout linearWeekdays;
    private List<Integer> selectedWeekdays = new ArrayList<>();
    private Calendar calendar = Calendar.getInstance();
    private SharedPreferences sharedPreferences;
    private String timeStr;

    // 요일을 인덱스로 사용하기위해 배열로 초기화
    private String[] weekdays = {"일", "월", "화", "수", "목", "금", "토"};

    private FragmentNotificationsBinding binding;

    // 실질적으로 랜더링 하는 메소드
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 시간을 설정할 버튼과 요일을 선택하기 위한 버튼 Layout
        tvTime = root.findViewById(R.id.btnTimePicker);
        btnTimePicker = root.findViewById(R.id.btnTimePicker);
        btnSet = root.findViewById(R.id.btnSet);
        linearWeekdays = root.findViewById(R.id.linearWeekdays);

        sharedPreferences = getContext().getSharedPreferences("ScheduleData", MODE_PRIVATE);

        // 요일을 선택하기 위해 순회하며 버튼을 만듦
        for (int i = 0; i < weekdays.length; i++) {
            Button dayButton = getButton(i);

            linearWeekdays.addView(dayButton);
        }

        // 설정 버튼이 눌렸을때 실행할 기능을 등록
        btnTimePicker.setOnClickListener(v -> showTimePicker());
        btnSet.setOnClickListener(v -> {
            saveSchedule();  // 로컬에 데이터 저장 밑 백그라운드 작업(알람 매니저를 통해 백그라운드 정책을 회피하여 실행) 등록

            resetWeekdaysSelection();  // 선택한 버튼을 처음 화면으로 초기화

            resetTimePickerButton();  // 선택한 시간을 초기 화면으로 초기화
        });

        return root;
    }

    // 각 요일 버튼의 스타일을 설정할 메소드
    @NonNull
    private Button getButton(int i) {
        Button dayButton = new Button(getContext());
        dayButton.setText(weekdays[i]);
        dayButton.setTag(i);
        dayButton.setTextSize(20f);
        dayButton.setTextColor(Color.WHITE);
        dayButton.setBackgroundResource(R.drawable.noset_button);
        dayButton.setOnClickListener(v -> toggleWeekday((int) v.getTag(), dayButton));  // 버튼이 눌렸을때 실행할 기능을 등록

        //Layout 설정
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(12, 20, 12, 20);  // 여백 설정

        // 설정 적용
        dayButton.setLayoutParams(params);
        return dayButton;
    }

    // 버튼이 눌렸을때 색상을 바꾸기 위한 메소드
    private void toggleWeekday(int index, Button button) {
        // 선택한 버튼의 index가 배열에 존재할때
        if (selectedWeekdays.contains(index)) {
            selectedWeekdays.remove((Integer) index);  // 배열에서 인덱스 제거
            button.setBackgroundResource(R.drawable.noset_button);  // 버튼의 색상을 선택 안한 색상으로 바꿈
        } else {
            selectedWeekdays.add(index);  // 배열에 인덱스 추가
            button.setBackgroundResource(R.drawable.set_button);  // 버튼의 색상을 선택한 색상ㅇ로 바꿈
        }
    }

    // 요일 버튼들을 초기로 되돌리기 위한 메소드
    private void resetWeekdaysSelection() {
        selectedWeekdays.clear();   // 배열 모두 삭제(공배열로 만듦)

        // 모든 버튼들의 색상을 선택 안함으로 설정
        for (int i = 0; i < linearWeekdays.getChildCount(); i++) {
            View child = linearWeekdays.getChildAt(i);
            if (child instanceof Button) {
                ((Button) child).setBackgroundResource(R.drawable.noset_button);
            }
        }
    }

    // 선택한 시간을 초기로 되돌리기
    private void resetTimePickerButton() {
        timeStr = null; // 선택된 시간 초기화
        btnTimePicker.setText("시간 선택");
    }

    // 시간 선택 버튼을 누르면 띄워질 안드로이드 타임피커 설정
    private void showTimePicker() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);  // 시간을 현재 시간으로 설정
        int minute = calendar.get(Calendar.MINUTE);     // 분을 현재 분으로 설정

        // 타임 피커 설정
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minuteOfHour) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);  // 타임 피커에 출력될 시간을 현재 시간으로 설정
                    calendar.set(Calendar.MINUTE, minuteOfHour);    // 타임 피커에 출력될 분을 현재 분으로 설정
                    timeStr = String.format("%02d:%02d", hourOfDay, minuteOfHour);  // 설정한 시간을 지정된 형식으로 변수에 할당
                    btnTimePicker.setText(timeStr);  // 설정한 시간으로 버튼의 텍스트 바꿈
                }, hour, minute, true);

        // 안드로이드 타임픽커 설정을 적용하여 출력
        timePickerDialog.show();
    }

    // 설정한 알람을 저장하고 백그라운드 작업을 등록하기 위한 메소드
    private void saveSchedule() {
        // 선택한 요일들이 있는 배열을 순회하며 각 요일에 맞게 시간을 로컬에 저장
        for(int day : selectedWeekdays) {
            Set<String> timeSet = sharedPreferences.getStringSet(String.valueOf(day), new HashSet<>());  // 기존에 해당하는 요일에 있는 데이터 가져옴
            Log.d("SharedPreferences", "Stored timeSet: " + day);

            timeSet.add(timeStr);  // 기존에 존재하는 데이터에 이어서 저장, 만일 데이터가 없으면 그냥 저장(set 데이터 구조를 이용하므로 중복된 시간은 저장하지 않음)

            sharedPreferences.edit().remove(String.valueOf(day)).apply();
            sharedPreferences.edit().putStringSet(String.valueOf(day), timeSet).apply();

            // 저장에 성공하면 보여줄 메세지 설정
            Toast.makeText(requireContext(), "Success", Toast.LENGTH_SHORT).show();

            try{
                // 백그라운드 작업(안드로이 알람 매니저) 등록
                scheduleAlarm(requireContext());
            } catch (Exception e) {
                Log.e("eeee", e.toString());
            }
        }
    }

    // 백그라운드에서 1분마다 현재 요일에 설정한 시간중 현재 시간과 일치하는지 확인하고 일치하는 시간이 있다면 백그라운드 작업을 실행시키기 위한 메소드
    public void scheduleAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, alarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // 알람 매니저를 등록하기까지 1분 소요(결론적으로 현재 알람 매소드를 등록하려 한다면 현재 시간 기준으로 1분 후에 등록되고 그 후부터 1분마다 실행)
        long interval = 60000;   // 실행할 시간 간격으로 1분 설정
        long startTime = System.currentTimeMillis() + interval;

        // 알람 매니저 설정
        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    startTime,
                    interval,
                    pendingIntent
            );
        }
    }

    // 현재 화면에 포커스가 취소됐을때 자원 회수를 위한 메소드
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}