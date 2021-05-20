package com.example.smartenergymeterapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.jjoe64.graphview.series.DataPoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.*;

import static android.content.ContentValues.TAG;

public class BluetoothMainActivity extends AppCompatActivity {

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("parameters");
    HashMap<String,String> messageMap;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    @IgnoreExtraProperties
    public static class Parameters {
        public String unixTime;
        public String Status;
        public String voltage;
        public String current;
        public String power;
        public String energy;

        public Parameters() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public Parameters(String unixTime, String Status, String voltage, String current,
                            String power, String energy) {
            this.unixTime = unixTime;
            this.Status = Status;
            this.voltage =  voltage;
            this.current = current;
            this.energy = energy;
            this.power = power;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_bluetooth);

        // UI Initialization
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        final TextView textViewInfo = findViewById(R.id.textViewInfo);
        final TextView textViewVal = findViewById(R.id.textViewVal);
        final Button buttonToggle = findViewById(R.id.buttonToggle);
        final Button buttonData = findViewById(R.id.dataPage);
        final Button graphButton = findViewById ( R.id.graphpage );
        buttonToggle.setEnabled(false);
        messageMap = new HashMap<>();
       // final ImageView imageView = findViewById(R.id.imageView);
       // imageView.setBackgroundColor(getResources().getColor(R.color.colorOff));

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progress and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                buttonToggle.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        textViewInfo.setText ( "Data Sharing is ON" );
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        String unixTime = String.valueOf(System.currentTimeMillis() / 1000L);
                        messageMap.put(unixTime,arduinoMsg);
                        textViewVal.setText (arduinoMsg);
                        /*
                        switch (arduinoMsg.toLowerCase()){
                            case "led is turned on":
                             //   imageView.setBackgroundColor(getResources().getColor(R.color.colorOn));
                                textViewInfo.setText( "Arduino Message : " + arduinoMsg );
                                break;
                            case "led is turned off":
                             //   imageView.setBackgroundColor(getResources().getColor(R.color.colorOff));
                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                        }
                         */
                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent( BluetoothMainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        // Button to ON/OFF LED on Arduino Board
        buttonToggle.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                String cmdText = null;
                String btnState = buttonToggle.getText().toString().toLowerCase();
                switch (btnState){
                    case "start":
                        buttonToggle.setText("Stop");
                        // Command to turn on LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn on>";
                        break;
                    case "stop":
                        buttonToggle.setText("Start");
                        // Command to turn off LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn off>";
                        break;
                }
                // Send command to Arduino board
                assert cmdText != null;
                connectedThread.write(cmdText);
            }
        });

        buttonData.setOnClickListener ( new View.OnClickListener () {
            @Override
            public void onClick(View v) {
                Intent i=new Intent ( BluetoothMainActivity.this, MainActivity.class );
            }
        } );

        /*
       buttonData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //String unixTime = String.valueOf(System.currentTimeMillis() / 1000L);
                //mDatabase.child("TestDataAdded").setValue(unixTime);
                if(messageMap.isEmpty()){
                    mDatabase.child(String.valueOf(System.currentTimeMillis() / 1000L)).setValue("Map is Empty");
                }
                Set<Map.Entry<String, String>> entrySet = messageMap.entrySet();
                for(Map.Entry<String,String> entry : entrySet){
                    String[] val=entry.getValue().split (" ");
                    mDatabase.child("message").child(entry.getKey()).setValue(entry.getValue());
                    if(val.length==6 && val[0].equals ( "<") && val[5].equals ( ">" ) ){
                        Parameters parameters = new Parameters ( entry.getKey ( ), val[1], val[2], val[3], val[4], val[4] );
                        mDatabase.child("parameters").child(entry.getKey()).setValue(parameters);
                    }
                }
                messageMap.clear();
            }
        });
         */
        graphButton.setOnClickListener ( new View.OnClickListener ( ) {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent( BluetoothMainActivity.this, ViewGraphActivity.class);
                startActivity(intent);
            }
        } );
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
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

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace ();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace ();
            }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
    /* ============================ Runnable for adding entries ====================== */
    @Override
    protected void onResume() {
        super.onResume ( );
        new Thread ( new Runnable ( ) {
            @Override
            public void run() {
                /*
                if(messageMap.isEmpty()){
                    mDatabase.child(String.valueOf(System.currentTimeMillis() / 1000L)).setValue("Map is Empty");
                }
                 */
                Set<Map.Entry<String, String>> entrySet = messageMap.entrySet();
                for(Map.Entry<String,String> entry : entrySet) {
                    runOnUiThread ( new Runnable ( ) {
                        @Override
                        public void run() {
                            String[] val = entry.getValue( ).split ( " " );
                            mDatabase.child("updates").child (String.valueOf(System.currentTimeMillis() / 1000L)).setValue ( "Adding Data" );
                            mDatabase.child ( "message" ).child ( entry.getKey ( ) ).setValue ( entry.getValue ( ) );
                            if ( val.length == 6 && val[0].equals ( "<" ) && (val[5].equals (
                                    ">\r" ) || val[5].equals ( ">" ) ) ){
                                Parameters parameters = new Parameters ( entry.getKey ( ), val[1], val[2], val[3], val[4], val[4] );
                                mDatabase.child ( "metrics" ).child ( entry.getKey ( ) ).setValue ( parameters );
                            } else
                                mDatabase.child ( "metrics" ).child ( entry.getKey ( ) ).setValue (
                                        "Invalid Arduino Message" );
                        }
                    } );
                    /*
                    try {
                        Thread.sleep ( 3000 );
                    } catch ( InterruptedException e ) {
                        Log.e("InterruptedException","Adding Entries thread not working");
                        e.printStackTrace ( );
                    }
                     */
                }
                messageMap.clear();
            }
        } ).start ();
    }
}
