/*------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        BluetoothActions.Java
 *
 * LAYOUT(S):   activity_.xml
 *
 * DESCRIPTION: Handles toggling of device Bluetooth statuses as well as discovery, pairing and
 *              connecting to nearby Bluetooth devices. active connection is passed to service to
 *              maintain connection while device is locked.
 *              (based on combination of test classes: "SerialWingood" and "BluetoothActions")
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v1.0    200315  Initial implementation.
 *------------------------------------------------------------------------------
 * NOTES:       
 *      +   
 *------------------------------------------------------------------------------
 * TO DO LIST:  
 *      //todo: complete activity implementation
 *      //todo: complete scanning for devices
 *      //todo: complete pairing to device
 *      //todo: complete listing of paired devices
 *      //todo: connect to paired device (headset)
 *      //todo: connect to paired device (bike)
 *      //todo: on connection (both) switch to host activity
 *      //todo: complete passing of bluetooth management to Foreground service (or own service: communicate with prime service)
 *      //todo: add icons to device cards depending on type of device
 *      //todo:
 *      //todo:
 *
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

/*--------------------------------------
    IMPORT LIST
--------------------------------------*/
public class BluetoothActions extends AppCompatActivity implements View.OnClickListener {
    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    private static final String TAG = BluetoothActions.class.getSimpleName();


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    //---LAYOUT---
    private ToggleButton toggle_enableBT;
    private Button button_discoverDevices;
    private Button button_pairedDevices;
    private TextView text_connectingInfo;
    private RecyclerView recyc_discoveredDevices;
    private RecyclerView recyc_pairedDevices;


    //---VARIABLES---
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> set_pairedDevices;

    private RecyclerView.Adapter recycAdapter;  //only provide items currently needed (performance)
    private RecyclerView.LayoutManager recycLayoutManager;

    //collection of device cards to populate recycler lists
    private ArrayList<DeviceCard> deviceCardList;


    /*--------------------------------------
        LIFECYCLE
    --------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_actions);
        Log.d(TAG, "onCreate: ");

        //---TOOLBAR---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getLocalBluetoothName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //todo: move above toolbar?

        //---VIEWS----
        toggle_enableBT = findViewById(R.id.toggle_bluetoothactions_enablebluetooth);
        toggle_enableBT.setOnClickListener(this);
        button_discoverDevices = findViewById(R.id.button_bluetoothactions_discoverdevices);
        button_discoverDevices.setOnClickListener(this);
        button_pairedDevices = findViewById(R.id.button_bluetoothactions_paireddevices);
        button_pairedDevices.setOnClickListener(this);

        text_connectingInfo = findViewById(R.id.infotext_bluetoothactions_connecting);

        recyc_discoveredDevices = findViewById(R.id.recycler_bluetoothactions_discovereddevices);
        recyc_pairedDevices = findViewById(R.id.recycler_bluetoothactions_paireddevices);


        //---EXECUTE---
        registerReceiverStatusChange();
        registerReceiverDiscover();
//todo: move to resume?

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        //(re) check if bluetooth enabled
        //todo: once selecting cards is possible: move this there as a 'check' before moving to next activity- leave bt to toggle button here
        checkBluetoothState();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        unregisterReceiver(broadcastReceiver_discover);
        unregisterReceiver(broadcastReceiver_status);
    }


    /*--------------------------------------
        LISTENERS
    --------------------------------------*/
    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick: ");
        switch (v.getId()) {
            case R.id.toggle_bluetoothactions_enablebluetooth:
                enableBluetooth();
                makeVisible();
                break;
            case R.id.button_bluetoothactions_discoverdevices:
                createDiscoveredList();
                break;
            case R.id.button_bluetoothactions_paireddevices:
                createPairedList();
                break;
        }
    }


    /*--------------------------------------
        METHODS
    --------------------------------------*/
    //-initialise status change listening
    public void registerReceiverStatusChange() {
        Log.d(TAG, "registerReceiverStatusChange: ");
        IntentFilter intentFilter = new IntentFilter();
        //device status changes (//todo: check the removal of action found here does not affect usage)
//        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        //adapter status changes
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);

        this.registerReceiver(broadcastReceiver_status, intentFilter);
    }


    //-initialise device discovery listening
    public void registerReceiverDiscover() {
        Log.d(TAG, "registerReceiverDiscover: ");
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver_discover, intentFilter);
    }


    //-toggle device bluetooth
    public void enableBluetooth() {
        Log.d(TAG, "enableBluetooth: ");
        if (toggle_enableBT.isChecked()) {
            Log.d(TAG, "enableBluetooth: isChecked: ");
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Log.e(TAG, "enableBluetooth: adapter is null. ");
                new AlertDialog.Builder(this)
                        .setTitle("Not compatible")
                        .setMessage("This device does not support Bluetooth. " +
                                "Application will exit.").setPositiveButton(
                        "Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface,
                                                int which) {
                                System.exit(0);
                            }
                        }).setIcon(android.R.drawable.ic_dialog_alert).show();
                finish();
            }

            if (!bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "enableBluetooth: enabling adapter. ");
                Intent intent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, 1);
            }
        } else {
            Log.w(TAG, "enableBluetooth: notChecked. ");
            Toast.makeText(getApplicationContext(),
                    " Bluetooth turned off", Toast.LENGTH_LONG).show();
            if (bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "enableBluetooth: disabling adapter. ");
                //disable the bt
                if (bluetoothAdapter != null) {
                    bluetoothAdapter.disable();
                }
            }
        }
    }


    //-display 'friendly name' of bluetooth device as text
    public String getLocalBluetoothName() {
        Log.d(TAG, "getLocalBluetoothName: getting local name... ");
        if (bluetoothAdapter == null) {
            Log.w(TAG, "getLocalBluetoothName: warning: Bluetooth adapter is null: getting default adapter...");
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        String name = bluetoothAdapter.getName();

        if (name == null) {
            Log.w(TAG, "getLocalBluetoothName: Warning: bluetooth adapter name is null: using address.");
            name = bluetoothAdapter.getAddress();
        }

        return name;
    }


    //show list of currently discovered bluetooth devices
    //(begin discovery: add to list by broadcastReceiver)
    public void createDiscoveredList() {
        Log.d(TAG, "listDiscovered: listing discovered devices...");

        //clear list
        deviceCardList = new ArrayList<>();

        if (bluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "listDiscovered: cancelling existing discovery. ");
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
        Toast.makeText(this, "Discovering devices...",
                Toast.LENGTH_LONG).show();
    }


    //todo: add checking
    //show recycler list of currently paired bluetooth devices
    private void createPairedList() {
        Log.d(TAG, "createPairedList: ");
        //set of current paired devices
        set_pairedDevices = bluetoothAdapter.getBondedDevices();

        //testing: (remove the need to work with set)
        ArrayList<BluetoothDevice> arrayDevices = new ArrayList<>(set_pairedDevices);

        //(re)create card list
        deviceCardList = new ArrayList<>();

        //each paired device found:
        for (int i = 0; i < set_pairedDevices.size(); i++) {
            //get type of device (pass icon and name from methods)
            deviceCardList.add(new DeviceCard(chooseIcon(arrayDevices.get(i)), arrayDevices.get(i).getName()));
        }

        buildRecyclerView(deviceCardList, recyc_pairedDevices);
    }


    //builds passed card list into recyclerView via adapter
    public void buildRecyclerView(ArrayList<DeviceCard> cardList, RecyclerView recyclerView) {
        Log.d(TAG, "buildRecyclerView: ");
        //set to true if known size of items will not change: increase performance.
        recyclerView.setHasFixedSize(true);

        recycLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recycLayoutManager);

        recycAdapter = new CardAdapterDevice(cardList);
        recyclerView.setAdapter(recycAdapter);
    }


    //-selects icon based on device type (only included those likely to be encountered in this area
    //(likely but unencumbered device types return android image)
    //todo: combine any catagories to return same icon resource
    public int chooseIcon(BluetoothDevice device) {
        Log.d(TAG, "chooseIcon: ");
        if (device != null) {
            BluetoothClass btClass = device.getBluetoothClass();
            int deviceClass = btClass.getDeviceClass();

            if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) {
                Log.d(TAG, "chooseIcon: HEADPHONES");

                return R.drawable.ic_noun_headset_165986;

            } else if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE) {
                //testing: todo: if occurs, combine with if statement above
                Log.d(TAG, "chooseIcon: HANDSFREE");
                return R.drawable.ic_launcher_foreground;

            } else if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER) {
                Log.d(TAG, "chooseIcon: LOUDSPEAKER");
                return R.drawable.ic_launcher_foreground;

            } else if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO) {
                Log.d(TAG, "chooseIcon: PORTABLE AUDIO");
                return R.drawable.ic_launcher_foreground;

            } else if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO) {
                Log.d(TAG, "chooseIcon: HIFI AUDIO");
                return R.drawable.ic_launcher_foreground;


            } else if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE) {
                Log.d(TAG, "chooseIcon: MICROPHONE");
                return R.drawable.ic_launcher_foreground;

            } else if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED) {
                Log.d(TAG, "chooseIcon: UNCATAGORISED");
                //todo: add audio uncatagorised icon
                return R.drawable.ic_launcher_foreground;

            } else if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
                Log.d(TAG, "chooseIcon: HEADSET");
                return R.drawable.ic_noun_headset_165986;

            } else if (deviceClass == BluetoothClass.Device.COMPUTER_LAPTOP) {
                Log.d(TAG, "chooseIcon: LAPTOP");
                return R.drawable.ic_launcher_foreground;

            } else if (deviceClass == BluetoothClass.Device.PHONE_CELLULAR) {
                Log.d(TAG, "chooseIcon: CELLPHONE");
                return R.drawable.ic_launcher_foreground;

            } else if (deviceClass == BluetoothClass.Device.PHONE_SMART) {
                Log.d(TAG, "chooseIcon: SMARTPHONE");
                return R.drawable.ic_noun_bluetooth_device_734717;

            } else if (deviceClass == BluetoothClass.Device.PHONE_UNCATEGORIZED) {
                Log.d(TAG, "chooseIcon: UNCATAGORISED PHONE");
                return R.drawable.ic_launcher_foreground;

            } else if (deviceClass == BluetoothClass.Device.WEARABLE_HELMET) {
                Log.d(TAG, "chooseIcon: HELMET");
                return R.drawable.ic_noun_helmet_2258845;

            } else if (deviceClass == BluetoothClass.Device.WEARABLE_UNCATEGORIZED) {
                Log.d(TAG, "chooseIcon: UNCATAGORISED WEARABLE");
                return R.drawable.ic_launcher_foreground;

            } else {
                Log.w(TAG, "chooseIcon: Warning: unrecognised device.");
                return R.drawable.ic_noun_bluetooth_unknown731903_cc;
            }
        } else {
            Log.e(TAG, "chooseIcon: Error: passed a NULL device");
            return -1;
        }

    }


    /*--------------------------------------
        (UNUSED) METHODS
    --------------------------------------*/
    //show list of connected devices
    //todo: currently incomplete (only shows bool if connection is currently established but cant find out how to specify to WHAT) - research suggests not possible.
    public void listConnected() {
        Log.d(TAG, "listConnected: listing connected devices...");
        //Todo: list connected devices: getConnectedDevices() does not work: why?
        if (isConnected()) {
            Toast.makeText(this, "IS CONNECTED", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "listConnected: IS connected");
        } else {
            Toast.makeText(this, "NOT CONNECTED", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "listConnected: NOT connected");
        }

    }


    /*--------------------------------------
        BROADCAST RECEIVERS
    --------------------------------------*/
    //-listen for change to bluetooth device/adapter (mostly monitoring status during development)
    private final BroadcastReceiver broadcastReceiver_status = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            //---DEVICE STATUS CHANGE---
            //device found
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d(TAG, "onReceive: DEVICE FOUND");
                if (device != null) {
                    BluetoothClass btClass = device.getBluetoothClass();

                    //resolve device class digits into text format (as no existing method available)
                    //logcat output already in method
                    String deviceClass = deviceClassToString(btClass);
                    //check major class (testing) logcat output in method
                    String deviceMajorClass = deviceMajorClassToString(btClass);

                } else {
                    Log.d(TAG, "onReceive (statusReceiver: FOUND DEVICE IS NULL");
                    //todo: add error handling for null device
                }
            }
            //device now connected
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.d(TAG, "onReceive: CONNECTED");
            }
            //device disconnecting
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                Log.d(TAG, "onReceive: DEVICE DISCONNECTING");
            }
            //device has disconnected
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.d(TAG, "onReceive: DEVICE DISCONNECTED");
            }

            //---ADAPTER STATUS CHANGE---
            //discovery begin
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "Bluetooth Adapter: DISCOVERY STARTED");
            }
            //discover finished
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Bluetooth Adapter: DISCOVERY FINISHED");
            }
            //adapter state changed
            else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                Log.d(TAG, "Bluetooth Adapter: STATE CHANGED");
            }
        }
    };


    //-listens for device discovery, pass discovered device as card to recycler builder
    private BroadcastReceiver broadcastReceiver_discover = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                Log.d(TAG, "onReceive: Device found...");
                BluetoothDevice device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE);

                //testing
//                String discoveredName = device.getName();
//                String discoveredAddress = device.getAddress();
//                Log.d(TAG, "onReceive: found (name & address): "
//                        + device.getName() + ":" + device.getAddress());

                //add discovered device to card list
                deviceCardList.add(new DeviceCard(chooseIcon(device), device.getName()));
                //update list
                buildRecyclerView(deviceCardList, recyc_discoveredDevices);
            }
        }
    };


    /*--------------------------------------
        HELPER METHODS
    --------------------------------------*/
    //-check bt is enabled on device
    private void checkBluetoothState() {
        Log.d(TAG, "checkBluetoothState: ");
        // Check device has Bluetooth and that it is turned on
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), "Device does not support Bluetooth",
                    Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }


    //-allow other devices to discover bluetooth
    //todo: combine this with toggle
    public void makeVisible() {
        Log.d(TAG, "makeVisible: making device visible to others... ");
        Intent discoverintent = new Intent(
                BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverintent.putExtra(
                BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000);
        startActivity(discoverintent);
    }


    //returns true if object is connected(always false prior to API ver 15 (iceCreamSandwich: 2011))
    public boolean isConnected() {
        Log.d(TAG, "isConnected: ");
        boolean retval = false;
        try {
            Method method = bluetoothAdapter.getClass().getMethod("getProfileConnectionState", int.class);

            retval = (Integer) method.invoke(bluetoothAdapter, 1) != 0;
        } catch (Exception e) {
            Toast.makeText(this, "something broke in isConnected()...",
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "listConnected: ERROR exception occurred. debugging needed!");
        }
        return retval;
    }


    //get readable version (string) of device class and output to logcat
    //(currently for debugging info. Order by verbatim class listing)
    public String deviceClassToString(BluetoothClass btClass) {
        Log.d(TAG, "deviceClassToString: ");
        //audio-focus
        if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_CAMCORDER");
            return "AUDIO_VIDEO_CAMCORDER";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_CAR_AUDIO");
            return "AUDIO_VIDEO_CAR_AUDIO";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_HANDSFREE");
            return "AUDIO_VIDEO_HANDSFREE";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_HEADPHONES");
            return "AUDIO_VIDEO_HEADPHONES";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_HIFI_AUDIO");
            return "AUDIO_VIDEO_HIFI_AUDIO";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_LOUDSPEAKER");
            return "AUDIO_VIDEO_LOUDSPEAKER";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_MICROPHONE");
            return "AUDIO_VIDEO_MICROPHONE";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_PORTABLE_AUDIO");
            return "AUDIO_VIDEO_PORTABLE_AUDIO";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_SET_TOP_BOX");
            return "AUDIO_VIDEO_SET_TOP_BOX";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_UNCATEGORIZED");
            return "AUDIO_VIDEO_UNCATEGORIZED";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_VCR) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_VCR");
            return "AUDIO_VIDEO_VCR";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_VIDEO_CAMERA");
            return "AUDIO_VIDEO_VIDEO_CAMERA";
        } else if (btClass.getDeviceClass() ==
                BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_VIDEO_CONFERENCING");
            return "AUDIO_VIDEO_VIDEO_CONFERENCING";
        } else if (btClass.getDeviceClass() ==
                BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER) {
            Log.d(TAG, "deviceClassToString: device type: " +
                    "AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER");
            return "AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_VIDEO_GAMING_TOY");
            return "AUDIO_VIDEO_VIDEO_GAMING_TOY";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_VIDEO_MONITOR");
            return "AUDIO_VIDEO_VIDEO_MONITOR";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
            Log.d(TAG, "deviceClassToString: device type: AUDIO_VIDEO_WEARABLE_HEADSET");
            return "AUDIO_VIDEO_WEARABLE_HEADSET";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.COMPUTER_DESKTOP) {
            Log.d(TAG, "deviceClassToString: device type: COMPUTER_DESKTOP");
            return "COMPUTER_DESKTOP";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA) {
            Log.d(TAG, "deviceClassToString: device type: COMPUTER_HANDHELD_PC_PDA");
            return "COMPUTER_HANDHELD_PC_PDA";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.COMPUTER_LAPTOP) {
            Log.d(TAG, "deviceClassToString: device type: COMPUTER_LAPTOP");
            return "COMPUTER_LAPTOP";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA) {
            Log.d(TAG, "deviceClassToString: device type: COMPUTER_PALM_SIZE_PC_PDA");
            return "COMPUTER_PALM_SIZE_PC_PDA";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.COMPUTER_SERVER) {
            Log.d(TAG, "deviceClassToString: device type: COMPUTER_SERVER");
            return "COMPUTER_SERVER";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.COMPUTER_UNCATEGORIZED) {
            Log.d(TAG, "deviceClassToString: device type: COMPUTER_UNCATEGORIZED");
            return "COMPUTER_UNCATEGORIZED";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.COMPUTER_WEARABLE) {
            Log.d(TAG, "deviceClassToString: device type: COMPUTER_WEARABLE");
            return "COMPUTER_WEARABLE";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.HEALTH_BLOOD_PRESSURE) {
            Log.d(TAG, "deviceClassToString: device type: HEALTH_BLOOD_PRESSURE");
            return "HEALTH_BLOOD_PRESSURE";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.HEALTH_DATA_DISPLAY) {
            Log.d(TAG, "deviceClassToString: device type: HEALTH_DATA_DISPLAY");
            return "HEALTH_DATA_DISPLAY";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.HEALTH_GLUCOSE) {
            Log.d(TAG, "deviceClassToString: device type: HEALTH_GLUCOSE");
            return "HEALTH_GLUCOSE";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.HEALTH_PULSE_OXIMETER) {
            Log.d(TAG, "deviceClassToString: device type: HEALTH_PULSE_OXIMETER");
            return "HEALTH_PULSE_OXIMETER";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.HEALTH_PULSE_RATE) {
            Log.d(TAG, "deviceClassToString: device type: HEALTH_PULSE_RATE");
            return "HEALTH_PULSE_RATE";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.HEALTH_THERMOMETER) {
            Log.d(TAG, "deviceClassToString: device type: HEALTH_THERMOMETER");
            return "HEALTH_THERMOMETER";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.HEALTH_UNCATEGORIZED) {
            Log.d(TAG, "deviceClassToString: device type: HEALTH_UNCATEGORIZED");
            return "HEALTH_UNCATEGORIZED";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.HEALTH_WEIGHING) {
            Log.d(TAG, "deviceClassToString: device type: HEALTH_WEIGHING");
            return "HEALTH_WEIGHING";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.PHONE_CELLULAR) {
            Log.d(TAG, "deviceClassToString: device type: PHONE_CELLULAR");
            return "PHONE_CELLULAR";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.PHONE_CORDLESS) {
            Log.d(TAG, "deviceClassToString: device type: PHONE_CORDLESS");
            return "PHONE_CORDLESS";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.PHONE_ISDN) {
            Log.d(TAG, "deviceClassToString: device type: PHONE_ISDN");
            return "PHONE_ISDN";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY) {
            Log.d(TAG, "deviceClassToString: device type: PHONE_MODEM_OR_GATEWAY");
            return "PHONE_MODEM_OR_GATEWAY";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.PHONE_SMART) {
            Log.d(TAG, "deviceClassToString: device type: PHONE_SMART");
            return "PHONE_SMART";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.PHONE_UNCATEGORIZED) {
            Log.d(TAG, "deviceClassToString: device type: PHONE_UNCATEGORIZED");
            return "PHONE_UNCATEGORIZED";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.TOY_CONTROLLER) {
            Log.d(TAG, "deviceClassToString: device type: TOY_CONTROLLER");
            return "TOY_CONTROLLER";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE) {
            Log.d(TAG, "deviceClassToString: device type: TOY_DOLL_ACTION_FIGURE");
            return "TOY_DOLL_ACTION_FIGURE";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.TOY_GAME) {
            Log.d(TAG, "deviceClassToString: device type: TOY_GAME");
            return "TOY_GAME";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.TOY_ROBOT) {
            Log.d(TAG, "deviceClassToString: device type: TOY_ROBOT");
            return "TOY_ROBOT";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.TOY_UNCATEGORIZED) {
            Log.d(TAG, "deviceClassToString: device type: TOY_UNCATEGORIZED");
            return "TOY_UNCATEGORIZED";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.TOY_VEHICLE) {
            Log.d(TAG, "deviceClassToString: device type: TOY_VEHICLE");
            return "TOY_VEHICLE";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.WEARABLE_GLASSES) {
            Log.d(TAG, "deviceClassToString: device type: WEARABLE_GLASSES");
            return "WEARABLE_GLASSES";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.WEARABLE_HELMET) {
            Log.d(TAG, "deviceClassToString: device type: WEARABLE_HELMET");
            return "WEARABLE_HELMET";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.WEARABLE_JACKET) {
            Log.d(TAG, "deviceClassToString: device type: WEARABLE_JACKET");
            return "WEARABLE_JACKET";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.WEARABLE_PAGER) {
            Log.d(TAG, "deviceClassToString: device type: WEARABLE_PAGER");
            return "WEARABLE_PAGER";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.WEARABLE_UNCATEGORIZED) {
            Log.d(TAG, "deviceClassToString: device type: WEARABLE_UNCATEGORIZED");
            return "WEARABLE_UNCATEGORIZED";
        } else if (btClass.getDeviceClass() == BluetoothClass.Device.WEARABLE_WRIST_WATCH) {
            Log.d(TAG, "deviceClassToString: device type: WEARABLE_WRIST_WATCH");
            return "WEARABLE_WRIST_WATCH";
        } else {
            Log.d(TAG, "deviceClassToString: DEVICE CLASS UNRECOGNISED: " +
                    "(device code possibly not specified) value: " + btClass.toString());
            //not predefined in BluetoothClass.class: return identifier to later research
            return "DEVICE CLASS UNRECOGNISED: value: " + btClass.toString();
        }
    }


    //return string of bluetooth device class major type, output to logcat
    public String deviceMajorClassToString(BluetoothClass btClass) {
        Log.d(TAG, "deviceMajorClassToString: ");
        if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO) {
            Log.d(TAG, "deviceMajorClassToString: device major type: AUDIO_VIDEO");
            return "AUDIO_VIDEO";
        } else if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.COMPUTER) {
            Log.d(TAG, "deviceMajorClassToString: device major type: COMPUTER");
            return "COMPUTER";
        } else if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.HEALTH) {
            Log.d(TAG, "deviceMajorClassToString: device major type: HEALTH");
            return "HEALTH";
        } else if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.IMAGING) {
            Log.d(TAG, "deviceMajorClassToString: device major type: IMAGING");
            return "IMAGING";
        } else if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.MISC) {
            Log.d(TAG, "deviceMajorClassToString: device major type: MISC");
            return "MISC";
        } else if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.NETWORKING) {
            Log.d(TAG, "deviceMajorClassToString: device major type: NETWORKING");
            return "NETWORKING";
        } else if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.PERIPHERAL) {
            Log.d(TAG, "deviceMajorClassToString: device major type: PERIPHERAL");
            return "PERIPHERAL";
        } else if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE) {
            Log.d(TAG, "deviceMajorClassToString: device major type: PHONE");
            return "PHONE";
        } else if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.TOY) {
            Log.d(TAG, "deviceMajorClassToString: device major type: TOY");
            return "TOY";
        } else if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.UNCATEGORIZED) {
            Log.d(TAG, "deviceMajorClassToString: device major type: UNCATEGORIZED");
            return "UNCATEGORIZED";
        } else if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.WEARABLE) {
            Log.d(TAG, "deviceMajorClassToString: device major type: WEARABLE");
            return "WEARABLE";
        } else {
            //not predefined in BluetoothClass.class: return identifier to later research
            Log.d(TAG, "deviceMajorClassToString: DEVICE CLASS UNRECOGNISED: " +
                    "(device code possibly not specified) value: " + btClass.toString());
            return "DEVICE CLASS UNRECOGNISED: value: " + btClass.toString();
        }
    }


    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth.
     * Putting the proper permissions in the manifest is not enough.
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    @TargetApi(23)
    private void checkBTPermissions() {
        Log.d(TAG, "checkBTPermissions: ");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck =
                    this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck +=
                    this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check: SDK version < LOLLIPOP.");
        }
    }

}
