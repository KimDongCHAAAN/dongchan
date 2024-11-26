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

// 설정한 알람을 요일별로 확인하고 삭제하기 위한 화면
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private LinearLayout linearWeekdays;
    private GridLayout timeGrid;
    private SharedPreferences sharedPreferences;
    private String selectedWeekday = "";

    // 요일을 인덱스로 사용하기위해 배열로 초기화
    private String[] weekdays = {"일", "월", "화", "수", "목", "금", "토"};

    // 실질적으로 랜더링 하는 메소드
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 요일을 선택하기 위한 버튼 Layout
        linearWeekdays = root.findViewById(R.id.linearWeekdays);

        // 설정한 시간을 보여줄 Layout
        timeGrid = root.findViewById(R.id.timegrid);

        sharedPreferences = getContext().getSharedPreferences("ScheduleData", MODE_PRIVATE);

        // 현재 요일이나 선택한 요일의 정보를 유지하기 위한 변수
        selectedWeekday = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1);

        // 요일을 선택할수 있는 버튼 설정
        for (int i = 0; i < weekdays.length; i++) {
            Button dayButton = new Button(getContext());
            dayButton.setText(weekdays[i]);
            dayButton.setTag(i);  // 각 요일 버튼에 할당될 데이터 (버튼을 눌렀을때 어플리케이션이 해당하는 데이터를 처리하도록 하기위해 설정)
            dayButton.setTextSize(20f);
            dayButton.setTextColor(Color.WHITE);
            dayButton.setBackgroundResource(R.drawable.noset_button);
            dayButton.setOnClickListener(v -> toggleWeekday((int) v.getTag(), dayButton));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(12, 20, 12, 150);
            dayButton.setLayoutParams(params);

            // 앱을 실행했을때 아무런 요일 버튼을 안눌렀어도 현재 요일에 맞게 데이터와 버튼 토글을 설정
            if (String.valueOf(i).equals(selectedWeekday)) {
                dayButton.setBackgroundResource(R.drawable.set_button);
                showTimes(String.valueOf(i));
            }

            // 설정 적용
            linearWeekdays.addView(dayButton);
        }

        return root;
    }

    // 선택한 요일 버튼을 지속적으로 다른색으로 출력하기 위한 메소드
    private void toggleWeekday(int index, Button button) {

        // 만일 선택한 요일 버튼의 index(데이터)가 이전에 선택한 버튼과 다를경우 실행
        if (!String.valueOf(index).equals(selectedWeekday)) {

            // 이전에 선택한 버튼의 색상을 선택 안한 색상으로 바꿈
            Button previousButton = (Button) linearWeekdays.getChildAt(Integer.parseInt(selectedWeekday));
            previousButton.setBackgroundResource(R.drawable.noset_button);

            // 선택한 버튼의 색상을 선택한 색상으로 바꿈
            button.setBackgroundResource(R.drawable.set_button);
            selectedWeekday = String.valueOf(index);

            // 선택한 요일에 맞는 데이터 가져와서 출력
            showTimes(selectedWeekday);
        }
    }

    //선택한 요일에 맞는 데이터를 로컬에서 가져와 출력
    private void showTimes(String dayIndex) {
        // 이전에 출력된 시간을 삭제
        timeGrid.removeAllViews();

        // 선택한 요일에 맞는 시간들을 가져옴 (Set : 중복을 허용하지 않는 배열구조의 데이터구조)
        Set<String> timeSet = sharedPreferences.getStringSet(dayIndex, new HashSet<>());

        // 가져온 시간 데이터들을 사용하기 쉽게 List 데이터 구조로 변환
        List<String> timeList = new ArrayList<>(timeSet);

        // 시간들을 오름 차순으로 정렬
        timeList.sort(String::compareTo);

        // 각각의 시간들을 출력하기 위해 순회하며 설정
        for (String time : timeList) {
            View timeItemView = LayoutInflater.from(getContext()).inflate(R.layout.time_item, null);
            TextView timeTextView = timeItemView.findViewById(R.id.timeText);
            ImageButton deleteButton = timeItemView.findViewById(R.id.deleteButton);

            timeTextView.setText(time);
            timeTextView.setTextSize(40f);

            // 삭제 버튼이 눌렸을때 실행할 기능 등록
            deleteButton.setOnClickListener(v -> {

                // 로컬에서 데이터 가져옴
                Set<String> updatedTimeSet = sharedPreferences.getStringSet(dayIndex, new HashSet<>());

                // 삭제를 원하는 시간이 존재 한다면 삭제 진행
                if (updatedTimeSet.contains(time)) {
                    updatedTimeSet.remove(time);

                    // 로컬에 데이터 지우고 다시저장
                    sharedPreferences.edit().remove(dayIndex).apply();
                    sharedPreferences.edit().putStringSet(dayIndex, updatedTimeSet).apply();

                    // 삭제된 상태를 화면에 반영하기 위해
                    timeGrid.removeView(timeItemView);
                }
            });

            timeGrid.addView(timeItemView);
        }
    }

    // 현재 화면에 포커스가 취소됐을때 자원 회수를 위한 메소드
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}