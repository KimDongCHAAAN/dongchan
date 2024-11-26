package com.Yproject.dailyw.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.Yproject.dailyw.util.weightStructure;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class homeRepository {
    private SharedPreferences sharedPreferences;
    private Gson gson;

    // 홈 화면에서 사용될 데이터를 다루기 위한 클래스
    public homeRepository(Context context) {
        sharedPreferences = context.getSharedPreferences("WeightData", Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // 더미 데이터 세팅 및 적용
    public void setDummyData() {

        // 만일 이미 데이터가 만들어져 있다면 만들지 않음
        if (!getWeights("11").isEmpty()) {
            Log.d("test222", getWeights("11").toString());
            return;
        }

        List<weightStructure> weights = new ArrayList<>();

        // 2024년 11월 1일부터 24일까지의 데이터를 만듦
        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.NOVEMBER, 1);

        Random random = new Random();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // 1일부터 30일에 적용될 랜덤한 데이터 만듦
        for (int i = 0; i < 24; i++) {
            String currentDateStr = dateFormat.format(calendar.getTime());

            float randomFraction = (float) (random.nextInt(100) / 100.0); // 0.00부터 0.99까지 랜덤
            float weight = 70 + (random.nextFloat() * 4) + randomFraction;

            Date currentDate = calendar.getTime();

            weightStructure weightRecord = new weightStructure(weight, currentDate, currentDateStr);
            weights.add(weightRecord);

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 로컬에 저장
        String json = gson.toJson(weights);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("11", json);
        editor.apply();
    }

    //로컬에 저장된 월 데이터를 현재 월에 맞게 가져오는 메소드
    public List<weightStructure> getWeights(String month) {
        String json = sharedPreferences.getString(month, "[]");
        Type type = new TypeToken<List<weightStructure>>(){}.getType();
        List<weightStructure> weights = gson.fromJson(json, type);

        weights.sort(Comparator.comparing(weightStructure::getDate));  // 로컬에 저장된 데이터를 날짜를 기준으로 오름차순으로 정렬

        return weights;
    }
}
