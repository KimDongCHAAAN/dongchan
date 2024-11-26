package com.Yproject.dailyw.ui.Bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.Yproject.dailyw.R;
import com.Yproject.dailyw.databinding.FragmentBluetoothBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//블루투스 연결 및 탐색 스크린
public class BluetoothFragment extends Fragment {

    private FragmentBluetoothBinding binding;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ListView bluetoothDeviceList;
    private ArrayAdapter<String> adapter;

    private final ActivityResultLauncher<Intent> enableBtLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(getContext(), "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Bluetooth enabling failed", Toast.LENGTH_SHORT).show();
                }
            });

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                String deviceInfo = device.getName() + "\n" + device.getAddress();
                adapter.add(deviceInfo);
            }
        }
    };

    // 실질적으로 랜더링 하는 메소드
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        BluetoothViewModel dashboardViewModel =
                new ViewModelProvider(this).get(BluetoothViewModel.class);

        binding = FragmentBluetoothBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 이미 등록이 되어 있거나 탐색하여 연결 가능한 블루투스 장치들의 정보를 보여줄 리스트 Layout
        bluetoothDeviceList = root.findViewById(R.id.bluetoothDeviceList);
        bluetoothDeviceList.setDivider(new ColorDrawable(Color.WHITE));
        bluetoothDeviceList.setDividerHeight(2);

        // 블루투스 장치 탐색 버튼
        Button scanButton = root.findViewById(R.id.scanButton);

        // 블루투스 탐색 및 연결하기 위한 블루투스 매니저와 어댑터 객체
        BluetoothManager bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // 만일 현재 모바일이 블루투스를 지원하지 않는다면 보여줄 메세지와 화면
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            return root;
        }

        // 블루투스가 비활성화 되어 있다면 강제로 블루투스 활성화
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
        }

        // 등록하거나 연결 가능한 블루투스 정보들을 출력할때 스타일 설정
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textView = view.findViewById(android.R.id.text1);

                textView.setTextSize(18);
                textView.setTextColor(Color.WHITE);
                textView.setPadding(10, 20, 10, 20);

                return view;
            }
        };

        // 설정한 스타일을 적용
        bluetoothDeviceList.setAdapter(adapter);

        // 이미 등록(페어링)된 장치의 정보를 리스트에 추가
        getPairedDevices();

        //탐색버튼(Scan)이 눌려졌을때 실행할 기능 등록
        scanButton.setOnClickListener(view -> startScanningForDevices());


        // 블루투스 장치들의 정보중 1개는 선택했을때 실행할 기능 등록
        bluetoothDeviceList.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDevice = adapter.getItem(position);
            if (selectedDevice != null) {

                // 장치의 MacAddress 가져옴
                String macAddress = selectedDevice.split("\n")[1];

                // MacAddress를 로컬에 저장
                saveDeviceAddress(macAddress);

                // 블루투스 연결
                connectToDevice(macAddress);
            }
        });

        //화면 랜더링
        return root;
    }

    // 등록(페어링)된 장치 정보 가져오는 메소드
    private void getPairedDevices() {
        // 필요한 권한 확인
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // 등록(페어링)된 장치 리스트 가져옴
        pairedDevices = bluetoothAdapter.getBondedDevices();

        // 리스트가 비어있지 않다면 실행
        if (!pairedDevices.isEmpty()) {

            // 각각의 장치들의 정보를 순회하며 필요한 정보만을 가져와서 리스트에 담음
            for (BluetoothDevice device : pairedDevices) {
                String deviceInfo = device.getName() + "\n" + device.getAddress();
                adapter.add(deviceInfo);
            }
        } else {

            // 만일 등록(페어링)된 장치가 없다면 보여줄 메세지
            Toast.makeText(getContext(), "No paired devices found", Toast.LENGTH_SHORT).show();
        }
    }

    //탐색 버튼이 눌렸을때 실행할 메소드
    private void startScanningForDevices() {
        // 필요한 권한 확인
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // 이미 탐색모드 상태면 탐색모드 종료
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // 탐색 시작
        bluetoothAdapter.startDiscovery();

        // 장치 발견시 실행할 이벤트 등록과 등록된 이벤트 리시버 설정
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getContext().registerReceiver(receiver, filter);
    }

    // 연결한 블루투스 장치 MacAddress 로컬에 저장하는 메소드
    private void saveDeviceAddress(String macAddress) {

        // tag : Bluetooth, Key : device_address, Data : 실제 MacAddress 형테로 저장
        getContext().getSharedPreferences("Bluetooth", Context.MODE_PRIVATE)
                .edit()
                .putString("device_address", macAddress)
                .apply();
    }

    // 블루투스 장치와 연결하는 메소드
    private void connectToDevice(String macAddress) {

        //macAddress로 연결시도
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        new ConnectThread(device).start();
    }

    // 블루투스 장치와 연결하기 위해 필요한 클래스(내부 클래스로 작성됨)
    private class ConnectThread extends Thread {
        private final BluetoothDevice device;
        private BluetoothSocket socket;

        // 블루투스 연결설정 메소드(내부 클래스의 메소드)
        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmpSocket = null;
            try {
                // 권한 확인
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                // 블루투스 연결을 위한 소켓 설정(SPP : 시리얼 통신 프로토콜로 연결, UUID는 SPP 연결을 위한 고장된 UUID)
                tmpSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = tmpSocket;
        }

        // 실제로 연결을 시도하는 메소드(내부 클래스의 메소드)
        public void run() {
            try {
                // 권환확인
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                // 소켓으로 연결
                socket.connect();

                // 연결이 되었다고 어플리케이션에 메세지 보냄
                handler.sendMessage(handler.obtainMessage(1, "Connected to " + device.getName()));
            } catch (IOException e) {
                e.printStackTrace();

                // 연결이 실패했다고 어플리케이션에 메세지 보냄
                handler.sendMessage(handler.obtainMessage(0, "Connection failed"));
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        // 어플리케이션에 메세지 보내기 위한 객체
        private final Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    Toast.makeText(getContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    // 현재 화면에 포커스가 취소됐을때 자원 회수를 위한 메소드
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (getContext() != null) {
            try {
                getContext().unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        binding = null;
    }
}



