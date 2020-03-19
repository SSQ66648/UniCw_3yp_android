/*--------------------------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System App (UEA.FINAL.PROJECT)
 *
 * FILE:        PrimeForegroundService.Java
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * DESCRIPTION: 8th version of main function of project app: started from host activity, runs
 *              continuously in background to receive location updates from GPS and network
 *              providers, use coordinates to create and send HTTP queries to OverpassAPI, receive
 *              and parse JSON response to find speed limit of road at current location.
 *              Also handles bluetooth input of bike "status" including speed, warning the user via
 *              bluetooth connection to headset if current speed exceeds the limit at location.
 *--------------------------------------------------------------------------------------------------
 * NOTES:
 *      +   Relies on AppNotificationWrapper class to create the channels used for notification
 *          (only in API>=26 - higher than test device (25): this causes the warning message to
 *          logcat but does not impede its functionality).
 *      +   Services run on main thread of application so major workload has been moved to AsyncTask
 *          (as they are run in own thread and have built in controls regarding execution).
 *      +   Testing for best values of queries (eg radius size and rate of location updates) are
 *          still ongoing.
 *      +   Some of the testing code has been left in place to help with the demonstration of
 *          functionality (see logcat).
 *      +   majority of testing has been done on physical-device due to the lack of being bale to
 *          implement usual test harness in each class while working in Android Studios.
 *      +   Location strategy for development has been:
 *              - ensure new location is more recent than previous,
 *              - if delay between updates is within acceptable threshold (15 seconds),
 *                  - prioritise whichever location has the best accuracy
 *                  - else: use the most recent location as user may have moved.
 *      +   Selection of road from JSON response (if more than one) as been to use the first result
 *          in the list (pending a way to order the results from the API by distance from exact
 *          coordinates queried)
 *      +   Bike status input of "rev counter" is currently always zero (as usage has not been
 *          implemented yet in app nor emulation-of in Arduino: planned usage to deliver red-line
 *          warning to user or possibly suggest gearshift)
 *      +   Dates are recorded in YYMMDD notation.
 *--------------------------------------------------------------------------------------------------
 * OUTSTANDING ISSUES:
 *      +
 *--------------------------------------------------------------------------------------------------
 * HISTORY:
 *      v1.0    200314  Copied to own project from testing project to reduce build time and
 *                      complexity of app through omission of the 50 test classes and activities
 *                      (see previous version history below and test files included in portfolio)
 *      v2.0    200317  Added code to establish bluetooth connection with bike on start command:
 *                      -incomplete: untested & unhandled unexpected behaviour
 *      v2.1    200318  Completed (potentially) debugging bluetooth socket connection :
 *                      displaying each bluetooth update to logcat for testing (warning level logs
 *                      to filter out debug of other logs)
 *      v2.1.1  200319  Changed bike status vector indicators and headlights to bool (makes more
 *                      mental-model sense and can reuse watchedBool class to trigger audio.
 *--------------------------------------------------------------------------------------------------
 * PREVIOUS HISTORY:
 *              v1.0    200223  Initial implementation. (completed logcat output, need to debug
 *                              mean/median as doesnt work, still getting time travelling locations:
 *                              to test with only one provider running when mean median working)
 *              v1.1    200224  mean median printed in full in log onDestroy, seems to work (note),
 *                              tidied code, location update log modified for legibility, replaced
 *                              getTime() with getRealTimeNanos(): this did not work: changed to
 *                              manual storing of systemClock elapsedRealTime values on location
 *                              update: result in 20 second uniform delay.
 *              v1.2    200224  changed gps delay v accuracy strategy to 15 seconds, added async for
 *                              http and interface override for completion listener interface.
 *              v1.3    200224  added start toast, constants for value changes, httpAsyncTask,
 *                              oncomplete listener (implemented interface), testing trigger point
 *                              for httpAsync (in third location update).
 *              v1.3.1  200224  added timestamp variables to test how long it takes the http query
 *                              to execute and return: will leave in place until parse etc tasks
 *                              testing also complete.
 *              v1.2    200302  implement & (initial) testing of parsing async task and controls.
 *              v1.3    200302  added begin & end logging to file method and permission methods
 *                              (some incomplete as for activities not service). logging content TBC
 *              v1.3.1  200302  added logging object variable assignment, logging iteration to file.
 *              v1.4    200304  added override listener for low memory to log to file
 *              v1.5    200304  added substitute values for parse returned road if none found in
 *                              query (avoid nullpointer)
 *              v1.6    200306  quick fixes made yesterday, added log error to file method to reduce
 *                              repeated object creation-open-write-close
 *              v1.7 - 2.0      (OMITTED: due to the sheer volume of minor rapid changes made while
 *                              attempting to debug the primary functionality: gps location, http
 *                              api query, parse response, [use data], repeat)
 *                              -see GIT commit history for further denials.
 *              v2.0    200310  (tentatively) have primary function working (major breakdown was
 *                              caused by loss of network (internet) signal): multiple checks are in
 *                              place to log each "error" to logfile and reset the state, awaiting
 *                              next iteration
 *              v2.1    200310  tidied code, removed obsolete sections/commented code/to do items.
 *                              removed try-catch around permissions for locationUpdates. condensed
 *                              repeated error log to file code into method. added major component
 *                              to-do lists.
 *              v2.2    200311  commented out timestamps from logfile for clarity, moved
 *                              onStartCommand to lifecycle section as instinctively look there.
 *                              added binder object and class, test method for audio triggering,
 *                              separate method for displaying to UI.
 *              v3.0    200311  added test version (reworked DOZENS of times) mediaplayer.
 *                              -unacceptable word-delay: may have to be replaced with sentences.
 *              v3.1    200313  removed all bound-service code after extensive bug hunting: does NOT
 *                              work, EVENTUALLY replaced with broadcast receiver and constants.
 *------------------------------------------------------------------------------
 * TO DO LIST:
 *              todo:   warning counter trigger (or by time)
 *              todo:   add warning if accuracy is above upper threshold radius
 *              todo:   need to check that old location more accurate than new, doesnt stack (ensure original timestamp)
 *              todo:   add checks for network/gps enabled etc
 *              todo:   add checks for permissions etc (additional to auto generated ones)
 *              todo:   add notifiaction to user / redirect on permission enable request etc
 *              todo:   add exceptions to else statements as part of error handling
 *              todo:   investigate benefit/drawbacks of asyncTask using global values rather than returning to next section
 *              todo:   change http lock logic from equal location object to equal lat/lon values
 *              todo:   add more lyfecycle (esp on pause/resume?
 *              todo:   service preservation tactic: (calling broadcast when dying, 2 part article!): https://fabcirablog.weebly.com/blog/creating-a-never-ending-background-service-in-android
 *              todo:   write exception clssses to remove try catch block and use throws customexception
 *              todo:   add start 1st update using distance travelled?
 *              todo:   add permission redirect to settings
 *              TODO:   address service re-creation steps/behaviour
 *              todo:   remove asset copied mp3s if not needed.
 *              todo:
 *              todo:
 *------------------------------------------------------------------------------
 * MAJOR ADDITIONS NEEDED:
 *              TODO:   retain/check time of last actual update used (override the accuracy selection of oldLocation)
 *              TODO:   if-exceed limit trigger logic
 *              TODO:   bluetooth bike-vector receipt
 *              TODO:   internet connection lost warning
 *              TODO:   audio playback code (debug why certain RAW files were static?)
 *              TODO:   bluetooth connectivity : use wingood activities to launch service from button
 *              TODO:   accuracy threshold warning
 *              TODO:   radius input option for testing (textinput view?)
 *              TODO:   sequential no-road-value warning (if no usable data for x seconds: treat as no location update warning)
 *              TODO:   ADD PROPER HANDLING FOR NO MAXSPEED: COULD BE ROAD DOESNT HAVE ONE SPECIFIED IN API
 *              TODO:   have to work around address issue taking too long: (not fixed by check on activity)
 *              TODO:
 *------------------------------------------------------------------------------
 * CODE HOUSEKEEPING TO DO LIST:
 *              todo:   change all toast notification to method: pass string
 *              todo:   prevent multiple logging error to file (task chain knock-on)
 *              todo:   change all errors in async task to concat strings format
 *              todo:   reorder lifecycle/methods
 *              todo:   default lbs if arg not provided?
 *              todo:   find need for/remove othererrcount var
 *              todo:   combine error log and reset methods?
 *              todo:
 * ---------------------------------------------------------------------------*/

////todo: --------------------------------------------------------------------------------------------------------------
////todo: MAC ADDRESS IS NULL....?
////todo: --------------------------------------------------------------------------------------------------------------


package UEA.FINAL.PROJECT;
/*--------------------------------------
    IMPORT LIST
--------------------------------------*/

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static UEA.FINAL.PROJECT.AppNotificationWrapper.CHANNEL_3YP;

public class PrimeForegroundService extends Service implements LocationListener,
        AsyncCompleteListener, MediaPlayer.OnCompletionListener {
    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    private static final String TAG = "PrimeForegroundService";
    //overpassAPI radius request value (const for easy changing during debug)
    public static final int API_RADIUS_VALUE = 20;
    //max seconds allowed to prioritise accuracy over newest location in locationchanged
    public static final int LOCATION_DELAY_THRESHOLD = 15;
    //async task completion listener identification flags
    public static final String TASK_COMPLETION_FLAG_HTTP = "httpComplete";
    public static final String TASK_COMPLETION_FLAG_PARSE = "httpParse";
    //line breaks for logging to file
    public static final String LOGFILE_LINEBREAK_STAR = "****************************************\n";
    public static final String LOGFILE_LINEBREAK_DASH = "----------------------------------------\n";
    public static final String LOGFILE_LINEBREAK_EQAL = "========================================\n";
    //used to receive broadcasts from activity: value unimportant
    public static final String SERVICE_BROADCASTRECEIVER_ACTION = "action";
    //triggers of methods broadcast from activity (default is zero: do nothing)
    public static final int METHODTRIGGER_TESTAUDIO = 1;
    public static final int METHODTRIGGER_TESTPRINT = 2;


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    /*------------------
        GPS Variables
    ------------------*/
    //previous and location from listener, and associated long of realtime since system boot
    Location finalLocation;
    long finalLocationRealTime;
    long finalLocationRealTimeSeconds;
    Location oldFinalLocation;
    long oldFinalLocationRealTime;
    long oldFinalLocationRealTimeSeconds;

    long diffSeconds_location_oldLocation;
    LocationManager locationManager;
    Handler handler;

    //testing: count of providers (not including first value: immediate assign to prev)
    int gpsCount = 0;
    int netCount = 0;
    int timeErrCount = 0;
    int otherErrCount = 0;
    //testing: count and total of time differences (to find average delay between updates)
    int updateCount = 0;
    int updateTotalTime = 0;
    int meanTime = 0;
    ArrayList<Integer> medianTime = new ArrayList<>();

    /*------------------
        HTTP Variables
    ------------------*/
    //use Double wrapper to check for null
    Double queryLatitude;
    Double queryLongitude;
    OkHttpClient httpClient = new OkHttpClient();
    AsyncHTTP httpTask;
    AsyncCompleteListener httpCompleteListener;
    //todo: remove possible duplicate listener?
    //used in start httpAsync: if location has not changed, do not bother making http request
    Location apiCheckDuplicateLocation;
    //lock to prevent unnecessary http queries (triggered by location update?) (true = prevent execution)
    boolean asyncLocked = false;

    //testing:(timestamp of httpTask beginning and ending to check duration required)
    long httpStartTime;
    long httpStopTime;

    /*------------------
        PARSE Variables
    ------------------*/
    AsyncPARSE parseTask;
    //use same AsyncCompleteListener

    /*------------------
        Speed-Check Variables
    ------------------*/
    String currentRoadName;
    int currentSpeedLimit = 0;

    /*------------------
        Testing/Log Variables
    ------------------*/
    PrimeServceLogObject logObject;
    //stream to file
    OutputStream oStream = null;
    File dir;
    File file;

    /*------------------
        Audio variables
    ------------------*/

    /*------------------
        Bluetooth Variables
    ------------------*/
    //handles incoming serial messages from bluetooth transmission
    private Handler bluetoothInputHandler;
    private final int handlerState = 0;
    private BluetoothAdapter bluetoothAdapter = null;
    //thread dealing with bluetooth connection
//    private ConnectedThread connectedThread;
    //assembly of recieved data
    private StringBuilder stringBuilder_input = new StringBuilder();
    // SPP UUID service for Hc05 module - this should work for most devices (replace if able to obtain UUID programmatically)
    private static final UUID BtModuleUUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //intent-passed bike address (again, include headset if able to obtain it later)
    private static String bikeAddress;
    //received data-values (aka 'bike statuses')
    private String[] receivedValues = new String[7];
    //current/previous serial transmission number (to check for dropped transmission)
    private int seqNew;
    private int seqOld;
    private BluetoothSocket bluetoothSocket_bike = null;
    private ConnectedThread mConnectedThread;

    /*------------------
        Bike Status Variables
    ------------------*/
    private float currentSpeed;
    //indicators
    private WatchedBool indicatorR;
    private WatchedBool indicatorL;
    //low/high beams
    private WatchedBool headlightL;
    private WatchedBool headlightH;
    //revCounter is currently always zero: no use implemented as of yet)
    private int revCounter = 0;


    /*--------------------------------------
        LIFECYCLE
    --------------------------------------*/
    //-called ONCE when service is first created
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            Log.e(TAG, "onCreate: Error: PERMISSIONS NOT GRANTED");
            return;
        }
        //begin location updates from both providers
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                5000,
                0,
                this,
                Looper.getMainLooper());
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                5000,
                0,
                this,
                Looper.getMainLooper());

        //register receiver for activity reqests to trigger service methods
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceBroadcastReceiver,
                new IntentFilter(PrimeForegroundService.SERVICE_BROADCASTRECEIVER_ACTION));
    }


    //-start service with "new" passed intent: repeatable (called on UI thread)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        ////todo: TESTING
        //setup handler to deal with incoming serial data
        createInputHandler();
        //setup bluetooth connection to bike
        setupBluetoothSockets(intent);
//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        //send to calling activity on click of notification
        Intent notificationIntent = new Intent(this,
                PrimeForegroundServiceHost.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        //create notification
        Notification notification = new NotificationCompat.Builder(this,
                CHANNEL_3YP)
                .setContentTitle("Gps Foreground Service")
                .setContentText("Running...")
                .setSmallIcon(R.drawable.ic_noun_road_4918)
                //start activity on notification click
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getApplicationContext(),
//                        "Starting service...",
//                        Toast.LENGTH_SHORT).show();
//            }
//        });

        //---BEGIN LOGGING---
        beginLoggingToFile();

        //restart asap with last intent given (though unlikely to be killed) - testing sticky as redeliver did not work
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");

        Log.d(TAG, "onDestroy: cancelling any active tasks...");
        cancelAsyncTasks();
        Log.d(TAG, "onDestroy: stopping updates...");
        stopUpdates();
        Log.d(TAG, "onDestroy: completing log to file...");
        endLoggingToFile();

        Log.d(TAG, "onDestroy: time difference values:");
        //get average delay value
        //avoid divide by zero errors
        if (updateCount == 0) {
            meanTime = 0;
        } else {
            meanTime = updateTotalTime / updateCount;

        }
        //get median delay value
        Collections.sort(medianTime);
//todo: add index out of bound checking
        int median = medianTime.get(medianTime.size() / 2);

        Log.d(TAG, "MEAN DELAY: " + meanTime + "\n" +
                "MEDIAN DELAY:" + median + "\n" +
                "number of updates: " + updateCount + "\n" +
                "\tgps total: " + gpsCount + "\n" +
                "\tnetwork total: " + netCount + "\n" +
                "time travel count: " + timeErrCount + "\n" +
                "other error total: " + otherErrCount);
        Log.d(TAG, "_");
        Log.d(TAG, "onDestroy: delay array:\n" +
                medianTime);

        //check if any open oStream still exists
        if (oStream != null) {
            Log.d(TAG, "onDestroy: nullifying oStream.");
            oStream = null;
        } else {
            Log.d(TAG, "onDestroy: no oStream to nullify.");
        }

        //notify user
//        showToastOnUI("stopping service...");


        //todo: test this

        //unregister receiver for activity messages
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceBroadcastReceiver);

        Log.d(TAG, "onDestroy: closing bluetooth sockets:");
        if (bluetoothSocket_bike != null) {
            try {
                bluetoothSocket_bike.close();
            } catch (IOException e) {
                Log.e(TAG, "onDestroy: Error: closing bluetooth sockets");
                //handle?
            }
        }

        Log.d(TAG, "onDestroy: stopping self...");
        stopSelf();

        super.onDestroy();
    }


    /*--------------------------------------
        LISTENERS
    --------------------------------------*/
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: ");
        //immediately get time since boot as location update time (.getTime() is NOT reliable)
        long locationRealTime = SystemClock.elapsedRealtime();
        long locationRealTimeSeconds = locationRealTime / 1000;
        Log.d(TAG, "onLocationChanged: location updated at " + locationRealTimeSeconds +
                " seconds since system boot\n_");

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            Log.e(TAG, "onLocationChanged: Error: permission check");
            String error_onLocation_permission = "Error: onLocationChanged:\n" +
                    "location permissions not granted";
            return;
        }

        //check provider (for clarity / debugging)
        if (location.getProvider().equals("gps")) {
            Log.d(TAG, "onLocationChanged: GPS location passed");
            gpsCount++;
            //             gpsLocation = location;
        } else if (location.getProvider().equals("network")) {
            Log.d(TAG, "onLocationChanged: NETWORK location passed");
            netCount++;
            //             networkLocation = location;
        } else {
            Log.e(TAG, "onLocationChanged: Error: unknown provider identified");
            logErrorToFile("onLocationChanged: unknown provider.", LOGFILE_LINEBREAK_STAR);
            otherErrCount++;
        }

        //use first location if previous does not exist yet, stop further execution
        if (oldFinalLocation == null) {
            Log.d(TAG, "onLocationChanged: no prior location yet: using first update.");
            //assign updated location and associated time
            oldFinalLocation = location;
            oldFinalLocationRealTime = locationRealTime;
            oldFinalLocationRealTimeSeconds = oldFinalLocationRealTime / 1000;
            diffSeconds_location_oldLocation = 0;
            Log.d(TAG, "onLocationChanged: update number: " + updateCount);
            Log.d(TAG, "onLocationChanged: ----------------------------------------\n_");
            updateCount++;
            medianTime.add(0);

            //begin https
            checkAsyncLock();
            return;
        }

        /*----------------------------
            LOCATION STRATEGY (accuracy vs time: prioritise old & accurate if within time tolerance)
        ----------------------------*/
        //get difference between updated location and old location in seconds
        diffSeconds_location_oldLocation = (locationRealTimeSeconds -
                oldFinalLocationRealTimeSeconds);
        //debugging:
        Log.d(TAG, "DIFF SECONDS TESTING: \n" +
                "new seconds: " + locationRealTimeSeconds + "\n" +
                "old seconds: " + oldFinalLocationRealTimeSeconds + "\n" +
                "dif: " + diffSeconds_location_oldLocation);

        //ensure new value is more recent than old value
        if (diffSeconds_location_oldLocation > 0) {
            Log.d(TAG, "onLocationChanged: newLocation is [" +
                    (diffSeconds_location_oldLocation + "] seconds newer"));

            //ensure difference between last update is within limit (prioritise accuracy)
            if (diffSeconds_location_oldLocation < LOCATION_DELAY_THRESHOLD) {
                Log.d(TAG, "onLocationChanged: new location within " +
                        LOCATION_DELAY_THRESHOLD + " seconds: " + "prioritise accuracy");
                if (location.getAccuracy() < oldFinalLocation.getAccuracy()) {
                    Log.d(TAG, "onLocationChanged: new location more accurate:\n" +
                            "old: " + oldFinalLocation.getAccuracy() +
                            " --- new: " + location.getAccuracy());
                    finalLocation = location;
                    finalLocationRealTime = locationRealTime;
                    finalLocationRealTimeSeconds = locationRealTimeSeconds;
                } else {
                    Log.d(TAG, "onLocationChanged: old location more accurate and within " +
                            "time window:\n" +
                            "new: " + location.getAccuracy() +
                            " --- old: " + oldFinalLocation.getAccuracy());
                    finalLocation = oldFinalLocation;
                    finalLocationRealTime = oldFinalLocationRealTime;
                    finalLocationRealTimeSeconds = oldFinalLocationRealTimeSeconds;
                }
            } else {
                Log.d(TAG, "onLocationChanged: new location took longer than ten seconds " +
                        "to arrive: prioritise newer value");
                Log.d(TAG, "debug:\n" +
                        "old accuracy: " + oldFinalLocation.getAccuracy() + "\n" +
                        "new accuracy: " + location.getAccuracy());

                finalLocation = location;
                finalLocationRealTime = locationRealTime;
                finalLocationRealTimeSeconds = locationRealTimeSeconds;
            }

        } else {
            Log.w(TAG, "****************************************");
            Log.w(TAG, "onLocationChanged: Warning: new value older than previous value!");
            Log.w(TAG, "****************************************");
            Log.w(TAG, "onLocationChanged: Warning: update is " +
                    diffSeconds_location_oldLocation + " seconds earlier than previous:\n" +
                    "ignoring and using previous value,\n" +
                    "time travel counter incremented");

            finalLocation = oldFinalLocation;
            finalLocationRealTime = oldFinalLocationRealTime;
            //todo: handle time difference if needed?
            timeErrCount++;
        }

        //debug print to log
        if (finalLocation != null) {
            //debugging info to logcat:
            Log.d(TAG, "_");
            Log.d(TAG, "----------------------------------------");
            Log.d(TAG, "final location found:\n" +
                    "Provider: " + finalLocation.getProvider() + "\n" +
                    "Time: " + finalLocationRealTime + "\n" +
                    "Lat: " + finalLocation.getLatitude() + "\n" +
                    "Lon: " + finalLocation.getLongitude() + "\n");
            Log.d(TAG, "onLocationChanged: update number: " + updateCount);
            Log.d(TAG, "----------------------------------------\n_");
        } else {
            Log.w(TAG, "********************");
            Log.e(TAG, "onLocationChanged: Error: finalLocation is null.");
            Log.w(TAG, "********************\n_");
            logErrorToFile("onLocationChanged: finalLocation is NULL", LOGFILE_LINEBREAK_STAR);
        }
        Log.d(TAG, "_");

        //testing: add time difference (seconds) to mean/median testing
        updateCount++;
        //second difference (debug: ensure cast from long is correct value
        int diff = (int) diffSeconds_location_oldLocation;

        //add seconds to total
        updateTotalTime = updateTotalTime + diff;
        //add value to list
        medianTime.add(diff);

        //update previous location
        oldFinalLocation = finalLocation;
        oldFinalLocationRealTime = finalLocationRealTime;
        oldFinalLocationRealTimeSeconds = finalLocationRealTimeSeconds;
        //todo: move time location debug to method?

        //attempt to begin http async
        checkAsyncLock();
    }


    //asyncTask post-execute notification to continue
    @Override
    public void onTaskComplete(String flag, Object product) {
        Log.d(TAG, "onTaskComplete:");
        //copy product to prevent orphan data
        Object localProduct = product;
        String localFlag = flag;

        if (localProduct == null) {
            Log.e(TAG, "onTaskComplete: Error: PRODUCT IS NULL");
            Log.e(TAG, "onTaskComplete: product obtained from: " + localFlag);

            logErrorToFile("onTaskComplete: Error: PRODUCT from [" + localFlag + "]IS NULL", LOGFILE_LINEBREAK_STAR);
            errorReset("onTaskCompleted: localProduct is null");
            return;
        }

        if (localFlag == TASK_COMPLETION_FLAG_HTTP) {
            //timestamp
            httpStopTime = SystemClock.elapsedRealtime();
            //add response time to log
            logObject.setQueryResponseTime(httpStopTime);
            Log.d(TAG, "onTaskComplete: httpTask flag found:");

            //testing:
            Log.d(TAG, "onTaskComplete: returned httpQuery string reads: \n" + localProduct);
            Log.d(TAG, "onTaskComplete: check if task still exists: " + httpTask);

            Log.d(TAG, "onTaskComplete: time taken for httpAsyncTask to complete: \n" +
                    "Start time: " + httpStartTime + "\n" +
                    "Stop time: " + httpStopTime + "\n" +
                    "nanoseconds taken :" + (httpStopTime - httpStartTime) + "\n" +
                    "(" + ((httpStopTime - httpStartTime) / 1000) + " seconds)");

            //check that response is string as expected
            if (localProduct instanceof String) {
                Log.d(TAG, "onTaskComplete: response IS string: continue:");
                //http complete: start parsing of road list
                startAsyncTaskParse((String) localProduct);
            } else {
                logErrorToFile("onTaskComplete: Error: API response is NOT string: cannot continue.", LOGFILE_LINEBREAK_STAR);
                errorReset("onTaskComplete: HTTP flag found: response is not string: reset.");
            }
        } else if (localFlag == TASK_COMPLETION_FLAG_PARSE) {
            Log.d(TAG, "onTaskComplete: parseTask flag found:");
            //check product is of roadTag as expected
            if (localProduct instanceof RoadTags) {
                Log.d(TAG, "onTaskComplete: rechecking road values:\n" +
                        "road name: " + ((RoadTags) localProduct).getRoadName() + "\n" +
                        "road speed: " + ((RoadTags) localProduct).getRoadSpeed());

                //log road name and limit
                logObject.setRoadName(((RoadTags) localProduct).roadName);
                logObject.setMaxSpeed(((RoadTags) localProduct).roadSpeed);

                //testing:
                currentRoadName = ((RoadTags) localProduct).getRoadName();
                currentSpeedLimit = ((RoadTags) localProduct).getRoadSpeed();
                Log.d(TAG, "onTaskComplete: service now has access to:\n" +
                        "road: " + currentRoadName + " with limit: " + currentSpeedLimit);

                //add parse completion time to log
                logObject.setParseCompleteTime(SystemClock.elapsedRealtime());

                //todo: add limit alarm logic
                updateCurrentSpeedInfo((RoadTags) localProduct);
            } else {
                Log.e(TAG, "onTaskComplete: Error: expected RoadTags object, "
                        + localProduct + "received.");
                logErrorToFile("onTaskComplete: Error: expected RoadTags object", LOGFILE_LINEBREAK_STAR);
                errorReset("onTaskComplete: PARSE flag found: localProduct is not a RoadTags object");
                return;
                //todo: handle?
            }
            //repeat async loop (triggered on location update): unlock async && new location check
            asyncLocked = false;

        } else {
            Log.e(TAG, "onTaskComplete: Error: flag not recognised:\n" + flag);
            logErrorToFile("onTaskComplete: Error: flag not recognised:", LOGFILE_LINEBREAK_STAR);
            errorReset("onTaskComplete: flag not recognised: reset.");
            return;
        }
    }


    //-attempt to diagnose if memory is cause of service being killed by logging to file
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory: ");
        //print memory warning to logfile
        String lowMemoryWarninglog = "****************************************\n" +
                "*************** LOW MEMORY WARNING! **************\n" +
                "******************* onLowMemory ******************\n" +
                "**************************************************\n";
        logErrorToFile(lowMemoryWarninglog, LOGFILE_LINEBREAK_STAR);
    }


    //-additional memory log-check
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.d(TAG, "onTrimMemory: ");
        //print memory warning to logfile
        String lowMemoryWarninglog = "****************************************\n" +
                "*************** LOW MEMORY WARNING! **************\n" +
                "******************* onTrimMemory *****************\n" +
                "**************************************************\n";
        logErrorToFile(lowMemoryWarninglog, LOGFILE_LINEBREAK_STAR);
    }


    /*--------------------------------------
        UNUSED LISTENERS
    --------------------------------------*/
    //-required by superclass
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    ////todo: sorth methods from listeners/classes etc!------------------------------------------------------------------------------------------

    //testing: to sort later:
    //todo: check final doesnt break anything
    MediaPlayer mediaPlayer;
    private final String[] tts_lola_NetworkConnectionLost = {
            "tts_mp3_lola_warning_warning.mp3",
            "tts_mp3_lola_connection_network.mp3",
            "tts_mp3_lola_connection_connection.mp3",
            "tts_mp3_lola_connection_lost.mp3"
    };

    public static final String TTS_LOLA_WARNING_NETWORK_LOST = "networkLost";
    //position in media player array
    int playIndex = 0;
    //variable array of resource file IDs (copied to per array choice)
    String[] resourceFilenameArray = new String[0];

    //-selects audio array to play
    public void playAudio(String playChoice) {
        switch (playChoice) {
            case TTS_LOLA_WARNING_NETWORK_LOST:
                resourceFilenameArray = tts_lola_NetworkConnectionLost;
                //todo: add more choices
        }
        play();
    }


    //-play audio at index in array or cease
    public void play() {
        if (playIndex > resourceFilenameArray.length - 1) {
            Log.d(TAG, "play: playlist finished.");
            //reset counter, array
            playIndex = 0;
            resourceFilenameArray = null;
            //release resources
            stopPlayer();
            return;
        } else {
            //repopulate player
            try {
                if (mediaPlayer == null) {
                    //iteration 1
                    mediaPlayer = new MediaPlayer();
                } else {
                    //iteration 2+
                    mediaPlayer.reset();
                }
                AssetFileDescriptor afd = getAssets().openFd(resourceFilenameArray[playIndex]);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                        afd.getLength());
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "play: Error: resetting/starting player: illegal argument");
                Log.e(TAG, "message: " + e.getMessage());
                e.printStackTrace();
            } catch (IllegalStateException e) {
                Log.e(TAG, "play: Error: resetting/starting player: illegal state");
                Log.e(TAG, "message: " + e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "play: Error: resetting/starting player I/O exception");
                Log.e(TAG, "message: " + e.getMessage());
                e.printStackTrace();
            }
            //move to next file
            playIndex++;
        }
    }


    //-mediaplayer listener: triggers on currently playing audiofile completion
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion: ");
        play();
    }


    //-release resources assigned to player if it exists
    public void stopPlayer() {
        if (mediaPlayer != null) {
            Log.d(TAG, "stopPlayer: releasing resources for player");
            mediaPlayer.release();
            mediaPlayer = null;
        } else {
            Log.w(TAG, "stopPlayer: no player exists to stop");
        }
    }


    /*--------------------------------------
        (UNUSED) LISTENERS
    --------------------------------------*/
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }


    /*--------------------------------------
        CLASSES (AsyncTask and STARTING METHODS)
    --------------------------------------*/
    //-preparation and triggers execution of http api query asyncTask
    public void startAsyncTaskHTTP() {
        Log.d(TAG, "startAsyncTaskHTTP: ");
        Log.d(TAG, "startAsyncTaskHTTP: location counter = " + updateCount);
        //get current location values
        queryLatitude = oldFinalLocation.getLatitude();
        queryLongitude = oldFinalLocation.getLongitude();
        //replace last check location with current location
        apiCheckDuplicateLocation = finalLocation;

        //todo: may need null checking HERE not in preExecute...?
        httpTask = new AsyncHTTP(this, this, httpClient, queryLatitude,
                queryLongitude);

        //timestamp
        httpStartTime = SystemClock.elapsedRealtime();
        httpTask.execute();
    }


    //-uses coordinates to query overpassAPI over network
    private static class AsyncHTTP extends AsyncTask<Void, Void, String> {
        //note: args passed: X:to background, Y:progressUpdate, Z:postExecute
        /*------------------
            MEMBER VAR
        ------------------*/
        private AsyncCompleteListener listener;
        private WeakReference<PrimeForegroundService> weakReference;
        String url;
        Double lat;
        Double lon;
        OkHttpClient client;
        String responseString;
        //allows debugging of response cause of error without making inner class final
        String queryResponseErrorOrigin = "no response Error";


        /*------------------
            CONSTRUCTOR
        ------------------*/
        //-pass service (activity) reference and coordinates
        AsyncHTTP(PrimeForegroundService activity, AsyncCompleteListener listener,
                  OkHttpClient client, double lat, double lon) {
            Log.d(TAG, "constructed: AsyncHTTP");
            weakReference = new WeakReference<>(activity);
            this.listener = listener;   //callback
            this.client = client;
            this.lat = lat;
            this.lon = lon;
        }


        /*------------------
           PRE EXECUTE
       ------------------*/
        @Override
        protected void onPreExecute() {
            PrimeForegroundService activity = weakReference.get();
            Log.d(TAG, "onPreExecute: AsyncHTTP");
            super.onPreExecute();

            //check if cancelled: do not execute
            if (isCancelled()) {
                Log.d(TAG, "onPreExecute: is cancelled: abort");
                //todo: follow this and observe behaviour
                activity.errorReset("AsyncHTTP: onPreExecute: cancelled");
                return;
            }

            //check variables are not null
            //todo: add checks for WHICH is null (and potentially need to set lat/lon to null once used, maybe in oncomplete)?
            if (weakReference == null | listener == null | lat == null | lon == null) {
                Log.e(TAG, "onPreExecute: HTTP: Error: \n" +
                        "passed variable is null: cannot continue.");

                String error_http_preExecute = "Error: HTTP onPreExecute:\n" +
                        "one or more variables have been found to be NULL:\n" +
                        "weakreference: " + weakReference + "\n" +
                        "listener: " + listener + "\n" +
                        "latitude: " + lat + "\n" +
                        "longitude: " + lon;

                //todo: be aware this may not work if activity is NULL (will require own oStream in task)
                activity.logErrorToFile(error_http_preExecute, LOGFILE_LINEBREAK_STAR);
                activity.errorReset("AsyncHttp: onPreExecute: variable null");
                return;
            } else {
                Log.d(TAG, "onPreExecute: all variables exist");
            }
        }


        /*------------------
            EXECUTE
        ------------------*/
        @Override
        protected String doInBackground(Void... voids) {
            Log.d(TAG, "doInBackground: AsyncHTTP");
            PrimeForegroundService activity = weakReference.get();

            //check if task has been cancelled
            if (isCancelled()) {
                Log.d(TAG, "AsyncHTTP: doInBackground: isCancelled(PRE-execute): exiting");
                //todo: follow this and check behaviour
                return null;
            }

            //build url from radius, lat, lon
            combineURL();
            //build http request
            Request request = new Request.Builder().url(url).build();
            //create latch to pause postExecute until received response
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            //log start of query request
            activity.logObject.setQuerySentTime(SystemClock.elapsedRealtime());
            Log.d(TAG, "doInBackground: beginning request...");

            //make http request
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.e(TAG, "onFailure: HTTP request failed.");
                    Log.w(TAG, "onFailure: " + e.getMessage());
                    e.printStackTrace();
                    responseString = null;
                    queryResponseErrorOrigin = "query error caused by onFailure()";
                    countDownLatch.countDown();
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response)
                        throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "onResponse: ");
                        //data retrieved from query
                        responseString = response.body().string();
                    } else {
                        Log.w(TAG, "onResponse: Warning: response was not successful.");
                        responseString = null;
                        queryResponseErrorOrigin = "query error caused by onresponse(): not " +
                                "successful.";
                    }
                    countDownLatch.countDown();
                }
            });

            try {
                Log.d(TAG, "doInBackground: API: awaiting response latch");
                //pause further execution of thread until response (or failure) has been obtained
                countDownLatch.await();
            } catch (InterruptedException e) {
                //unsure if this will crash service or not?
                Log.e(TAG, "doInBackground: API: error awaiting response");
                activity.logErrorToFile("AsyncHTTP: odInBackground: error waiting " +
                        "for countdown", LOGFILE_LINEBREAK_STAR);
                activity.errorReset("AsyncHTTP: odInBackground: await()");
            }

            //testing
            if (isCancelled()) {
                Log.d(TAG, "AsyncHTTP: doInBackground: isCancelled(POST-execute): exiting");
                activity.errorReset("AsyncHTTP: doInBackground: isCancelled:");
                return null;
            }

            //check string status (eg null if query failed)
            if (responseString == null) {
                Log.e(TAG, "AsyncHTTP: doInBackground: Error: responseString is null:\n" +
                        "preventing further execution: exit and reset.");
                activity.logErrorToFile("AsyncHTTP: doInBackground: response is null.",
                        LOGFILE_LINEBREAK_STAR);
                activity.errorReset("AsyncHTTP: doInBackground: responseString null.");
                return null;
            }
            return responseString;
        }


        /*------------------
            POST EXECUTE
        ------------------*/
        @Override
        protected void onPostExecute(String responseString) {
            super.onPostExecute(responseString);
            Log.d(TAG, "onPostExecute: AsyncHTTP");
            PrimeForegroundService activity = weakReference.get();

            //cancel check: exit regardless of string status
            if (isCancelled()) {
                Log.d(TAG, "AsyncHTTP: onPostExecute: cancelled: reset and return null.");
                activity.errorReset("AsyncHTTP: onPostExecute: cancelled: " +
                        "reset and exit.");
                return;
            } else {
                //check if null
                if (responseString == null) {
                    Log.e(TAG, "AsyncHTTP: onPostExecute: Error: " +
                            "responseString is NULL: reset and exit.");
                    activity.errorReset("AsyncHTTP: onPostExecute: " +
                            "responseString is NULL: reset.");
                    return;
                } else {
                    //testing
                    Log.d(TAG, "onPostExecute: listener value: " + listener);
                    Log.d(TAG, "onPostExecute: flag value: " + TASK_COMPLETION_FLAG_HTTP);
                    listener.onTaskComplete(TASK_COMPLETION_FLAG_HTTP, responseString);
                }
            }
        }


        /*------------------
            CANCEL
        ------------------*/
        @Override
        protected void onCancelled() {
            Log.d(TAG, "onCancelled: AsyncHTTP");
            super.onCancelled();
        }


        /*------------------
            METHODS
        ------------------*/
        //-creates URL string from desired parameters and current lat/lon variables
        public void combineURL() {
            Log.d(TAG, "combineURL: ");

            String url_A = "http://overpass-api.de/api/interpreter?data=[out:json];way[";
            String tag = "maxspeed";    //list of tags required in response from API :todo: investigate if any more are possible/useful to project objective
            String url_B = "](around:";
            String url_C = ");out tags;";

            //combine url: beginning, tags, radius, latitude, longitude, end
            url = url_A + tag + url_B + API_RADIUS_VALUE + "," + lat + ","
                    + lon + url_C;

            Log.d(TAG, "combineUrl: URL concatenated reads:\n" + url);
        }
    }


    //-preparation and execution of AsyncPARSE
    public void startAsyncTaskParse(String roadList) {
        Log.d(TAG, "startAsyncTaskParse: ");
        parseTask = new AsyncPARSE(this, this, roadList);
        parseTask.execute();
    }


    //-parse api query response json object tags (esp. maxSpeed)
    private static class AsyncPARSE extends AsyncTask<Void, Void, Void> {
        /*------------------
            MEMBER VAR
        ------------------*/
        private WeakReference<PrimeForegroundService> weakReference;
        private AsyncCompleteListener listener;
        private String response;
        //testing: get array of both names and speeds to select closest option from
        ArrayList<String> roadNames = new ArrayList<>();
        ArrayList<Integer> roadSpeeds = new ArrayList<>();
        //container class to return both road and speed to enclosing service
        RoadTags returnedRoad;

        //create error message by class section todo: check if this is preferred version of error string creation or not: replace other version
        String errorMessageString = "";
        String errorMessageClass = "Error: AsyncPARSE: ";
        String errorMessageMethod = "";


        /*------------------
            CONSTRUCTOR
        ------------------*/
        //-pass service (activity) reference
        AsyncPARSE(PrimeForegroundService activity, AsyncCompleteListener listener, String response) {
            Log.d(TAG, "constructed: AsyncPARSE:");
            weakReference = new WeakReference<>(activity);
            this.listener = listener;
            this.response = response;
        }


        /*------------------
            PRE EXECUTE
        ------------------*/
        @Override
        protected void onPreExecute() {
            Log.d(TAG, "onPreExecute: AsyncPARSE");
            super.onPreExecute();
            PrimeForegroundService activity = weakReference.get();
            //update error msg
            errorMessageMethod = "onPreExecute: ";

            //check if cancelled todo: follow behaviour
            if (isCancelled()) {
                activity.errorReset(errorMessageClass.concat(
                        errorMessageMethod.concat("cancelled:")));
            }

            //debugging:
            if (response == null) {
                Log.e(TAG, "onPreExecute: Error: response is null: reset and exit.");
                errorMessageString = errorMessageClass.concat(errorMessageMethod.concat(
                        "response is null: reset and exit."));

                Log.e(TAG, errorMessageString);
                activity.errorReset(errorMessageString);
                return;
            }
        }


        /*------------------
            EXECUTE
        ------------------*/
        @Override
        protected Void doInBackground(Void... voids) {
            PrimeForegroundService activity = weakReference.get();
            Log.d(TAG, "doInBackground: AsyncPARSE");
            errorMessageMethod = "doInBackground: ";

            if (isCancelled()) {
                Log.w(TAG, "AsyncPARSE: doInBackground: Warning: cancelled: reset and exit.");
                activity.errorReset(errorMessageClass.concat(errorMessageMethod.concat(
                        " cancelled.")));
                return null;
            } else if (response == null) {
                Log.e(TAG, "AsyncPARSE: doInBackground: Error: response is null: reset.");
                activity.errorReset(errorMessageClass.concat(errorMessageMethod.concat(
                        " response is null.")));
                return null;
            }

            //debugging: log response to file:
            logJsonResponse(response);

            //not cancelled, response not null
            try {
                JSONObject responseObj = new JSONObject(response);
                //get array of elements from object
                JSONArray responseArr = responseObj.getJSONArray("elements");
                Log.d(TAG, "doInBackground: PARSE: elements:\n" +
                        responseArr);

                for (int i = 0; i < responseArr.length(); i++) {
                    //get current element object from string
                    String elementString = responseArr.getString(i);
                    JSONObject elementObj = new JSONObject(elementString);
                    //logcat:
                    Log.d(TAG, "----------------------------------------");

                    //add road name(s) to array list
                    try {
                        //for each element, get string of name
                        roadNames.add(elementObj.getJSONObject("tags").getString("name"));
                        Log.d(TAG, "AsyncPARSE: ROADNAME RETRIEVED:");

                        //testing:
                        Log.d(TAG, "doInBackground: NAME: " + roadNames.get(i));
                    } catch (JSONException e) {
                        Log.w(TAG, "AsyncPARSE: JSON exception occurred: no NAME for road");
                        roadNames.add("No name found");
                    }

                    //add max speed to array list
                    try {
                        //debugging:
                        Log.d(TAG, "doInBackground: debug: speed string contents: ["
                                + elementObj.getJSONObject("tags").getString("maxspeed")
                                + "]");

                        //remove all non numeric from string of speed limit and parse to int
                        String charlessSpeedString = elementObj.getJSONObject("tags")
                                .getString("maxspeed");
                        charlessSpeedString = charlessSpeedString.replaceAll(
                                "[^\\d.]", "");
//todo: find why this line was possible to crash parsing "" to int? :
// included a QUICK FIX AS CANNOT FIND WHY THIS HAPPENED?
                        //find why/how:
                        if (charlessSpeedString.equals("")) {
                            Log.e(TAG, "doInBackground: Error: speedString is EMPTY");
                            activity.logErrorToFile("AsyncPARSE: doInBackground: " +
                                    "maxspeed string is empty", LOGFILE_LINEBREAK_STAR);
                            activity.logErrorToFile("object received: " +
                                    response, LOGFILE_LINEBREAK_STAR);
                            roadSpeeds.add(-1);
                        } else {
                            roadSpeeds.add(Integer.parseInt(charlessSpeedString));
                            Log.d(TAG, "AsyncPARSE: SPEED RETRIEVED:");
                        }

                        //testing:
                        Log.d(TAG, "AsyncPARSE: doInBackground: SPEED: " + roadSpeeds.get(i));
                    } catch (JSONException e) {
                        Log.w(TAG, "AsyncPARSE: doInBackground: JSONException occurred: " +
                                "no speed for road.");
                        activity.logErrorToFile("AsyncPARSE: doInBackground: JSON " +
                                        "exception occurred: error getting max speed value: " +
                                        "(not resetting: attampt to continue function)",
                                LOGFILE_LINEBREAK_STAR);
                        //insert negative to signal speed-exception to logic
                        roadSpeeds.add(-1);
                    }
                    //logcat clarity:
                    Log.d(TAG, "----------------------------------------");
                }
            } catch (JSONException e) {
                //todo: replace
                Log.e(TAG, "AsyncPARSE: doInBackground: Error: parsing response string.");
                //debugging cause of killed service: print error to log
                activity.logErrorToFile("AsyncPARSE: doInBackground: JSON exception " +
                        "occurred: outer JSON try brace.", LOGFILE_LINEBREAK_STAR);
                //todo: check if reset is even needed here?
                activity.errorReset("outer JSON parsing exception: reset");
                e.getMessage();
                e.printStackTrace();
            }

            //add number of roads found in radius to logfile
            activity.logObject.setRadiusTotal(roadNames.size());
            //testing:
            Log.d(TAG, "doInBackground: roadnames size = " + roadNames.size());

            //more than one road returned: choose one to use
            if (roadNames.size() > 1) {
                Log.d(TAG, "doInBackground: roadnames.size > 1");
            } else if (roadNames.size() == 1) {
                Log.d(TAG, "doInBackground: roadNames size = 1");
                //todo: potential to skip chooseroad? (ensure not further function called?)
            } else if (roadNames.size() == 0) {
                Log.w(TAG, "doInBackground: Warning: roadsize = 0: check IDE claim this " +
                        "is always true...");
                //no roads added to list: handle potential errors further in code by substitution of values
                Log.w(TAG, "doInBackground: Warning: NO ROAD RETURNED");
                roadNames.add("WARNING: NO ROAD NAME RETURNED (possible error on API retrieval)");
                roadSpeeds.add(-1);
            }

            //check if cancelled: prevent further work
            if (isCancelled()) {
                errorMessageString = errorMessageClass.concat(errorMessageMethod.concat(
                        "cancelled."));
                Log.w(TAG, "AsyncPARSE: Warning: ".concat(errorMessageMethod.concat("cancelled.")));
                activity.errorReset(errorMessageString);
                return null;
            }

            chooseRoad();
            return null;
        }


        /*------------------
            POST EXECUTE
        ------------------*/
        @Override
        protected void onPostExecute(Void aVoid) {
            PrimeForegroundService activity = weakReference.get();
            super.onPostExecute(aVoid);
            Log.d(TAG, "AsyncPARSE: onPostExecute: ");
            errorMessageMethod = "onPostExecute: ";

            //RE check for missing information (ie no roads returned from query)
            //todo: id if this is cause /contributing to multiple error logging behaviour
            if (returnedRoad == null) {
                Log.w(TAG, "onPostExecute: Error: returned road is null:");
                errorMessageString = errorMessageClass.concat(errorMessageMethod.concat(
                        "returnedRoad is NULL."));
                activity.logErrorToFile(errorMessageString, LOGFILE_LINEBREAK_STAR);
                activity.errorReset(errorMessageString);
                return;
            }

            //logcat:
            Log.d(TAG, "--------------------");

            //debug:
            Log.d(TAG, "AsyncPARSE: onPostExecute: task complete: ");
            //check if cancelled: prevent further work
            if (isCancelled()) {
                errorMessageString = errorMessageClass.concat(errorMessageMethod.concat(
                        "cancelled."));
                Log.w(TAG, "AsyncPARSE: Warning: ".concat(errorMessageMethod.concat("cancelled.")));
                activity.errorReset(errorMessageString);
                return;
            }

            //signal completion and return parsed road values
            listener.onTaskComplete(TASK_COMPLETION_FLAG_PARSE, returnedRoad);
        }


        /*------------------
            CANCEL
        ------------------*/
        @Override
        protected void onCancelled() {
            Log.d(TAG, "onCancelled: AsyncPARSE");
            super.onCancelled();
        }


        /*------------------
            METHODS
        ------------------*/
        //-logs JOSN response to logfile (debugging attempt to track actual response contents)
        public void logJsonResponse(String response) {
            PrimeForegroundService activity = weakReference.get();
            try {
                activity.oStream = new FileOutputStream(activity.file, true);
                activity.oStream.write(LOGFILE_LINEBREAK_EQAL.getBytes());
                activity.oStream.write("JSON RESPONSE:\n".getBytes());
                activity.oStream.write(response.getBytes());
                activity.oStream.write("\n".getBytes());
                activity.oStream.write(LOGFILE_LINEBREAK_EQAL.getBytes());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
                Log.e(TAG, "logJsonResponse: Error: logJSONresponse null pointer.");
            }
        }


        //-selection logic for values to return if more than one in API response
        private void chooseRoad() {
            PrimeForegroundService activity = weakReference.get();
            Log.d(TAG, "AsyncPARSE: chooseRoad: ");
            errorMessageMethod = "chooseRoad: ";

            //debugging:
            if (roadNames == null) {
                Log.e(TAG, "chooseRoad: Error: roadNames is null");
                errorMessageString = errorMessageClass.concat(errorMessageMethod.concat(
                        "roadNames is null."));
                activity.errorReset(errorMessageString);
                return;
            }

            //todo: maybe need null/empty check on arrays?
            //TODO: RESEARCH IF ANY WAY TO DETERMINE SELECTION LOGIC (ORDER OF API RESULTS?): INSERT HERE

            //ensure name and speed arrays match
            if (roadNames.size() != roadSpeeds.size()) {
                Log.e(TAG, "AsyncPARSE: chooseRoad: Error: array lengths do not match");
                //debugging cause of killed service: print error to log
                activity.logErrorToFile("AsyncPARSE: chooseRoad(): name and speed " +
                        "arrays do not match", LOGFILE_LINEBREAK_STAR);
                activity.errorReset("chooseRoad() array length mismatch.");
            } else if (!(roadNames.size() > 0)) {
                //list is empty
                Log.w(TAG, "chooseRoad: Warning: road list is EMPTY");
                activity.logErrorToFile(errorMessageClass.concat(errorMessageMethod.concat(
                        "roadNames list is empty")), LOGFILE_LINEBREAK_STAR);
            } else {
                //todo: further logic checks (eg first speed value that exists?)

                //TESTING VERSION: return first value found: todo: better selection method:
                Log.d(TAG, "AsyncPARSE: chooseRoad: road chosen from API returned list:\n"
                        + roadNames.get(0) + " @ " + roadSpeeds.get(0) + "mph");
                returnedRoad = new RoadTags(roadNames.get(0), roadSpeeds.get(0));

                //record total roads found in return for API radius
                activity.logObject.setRadiusTotal(roadNames.size());
                //change number found to zero for log
                if (roadSpeeds.get(0) == -1) {
                    activity.logObject.setRadiusTotal(0);
                } else {
                    //set selected road index position in road array todo: replace with logic for however road is selected
                    activity.logObject.setRoadArrayIndex(1);
                }

                //testing:
                Log.d(TAG, "chooseRoad: returnedRoad values:\n" +
                        "\troad name: " + returnedRoad.getRoadName() + "\n" +
                        "\troad speed: " + returnedRoad.getRoadSpeed() + "\n");
            }
        }
    }


    /*--------------------------------------
        INNER CLASSES / CONTAINERS
    --------------------------------------*/
    //-container for both road name and speed (and any other tags deemed useful later) back from AsyncPARSE
    private static class RoadTags {
        /*------------------
             Variables
        ------------------*/
        String roadName;
        int roadSpeed;

        /*------------------
            CONSTRUCTOR
        ------------------*/
        RoadTags(String name, int speed) {
            this.roadName = name;
            this.roadSpeed = speed;
        }

        /*------------------
            ACCESSORS
        ------------------*/
        public String getRoadName() {
            return roadName;
        }

        public int getRoadSpeed() {
            return roadSpeed;
        }
    }


    //-thread for bluetooth connection(s)
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
                    bluetoothInputHandler.obtainMessage(handlerState, bytes, -1,
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
                Toast.makeText(getApplicationContext(), "Connection Failure",
                        Toast.LENGTH_LONG).show();
//                finish();
            }
        }
    }

    /*--------------------------------------
        METHODS
    --------------------------------------*/
    //-setup bluetooth adapter and sockets
    public void setupBluetoothSockets(Intent intent) {
        Log.d(TAG, "onStartCommand: obtaining bluetooth adapter");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //todo: check bluetooth state?
        bikeAddress = intent.getStringExtra(BluetoothActions.EXTRA_DEVICE_ADDRESS);
        Log.d(TAG, "onStartCommand: received bluetooth address: " + bikeAddress);

        //todo: fix workaround: hard coded address if intent extra is lost enroute: why/ how can this happen??
        if (bikeAddress == null) {
            Log.e(TAG, "requestConnectDevice: Error: BIKE ADDRESS IS NULL: employing work-around of hard coded value for development.");
            bikeAddress = "FC:A8:9A:00:4A:DF";
        }

        //create device and set the MAC address
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(bikeAddress);
            Log.d(TAG, "onResume: device created: " + device.getName() + " : "
                    + device.getAddress());
            try {
                bluetoothSocket_bike = createBluetoothSocket(device);
                Log.d(TAG, "onResume: create bluetooth socket");
            } catch (IOException e) {
                Log.d(TAG, "onResume: Socket creation failed.");
                Toast.makeText(getApplicationContext(), "Socket creation failed",
                        Toast.LENGTH_LONG).show();
            }
            // Establish the Bluetooth socket connection.
            try {
                bluetoothSocket_bike.connect();
                Log.d(TAG, "onResume: socket connect...");
            } catch (IOException e) {
                Log.d(TAG, "onResume: socket connection error.");
                try {
                    bluetoothSocket_bike.close();
                    Log.d(TAG, "onResume: socket closed");
                } catch (IOException e2) {
                    Log.d(TAG, "onResume: Error on closing socket during establish failure!");
                    //insert code to deal with this
                }
            }

            mConnectedThread = new ConnectedThread(bluetoothSocket_bike);
            mConnectedThread.start();
            //send character when resuming.beginning transmission to check device is connected
            mConnectedThread.write("x");
        } catch (Exception e) {
            Log.e(TAG, "onResume: error creating device.");
        }
    }


    //creates secure outgoing connection with BT device using UUID
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BtModuleUUID);
    }


    //-create handler for incoming bluetooth serial data todo: move to static class to prevent leaks (warning suppressed)
    //todo: rename sensorvalues
    //todo: change print to logd
    @SuppressLint("HandlerLeak")
    public void createInputHandler() {
        Log.d(TAG, "createInputHandler: ");
        bluetoothInputHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                Log.d(TAG, "handleMessage: ");
                if (msg.what == handlerState) {
                    // msg.arg1 = bytes from connect thread
                    String readMessage = (String) msg.obj;
                    //appending to string until find end of message char (~)
                    stringBuilder_input.append(readMessage);
                    //set index for end of message
                    int endOfLineIndex = stringBuilder_input.indexOf("~");
                    //check any data exists before ~
                    if (endOfLineIndex > 0) {
                        //extract string (currently only used to get length after splitString)
                        String dataInPrint = stringBuilder_input.substring(0, endOfLineIndex);
                        Log.d(TAG, "handleMessage: Data Received = \n" + dataInPrint);
                        //get length of data received (25char initially, grows with triple digits)
                        int dataLength = dataInPrint.length();
                        Log.d(TAG, "handleMessage: String Length = " + String.valueOf(dataLength));

                        //check for beginning character of '#' -signifies desired transmission
                        if (stringBuilder_input.charAt(0) == '#') {
                            //string array for sensor values
                            String[] sensorValues = new String[7];

                            //remove first character ('#') to simplify splitting string
                            stringBuilder_input.deleteCharAt(0);

                            //convert stringBuilder to string array
                            receivedValues = stringBuilder_input.toString().split("\\+");

                            //assign and check result of splitString
                            for (int i = 0; i < receivedValues.length - 1; i++) {
                                sensorValues[i] = receivedValues[i];
//                                System.out.println("array pos " + i + " value: "
//                                        + sensorValues[i]);
                            }


                            //todo: implement logging output to test incoming data----------------------------------------------------------------------------------------------------

                            //(testing): values to views
//                            txt_sequence.setText("SEQ No. = " + sensorValues[0]);
//                            txt_speedView.setText(" Speed = " + sensorValues[1] + " mph");
//                            txt_indicatorL.setText("LEFT indicator = " + sensorValues[2]);
//                            txt_indicatorR.setText("RIGHT indicator = " + sensorValues[3]);
//                            txt_lowbeam.setText("LOWbeams = " + sensorValues[4]);
//                            txt_highbeam.setText("HIGHbeams = " + sensorValues[5]);
//                            txt_revcount.setText("REVCOUNTER = " + sensorValues[6]);

                            //testing send values to logcat via warning (allows filtering of debug level logs and below)
                            Log.w(TAG, "----------------------------------------");
                            Log.w(TAG, "handleMessage: SEQ No. = " + sensorValues[0]);
                            Log.w(TAG, "handleMessage: Speed = " + sensorValues[1] + " mph");
                            Log.w(TAG, "handleMessage: LEFT indicator = " + sensorValues[2]);
                            Log.w(TAG, "handleMessage: RIGHT indicator = " + sensorValues[3]);
                            Log.w(TAG, "handleMessage: LOW beams = " + sensorValues[4]);
                            Log.w(TAG, "handleMessage: HIGHbeams = " + sensorValues[5]);
                            Log.w(TAG, "handleMessage: REVCOUNTER = " + sensorValues[6]);
                            Log.w(TAG, "----------------------------------------");

                            //get local copy that can be monitored)
                            //todo: !!!! parse this to number that can be monitored!!!!!!!----------------------------------------------------------------------
                            String currentSpeeds = sensorValues[1] + "mph";
                            indicatorL.setValue(Boolean.parseBoolean(sensorValues[2]));
                            indicatorR.setValue(Boolean.parseBoolean(sensorValues[3]));
                            headlightL.setValue(Boolean.parseBoolean(sensorValues[4]));
                            headlightH.setValue(Boolean.parseBoolean(sensorValues[5]));


                            sendUiUpdate(currentSpeedLimit, currentSpeeds, indicatorL, indicatorR, headlightL, headlightH);


                            //todo: better catch for non-sequential data
                            //convert string to int
                            try {
                                seqNew = Integer.parseInt(sensorValues[0]);
                            } catch (Exception e) {
                                Log.e(TAG, "handleMessage: string to int parse error");
                                e.printStackTrace();
                            }

                            System.out.println("OLD sequence no. = " + seqOld);
                            System.out.println("NEW sequence no. = " + seqNew);

                            //check if new message is exactly one more than previous
                            if (seqNew - seqOld != 1) {
                                //testing
                                Log.e(TAG, "handleMessage: incorrect message sequence");
                                //todo: add handling for incorrect input sequence
                            }
                            //assign current to old sequence variable
                            seqOld = seqNew;
                        }

                        //clear all string data
                        //todo:need to delete splitstring as seen here?
                        stringBuilder_input.delete(0, stringBuilder_input.length());
                    }
                }
            }
        };
    }


    //-method to reset status of class in event of unhandled error/exception (attempt to continue after logging to file)
    public void errorReset(String methodName) {
        //pass method name that called this method to identify origin of error
        //(local copy in case asyncTask goes out of scope)
        String localMethodName = methodName;

        //log error cause to file
        logErrorToFile(localMethodName, LOGFILE_LINEBREAK_STAR);
        //todo: complete reset item list
        //cancel any async tasks if running:
        cancelAsyncTasks();
        //release lock for awaiting next iteration
        asyncLocked = false;

        //todo: revisit task to null idea?
    }


    //-log error string to file (tidy code: repeated object creation and writing)
    public void logErrorToFile(String errorMessage, String linebreak) {
//todo: consider adding lbs to method, not ever call
        Log.d(TAG, "logErrorToFile: ");
        //get local copy of error message string: in case passing async task is destroyed
        String localErrorMessage = errorMessage;

        //create log object
        try {
            oStream = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "logErrorToFile: Error: opening oStream");
            e.printStackTrace();
        }

        //write error to file
        try {
            oStream.write("\n".getBytes());
            oStream.write(LOGFILE_LINEBREAK_STAR.getBytes());
            //oStream.write(getIsoTime(System.currentTimeMillis()).getBytes());
            oStream.write(("Error logged @ : " + getIsoTime(System.currentTimeMillis()) +
                    "\n").getBytes());
            oStream.write("*****\n".getBytes());
            oStream.write("Error message:\n".getBytes());
            oStream.write((localErrorMessage + "\n").getBytes());
            oStream.write(LOGFILE_LINEBREAK_STAR.getBytes());
            oStream.write("\n".getBytes());
        } catch (IOException e) {
            Log.e(TAG, "logErrorToFile: Error: writing error to log file");
            e.printStackTrace();
        } finally {
            try {
                oStream.close();
            } catch (IOException e) {
                Log.e(TAG, "logErrorToFile: Error: error closing oStream");
                e.printStackTrace();
            }
        }
    }


    //-handles the initial permission and directory checks to set up logging to file
    public void beginLoggingToFile() {
        Log.d(TAG, "beginLoggingToFile: ");

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Log.d(TAG, "beginLoggingToFile: media mounted");
            if (Build.VERSION.SDK_INT >= 23) {
                Log.d(TAG, "beginLoggingToFile: build version >= 23");
                if (checkWritePermission()) {
                    Log.d(TAG, "beginLoggingToFile: permission granted");
                    File sdcard = Environment.getExternalStorageDirectory();
                    dir = new File(sdcard.getAbsolutePath() + "/logFiles/");
                    Log.d(TAG, "beginLoggingToFile: directory to check/ create: \n" + dir);
                    if (!dir.exists()) {
                        Log.d(TAG, "beginLoggingToFile: directory doesnt exist: creating...");
                        if (dir.mkdir()) {
                            Log.d(TAG, "beginLoggingToFile: directory created");
                        } else {
                            Log.e(TAG, "beginLoggingToFile: Error: directory failed to be " +
                                    "created!");
                            //todo: handle
                        }
                    } else {
                        Log.d(TAG, "beginLoggingToFile: directory exists");
                    }

                    //create file name/path from date
                    String filenameByDate = "gpsRoadLogFile_" + getIsoDate() + ".txt";
                    Log.d(TAG, "beginLoggingToFile: logfile name by date: " + filenameByDate);
                    file = new File(dir, filenameByDate);

                    //debugging
                    Log.d(TAG, "beginLoggingToFile: current date file already exists = " +
                            file.exists());
//                    Toast.makeText(this, "confirmation if current date file exists = " +
//                            file.exists(), Toast.LENGTH_SHORT).show();

                    //reformat date to add time
                    SimpleDateFormat timeFormat = new SimpleDateFormat("yyMMdd - HH:mm");
                    String isoTimeString = timeFormat.format(new Date());

                    try {
                        oStream = new FileOutputStream(file, true);
                        oStream.write(("BEGIN LOG: " + isoTimeString + ":\n").getBytes());
                        oStream.write(("---------------------------------------------------------" +
                                "-----------------------\n").getBytes());
                        Log.d(TAG, "beginLoggingToFile: Output stream OPENED: log header " +
                                "appended.");
                    } catch (IOException e) {
                        Log.e(TAG, "beginLoggingToFile: error creating outputStream");
                        e.printStackTrace();
                    } finally {
                        try {
                            oStream.close();
                            Log.d(TAG, "beginLoggingToFile: stream closed");
                        } catch (IOException e) {
                            Log.e(TAG, "beginLoggingToFile: error closing stream");
                            e.printStackTrace();
                        }
                    }
                } else {
                    Log.e(TAG, "beginLoggingToFile: ERROR: PERMISSION NOT GRANTED");
                    //todo: handle this (send user to screen to grant permissions)
                    String error_beginLogging_permissionNotGranted = "error: begin logging to " +
                            "file:\nwrite permission not granted";
                }
            } else {
                Log.e(TAG, "beginLoggingToFile: NEED TO ADD LOWER API LOG METHOD");
                String error_beginLogging_ApiVersionTooLow = "Error: begin logging to file:\n" +
                        "Api version too low and alternative not in place:\n" +
                        "TO BE IMPLEMENTED";
                //todo: this may crash(?) -not on UI thread?
//                Toast.makeText(this, "API TOO LOW TO USE LOGGING METHOD IN PLACE",
//                        Toast.LENGTH_SHORT).show();
                //todo: implement send to settings
                logErrorToFile(error_beginLogging_ApiVersionTooLow, LOGFILE_LINEBREAK_STAR);
            }
        } else {
            //todo: fully handle this (send user to settings to enable permissions)
            String error_mediaMounted = "MEDIA_MOUNTED does not match current state: storage may " +
                    "not be installed/accessible";
            logErrorToFile(error_mediaMounted, LOGFILE_LINEBREAK_STAR);
        }
    }


    //-handles the end of the logging to file
    public void endLoggingToFile() {
        Log.d(TAG, "endLoggingToFile: ");
        //line break at end of current file (multiple same day tests are stored in single file)
        String eof =
                "********************************************************************************";
        try {
            Log.d(TAG, "endLog: reopening final log write");
            oStream = new FileOutputStream(file, true);
            oStream.write(("\n\nEND OF LOG:\n" + eof + "\n\n\n").getBytes());
        } catch (IOException e) {
            Log.e(TAG, "endLog: error writing to log");
            e.printStackTrace();
        } finally {
            try {
                Log.d(TAG, "endLog: closing stream");
                //null fix for debug
                if (oStream != null) {
                    oStream.close();
                    oStream = null;
                } else {
                    Log.w(TAG, "endLoggingToFile: Warning: no oStream object to close.");
                }

            } catch (IOException e) {
                Log.e(TAG, "endLog: error closing output stream");
                e.printStackTrace();
            }
        }
    }


    //-update the current speed limit in memory
    public void updateCurrentSpeedInfo(RoadTags product) {
        Log.d(TAG, "updateCurrentSpeedInfo: ");
        currentSpeedLimit = product.getRoadSpeed();
        currentRoadName = product.getRoadName();

        //testing:
        Log.d(TAG, "updateCurrentSpeedInfo: Road name: " + currentRoadName);
        Log.d(TAG, "updateCurrentSpeedInfo: Road speed: " + currentSpeedLimit);

        //complete logging this iteration
        logObject.setLimitUpdateTime(SystemClock.elapsedRealtime());
        //use log object to fill out one pass of primary loop logging to file
        logIteration();
    }


    //-print current iteration of primary loop logObject to file
    public void logIteration() {
        Log.d(TAG, "logIteration:");
        if (logObject == null) {
            Log.e(TAG, "logIteration: Error: logObject is null: cannot continue logging.");
            logErrorToFile("logIteration: logObject is null.", LOGFILE_LINEBREAK_STAR);
            return;
        }

        if (logObject.checkIterationComplete()) {
            Log.d(TAG, "logIteration: checkiteration complete.");
            try {
                oStream = new FileOutputStream(file, true);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "logIteration: Error: log file not found!");
                e.getMessage();
                e.printStackTrace();
            }

            //TODO: ADD ROAD ARRAY INDEX TO LOGFILE!
            //append to log file:
            try {
                Log.d(TAG, "logIteration: logging object contnents to file");
                //testing:
                long startLogTime = SystemClock.elapsedRealtime();

                oStream = new FileOutputStream(file, true);

                oStream.write(("\n").getBytes());
                oStream.write(("----------------------------------------\n").getBytes());
                //attempt to store current time to logs to make interpreting them easier
                oStream.write((("LOG ENTERED AT ") + getIsoTime(System.currentTimeMillis()) +
                        " (realtime)\n").getBytes());
                oStream.write(("----------------------------------------\n").getBytes());
                oStream.write("LOCATION FIXED:\n".getBytes());
                oStream.write(("\tLAT: " + logObject.getLatitude() + "\n").getBytes());
                oStream.write(("\tLON: " + logObject.getLongitude() + "\n").getBytes());
                oStream.write(("PROVIDER: " + logObject.getProvider() + "\n").getBytes());
                oStream.write(("ACCURACY: " + logObject.getAccuracy() + "\n").getBytes());
                oStream.write(("----------\n").getBytes());
                oStream.write(("API RADIUS: " + API_RADIUS_VALUE + "\n").getBytes());
                oStream.write(("ROAD TOTAL IN RADIUS: " + logObject.getRadiusTotal() + "\n")
                        .getBytes());
                oStream.write(("----------\n").getBytes());
                oStream.write(("ROAD NAME: " + logObject.getRoadName() + "\n").getBytes());
                oStream.write(("SPEED LIMIT: " + logObject.getMaxSpeed() + "mph\n").getBytes());
                oStream.write(("----------\n").getBytes());
//commented out lines for log clarity: (possible move to helper method later)
//                oStream.write(("LOCATION FIX TIME: " + getIsoTime(logObject.getLocationFixTime()) + "\n").getBytes());
//                oStream.write(("LOCATION USE TIME: " + getIsoTime(logObject.getLocationUseTime()) + "\n").getBytes());
//                oStream.write(("QUERY SENT TIME: " + getIsoTime(logObject.getQuerySentTime()) + "\n").getBytes());
//                oStream.write(("QUERY RESPONSE TIME: " + getIsoTime(logObject.getQueryResponseTime()) + "\n").getBytes());
//                oStream.write(("PARSE COMPLETE TIME: " + getIsoTime(logObject.getParseCompleteTime()) + "\n").getBytes());
//                oStream.write(("LIMIT UPDATE TIME: " + getIsoTime(logObject.getLimitUpdateTime()) + "\n").getBytes());
//                oStream.write(("----------\n").getBytes());
                oStream.write(("MILLISECONDS REQUIRED: " + logObject.getTotalTimeRequired() + "\n")
                        .getBytes());
                oStream.write(("----------------------------------------\n").getBytes());
                oStream.write(("\n").getBytes());

                //testing:
                long endLogTime = SystemClock.elapsedRealtime();
                Log.d(TAG, "logIteration: TIME TAKEN TO LOG TO FILE: " +
                        (endLogTime - startLogTime));

            } catch (IOException e) {
                Log.e(TAG, "logIteration: Error: issue logging to file!");
                e.printStackTrace();
            }

            //reset log object (new() should take care of this anyway)
            logObject = null;

            try {
                oStream.close();
            } catch (IOException e) {
                Log.e(TAG, "logIteration: Error: problem closing stream");
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "logIteration: Error: checkIteration FAILED: logObject is missing " +
                    "at least one required variable for logging.");
            logErrorToFile("logIteration: variable check fail.",
                    LOGFILE_LINEBREAK_STAR);
        }
    }


    //-send current values to activity to update UI output (for testing/demo)
    public void sendUiUpdate(int currentLimit, String actualSpeed, WatchedBool indicateLeft, WatchedBool indicateRight, WatchedBool lightLow, WatchedBool lightHigh) {
        //convert current limit
        String currentLimitString = Integer.toString(currentLimit);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent(PrimeForegroundServiceHost.SERVICE_BROADCASTRECEIVER_UI_UPDATE);
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_INDICATOR_LEFT, indicateLeft.getValueString());
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_INDICATOR_RIGHT, indicateRight.getValueString());
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_LIGHTS_LOW, lightLow.getValueString());
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_LIGHTS_HIGH, lightHigh.getValueString());
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_SPEED_LIMIT, currentLimitString);
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_SPEED_ACTUAL, actualSpeed);

        lbm.sendBroadcast(intent);
    }

    /*--------------------------------------
        HELPER METHODS
    --------------------------------------*/
    //-cancels both async tasks (if they exist)
    public void cancelAsyncTasks() {
        Log.d(TAG, "cancelAsyncTasks: ");
        //http task check exists
        if (!(httpTask == null)) {
            Log.d(TAG, "cancelAsyncTasks: cancelling http task...");
            httpTask.cancel(true);
        } else {
            Log.d(TAG, "cancelAsyncTasks: no http task to cancel");
        }
        //parse task check exists
        if (!(parseTask == null)) {
            Log.d(TAG, "cancelAsyncTasks: cancelling parse task...");
            parseTask.cancel(true);
        } else {
            Log.d(TAG, "cancelAsyncTasks: no parse task to cancel");
        }
    }


    //-stops updates to gps/network location providers (testing moved/duplicated these so moved to own method)
    public void stopUpdates() {
        Log.d(TAG, "stopUpdates: ");
        //*2 to stop both providers:
        locationManager.removeUpdates(this);
        locationManager.removeUpdates(this);
    }


    //-checks if both conditions are appropriate for next asyncTask loop to begin
    public void checkAsyncLock() {
        if (asyncLocked) {
            //boolean lock in place: do not execute
            Log.w(TAG, "checkAsyncLock: asyncTask(s) in progress, prevent execution");
            //todo: not logging to file (yet)...may be needed for debugging?
            return;
        } else {
            //lock async asap (attempt to control conditions)
            //todo: research how to make this lock exclusive
            asyncLocked = true;

            //check if previous location matches last query check location record (treat null as same)
            if (apiCheckDuplicateLocation == null) {
                Log.d(TAG, "checkAsyncLock: duplicate location is null:");
                if (finalLocation != null) {
                    Log.d(TAG, "checkAsyncLock: setting current location as duplicate " +
                            "(begin queries next update)");
                    apiCheckDuplicateLocation = finalLocation;
                    //have 'previous location' now, fall through to begin asyncAPI
                } else {
                    Log.w(TAG, "checkAsyncLock: Warning: current location is still NULL: " +
                            "ignoring");
                    asyncLocked = false;
                    return;
                }
            } else if (apiCheckDuplicateLocation.equals(finalLocation)) {
                //no update since last query run: do not execute
                Log.w(TAG, "checkAsyncLock: current location is same as last query, " +
                        "prevent execution");
                asyncLocked = false;
                return;
            }

            Log.d(TAG, "checkAsyncLock: CONDITIONS MET: begin AsyncHttp");
            //create log object(location's fix time and use time)
            logObject = new PrimeServceLogObject(finalLocation.getLatitude(),
                    finalLocation.getLongitude(), finalLocation.getAccuracy(),
                    finalLocationRealTime, SystemClock.elapsedRealtime());
            //todo: add this to creation above
            logObject.setProvider(finalLocation.getProvider());

            //set async primary loop in motion
            startAsyncTaskHTTP();
        }
    }


    //-return current date in iso (YYMMDD) format:
    public String getIsoDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
        String isoDateString = formatter.format(new Date());
        return isoDateString;
    }


    //-return time in milliseconds long as readable TIME (hh:mm)
    public String getIsoTime(long milliTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
        Date date = new Date(milliTime);
        String isoTime = simpleDateFormat.format(date);
        return isoTime;
    }


    //-checks external storage permission
    public boolean checkWritePermission() {
        int result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }


    //-display toast to UI (remove repeated code from class)
//    public void showToastOnUI(final String toastMessage) {
//        Log.d(TAG, "showToastOnUI: displaying Toast...");
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getApplicationContext(),
//                        toastMessage,
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
//    }


    /*--------------------------------------
        TESTING METHODS
    --------------------------------------*/
    //-testing of audio warning playback (triggered from activity button as currently no bike-speed to check against)
    public void testAudioFromButton() {
        //initial testing of binding of service:
//        showToastOnUI("binding of service succeeded:\n" +
//                "continue to test audio.");

        playAudio(TTS_LOLA_WARNING_NETWORK_LOST);
    }



    /*--------------------------------------
        INCOMPLETE/OBSOLETE METHODS
    --------------------------------------*/
    //-request write to external memory permission for logging
    //INCOMPLETE DUE TO SERVICE INSTEAD OF ACTIVITY USE
//    private void requestPermission() {
//        if (ActivityCompat.shouldShowRequestPermissionRationale(
//                PrimaryForegroundServiceActivity.,
//                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//            Toast.makeText(TestForegroundServiceActivity.this,
//                    "Write External Storage permission required to create publicly " +
//                            "accessible log files. Please allow this permission in App Settings.",
//                    Toast.LENGTH_LONG).show();
//        } else {
//            ActivityCompat.requestPermissions(TestForegroundServiceActivity.this,
//                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    PERMISSION_REQUEST_CODE);
//        }
//    }

    //-feedback to user when write permission granted (INCOMPLETE DUE TO BEING SERVICE NOT ACTIVITY)
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[],
//                                           int[] grantResults) {
//        switch (requestCode) {
//            case PERMISSION_REQUEST_CODE:
//                if (grantResults.length > 0 && grantResults[0] ==
//                        PackageManager.PERMISSION_GRANTED) {
//                    Log.e("value", "Permission Granted, Now you can use local drive .");
//                } else {
//                    Log.e("value", "Permission Denied, You cannot use local drive .");
//                }
//                break;
//        }
//    }


    /*--------------------------------------
        BROADCAST RECEIVERS
    --------------------------------------*/
    //-receive instructions to trigger service methods from activity
    private BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: method action broadcast from activity...");
            //get number value of constant (to trigger switch method choice below)
            int broadcastMsg = intent.getIntExtra(PrimeForegroundServiceHost.METHOD_TRIGGER,
                    0);

            switch (broadcastMsg) {
                case METHODTRIGGER_TESTAUDIO:
                    testAudioFromButton();
                    break;
                case METHODTRIGGER_TESTPRINT:
                    Log.d(TAG, "onReceive: SWITCH TEST PRINT!");
                    break;
                default:
                    Log.w(TAG, "onReceive: Warning: unexpected default methodTrigger encountered");
            }
        }
    };


}
