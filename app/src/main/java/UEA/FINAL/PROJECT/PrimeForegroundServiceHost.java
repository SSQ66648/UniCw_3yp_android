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
 *------------------------------------------------------------------------------
 * NOTES:
 *          +   not currently stopping service on destroy as this has proved problematic (if switch
 *              to another app or notification for example)
 *              todo: will need to consider how to handle this if it is a problem? - ie if activity gone, have to reopen to get to stop button -tie ervice stop to application lifecycle somehow?
 *          +   will be attempting (200223) to not include any binding of service as previous
 *              experiment did not show much (if any) need for it: will return to this if problems
 *              are encountered.
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
 *      //todo: add status display for bt devices
 *      //todo: checking for bt connections?
 *      //todo: consider enum for method choices (const)?
 *      //todo: COMBINE BTACTIONS ACTIVITY??
 *      //todo: DISPLAY VALUES IF THEY ARE AVAILABLE OTHERWISE DONT CRASH SERVICE
 *
 -----------------------------------------------------------------------------*/
package UEA.FINAL.PROJECT;
/*--------------------------------------
    IMPORT LIST
--------------------------------------*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;

public class PrimeForegroundServiceHost extends AppCompatActivity {
    private static final String TAG = "GpsForegroundServiceAct";

    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    //name of intent extra sent via broadcast to method receiver in service
    public static final String METHOD_TRIGGER = "methodMessage";


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    //---LAYOUT---
    Button startService;
    Button stopService;
    Button testBatt;
    Button testAudio;

    //---VARIABLES---
    Intent foregroundIntent;
    //local copy of bluetoothActions intent-passed device address
    String bike_MAC;


    /*--------------------------------------
        LIFECYCLE
    --------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primaryforegroundservicehost);
        Log.d(TAG, "onCreate: ");

        //---TOOLBAR---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Primary Service Host Activity");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //---VIEWS---
        startService = findViewById(R.id.button_gpsforegroundservice_start);
        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: start service");
//                Toast.makeText(getApplicationContext(), "Starting Service...", Toast.LENGTH_SHORT).show();
                startGpsService();
            }
        });

        stopService = findViewById(R.id.button_gpsforegroundservice_stop);
        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: stop service");
                stopGpsService();
//                Toast.makeText(getApplicationContext(), "Stopping Service...", Toast.LENGTH_SHORT).show();
            }
        });

        //check app battery optimisation
        testBatt = findViewById(R.id.button_test_battery_optimisation);
        testBatt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: check batter optimization");
                checkOptimization();
            }
        });

        //test audio of service
        testAudio = findViewById(R.id.button_test_audiofile_gpsforegroundservice);
        testAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: test audio");
                sendMsg();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: (re) creating service intent");
        foregroundIntent = new Intent(this, PrimeForegroundService.class);

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        //todo: add handling in event of intent lost? (back button / testing shortcut?)
        //get bluetooth address (plural, possibly with UUIDs as well if they can be obtained later):
        Intent intent = getIntent();
        bike_MAC = intent.getStringExtra(BluetoothActions.EXTRA_DEVICE_ADDRESS);
        Log.d(TAG, "onResume: obtained MAC address: " + bike_MAC + "\n" +
                "Adding bike address to foreground service intent...");

        //todo: fix work around: (dont know why intent is losing the address en-route?
        if (bike_MAC == null) {
            Log.e(TAG, "requestConnectDevice: Error: BIKE ADDRESS IS NULL: employing work-around of hard coded value for development.");
            bike_MAC = "FC:A8:9A:00:4A:DF";
        }

        //add address to foreground intent
        foregroundIntent.putExtra(BluetoothActions.EXTRA_DEVICE_ADDRESS, bike_MAC);
    }


    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: ");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
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
        Log.d(TAG, "checkOptimization: ");
        String packageName = getApplicationContext().getPackageName();
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            Log.d(TAG, "checkOptimization: pwermanager found");
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Log.w(TAG, "checkOptimization: Warning: manager not ignoring optimisation");
                Intent intent = new Intent();
                intent.setAction(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                this.startActivity(intent);
            } else {
                Log.d(TAG, "checkOptimization: manager ignoring optimisation");
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
    //-starts the foreground service
    public void startGpsService() {
        Log.d(TAG, "startGpsService: ");
        startService(foregroundIntent);
    }


    //-stops the foreground service
    public void stopGpsService() {
        Log.d(TAG, "stopGpsService: ");
        stopService(foregroundIntent);
    }


}
