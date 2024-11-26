package com.Yproject.dailyw.ui.home;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.Yproject.dailyw.R;
import com.Yproject.dailyw.databinding.FragmentHomeBinding;
import com.Yproject.dailyw.util.weightStructure;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private LineChart lineChart;
    private SharedPreferences sharedPreferences;
    private Calendar calendar;
    private Gson gson;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        lineChart = root.findViewById(R.id.lineChart);
        gson = new Gson();
        calendar = Calendar.getInstance();
        homeRepository repo = new homeRepository(requireContext());

        repo.setDummyData();

        List<weightStructure> weights = repo.getWeights(String.valueOf(calendar.get(Calendar.MONTH) + 1));
        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        for (int i = 0; i < weights.size(); i++) {
            weightStructure weight = weights.get(i);
            entries.add(new Entry(i, weight.getWeight()));
            xLabels.add(weight.getDateStr());
        }


        LineDataSet dataSet = new LineDataSet(entries, "Weight");
        dataSet.setColor(Color.WHITE);
        dataSet.setDrawValues(false);
        dataSet.setCircleColor(Color.WHITE);
        dataSet.setCircleRadius(8f);
        dataSet.setHighLightColor(Color.TRANSPARENT);


        LineData lineData = new LineData(dataSet);
        TextView valueTextView = root.findViewById(R.id.valueTextView);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = sdf.format(new Date());

        valueTextView.setText(currentDate);

        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                double value = e.getY();

                String text = String.format(Locale.ROOT, "%.2f", value);
                valueTextView.setText(text);
            }

            @Override
            public void onNothingSelected() {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String currentDate = sdf.format(new Date());

                valueTextView.setText(currentDate);
            }
        });

        lineChart.getLegend().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.setData(lineData);
        lineChart.getXAxis().setEnabled(true);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setGranularityEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setVisibleXRangeMaximum(4);
        lineChart.getXAxis().setAxisMaximum(31);
        lineChart.setScrollContainer(true);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setExtraOffsets(20f, 0f, 20f, 20f);
        lineChart.getXAxis().setEnabled(true);
        lineChart.getXAxis().setTextSize(16f);
        lineChart.getXAxis().setTextColor(Color.WHITE);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setDrawLabels(false);
        lineChart.getXAxis().setLabelRotationAngle(16f);

        lineChart.invalidate();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}