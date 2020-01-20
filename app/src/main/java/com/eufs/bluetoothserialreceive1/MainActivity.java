package com.eufs.bluetoothserialreceive1;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothSerialRcv";
    private static final String appName = "BluetoothSerialReceive1";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    Button btn_start_connection;
    TextView tv_receiving;
    TextView tv_speed;
    TextView tv_rpm;
    TextView tv_status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_start_connection = findViewById(R.id.btn_open_connection);
        tv_receiving = findViewById(R.id.tv_receiving);
        tv_speed = findViewById(R.id.tv_speed);
        tv_rpm = findViewById(R.id.tv_rpm);
        tv_status = findViewById(R.id.tv_status);

        Log.d(TAG, "onCreate: Starting service.");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btn_start_connection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBTService();
            }
        });

    }

    private void startBTService() {
        
    }
}
