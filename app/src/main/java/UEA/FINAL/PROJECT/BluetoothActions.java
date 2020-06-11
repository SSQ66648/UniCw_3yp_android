/*--------------------------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System Mobile Device App
 * FILE:        BluetoothActions.Java
 * LAYOUT(S):   activity_bluetooth_actions.xml
 * DESCRIPTION: Handles toggling of device Bluetooth statuses as well as discovery, pairing and
 *              connecting to nearby Bluetooth devices. active connection is passed to service to
 *              maintain connection while device is locked.
 *              (based on combination of test classes: "SerialWingood" and "BluetoothActions")
 *              Actual "connection" work has been moved to foreground service due to needing to host
 *              the Bluetooth socket there. Currently forwards the MAC address (if available) to
 *              following activity.
 * AUTHOR:      SSQ16SHU / 100166648
 *--------------------------------------------------------------------------------------------------
 * HISTORY:     v1.0    200315  Initial implementation (from previous test classes).
 *              v1.1    200315  Added click events to cards, additional dialogs.
 *              v1.2    200316  Added card click connect/disconnect logic, solved switch device
 *                              logic, added watchedBooleans and listeners to trigger the added
 *                              create activity intent method, also show/hiding start intent button
 *              v1.3    200317  Added enabled checking for buttons, moved some checks to onResume.
 *              v1.3.1  200317  Added default value for device address: workaround for development.
 *              v1.3.2  200321  Added development workaround: deliberate assignment of bike icon to
 *                              HC-05 module (as it has no class to detect).
 *              v1.4    200321  Changed layout to include constraint container for (now green) start
 *                              system button.
 *              v1.4.1  200321  Added audio playback to test if helmet is connected or not
 *                              (potentially followed by a confirmation of success dialog layout).
 *--------------------------------------------------------------------------------------------------
 * NOTES:
 *      +   logcat records "errors" of no adapter attached, however attempting to solve this has
 *          resulted in much wasted time: as this does not affect the intended behaviour,
 *          indefinitely postponed.
 *      +   initial pairing code idea has been abandoned as multiple sources claim best to let
 *          android handle it (attempting to connect to unpaired device prompts pairing by system).
 *--------------------------------------------------------------------------------------------------
 * FUTURE IMPROVEMENTS:
 *      +   current buttons being subject to expanding recyclerview is not ideal:
 *              either move buttons above and focus switch to selected,
 *              use same view for both buttons by click,
 *              add ability to collapse view to zero on second click
 *      +   setting card clicked to color/change text is proving very difficult to work out:
 *          unfortunately postponed indefinitely as it alone has taken too many hours of work:
 *          prioritise functionality
 *      +   has been absorbed into the service host activity layout, however this has been postponed
 *          until the improved UI from user testing is complete
 *--------------------------------------------------------------------------------------------------
 * TO DO LIST:
 *      //todo: add enable bt toast to button click if not enabled
 *      //todo: potentially build recycleres once and then update adapter when needed
 *      //todo: add green on connect
 *      //todo: address orientation of yes/no mental model
 *      //todo: add device type checking on connect as? - change from option to if these types of device, connect as, else if these he;lmet types connect as
 *      //todo: add set device type to switch prompt
 *      //todo: use same getcardlist logic to access specific card ... can this be used to change background color?
 *      //todo: select TYPE of connection using connect logic, then on final button to next activity: connect to 'both'?
 *      //todo: add 'collapse recycler' when button clicked 2nd time
 *      //todo: add countdown/bool listener for list button bt check fallthrough
 *      //todo: reword helmet assumption toast
 *      //todo: add toasts re status - bt enabled etc
 *      //todo: add check that paired device has address in it. and/or begin discovery for paired devices too:
 *          (how to show if found or not: need to change card: back to how to change background of card)
 *      //todo: pair discovered device connecting to
 *--------------------------------------------------------------------------------------------------
 * OPEN ISSUES:
 *      +   Despite eatensive searching, a way to obtain the view ID of the clicked cardView to set
 *          it's background to green upon user selection has not been possible. this is postponed
 *          for functionality focus
 -------------------------------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;
/*--------------------------------------
    IMPORT LIST
--------------------------------------*/

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
//import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class BluetoothActions extends AppCompatActivity implements View.OnClickListener {
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
    private Button button_moveActivity;

    /*------------------
        Card List Variables
    ------------------*/
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> set_pairedDevices;
    //only provide items currently needed (performance)
    private CardAdapterDevice recycAdapter;
    private RecyclerView.LayoutManager recycLayoutManager;
    //collection of device cards to populate recycler lists
    private ArrayList<DeviceCard> deviceCardList;

    /*------------------
        Connection Logic Variables
    ------------------*/
    //EXTRA string to send on to mainActivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    //intent to move activities when selected both devices
    Intent bluetoothActivityIntent;
    //address of 'bike' bluetooth module to pass as extra with intent
    private String bikeMacAddress;
    //watched booleans to trigger activity change
    private WatchedBool helmetWatchedBool = new WatchedBool();
    private WatchedBool bikeWatchedBool = new WatchedBool();
    //switch device latch: wait for disconnect complete before reconnecting
    CountDownLatch countDownLatch;


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

        //---VIEWS----
        toggle_enableBT = findViewById(R.id.toggle_bluetoothactions_enablebluetooth);
        toggle_enableBT.setOnClickListener(this);
        button_discoverDevices = findViewById(R.id.button_bluetoothactions_discoverdevices);
        button_discoverDevices.setOnClickListener(this);
        button_pairedDevices = findViewById(R.id.button_bluetoothactions_paireddevices);
        button_pairedDevices.setOnClickListener(this);
        button_moveActivity = findViewById(R.id.button_bluetoothactions_start_activity);
        button_moveActivity.setOnClickListener(this);
        text_connectingInfo = findViewById(R.id.infotext_bluetoothactions_connecting);
        recyc_discoveredDevices = findViewById(R.id.recycler_bluetoothactions_discovereddevices);
        recyc_pairedDevices = findViewById(R.id.recycler_bluetoothactions_paireddevices);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        //remove any connecting message if displayed
        text_connectingInfo.setVisibility(View.GONE);
        //check if both are still connected: show/remove button to next activity
        checkDevicesConnected();
        //check if bt enabled and set toggle as needed
        toggle_enableBT.setChecked(checkBluetoothState());

        //testing: moved from onCreate::
        registerReceiverStatusChange();
        registerReceiverDiscover();

        //set bool listeners
        helmetWatchedBool.setBooleanChangeListener(new VariableChangeListener() {
            @Override
            public void onVariableChanged(Object... newValue) {
                Log.d(TAG, "onVariableChanged: ");
                checkDevicesConnected();
            }
        });

        bikeWatchedBool.setBooleanChangeListener(new VariableChangeListener() {
            @Override
            public void onVariableChanged(Object... newValue) {
                Log.d(TAG, "onVariableChanged: ");
                //opposite of above
                checkDevicesConnected();
            }
        });

        //cannot use here as will enter loop if not granted.
//        checkBluetoothState();
    }


    @Override
    protected void onStop() {
        super.onStop();
        //todo: close release any resources (might not be needed if not sockets in activity
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
                break;
            case R.id.button_bluetoothactions_discoverdevices:
                //check if bt enabled
                if (checkBluetoothState()) {
                    Log.d(TAG, "onClick: BT enabled.");
                } else {
                    Log.d(TAG, "onClick: BT not enabled: user prompted.");
                }
                createDiscoveredList();
                break;
            case R.id.button_bluetoothactions_paireddevices:
                //check if bt enabled
                if (checkBluetoothState()) {
                    Log.d(TAG, "onClick: BT enabled.");
                } else {
                    Log.d(TAG, "onClick: BT not enabled: user prompted.");
                }
                createPairedList();
                break;
            case R.id.button_bluetoothactions_start_activity:
                //hide button, replace with connecting message
                button_moveActivity.setVisibility(View.GONE);
                text_connectingInfo.setVisibility(View.VISIBLE);
                startActivity(bluetoothActivityIntent);
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
            //combine methods: if adapter has been enabled: make device visible
            makeVisible();
        } else {
            Log.w(TAG, "enableBluetooth: notChecked. ");
//            Toast.makeText(getApplicationContext(),
//                    " Bluetooth turned off", Toast.LENGTH_LONG).show();
            if (bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "enableBluetooth: disabling adapter. ");
                //disable the bt
                if (bluetoothAdapter != null) {
                    bluetoothAdapter.disable();
                }
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
//        Toast.makeText(getApplicationContext(), "Discovering devices...",
//                Toast.LENGTH_LONG).show();
    }


    //todo: add checking::::: if bluetooth off Toast
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
            //todo: test device exists check
            if (arrayDevices.get(i) != null) {
                //get type of device (pass icon and name from methods) (testing: add device to card itself for connecting...?)
                deviceCardList.add(new DeviceCard(chooseIcon(arrayDevices.get(i)), arrayDevices.get(i).getName(), arrayDevices.get(i)));
            } else {
                //paired device does not have any data found (not (re)discovered if not requested)
                Log.e(TAG, "createPairedList: Error: device not included in card");
//                Toast.makeText(getApplicationContext(), "ERROR: paired device has not been discovered!", Toast.LENGTH_SHORT).show();
            }
        }
        buildRecyclerView(deviceCardList, recyc_pairedDevices);
    }


    //builds passed card list into recyclerView via adapter (including itemClickListener)
    public void buildRecyclerView(final ArrayList<DeviceCard> cardList, RecyclerView recyclerView) {
        Log.d(TAG, "buildRecyclerView: ");
        //set to true if known size of items will not change: increase performance.
        recyclerView.setHasFixedSize(true);

        recycLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recycLayoutManager);

        recycAdapter = new CardAdapterDevice(cardList);
        recyclerView.setAdapter(recycAdapter);

        //task to carry out when clicking a card item
        recycAdapter.setOnItemClickListener(new CardAdapterDevice.OnItemClickListener() {
            @Override
            public void onItemClick(final int position) {

                //use same dialog object for both cases:
                final Dialog dialog = new Dialog(BluetoothActions.this);

                //todo: check if card device is connected
                if (!cardList.get(position).getConnectionStatus()) {
                    //not connected: prompt to connect

                    //create pop up choice to connect to device
                    dialog.setContentView(R.layout.dialog_popup_connect_to_device);
                    dialog.setTitle("Connect to Device");

                    //assign pop up buttons
                    Button dialogButton_helmet = dialog.findViewById(R.id.button_popup_connect_helmet);
                    dialogButton_helmet.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestConnectDevice(cardList.get(position), DeviceCard.CONNECTION_HELMET);
                            dialog.dismiss();
                        }
                    });

                    Button dialogButton_bike = dialog.findViewById(R.id.button_popup_connect_bike);
                    dialogButton_bike.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestConnectDevice(cardList.get(position), DeviceCard.CONNECTION_BIKE);
                            dialog.dismiss();

                        }
                    });

                    Button dialogButton_cancel = dialog.findViewById(R.id.button_popup_connect_cancel);
                    dialogButton_cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();

                } else {

                    //device is already connected: prompt disconnect todo: add handling if unexpected type?
                    requestDisconnectDevice(cardList.get(position), cardList.get(position).getConnectionType());
                }
            }
        });
    }


    //todo: ADD ENABLED CHECK HERE OR ....disable all card clicks if not enabled: show toast to enable?
    //-attempts to connect to device and sets bool flag which type is connected
    public void requestConnectDevice(final DeviceCard card, final String type) {
        Log.d(TAG, "requestConnectDevice: passed card: " + card.toString());

        //check both not already connected? (should not be possible)
        switch (type) {
            case DeviceCard.CONNECTION_HELMET:
                //helmet connection requested:
                if (!helmetWatchedBool.getValue()) {
                    Log.d(TAG, "requestConnectDevice: helmet not connected: proceed.");
                    //demo measures:
//                    Toast.makeText(getApplicationContext(), "THIS PROGRAM HAS TO ASSUME YOUR HEADSET IS ALREADY CONNECTED", Toast.LENGTH_SHORT).show();

                    //set color (ABANDONED)
//            view.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                    //record type and specific card connected
                    helmetWatchedBool.setValue(true);
                    card.setConnectionStatus(true, DeviceCard.CONNECTION_HELMET);
                    Log.d(TAG, "requestConnectDevice: HELMET CONNECTED");

                    //audio check for connection (used in case of having to pair and connect outside of this app):
                    final MediaPlayer mediaplayer = new MediaPlayer();
                    AssetFileDescriptor afd = null;
                    try {
                        Log.d(TAG, "requestConnectDevice: create media player for helmet test");
                        afd = getAssets().openFd("tts_lola_prompt_helmetbluetoothtest.mp3");
                        mediaplayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                                afd.getLength());
                        mediaplayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                //release resources from priority player
                                mediaplayer.stop();
                                mediaplayer.reset();
                                mediaplayer.release();
                            }
                        });
                        mediaplayer.prepare();
                        mediaplayer.start();
                    } catch (IOException e) {
                        Log.e(TAG, "requestConnectDevice: Error: media player error");
                        e.printStackTrace();
                    }
                } else {
                    Log.w(TAG, "requestConnectDevice: Warning: Helmet device already connected: prompt for switch connect");
                    switchDevice(card, type);
                }
                break;
            case DeviceCard.CONNECTION_BIKE:
                //bike connection requested
                if (!bikeWatchedBool.getValue()) {
                    Log.d(TAG, "requestConnectDevice: bike not connected: proceed.");
                    //set color (ABANDONED)
//            view.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                    bikeWatchedBool.setValue(true);
                    card.setConnectionStatus(true, DeviceCard.CONNECTION_BIKE);
                    //get bluetooth address
                    bikeMacAddress = card.getDevice().getAddress();

                    //todo: ensure device exists here?
                    Log.d(TAG, "requestConnectDevice: bike address: " + bikeMacAddress);
                    if (card.getDevice().getAddress() == null) {
                        Log.e(TAG, "requestConnectDevice: Error: BIKE ADDRESS IS NULL: employing work-around of hard coded value for development.");
                        bikeMacAddress = "FC:A8:9A:00:4A:DF";
                    }

                    Log.d(TAG, "requestConnectDevice: BIKE \"CONNECTED:\"");
                } else {
                    Log.w(TAG, "requestConnectDevice: Warning: bike device already connected: prompt for disconnect");
                    requestDisconnectDevice(card, type);
                }
                break;
            default:
                //unrecognised connection type
                Log.e(TAG, "requestConnectDevice: Error: unrecognised connection type requested");
                //todo: how to handle? (should not be possible but have fallen into that trap before)
                break;
        }
    }


    //-disconnects previous device of type and connects current device
    public void switchDevice(final DeviceCard card, final String type) {
        Log.d(TAG, "switchDevice: ");
        //prompt user for switch
        final Dialog dialog = new Dialog(BluetoothActions.this);
        dialog.setContentView(R.layout.dialog_popup_switch_device_connection);

        Button dialogButton_switchDevice = dialog.findViewById(R.id.button_popup_switch_device);
        dialogButton_switchDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: switch device");
                dialog.dismiss();
                //stop code until disconnected
                countDownLatch = new CountDownLatch(1);

                //disconnect old connection
                for (int i = 0; i < recycAdapter.getItemCount(); i++) {
                    //iterate all cards until existing connection type is found, then disconnect
                    if (recycAdapter.getCardList().get(i).getConnectionType() == type) {
                        disconnectDevice(recycAdapter.getCardList().get(i), type);
                    }
                }

                try {
                    Log.d(TAG, "onClick: awaiting disconnect latch");
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    Log.e(TAG, "onClick: Error: awaiting countdownLatch");
                    e.printStackTrace();
                }

                //establish new connection:
                requestConnectDevice(card, type);
            }
        });
        Button dialogButton_switchDeviceCancel = dialog.findViewById(R.id.button_popup_switch_cancel);
        dialogButton_switchDeviceCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: cancel");
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    //-disconnects device from passed card
    public void requestDisconnectDevice(final DeviceCard card, final String type) {
        Log.d(TAG, "requestDisconnectDevice: ");
        //create dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_popup_disconnect_from_device);
        dialog.setTitle("Disconnect from [" + card.getConnectionType() + "] Device?");

        //set buttons
        Button dialogButton_disconnect = dialog.findViewById(R.id.button_popup_disconnect_device);
        dialogButton_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: disconnect");
                disconnectDevice(card, type);
                dialog.dismiss();
            }
        });

        Button dialogButton_disconnectCancel = dialog.findViewById(R.id.button_popup_disconenct_cancel);
        dialogButton_disconnectCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: cancel");
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    public void disconnectDevice(DeviceCard card, String type) {
        Log.d(TAG, "disconnectDevice: ");
        //todo: DISCONNECT CODE
        //set local flag (then card) to disconnected
        if (card.getConnectionType() == DeviceCard.CONNECTION_HELMET) {
            helmetWatchedBool.setValue(false);
        } else if (card.getConnectionType() == DeviceCard.CONNECTION_BIKE) {
            bikeWatchedBool.setValue(false);
        } else if (card.getConnectionType() == DeviceCard.CONNECTION_NONE) {
            Log.e(TAG, "onClick: Error: card connection type indicates is NOT connected");
            //todo: handle ?
        } else {
            Log.e(TAG, "onClick: Error: unexpected card type");
            //todo: handling?
        }
        card.setConnectionStatus(false, DeviceCard.CONNECTION_NONE);
        Log.d(TAG, "onClick: device disconnected");
        //allow switch to recall requestConnect
        if (countDownLatch != null) {
            Log.d(TAG, "disconnectDevice: latch countdown");
            countDownLatch.countDown();
        } else {
            Log.d(TAG, "disconnectDevice: no latch to countdown");
        }
    }


    //-check if both bike and helmet are connected
    public void checkDevicesConnected() {
        Log.d(TAG, "checkDevicesConnected: ");
        if (helmetWatchedBool.getValue() && bikeWatchedBool.getValue()) {
            Log.d(TAG, "checkDevicesConnected: both connected...");
            //create intent to next activity
            bluetoothActivityIntent = new Intent(BluetoothActions.this, PrimeForegroundServiceHost.class);
            //include bike address
            bluetoothActivityIntent.putExtra(EXTRA_DEVICE_ADDRESS, bikeMacAddress);
            //todo: either trigger here or use button?

            //show button
            button_moveActivity.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "checkDevicesConnected: both not connected...");
            //no longer have both connected: remove button
            button_moveActivity.setVisibility(View.GONE);
        }
    }

    //-selects icon based on device type (only included those likely to be encountered in this area
    //(likely but unencumbered device types return android image)
    //todo: combine any catagories to return same icon resource
    public int chooseIcon(BluetoothDevice device) {
        Log.d(TAG, "chooseIcon: ");
        if (device != null) {
            BluetoothClass btClass = device.getBluetoothClass();
            int deviceClass = btClass.getDeviceClass();

            //testing:
            Log.d(TAG, "chooseIcon: device class:\n\t" +
                    deviceClassToString(btClass));

            //development workaround: bluetooth module in prototype has no class: manually assign:
            if (device.getName() != null && device.getName().equals("HC-05")) {
                return R.drawable.ic_small_noun_motorcycle_2713773;
            }


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
        (UNUSED TEST) METHODS
    --------------------------------------*/
    //show list of connected devices
    public void listConnected() {
        Log.d(TAG, "listConnected: listing connected devices...");
        //Todo: list connected devices: getConnectedDevices() does not work: why?
        if (isConnected()) {
//            Toast.makeText(getApplicationContext(), "IS CONNECTED", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "listConnected: IS connected");
        } else {
//            Toast.makeText(getApplicationContext(), "NOT CONNECTED", Toast.LENGTH_SHORT).show();
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
                deviceCardList.add(new DeviceCard(chooseIcon(device), chooseName(device), device));
                //update list
                buildRecyclerView(deviceCardList, recyc_discoveredDevices);
            }
        }
    };


    /*--------------------------------------
        HELPER METHODS
    --------------------------------------*/
    //-assigns address as device name if one not available
    public String chooseName(BluetoothDevice device) {
        if (device.getName() != null) {
            //use existing name
            return device.getName();
            //substitute class type
        } else if (deviceClassToString(device.getBluetoothClass()) != "DEVICE CLASS UNRECOGNISED") {
            return deviceClassToString(device.getBluetoothClass());
        } else if (device.getAddress() != null) {
            return device.getAddress();
        } else {
            return "DEVICE CANNOT BE IDENTIFIED";
        }
    }


    //-check bt is enabled on device (prompt user to turn on with dialog box) //todo: could use this to "disable" cards on click if not enabled?
    private boolean checkBluetoothState() {
        Log.d(TAG, "checkBluetoothState: ");
        // Check device has Bluetooth and that it is turned on
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
//            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth",
//                    Toast.LENGTH_SHORT).show();
            return false;
        } else {
            if (bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
                return true;
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
                return false;
            }
        }
    }


    //returns true if object is connected(always false prior to API ver 15 (iceCreamSandwich: 2011))
    public boolean isConnected() {
        Log.d(TAG, "isConnected: ");
        boolean retval = false;
        try {
            Method method = bluetoothAdapter.getClass().getMethod("getProfileConnectionState", int.class);

            retval = (Integer) method.invoke(bluetoothAdapter, 1) != 0;
        } catch (Exception e) {
//            Toast.makeText(getApplicationContext(), "something broke in isConnected()...",
//                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "listConnected: ERROR exception occurred. debugging needed!");
            e.printStackTrace();
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
            Log.d(TAG, "deviceClassToString: DEVICE CLASS UNRECOGNISED: value: " + btClass.toString());
            return "DEVICE CLASS UNRECOGNISED";
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
