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

// 홈 화면 출력
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private LineChart lineChart;
    private SharedPreferences sharedPreferences;
    private Calendar calendar;
    private Gson gson;

    // 실질적으로 랜더링 하는 메소드
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 출력을 위해 필요한 객체와 Layout 초기화
        lineChart = root.findViewById(R.id.lineChart);
        gson = new Gson();
        calendar = Calendar.getInstance();
        homeRepository repo = new homeRepository(requireContext());

        // 더미 데이터 만듦
        repo.setDummyData();

        // 현재 월에 맞는 저장된 체중 데이터 가져옮
        List<weightStructure> weights = repo.getWeights(String.valueOf(calendar.get(Calendar.MONTH) + 1));
        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        // 가져온 체중 데이터를 어플리케이션에서 사용하기 위한 구조로 변형
        for (int i = 0; i < weights.size(); i++) {
            weightStructure weight = weights.get(i);
            entries.add(new Entry(i, weight.getWeight()));
            xLabels.add(weight.getDateStr());
        }


        // 데이터를 차트로 보여주기 위한 차트 설정
        LineDataSet dataSet = new LineDataSet(entries, "Weight");
        dataSet.setColor(Color.WHITE);
        dataSet.setDrawValues(false);
        dataSet.setCircleColor(Color.WHITE);
        dataSet.setCircleRadius(8f);
        dataSet.setHighLightColor(Color.TRANSPARENT); // 차트에 있는 점을 눌러도 다른 색깔로 하이라이트 표시 안하게 투명으로 설정

        // 가져온 데이터와 차트 설정을 적용
        LineData lineData = new LineData(dataSet);
        TextView valueTextView = root.findViewById(R.id.valueTextView);

        // 하단에 출력될 텍스트 설정(아무런 점도 선택 안했을시 출력될 현재 날짜)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = sdf.format(new Date());
        valueTextView.setText(currentDate);

        // 차트 위의 점을 눌렀을때 실행할 기능 등록 (익명 클래스로 등록)
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {

            // 눌려진 점이 가지고 있는 데이터를 하단에 출력하기 위한 메소드
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                double value = e.getY();

                String text = String.format(Locale.ROOT, "%.2f", value);
                valueTextView.setText(text);
            }

            // 차트위의 동일한 점을 눌렀을때 실행 할 메소드
            @Override
            public void onNothingSelected() {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String currentDate = sdf.format(new Date());

                valueTextView.setText(currentDate);
            }
        });

        // 차트 위에 그려질 선과 라벨들 설정
        lineChart.getLegend().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);   // X값에 출력될 라벨 위치 설정
        lineChart.setData(lineData);
        lineChart.getXAxis().setEnabled(true);  // X축 기준선 출력
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        lineChart.getXAxis().setGranularity(1f);  // X축 간격 1로 설정
        lineChart.getXAxis().setGranularityEnabled(true);
        lineChart.setDragEnabled(true);  // 드래그로 수평 스크롤 활성화
        lineChart.setVisibleXRangeMaximum(4);  // 한 화면에 출력시킬 데이터의 갯수를 4개로 설정
        lineChart.getXAxis().setAxisMaximum(31);  // 차트에 담겨질 데이터의 최대 수를 31개로 설정
        lineChart.setScrollContainer(true);   // 스크롤 활성화
        lineChart.setScaleEnabled(false);  // 확대 축소 비활성화
        lineChart.setPinchZoom(false);     // 두손가락으로 확대 축소 비활성화
        lineChart.setExtraOffsets(20f, 0f, 20f, 20f);
        lineChart.getXAxis().setEnabled(true);  //X축 라벨 활성화
        lineChart.getXAxis().setTextSize(16f);  // X출 라벨 사이즈
        lineChart.getXAxis().setTextColor(Color.WHITE);
        lineChart.getDescription().setEnabled(false);  // 차트 설명 텍스트 비활성화
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setDrawLabels(false);  // Y축 라벨 비활성화
        lineChart.getXAxis().setLabelRotationAngle(16f);  // X축 라벨 회전 시킴

        lineChart.invalidate();  // 차트 그리기
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}