package com.example.chris.simplebt;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.os.Handler;
import android.util.Log;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
//import android.content.BroadcastReceiver;
//import android.content.Context;
import java.util.Set;
import android.content.Intent;
//import android.content.IntentFilter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Message;

public class MainActivity extends Activity {
    public static final String TAG = "simpleBT";
    private static final int REQUEST_ENABLE_BT = 1;
    private Button onBtn;
    private Button offBtn;
    private Button listBtn;
    //private Button findBtn;
    private TextView text;
    private BluetoothAdapter myBluetoothAdapter;
    protected Set<BluetoothDevice> pairedDevices;
    protected BluetoothDevice mDevice;
    protected ListView myListView;
    private ArrayAdapter<String> BTArrayAdapter;
    private ArrayAdapter<String> BTArrayAdapterMAC;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // take an instance of BluetoothAdapter - Bluetooth radio
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter == null) {
            onBtn.setEnabled(false);
            offBtn.setEnabled(false);
            listBtn.setEnabled(false);
            //findBtn.setEnabled(false);
            text.setText("Status: not supported");

            Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
        } else {
            text = findViewById(R.id.text);
            onBtn = findViewById(R.id.turnOn);
            onBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    on(v);
                }
            });

            offBtn = findViewById(R.id.turnOff);
            offBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    off(v);
                }
            });

            listBtn = findViewById(R.id.paired);
            listBtn.setOnClickListener(new OnClickListener() {


                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    list(v);
                }

            });

            myListView = findViewById(R.id.listView1);
            // create the arrayAdapter that contains the BTDevices, and set it to the ListView
            BTArrayAdapterMAC = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            BTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            myListView.setAdapter(BTArrayAdapter);

            myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    mDevice = myBluetoothAdapter.getRemoteDevice(BTArrayAdapterMAC.getItem(position));
                    ConnectThread mConnectThread = new ConnectThread(mDevice);
                    mConnectThread.start();
                }
            });
        }
    }



    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                Log.v(TAG, "Creating BT socket");
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            //myBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.

                Log.v(TAG, "Connecting to " + mmDevice.getName() + "...");

                mmSocket.connect();

                Log.v(TAG, "Connected");
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    Log.v(TAG, "Error connecting to "+mmDevice.getName());
                    mmSocket.close();
                    Log.v(TAG, "Closing socket");
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);

                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            ConnectedThread mConnectedThread = new ConnectedThread(mmSocket);
            //mConnectedThread.start();

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Could not create in & out inputs stream", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            Log.v(TAG, "Creating input & output stream");
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            Log.v(TAG, "Waiting arduino signals...");
            while (true) {
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for(int i = begin; i < bytes; i++) {
                        if(buffer[i] == "#".getBytes()[0]) {
                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if(i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Could Write message", e);
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = msg.arg1;
            int end = msg.arg2;

            switch (msg.what) {
                case 1:
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    Log.v(TAG,"Incoming message: " + writeMessage);
                    break;
            }
            return false;
        }
    });


    public void on(View view){
        if (!myBluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

            Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
                    Toast.LENGTH_LONG).show();
            Log.v(TAG, "Bluetooth ON");
        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on",
                    Toast.LENGTH_LONG).show();
            Log.v(TAG, "Bluetooth already On");
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if(requestCode == REQUEST_ENABLE_BT){
            if(myBluetoothAdapter.isEnabled()) {
                text.setText("Status: Enabled");
            } else {
                text.setText("Status: Disabled");
            }
        }
    }

    public void list(View view){
        // get paired devices
        pairedDevices = myBluetoothAdapter.getBondedDevices();
        // put it's one to the adapter
        Log.v(TAG, "Listing paired devices:");
        for(BluetoothDevice device : pairedDevices) {
            BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            BTArrayAdapterMAC.add(device.getAddress());
            Log.v(TAG,device.getName() + " : " + device.getAddress());
        }

        Toast.makeText(getApplicationContext(),"Show Paired Devices",
                Toast.LENGTH_SHORT).show();
    }


    public void off(View view){
        myBluetoothAdapter.disable();
        text.setText("Status: Disconnected");

        Toast.makeText(getApplicationContext(),"Bluetooth turned off",
                Toast.LENGTH_LONG).show();
        Log.v(TAG, "Bluetooth OFF");
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        //unregisterReceiver(bReceiver);
    }

}

