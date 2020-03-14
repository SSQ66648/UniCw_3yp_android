/*------------------------------------------------------------------------------
 *
 * TITLE:       3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        SerialWingood.Java
 *
 * ASSOCIATED   activity_serial_wingood.xml
                content_serial_wingood.xml
 *
 * DESCRIPTION: Code adopted from (poorly written) bluetooth tutorial to work with own requirements.
 *              will be further adapted in later class(es)
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v0.0    200105  Initial (non functional) implementation.
 *              v1.0    200105  successfully adapted to work with own setup. changed method of
 *                              received variables and structure.
 *              v1.1    200106  tidied code, added comments. converted layout to constraintLayout
 *                              (solved the missing buttons bug) added error handling for send
 *                              command buttons (LED on/off). send led commands successful.
 *              v1.1.01 200109  changed no socket to close toast to log (null socket expected if
 *                              activity not accessed via connection process).
 *              v1.2    200110  tidied code. changed class template formatting.
 *
 * NOTES:       date notation: YYMMDD
 *              comment format:
 *                  //---GROUP---
 *                  //explanation
 *
 *------------------------------------------------------------------------------
 * TO DO LIST:
 *              todo:
 *              todo:   add check for received values (ie wrong received mesg (i val v 4 val test)
 *              todo:   address inline to do items
 *              todo:   investigate warning of inner class (if not an issue, find a way of muting warning block)
 *              todo:   research pausing and resumes: additional ondestroy or similar required?
 *              todo:   explore how (if possible) to terminate existing connections with selected device (possibly only on same device? eg if terminal already open with hc05?)
 *              todo:   add suggestion of open socket to socket error toast etc
 *              todo:   add/improve error handling throughout
 *
 * TO DO TODAY:
 *              TODO:
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;
/*--------------------------------------
    IMPORT LIST
--------------------------------------*/

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class SerialWingood_WORKINTOPRIMARYHOST extends AppCompatActivity {
    private static final String TAG = "SerialWingood";

    /*--------------------------------------
        CLASS VARIABLES
    --------------------------------------*/
    //---LAYOUT---
    Button btn_tst_LedOn, btn_tst_LedOff, btn_tst_audioOut;
    TextView txt_string, txt_stringLength, txt_sensorView0, txt_sensorView1, txt_sensorView2,
            txt_sensorView3;

    //---VARIABLES---
    Handler hnd_bluetoothIn;
    //identify handler message
    final int handlerState = 0;
    //bluetooth
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private ConnectedThread mConnectedThread;
    //assembly of received variables
    private StringBuilder strBld_recDataString = new StringBuilder();
    // SPP UUID service - this should work for most devices
    private static final UUID BtModuleUUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String for MAC address
    private static String address;
    //array for received data values (once split)
    String[] receivedValues = new String[4];


    /*--------------------------------------
        CREATE
    --------------------------------------*/
    //todo: research if this warning needs to be addressed or remain suppressed
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_bluetooth_connection);

        //---TOOLBAR---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Bluetooth values received");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Link the buttons and textViews to views
        btn_tst_LedOn = (Button) findViewById(R.id.buttonOn);
        btn_tst_LedOff = (Button) findViewById(R.id.buttonOff);
        btn_tst_audioOut = (Button) findViewById(R.id.btn_test_headsetAudio);

        txt_string = (TextView) findViewById(R.id.txtString);
        txt_stringLength = (TextView) findViewById(R.id.testView1);
        txt_sensorView0 = (TextView) findViewById(R.id.sensorView0);
        txt_sensorView1 = (TextView) findViewById(R.id.sensorView1);
        txt_sensorView2 = (TextView) findViewById(R.id.sensorView2);
        txt_sensorView3 = (TextView) findViewById(R.id.sensorView3);

        //create handler object for incoming message
        hnd_bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    // msg.arg1 = bytes from connect thread
                    String readMessage = (String) msg.obj;
                    //appending to string until find end of message char (~)
                    strBld_recDataString.append(readMessage);
                    //set index for end of message
                    int endOfLineIndex = strBld_recDataString.indexOf("~");
                    //check any data exists before ~
                    if (endOfLineIndex > 0) {
                        //extract string (currently only used to get length after splitString)
                        String dataInPrint = strBld_recDataString.substring(0, endOfLineIndex);
                        txt_string.setText("Data Received = " + dataInPrint);
                        //get length of data received (25char initially, grows with triple digits)
                        int dataLength = dataInPrint.length();
                        txt_stringLength.setText("String Length = " + String.valueOf(dataLength));

                        //check for beginning character of '#' -signifies desired transmission
                        if (strBld_recDataString.charAt(0) == '#') {
                            //string array for sensor values
                            String sensorValues[] = new String[4];

                            //remove first character ('#') to simplify splitting string
                            strBld_recDataString.deleteCharAt(0);

                            //convert stringBuilder to string array
                            receivedValues = strBld_recDataString.toString().split("\\+");

                            //assign and check result of splitString
                            for (int i = 0; i < 4; i++) {
                                sensorValues[i] = receivedValues[i];
                                System.out.println("displayed sensor" + i + " value: "
                                        + sensorValues[i]);
                            }

                            //set received values as textView 'output'
                            txt_sensorView0.setText(" Sensor 0 Voltage = " + sensorValues[0] + "V");
                            txt_sensorView1.setText(" Sensor 1 Voltage = " + sensorValues[1] + "V");
                            txt_sensorView2.setText(" Sensor 2 Voltage = " + sensorValues[2] + "V");
                            txt_sensorView3.setText(" Sensor 3 Voltage = " + sensorValues[3] + "V");
                        }

                        //clear all string data
                        //todo:need to delete splitstring as seen here?
                        strBld_recDataString.delete(0, strBld_recDataString.length());
                    }
                }
            }
        };

        // get Bluetooth adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        // Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        //todo:move these to external listeners if possible
        btn_tst_LedOff.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    mConnectedThread.write("0");    // Send "0" via Bluetooth
                    Toast.makeText(getBaseContext(), "Turn off LED",
                            Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(SerialWingood_WORKINTOPRIMARYHOST.this,
                            "Error sending: check connection", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onClick: btn_tst_LedOff: Error sending via Bluetooth");
                }
            }
        });

        btn_tst_LedOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    mConnectedThread.write("1");    // Send "1" via Bluetooth
                    Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(SerialWingood_WORKINTOPRIMARYHOST.this,
                            "Error sending: check connection", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onClick: btn_tst_LedOn: Error sending via Bluetooth");
                }
            }
        });

        //mediaplayer: create audido playback (via headset) while receiving bluetooth variables
        btn_tst_audioOut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaPlayer mMediaPlayer = new MediaPlayer();
//                Uri mediaPath = Uri.parse("android.resource://" + getPackageName() + "/"
//                        + R.raw.counting);
                try {
//                    mMediaPlayer.setDataSource(getApplicationContext(), mediaPath);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /*--------------------------------------
        PAUSE
    --------------------------------------*/
    @Override
    public void onPause() {
        super.onPause();
        //nullcheck to avoid crash
        if (btSocket != null) {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                btSocket.close();
            } catch (IOException e2) {
                //todo: insert code to deal with this (if even possible to get here: see null crash)
            }
        } else {
            Log.d(TAG, "onPause: No established Bluetooth socket to close");
        }
    }


    /*--------------------------------------
        RESUME
    --------------------------------------*/
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: begin resume");
        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();
        //Get the MAC address from the DeviceListActivity via EXTRA
        address = intent.getStringExtra(BluetoothConnectDevices.EXTRA_DEVICE_ADDRESS);
        Log.d(TAG, "onResume: address = " + address);

        //create device and set the MAC address
        try {
            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            Log.d(TAG, "onResume: device created: " + device.getName() + " : "
                    + device.getAddress());
            try {
                btSocket = createBluetoothSocket(device);
                Log.d(TAG, "onResume: create bluetooth socket");
            } catch (IOException e) {
                Log.d(TAG, "onResume: Socket creation failed.");
                Toast.makeText(getBaseContext(), "Socket creation failed",
                        Toast.LENGTH_LONG).show();
            }
            // Establish the Bluetooth socket connection.
            try {
                btSocket.connect();
                Log.d(TAG, "onResume: socket connect...");
            } catch (IOException e) {
                Log.d(TAG, "onResume: socket connection error.");
                try {
                    btSocket.close();
                    Log.d(TAG, "onResume: socket closed");
                } catch (IOException e2) {
                    Log.d(TAG, "onResume: Error on closing socket during establish failure!");
                    //insert code to deal with this
                }
            }

            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();
            //send character when resuming.beginning transmission to check device is connected
            mConnectedThread.write("x");
        } catch (Exception e) {
            Log.d(TAG, "onResume: error creating device.");
        }
    }


    /*--------------------------------------
        INNER CLASSES
    --------------------------------------*/
    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                //todo: insert code to handle exception
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    //read bytes from input buffer
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    hnd_bluetoothIn.obtainMessage(handlerState, bytes, -1,
                            readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write to remote device
        public void write(String input) {
            //convert String into bytes
            byte[] msgBuffer = input.getBytes();
            try {
                //write bytes over bluetooth outStream
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                //if you cannot write, close application
                Toast.makeText(getBaseContext(), "Connection Failure",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    /*--------------------------------------
        METHODS
    --------------------------------------*/
    //creates secure outgoing connection with BT device using UUID
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BtModuleUUID);
    }


    //Check device Bluetooth is available: prompt to enable if not
    private void checkBTState() {
        if (btAdapter == null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth",
                    Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

}