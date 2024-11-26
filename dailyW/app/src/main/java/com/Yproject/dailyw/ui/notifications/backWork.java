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

public class backWork extends Worker {
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public backWork(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Bluetooth", Context.MODE_PRIVATE);
        String macAddress = sharedPreferences.getString("device_address", null);
        Context context = getApplicationContext();

        if (macAddress == null) {
            return Result.failure();
        }

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            return Result.failure();
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);

        if (!initializeConnection(device)) {
            return Result.failure();
        }

        try {
            boolean responseReceived = sendBluetoothDataAndWaitForResponse("A");

            if (responseReceived) {
                String response = waitForDMessage(4000);

                if ("D".equals(response)) {
                    String weightData = sendToBluetooth("W");

                    if (Float.parseFloat(weightData) < 3.0) {
                        return Result.failure();
                    }

                    float weight = Float.parseFloat(weightData);
                    saveWeightData(weight);

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

    private boolean initializeConnection(BluetoothDevice device) {
        try {
            if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }

            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                return true;
            }

            UUID uuid = device.getUuids()[0].getUuid();
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);

            bluetoothSocket.connect();

            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();

            return false;
        }
    }

    private void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean sendBluetoothDataAndWaitForResponse(String data) {
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();

            long startTime = System.currentTimeMillis();
            byte[] buffer = new byte[1024];
            int bytesRead;

            while (System.currentTimeMillis() - startTime < 100) {
                if ((bytesRead = inputStream.read(buffer)) != -1) {
                    String response = new String(buffer, 0, bytesRead).trim();
                    Log.d("BluetoothResponse", response);
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

    private String waitForDMessage(long timeout) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeout) {
            String response = readFromBluetooth();

            if ("D".equals(response)) {
                return response;
            }

            Thread.sleep(100);
        }
        return null;
    }

    private String readFromBluetooth() {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);

            if (bytesRead != -1) {
                return new String(buffer, 0, bytesRead).trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String sendToBluetooth(String message) {
        try {
            outputStream.write(message.getBytes());
            outputStream.flush();
            Log.d("BluetoothSend", "Message sent: " + message);

            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);

            if (bytesRead != -1) {
                String response = new String(buffer, 0, bytesRead).trim();
                Log.d("BluetoothResponse", "Response received: " + response);

                return response;
            }
        } catch (IOException e) {
            Log.e("BluetoothError", "Failed to send or receive data.", e);

            return null;
        }

        return null;
    }

    private void saveWeightData(float weight) {
        Gson gson = new Gson();
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        String currentDateStr = new SimpleDateFormat("yyyy-MM-dd").format(currentDate);

        SharedPreferences sharedPreferencesWeight = getApplicationContext().getSharedPreferences("WeightData", Context.MODE_PRIVATE);

        String json = sharedPreferencesWeight.getString(String.valueOf(calendar.get(Calendar.MONTH) + 1), "[]");
        Type type = new TypeToken<List<weightStructure>>(){}.getType();
        List<weightStructure> weights = gson.fromJson(json, type);

        if(weights != null) {
            boolean dateExists = false;

            for (int i = 0; i < weights.size(); i++) {
                if (weights.get(i).getDateStr().equals(currentDateStr)) {
                    weights.set(i, new weightStructure(weight, currentDate, currentDateStr));
                    dateExists = true;
                    break;
                }
            }

            if (!dateExists) {
                weights.add(new weightStructure(weight, currentDate, currentDateStr));
            }

            sharedPreferencesWeight.edit().remove(String.valueOf(calendar.get(Calendar.MONTH) + 1)).apply();
            sharedPreferencesWeight.edit().putString(String.valueOf(calendar.get(Calendar.MONTH) + 1), gson.toJson(weights)).apply();
        }
        else {
            weightStructure newRecord = new weightStructure(weight, currentDate, currentDateStr);
            Objects.requireNonNull(weights).add(newRecord);

            sharedPreferencesWeight.edit().putString(String.valueOf(calendar.get(Calendar.MONTH) + 1), gson.toJson(weights)).apply();
        }
    }
}
