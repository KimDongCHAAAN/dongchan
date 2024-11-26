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

public class alarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String formattedTime = sdf.format(currentDate);

        SharedPreferences sharedPreferences = context.getSharedPreferences("ScheduleData", MODE_PRIVATE);

        Set<String> timeSet = sharedPreferences.getStringSet(String.valueOf(calendar.get(Calendar.DAY_OF_WEEK) - 1), new HashSet<>());

        Log.d("Good", String.valueOf(calendar.get(Calendar.DAY_OF_WEEK) - 1));
        if(timeSet.contains(formattedTime)) {
            Log.d("Good", "" + timeSet);
            Log.d("Good", formattedTime);

            triggerWorkManager(context);
        }
    }

    private void triggerWorkManager(Context context) {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(backWork.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
