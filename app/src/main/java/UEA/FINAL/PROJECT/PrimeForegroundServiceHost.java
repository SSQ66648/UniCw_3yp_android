/*------------------------------------------------------------------------------
 *
 * PROJECT:     3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        GpsForegroundServiceActivity.Java
 *
 * LAYOUT(S):   activity_gps_foreground_service_activity.xml
 *              content_gps_foreground_service_activity.xml
 *
 * DESCRIPTION: host activity for implementation of gps containing foreground service class.
 *
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v1.0    200223  Initial implementation.
 *              v1.1    200224  added button (unnecessarily: easier to test from within service than
 *                              bind to activity) -remove later if needed
 *              v1.2    200304  added battery manager check method to spare button.
 *              v1.3    200311  added test audio button and binding of service methods/class.
 *              v1.4    200313  many failed attempts at using various bindings to send method
 *                              trigger to service.
 *              v1.5    200313  added broadcast to send intent with method choice as extra (avoid
 *                              binding: as either unbinding kills service, or cannot reconnect
 *                              (bind is new instance every time), removed many dead lines.
 *              v1.6    200317  Added incoming bluetooth address from BluetoothActions class.
 *              v1.6.1  200318  Added workaround-default check for intent extra bike address.
 *              v1.7    200319  Added broadcast listener for UI updates from service (efficient and
 *                              wont crash if no activity loaded to receive message ie screen off),
 *                              layout incl. textViews to set with values of received extras.
 *              v1.8    200321  Added audio to broadcast listener as service being killed too
 *                              quickly to notify user from there.
 *              v1.9    200416  added button to demo mode, changed startService to take flags
 *              v2.0    200419  changed layout significantly to include bottom 'button bar' and
 *                              various sections set to "gone" - to be used in event bluetooth
 *                              activity can be integrated into this one with time available
 *              v2.1    200419  commented out test buttons (to be moved to main menu later)
 *              v3.0    200419  added array of image resources to be displayed when demo mode
 *                              updates location
 *              v3.1    200420  Naive version of button visibility swapping (not yet added listener
 *                              or other checks for running service)
 *------------------------------------------------------------------------------
 * NOTES:
 *          +   not currently stopping service on destroy as this has proved problematic (if switch
 *              to another app or notification for example)
 *              todo: will need to consider how to handle this if it is a problem? - ie if activity gone, have to reopen to get to stop button -tie ervice stop to application lifecycle somehow?
 *          +   no enabled/etc have been implemented for time (other than debug logs): will include
 *              them in next iteration if this version of the service works
 *
 *              date notation: YYMMDD
 *              comment format:
 *                  //---GROUP---
 *                  //explanation
 *
 *------------------------------------------------------------------------------
 * TO DO LIST:
 * //todo: restore buttons to main menu
 *      //todo: add status display for bt devices
 *      //todo: ADD CHECKING FOR RUNNING SERVICE TO CONTROL START/STOP  BUTTON VISIBILITY STATUS
 *      //todo: checking for bt connections?
 *      //todo: consider enum for method choices (const)?
 *      //todo: COMBINE BTACTIONS ACTIVITY??
 *      //todo: hide/collapse bike status until something to view? (visible on connect?)
 *      //todo: add better notification to user re bt connecting
 *      //todo: add check for bt connection failure (if fail: notify user and maybe dont start service?) -add testing override of course.
 *      //todo: debug no network on start crash?
 -----------------------------------------------------------------------------*/
package UEA.FINAL.PROJECT;
/*--------------------------------------
    IMPORT LIST
--------------------------------------*/

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;

import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;

public class PrimeForegroundServiceHost extends AppCompatActivity {
    private static final String TAG = "GpsForegroundServiceAct";

    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    //name of intent extra sent via broadcast to method receiver in service
    public static final String METHOD_TRIGGER = "methodMessage";
    //used to receive broadcasts from activity: value unimportant
    public static final String SERVICE_BROADCASTRECEIVER_UI_UPDATE = "updateUi";
    public static final String SERVICE_BROADCASTRECEIVER_CONNECTION_ERROR = "btError";
    //view identifier constants for broadcast listener
    public static final String UI_UPDATE_INDICATOR_LEFT = "indLeft";
    public static final String UI_UPDATE_INDICATOR_RIGHT = "indRight";
    public static final String UI_UPDATE_LIGHTS_LOW = "lightsLow";
    public static final String UI_UPDATE_LIGHTS_HIGH = "lightsHigh";
    public static final String UI_UPDATE_SPEED_LIMIT = "speedLimit";
    public static final String UI_UPDATE_SPEED_ACTUAL = "speedActual";


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    //---LAYOUT---
    private Button startService;
    private Button stopService;
    private Button testBatt;
    private Button testAudio;
    private Button demoMode;

    private TextView textIndicateR;
    private TextView textIndicateL;
    private TextView textLightL;
    private TextView textLightH;
    private TextView textSpeedLimit;
    private TextView textSpeedActual;

    private TextView mapRoadName;
    private ImageView mapImage;

    //---VARIABLES---
    Intent foregroundIntent;
    //local copy of bluetoothActions intent-passed device address
    String bike_MAC;
    //map image resources for demo mode
    int[] mapResources = {R.drawable.map_01, R.drawable.map_02, R.drawable.map_03,
            R.drawable.map_04, R.drawable.map_05, R.drawable.map_06, R.drawable.map_07,
            R.drawable.map_08, R.drawable.map_09, R.drawable.map_10, R.drawable.map_11,
            R.drawable.map_12, R.drawable.map_13, R.drawable.map_14, R.drawable.map_15,
            R.drawable.map_16, R.drawable.map_17, R.drawable.map_18, R.drawable.map_19,
            R.drawable.map_20, R.drawable.map_21, R.drawable.map_22, R.drawable.map_23,
            R.drawable.map_24, R.drawable.map_25, R.drawable.map_26};


    /*--------------------------------------
        LIFECYCLE
    --------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primaryforegroundservicehost);
        Log.v(TAG, "onCreate: ");

        //---TOOLBAR---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Primary Service Host Activity");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //---VIEWS---
        //status textViews
        textIndicateL = findViewById(R.id.textupdate_indicate_left);
        textIndicateR = findViewById(R.id.textupdate_indicate_right);
        textLightL = findViewById(R.id.textupdate_lights_low);
        textLightH = findViewById(R.id.textupdate_lights_high);
        textSpeedLimit = findViewById(R.id.textupdate_speed_limit);
        textSpeedActual = findViewById(R.id.textupdate_speed_actual);

        //map views
        mapRoadName = findViewById(R.id.text_label_demomap_or_roadname);
        mapImage = findViewById(R.id.image_demomap);

        //---BUTTONS---
        startService = findViewById(R.id.button_gpsforegroundservice_start);
        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "onClick: start service: IRL");
//                Toast.makeText(getApplicationContext(), "Starting Service...", Toast.LENGTH_SHORT).show();
                startGpsService("irl");

                //naive button visibility swap
                stopService.setVisibility(View.VISIBLE);
                startService.setVisibility(View.GONE);
                demoMode.setVisibility(View.GONE);
            }
        });

        //run demo mode version of primary service
        demoMode = findViewById(R.id.button_demoservice_start);
        demoMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: start service: DEMO");
                startGpsService("demo");

                //naive button visibility swap
                stopService.setVisibility(View.VISIBLE);
                startService.setVisibility(View.GONE);
                demoMode.setVisibility(View.GONE);
            }
        });

        stopService = findViewById(R.id.button_gpsforegroundservice_stop);
        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "onClick: stop service");
                stopGpsService();
//                Toast.makeText(getApplicationContext(), "Stopping Service...", Toast.LENGTH_SHORT).show();

                //naive button visibility swap
                stopService.setVisibility(View.GONE);
                startService.setVisibility(View.VISIBLE);
                demoMode.setVisibility(View.VISIBLE);
            }
        });

        //todo: commented out buttons for testing: move functions to main menu

//        //check app battery optimisation
//        testBatt = findViewById(R.id.button_test_battery_optimisation);
//        testBatt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.v(TAG, "onClick: check batter optimization");
//                checkOptimization();
//            }
//        });
//
//        //test audio of service
//        testAudio = findViewById(R.id.button_test_audiofile_gpsforegroundservice);
//        testAudio.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.v(TAG, "onClick: test audio");
//                sendMsg();
//            }
//        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart: (re) creating service intent");
        foregroundIntent = new Intent(this, PrimeForegroundService.class);


    }


    @Override
    protected void onResume() {
        Log.v(TAG, "onResume: ");
        super.onResume();
        //todo: add handling in event of intent lost? (back button / testing shortcut?)
        //get bluetooth address (plural, possibly with UUIDs as well if they can be obtained later):
        Intent intent = getIntent();
        bike_MAC = intent.getStringExtra(BluetoothActions.EXTRA_DEVICE_ADDRESS);
        Log.v(TAG, "onResume: obtained MAC address: " + bike_MAC + "\n" +
                "Adding bike address to foreground service intent...");

        //todo: fix work around: (dont know why intent is losing the address en-route?
        if (bike_MAC == null) {
            Log.e(TAG, "requestConnectDevice: Error: BIKE ADDRESS IS NULL: employing work-around of hard coded value for development.");
            bike_MAC = "FC:A8:9A:00:4A:DF";
        }

        //add address to foreground intent
        foregroundIntent.putExtra(BluetoothActions.EXTRA_DEVICE_ADDRESS, bike_MAC);

        //register receiver for service requests to trigger UI updates
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceBroadcastReceiver,
                new IntentFilter(PrimeForegroundServiceHost.SERVICE_BROADCASTRECEIVER_UI_UPDATE));

        //register receiver for signalling bluetooth setup error
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceBroadcastReceiver_BT_error,
                new IntentFilter(
                        PrimeForegroundServiceHost.SERVICE_BROADCASTRECEIVER_CONNECTION_ERROR));
    }


    @Override
    protected void onPause() {
        Log.v(TAG, "onPause: ");
        super.onPause();

        Log.v(TAG, "onPause: unregistering broadcastReceiver");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceBroadcastReceiver);
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mServiceBroadcastReceiver_BT_error);
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop: ");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy: ");
        super.onDestroy();

        //todo: review how to stop "rouge" service is app no longer exists? (stopping it here causes issues when activity interrupted)
        //stopGpsService();
    }


    /*--------------------------------------
        METHODS
    --------------------------------------*/
    //-check battery optimisation (testing: check for cause of process death)
    @SuppressLint({"NewApi", "BatteryLife"})
    private void checkOptimization() {
        Log.v(TAG, "checkOptimization: ");
        String packageName = getApplicationContext().getPackageName();
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            Log.v(TAG, "checkOptimization: pwermanager found");
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Log.w(TAG, "checkOptimization: Warning: manager not ignoring optimisation");
                Intent intent = new Intent();
                intent.setAction(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                this.startActivity(intent);
            } else {
                Log.v(TAG, "checkOptimization: manager ignoring optimisation");
//                Toast.makeText(getApplicationContext(), "battery optimisation successfully ignored", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //-send broadcast to running service with service method to trigger from activity (primarily for testing)
    public void sendMsg() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent(PrimeForegroundService.SERVICE_BROADCASTRECEIVER_ACTION);
        intent.putExtra(METHOD_TRIGGER, PrimeForegroundService.METHODTRIGGER_TESTAUDIO);
        lbm.sendBroadcast(intent);
    }


    /*--------------------------------------
        HELPER METHODS
    --------------------------------------*/
    //-starts the foreground service (using string extra as identifier for demo mode)
    public void startGpsService(String servicePassed) {
        Log.v(TAG, "startGpsService: ");
        foregroundIntent.putExtra("serviceType", servicePassed);
        startService(foregroundIntent);
    }


    //-stops the foreground service (clear extra for next starting choice)
    public void stopGpsService() {
        Log.v(TAG, "stopGpsService: ");
        stopService(foregroundIntent);
        foregroundIntent.removeExtra("serviceType");
    }


    /*--------------------------------------
        BROADCAST RECEIVERS
    --------------------------------------*/
    //-update UI bike status info
    private BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive:view update broadcast from service...");

            //todo: test this additional additional nullcheck
            Bundle extras = intent.getExtras();
            if (extras != null) {
                //update views from passed extras
                textIndicateL.setText(intent.getStringExtra(UI_UPDATE_INDICATOR_LEFT));
                textIndicateR.setText(intent.getStringExtra(UI_UPDATE_INDICATOR_RIGHT));
                textLightL.setText(intent.getStringExtra(UI_UPDATE_LIGHTS_LOW));
                textLightH.setText(intent.getStringExtra(UI_UPDATE_LIGHTS_HIGH));
                textSpeedLimit.setText(intent.getStringExtra(UI_UPDATE_SPEED_LIMIT));
                textSpeedActual.setText(intent.getStringExtra(UI_UPDATE_SPEED_ACTUAL));

                //if extras are attached: demo mode is active: update map on UI
                if (extras.containsKey("mapIndex")) {
                    //todo: set map image from resource array
                    mapImage.setImageResource(mapResources[extras.getInt("mapIndex")]);
                }
                if (extras.containsKey("roadName")) {
                    //todo: set road name text
                    mapRoadName.setText(extras.getString("roadName"));
                }
            }
        }
    };


    //-receive notification that service has experienced error setting up bluetooth connection
    private BroadcastReceiver mServiceBroadcastReceiver_BT_error = (new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "onReceive: Error: bluetooth connection has failed: stop service");

            //stop service
            stopService(foregroundIntent);

            final MediaPlayer mediaplayer = new MediaPlayer();
            AssetFileDescriptor afd = null;
            try {
                Log.d(TAG, "onReceive: bluetooth connection error siganl received.");
                afd = getAssets().openFd("tts_lola_prompt_bluetootherror_.mp3");
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

            //create dialog to prompt user to retry
            final Dialog dialog = new Dialog(PrimeForegroundServiceHost.this);
            dialog.setContentView(R.layout.dialog_popup_bluetooth_connection_error);
            dialog.setTitle("Bike Connection Error");

            Button dialogButton_retryService = dialog.findViewById(R.id.button_popup_retry_service);
            dialogButton_retryService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //attempt to restart service
                    startGpsService(foregroundIntent.getStringExtra("serviceType"));
                    Log.d(TAG, "onClick: retry service: type [" +
                            foregroundIntent.getStringExtra("serviceType") + "]");
                    dialog.dismiss();
                }
            });

            Button dialogButton_cancelService = dialog.findViewById(R.id.button_popup_cancel_service);
            dialogButton_cancelService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        }
    });
}
