package com.Yproject.dailyw.ui.notifications;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.Yproject.dailyw.util.weightStructure;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// 백그라운드에서 실행될 기능 (블루투스 연결, 통신, 데이터 로컬에 저장)
public class backWork extends Worker {
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public backWork(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    // 실질적으로 실행될 메소드
    @NonNull
    @Override
    public Result doWork() {
        // 로컬에 저장되어 있는 MacAddress 가져옴
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Bluetooth", Context.MODE_PRIVATE);
        String macAddress = sharedPreferences.getString("device_address", null);
        Context context = getApplicationContext();

        // macAddress가 없다면 작업 실패로 알리고 종료
        if (macAddress == null) {
            return Result.failure();
        }


        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // 블루투스 사용 불가면 작업 실패로 알리고 종료
        if (bluetoothAdapter == null) {
            return Result.failure();
        }

        // macAddress를 기반으로 연결할 장치의 정보를 가져옴
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);

        // 블루투스 연결 초기화를 진행 만일 연결 실패면 작업 실패로 알리고 종료
        if (!initializeConnection(device)) {
            return Result.failure();
        }

        try {
            // 장치에 알람을 가동하라고 요청함
            boolean responseReceived = sendBluetoothDataAndWaitForResponse("A");

            // 장치가 알람 가동에 성공하면 실행
            if (responseReceived) {
                // 장치로부터 D 메세지 기다림
                String response = waitForDMessage(4000);

                // D 메세지가 정상적으로 반환되었다면 실행
                if ("D".equals(response)) {
                    // 무게 데이터를 요청
                    String weightData = sendToBluetooth("W");

                    // 무게 데이터가 3.0 이하면 작업 실패로 알리고 종료 (노이즈 및 예외상황 방어를 위해)
                    if (Float.parseFloat(weightData) < 3.0) {
                        return Result.failure();
                    }

                    // 무게데이터를 로컬에 저장
                    float weight = Float.parseFloat(weightData);
                    saveWeightData(weight);

                    // 작업 성공으로 알리고 종료(1회성 작업이므로 다시 동일 작업이 따로 예약되지 않음)
                    return Result.success();
                } else {
                    return Result.failure();
                }
            } else {
                return Result.failure();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        } finally {
            closeConnection();
        }
    }

    // 블루투스 연결 초기화를 위한 메소드
    private boolean initializeConnection(BluetoothDevice device) {
        try {
            // 권한 확인
            if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }

            // 이미 연결 되어있다면 true 반환
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                return true;
            }

            // macAddress를 기반으로 연결할 장치의 정보를 가져와서 연결을 위한 UUID 설정(어떤 프로토콜로 연결하지 여기서 결정)
            UUID uuid = device.getUuids()[0].getUuid();
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);

            // 블루투스 연결 사도
            bluetoothSocket.connect();

            // 연결되었다면 inputStream과 outputStream을 열기
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();

            return false;
        }
    }

    // 작업이 종료되면 자원회수
    private void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 장치에게 알람을 가동시키라는 요청과 응답을 받기 위한 메소드
    private boolean sendBluetoothDataAndWaitForResponse(String data) {
        try {
            // A 메세지를 보냄
            outputStream.write(data.getBytes());
            outputStream.flush();

            // 응답을 받기위한 변수 설정
            long startTime = System.currentTimeMillis();
            byte[] buffer = new byte[1024];
            int bytesRead;

            // 100ms 동안만 응답을 기다림
            while (System.currentTimeMillis() - startTime < 100) {
                // inputStream에 데이터가 있다면 실행
                if ((bytesRead = inputStream.read(buffer)) != -1) {
                    String response = new String(buffer, 0, bytesRead).trim(); // inputStream에서 가져온 데이터를 "\n" 제거해서 변수에 할당
                    Log.d("BluetoothResponse", response);

                    // 응답받은 데이터가 A라면 true 반환
                    if ("A".equals(response)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }

        return false;
    }

    // D 메세지를 기다리기 위한 메소드
    private String waitForDMessage(long timeout) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        // 4동안만 기다림
        while (System.currentTimeMillis() - startTime < timeout) {
            // 블루투스로 들어온 데이터를 읽음
            String response = readFromBluetooth();

            // 응답 받은 데이터가 D가 맞다면 D를 반환
            if ("D".equals(response)) {
                return response;
            }

            // 바로 반복문을 실행하지 않고 100ms 대기
            Thread.sleep(100);
        }
        return null;
    }

    // 블루투스로 들어온 데이터를 읽기위한 메소드
    private String readFromBluetooth() {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);

            if (bytesRead != -1) {
                return new String(buffer, 0, bytesRead).trim(); // inputStream에서 가져온 데이터를 "\n" 제거해서 반환
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 블루투스로 요청을 보내고 응답을 받기 위한 메소드
    private String sendToBluetooth(String message) {
        try {
            // 블루투스로 요청 보냄(데이터 보냄)
            outputStream.write(message.getBytes());
            outputStream.flush();
            Log.d("BluetoothSend", "Message sent: " + message);

            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);  // 응답이 inputStream에 들어올때까지 대기

            if (bytesRead != -1) {
                String response = new String(buffer, 0, bytesRead).trim();  // inputStream에서 가져온 데이터를 "\n" 제거해서 변수에 할당
                Log.d("BluetoothResponse", "Response received: " + response);

                return response;
            }
        } catch (IOException e) {
            Log.e("BluetoothError", "Failed to send or receive data.", e);

            return null;
        }

        return null;
    }

    // 데이터를 로컬에 저장하기 위한 메소드
    private void saveWeightData(float weight) {
        // 로컬에 데이터 저장하기 위해 필요한 변수들 초기화
        Gson gson = new Gson();
        Date currentDate = new Date();  //현재 날짜와 시간을 객체로 가져옴
        Calendar calendar = Calendar.getInstance();  //현재 날짜와 시간을 객체로 가져옴
        String currentDateStr = new SimpleDateFormat("yyyy-MM-dd").format(currentDate);  //현재 날짜와 시간을 지정한 형식으로 수정해서 변수에 할당

        SharedPreferences sharedPreferencesWeight = getApplicationContext().getSharedPreferences("WeightData", Context.MODE_PRIVATE);

        // 로컬에 존재하는 현재 월 데이터를 가져옴
        String json = sharedPreferencesWeight.getString(String.valueOf(calendar.get(Calendar.MONTH) + 1), "[]");  // 1 더한 이유는 Calendar.MONTH가 현재 월에서 1뺀 값으로 가져와서
        Type type = new TypeToken<List<weightStructure>>(){}.getType();
        List<weightStructure> weights = gson.fromJson(json, type);  // JSON 문자열로 저장되어 있어 사용하기 쉬운 구조로 바꿈

        // 기존에 데이터가 존재할 경우
        if(weights != null) {
            boolean dateExists = false;

            // 현재 날짜에 맞는 데이터가 이미 존재할 경우 그 데이터만을 새로운 데이터로 변경
            for (int i = 0; i < weights.size(); i++) {
                if (weights.get(i).getDateStr().equals(currentDateStr)) {
                    weights.set(i, new weightStructure(weight, currentDate, currentDateStr));
                    dateExists = true;
                    break;
                }
            }

            // 현재 날짜에 맞는 데이터가 존재하지 않을경우 기존에 존재하는 데이터에 이어서 저장
            if (!dateExists) {
                weights.add(new weightStructure(weight, currentDate, currentDateStr));
            }

            // 로컬에 데이터 삭제후 다시 저장
            sharedPreferencesWeight.edit().remove(String.valueOf(calendar.get(Calendar.MONTH) + 1)).apply();
            sharedPreferencesWeight.edit().putString(String.valueOf(calendar.get(Calendar.MONTH) + 1), gson.toJson(weights)).apply();
        }
        else {
            // 기존에 현재 월에 해당하는 데이터가 존재하지 않을경우 새롭게 구조를 만들어서 저장
            weightStructure newRecord = new weightStructure(weight, currentDate, currentDateStr);
            Objects.requireNonNull(weights).add(newRecord);

            sharedPreferencesWeight.edit().putString(String.valueOf(calendar.get(Calendar.MONTH) + 1), gson.toJson(weights)).apply();
        }
    }
}
