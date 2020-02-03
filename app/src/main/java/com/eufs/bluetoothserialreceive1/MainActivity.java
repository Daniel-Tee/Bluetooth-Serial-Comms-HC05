package com.eufs.bluetoothserialreceive1;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothSerialRcv";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    Button btn_start_connection;
    TextView tv_receiving;
    TextView tv_speed;
    TextView tv_tps;
    TextView tv_rpm;
    TextView tv_status;
    TextView tv_gear;

    ProgressBar pb_tps;
    ProgressBar pb_rpm;

    int pb_tps_val;
    int pb_rpm_val;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        btn_start_connection = findViewById(R.id.btn_open_connection);
        tv_receiving = findViewById(R.id.tv_receiving);
        tv_speed = findViewById(R.id.tv_speed);
        //tv_tps = findViewById(R.id.tv_tps);
        tv_rpm = findViewById(R.id.tv_rpm);
        tv_status = findViewById(R.id.tv_status);
        tv_gear = findViewById(R.id.tv_gear);
        pb_tps = findViewById(R.id.pb_tps);
        pb_rpm = findViewById(R.id.pb_rpm);

        pb_tps.setProgressTintList(ColorStateList.valueOf(Color.BLUE));

        Log.d(TAG, "onCreate: Starting service.");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btn_start_connection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBTService();
            }
        });

    }

    public void screenTapped(View view) {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void startBTService() {
        // Initialise HW adapter
        if(mBluetoothAdapter == null)
        {
            Log.d(TAG, "startBTService: No bluetooth adapter detected.");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        // Connect to HC-05 if in paired device list
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                // TODO: Change this to use MAC address instead of name
                if(device.getName().equals("HC-05"))
                {
                    Log.d(TAG, "startBTService: Found HC-05. Registering.");
                    mmDevice = device;
                    break;
                }
            }
        }

        // Setup connection services
        try {
            Log.d(TAG, "startBTService: Creating socket with UUID: " + MY_UUID_INSECURE);
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
        } catch (IOException e) {
            Log.e(TAG, "startBTService: Cannot create socket: ", e);
        }

        try {
            Log.d(TAG, "startBTService: Connecting socket.");
            mmSocket.connect();
        } catch (IOException e) {
            Log.e(TAG, "startBTService: Cannot connect to socket: ", e);
        }

        try {
            Log.d(TAG, "startBTService: Getting output stream.");
            mmOutputStream = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "startBTService: Error getting output stream", e);
        }

        try {
            Log.d(TAG, "startBTService: Getting input stream..");
            mmInputStream =  mmSocket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "startBTService: Error getting input stream", e);
        }

        Log.d(TAG, "startBTService: Connection opened successfully. Starting RCV process.");
        tv_receiving.setText(getString(R.string.textView_header));

        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String dataString = new String(encodedBytes, StandardCharsets.US_ASCII);
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Log.d(TAG, "run: Received data: " + dataString);
                                            String[] dataSplit = dataString.split(",");
                                            // TODO: Test the strings!
                                            if(dataSplit.length >= 5) {
                                                tv_speed.setText(dataSplit[0]);
                                                //tv_tps.setText(String.format(getString(R.string.tps_rcv), dataSplit[1]));
                                                tv_rpm.setText(dataSplit[2]);
                                                tv_status.setText(String.format(getString(R.string.status_rcv), dataSplit[3]));
                                                tv_gear.setText(dataSplit[4]);

                                                pb_rpm_val = Integer.parseInt(dataSplit[2]) / 130;
                                                pb_rpm.setProgress(Integer.parseInt(dataSplit[2]) / 130);
                                                if (pb_rpm_val >= 70) {
                                                    pb_rpm.setProgressTintList(ColorStateList.valueOf(Color.YELLOW));
                                                } else if (pb_rpm_val >= 90) {
                                                    pb_rpm.setProgressTintList(ColorStateList.valueOf(Color.RED));
                                                } else {
                                                    pb_rpm.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                                                }

                                                pb_tps_val = Integer.parseInt(dataSplit[1]);
                                                pb_tps.setProgress(pb_tps_val);
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                        Log.e(TAG, "run: IO Exception running receive.", ex);
                    }
                }
            }
        });

        workerThread.start();
    }

}
