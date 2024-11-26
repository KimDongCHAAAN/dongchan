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

    public homeRepository(Context context) {
        sharedPreferences = context.getSharedPreferences("WeightData", Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void setDummyData() {
        if (!getWeights("11").isEmpty()) {
            Log.d("test222", getWeights("11").toString());
            return;
        }

        List<weightStructure> weights = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.NOVEMBER, 1);

        Random random = new Random();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < 24; i++) {
            String currentDateStr = dateFormat.format(calendar.getTime());

            float randomFraction = (float) (random.nextInt(100) / 100.0); // 0.00부터 0.99까지 랜덤
            float weight = 70 + (random.nextFloat() * 4) + randomFraction;

            Date currentDate = calendar.getTime();

            weightStructure weightRecord = new weightStructure(weight, currentDate, currentDateStr);
            weights.add(weightRecord);

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        String json = gson.toJson(weights);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("11", json);
        editor.apply();
    }

    public List<weightStructure> getWeights(String month) {
        String json = sharedPreferences.getString(month, "[]");
        Type type = new TypeToken<List<weightStructure>>(){}.getType();
        List<weightStructure> weights = gson.fromJson(json, type);

        weights.sort(Comparator.comparing(weightStructure::getDate));

        return weights;
    }
}
