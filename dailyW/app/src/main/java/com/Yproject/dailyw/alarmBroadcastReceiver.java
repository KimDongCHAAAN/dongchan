package com.Yproject.dailyw;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.Yproject.dailyw.ui.notifications.backWork;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

// 알람 매니저가 등록된 후 실행 할 기능 (알람 매니저는 백그라운드에서 명시적으로 종료를 시키지 않는 이상 설정한 시간 간격으로 계속 실행됨)
public class alarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Calendar calendar = Calendar.getInstance();  // 현재 날짜와 시간을 포함하는 객체로 가져옴
        Date currentDate = calendar.getTime();  // 현재 시간을 객체로 가져옴

        // 현재 시간을 지정한 형식으로 변환시켜 변수에 할당
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String formattedTime = sdf.format(currentDate);

        SharedPreferences sharedPreferences = context.getSharedPreferences("ScheduleData", MODE_PRIVATE);

        // 로컬에서 현재 요일에 맞는 설정한 시간들을 가져옴
        Set<String> timeSet = sharedPreferences.getStringSet(String.valueOf(calendar.get(Calendar.DAY_OF_WEEK) - 1), new HashSet<>());

        Log.d("Good", String.valueOf(calendar.get(Calendar.DAY_OF_WEEK) - 1));

        // 만일 현재 시간이 이전에 저장해둔 시간들 안에 존재한다면 실행
        if(timeSet.contains(formattedTime)) {
            Log.d("Good", "" + timeSet);
            Log.d("Good", formattedTime);

            triggerWorkManager(context);  // 백그라운드 작업 실행
        }
    }

    // 백그라운드 작업을 실행시키기 위한 설정을 적용하는 메소드
    private void triggerWorkManager(Context context) {
        // 어떤 작업을 진행할건지 설정(블루투스 연결해서 통신하고 체중데이터를 로컬에 저장하는 작업, 작업이 실패하던 성공하던 1번만 실행하고 다음 작업을 예약하지 않는 1회성 작업으로 설정)
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(backWork.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
