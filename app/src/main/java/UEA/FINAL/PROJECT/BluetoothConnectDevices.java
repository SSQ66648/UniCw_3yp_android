/*------------------------------------------------------------------------------
 *
 * TITLE:       3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        DeviceList2.Java
 *
 * ASSOCIATED   activity_device_list2.xml
                content_device_list2.xml
 *
 * DESCRIPTION: paired bluetooth device list and launching platform (on item select) for
 *              serialWingood.class. Adapted from followed tutorial, will continue to adapt for own
 *              needs in later class.
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v1.0    200105  Initial implementation.
 *              v1.1    200106  tidied code and added comments.
 *              v1.2    200109  very small changes eg removed obsolete button variable, commenting.
 *              v1.2.1  200110  tidied code.
 *
 * NOTES:       date notation: YYMMDD
 *              comment format:
 *                  //---GROUP---
 *                  //explanation
 *
 *------------------------------------------------------------------------------
 * TO DO LIST:
 *              todo:
 *              todo:   add further logs/ debugging & remove obsolete
 *              todo:   ensure methods function as expected
 *              todo:   add (if possible) "claim socket of selected device" method
 *              todo:   add 'status' indication to device list (eg socket available/occupied, paired but not found etc)
 *
  * TO DO TODAY:
 *              TODO:
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;
/*--------------------------------------
    IMPORT LIST
--------------------------------------*/

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Set;

public class BluetoothConnectDevices extends AppCompatActivity {
    private static final String TAG = "Devicelist2";

    /*--------------------------------------
        CLASS VARIABLES
    --------------------------------------*/
    //---LAYOUT---
    //connecting message
    TextView textView1;

    //---VARIABLES---
    //EXTRA string to send on to mainActivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    //bluetooth adapter and array for devices
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;


    /*--------------------------------------
        CREATE
    --------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect_devices);

        //---TOOLBAR---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("titleHere");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    //TODO: figure out if onPause is needed for this activity?


    /*--------------------------------------
        RESUME
    --------------------------------------*/
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        //check state on resume as something may have changed while paused
        checkBTState();
        //set format of connecting message
        textView1 = findViewById(R.id.connecting);
        textView1.setTextSize(40);
        textView1.setText(" ");

        Log.d(TAG, "onResume: initialise arrayAdapter");
        // Initialize array adapter for paired devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.format_bluetooth_device_name);

        Log.d(TAG, "onResume: set up ListView of paired devices");
        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices and append to 'pairedDevices'
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Add previously paired devices to the array
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }


    /*--------------------------------------
            METHODS
    --------------------------------------*/

    // Set up on-click listener for the list items
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            Log.d(TAG, "onItemClick: item clicked");


            textView1.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Make an intent to start next activity while taking an extra of MAC address.
            Intent i = new Intent(BluetoothConnectDevices.this, SerialWingood_WORKINTOPRIMARYHOST.class);
            i.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(i);
        }
    };

    //check bt is enabled on device
    private void checkBTState() {
        Log.d(TAG, "checkBTState: ");
        // Check device has Bluetooth and that it is turned on
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(getBaseContext(), "Device does not support Bluetooth",
                    Toast.LENGTH_SHORT).show();
        } else {
            if (mBtAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

}
