package com.Yproject.dailyw;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.Yproject.dailyw.databinding.ActivityMainBinding;

// 전체적으로 어플리케이션을 초기화 하고 실행 시키는 진입점
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_BLUETOOTH_PERMISSION = 101;
    private ActivityMainBinding binding;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_bluetooth)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        // 화면 전환을 위한 설정객체 적용
        NavigationUI.setupWithNavController(binding.navView, navController);

        //위치 권한이 이전에 승인 되었는지 확인, 안되어 있으면 요청 진행
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION);
        }

        // 블루투스 권한이 이전에 승인 되었는지 확인, 안되어 있으면 요청 진행
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                    REQUEST_CODE_BLUETOOTH_PERMISSION);
        }
    }

    // 권한을 요청하기 위한 메소드(사용자 위치 탐색과 사용권한, 블루투스 탐색 연결 통신 권한)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //위치 권한을 요청하고 거부할 경우 앱을 종료시킴
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finish();
            }
        }

        //블루투스 권한을 요청하고 거부할 경우 앱을 종료시킴
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finish();
            }
            if (!(grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                finish();
            }
        }
    }
}