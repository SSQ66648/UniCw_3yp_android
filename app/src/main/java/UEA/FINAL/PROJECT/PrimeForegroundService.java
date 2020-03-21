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
 *      +   While soundPool seems the better choice for the SFX playback, there have been far too
 *          many issues with it and time wasted in debugging it, so (as have been advised by
 *          multiple people online) mediaPlayer has been re-used in its place.
 *      +   Desired implementation of a 'word-library' to select from when new phrase was needed for
 *          voice feedback has been paused (due to intolerable delay in each word: fix planned as
 *          two players, one playing while the other queues the next file). For time constraint
 *          reasons, additional whole-sentences have been used in their place for now.
 *      +   The above is the same reason that the audio file string constants are not simply the
 *          filename: retention of format used for the array selection (cant pass array to switch).
 *      +   The online source of the TTS audio clips changed format during development resulting in
 *          a slightly different sounding voice for later full sentence files (these files are
 *          marked by a trailing '_' before their file type (.mp3).
 *      +   Dates are recorded in YYMMDD notation.
 *--------------------------------------------------------------------------------------------------
 * OUTSTANDING ISSUES:
 *      +   todo: Limit update to value lower than current speed results in warning being placed in middle of update notification queue: still need to add a way of prioritising playback (perhaps own playeer, pause notification player, restart (seeking)?
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
 *      v2.2    200319  Added completion of intake of bluetooth data, broadcast sending values to
 *                      activity for UI updates, mediaPlayer version of indicator audio feedback.
 *      v2.3    200319  Added network connectivity change listener (early testing version).
 *      v2.4    200320  Tidied Code. Added bulk of audio files, Const identifiers (also to switch).
 *      v2.4.1  200320  Added audio prompts throughout setup phase of service.
 *      v2.5    200320  Added (and re-added) speed limit exceeded warning playback: needs priorities
 *      v2.5.1  200321  Extended section header labels to character line (code becoming complex).
 *      v2.6    200321  Added method for interruption of lower-priority audio playback.
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
 *              todo:   add warning if accuracy is above upper threshold radius
 *              todo:   need to check that old location more accurate than new, doesnt stack (ensure original timestamp)
 *              todo:   add checks for network/gps enabled etc
 *              todo:   add checks for permissions etc (additional to auto generated ones)
 *              todo:   add notifiaction to user / redirect on permission enable request etc
 *              todo:   change http lock logic from equal location object to equal lat/lon values
 *              todo:   add start 1st update using distance travelled?
 *              todo:   add permission redirect to settings
 *              todo:   change all string const to filenames?
 *------------------------------------------------------------------------------
 * MAJOR ADDITIONS NEEDED:
 *              TODO:   retain/check time of last actual update used (override the accuracy selection of oldLocation)
 *              TODO:   accuracy threshold warning
 *              TODO:   radius input option for testing (textinput view?)
 *              TODO:   sequential no-road-value warning (if no usable data for x seconds: treat as no location update warning)
 *              TODO:   ADD PROPER HANDLING FOR NO MAXSPEED: COULD BE ROAD DOESNT HAVE ONE SPECIFIED IN API
 *              TODO:   if bluetooth connection error: show button to retry? (instead of stop-starting service)
 *              //todo: find way of prompting user to stop service? (some way of adding a clickable kill button to notification display?)
 *------------------------------------------------------------------------------
 * CODE HOUSEKEEPING TO DO LIST:
 *              todo:   change all toast notification to method: pass string
 *              todo:   prevent multiple logging error to file (task chain knock-on)
 *              todo:   change all errors in async task to concat strings format
 *              todo:   combine error log and reset methods?
 *              //todo: clean up finished todo items
 *              //todo: change prioriies of log.d/v/i as needed
 *              //todo: add warning to lost bt connection: need own broadcast listener in service or...?
 * ---------------------------------------------------------------------------*/

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
import android.net.ConnectivityManager;
import android.net.Network;
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
    /*----------------------------------------------------------------------------------------------
        CONSTANTS
    ----------------------------------------------------------------------------------------------*/
    private static final String TAG = "PrimeForegroundService";
    //overpassAPI radius request value (const for easy changing during debug) todo: add UI change of value for testing
    public static final int API_RADIUS_VALUE = 20;
    //max seconds allowed to prioritise accuracy over newest location in locationChanged
    public static final int LOCATION_DELAY_THRESHOLD = 15;
    //async task completion listener identification flags
    public static final String TASK_COMPLETION_FLAG_HTTP = "httpComplete";
    public static final String TASK_COMPLETION_FLAG_PARSE = "httpParse";
    //line breaks for logging to file (remove duplicate code) todo: add line directly to method: (as always passing same one?)
    public static final String LOGFILE_LINEBREAK_STAR =
            "****************************************\n";
    public static final String LOGFILE_LINEBREAK_DASH =
            "----------------------------------------\n";
    public static final String LOGFILE_LINEBREAK_EQAL =
            "========================================\n";
    //used to receive broadcasts from activity: value unimportant
    public static final String SERVICE_BROADCASTRECEIVER_ACTION = "action";
    //triggers of service methods, broadcast from activity (default is zero: do nothing)
    public static final int METHODTRIGGER_TESTAUDIO = 1;
    public static final int METHODTRIGGER_TESTPRINT = 2;

    /*------------------
        Audio Identification Strings (used for selection of corresponding (fragmented) audio arrays
    ------------------*/
    public static final String TTS_LOLA_WARNING_NETWORK_LOST = "networkLost";

    /*------------------
        Audio Identification Strings (used for selection of corresponding (full) audio files
    ------------------*/
    //alerts (urgent prompt to user)
    public static final String TTS_LOLA_ALERT_SPEEDLIMIT_EXCEEDED = "limitExceeded";
    //notifications (system status / update information)
    public static final String TTS_LOLA_NOTIFY_WIFI_ONLINE = "wifiOnline";
    public static final String TTS_LOLA_NOTIFY_LIMIT_CHANGE_20 = "limit20";
    public static final String TTS_LOLA_NOTIFY_LIMIT_CHANGE_30 = "limit30";
    public static final String TTS_LOLA_NOTIFY_LIMIT_CHANGE_40 = "limit40";
    public static final String TTS_LOLA_NOTIFY_LIMIT_CHANGE_50 = "limit50";
    public static final String TTS_LOLA_NOTIFY_LIMIT_CHANGE_60 = "limit60";
    public static final String TTS_LOLA_NOTIFY_LIMIT_CHANGE_70 = "limit70";
    public static final String TTS_LOLA_NOTIFY_BEGIN_LOCATION_UPDATES = "beginLocation";
    public static final String TTS_LOLA_NOTIFY_BLUETOOTH_ESTABLISHED = "bluetoothLinked";
    public static final String TTS_LOLA_NOTIFY_NETWORK_ONLINE = "networkOnline";
    public static final String TTS_LOLA_NOTIFY_GPS_ONLINE = "gpsOnline";
    public static final String TTS_LOLA_NOTIFY_MOBILE_ONLINE = "mobileOnline";
    public static final String TTS_LOLA_NOTIFY_SPEEDLIMIT_CHANGED = "limitChanged";
    public static final String TTS_LOLA_NOTIFY_START_SERVICE = "startService";
    public static final String TTS_LOLA_NOTIFY_STOP_SERVICE = "stopService";
    //prompts (background information or instructions to user) //todo: consider splitting info from prompts
    public static final String TTS_LOLA_PROMPT_BLUETOOTH_ERROR = "bluetoothConnectingError";
    public static final String TTS_LOLA_PROMPT_CHECK_BLUETOOTH = "checkBluetooth"; //todo: remove this one?
    public static final String TTS_LOLA_PROMPT_DEV_VERSION = "developmentVersion";
    public static final String TTS_LOLA_PROMPT_DISCLAIMER = "disclaimer";
    public static final String TTS_LOLA_PROMPT_HELMET_TEST = "helmetTest";
    public static final String TTS_LOLA_PROMPT_CONFIRM_VOICE = "confirmVoiceSelection";
    public static final String TTS_LOLA_PROMPT_ENABLE_PERMISSIONS = "enablePermissions";
    public static final String TTS_LOLA_PROMPT_SERVICE_DEVICE_SETTINGS = "troubleshootingService";
    public static final String TTS_LOLA_PROMPT_VOICETEST = "voiceTest";
    //warnings (important (system) status update to user)
    public static final String TTS_LOLA_WARNING_CONNECTIONS_REQUIRED = "connectionsRequired";
    public static final String TTS_LOLA_WARNING_BLUETOOTH_LOST = "bluetoothLost";
    public static final String TTS_LOLA_WARNING_ERROR_OCCURRED = "miscErrorOccurred";
    public static final String TTS_LOLA_WARNING_GPS_LOST = "gpsLost";
    public static final String TTS_LOLA_WARNING_NETWORK_LOST_FULL = "networkLost";
    public static final String TTS_LOLA_WARNING_NO_ROAD_DATA = "noRoadData";
    public static final String TTS_LOLA_WARNING_NO_SPEED_DATA = "noSpeedData";
    public static final String TTS_LOLA_WARNING_UNABLE_TO_CONTINUE = "criticalError";


    /*----------------------------------------------------------------------------------------------
        MEMBER VARIABLES
    ----------------------------------------------------------------------------------------------*/
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
    //used in start httpAsync: if location has not changed, do not bother making http request
    Location apiCheckDuplicateLocation;
    //lock to prevent unnecessary http queries (true = prevent execution)
    boolean asyncLocked = false;

    //testing:(timestamp of httpTask beginning and ending to check duration required)
    long httpStartTime;
    long httpStopTime;

    /*------------------
        PARSE Variables
    ------------------*/
    AsyncPARSE parseTask;

    /*------------------
        Speed-Check Variables
    ------------------*/
    private WatchedFloat currentSpeed = new WatchedFloat();
    WatchedInteger currentSpeedLimit = new WatchedInteger();
    String currentRoadName;
    //plays limit-exceeded warning every x seconds in warningLoop (re-defined as needed)
    private Handler warningHandler;
    private Runnable warningLoop;
    //prevent contents of limit-exceeded check executing more than once
    boolean speedingInProgress = false;


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
    MediaPlayer mediaPlayer_voice;
    MediaPlayer mediaPlayer_sfx_indicator;
    //detection of network connection
    private ConnectivityManager connectivityManager;
    //position in media player array
    int playIndex = 0;
    //variable array of resource file IDs (copied to per array choice)
    String[] resourceFilenameArray = new String[0];
    //lock to control playQueue
    WatchedBool mediaLock = new WatchedBool();
    //container of queued audio playback
    ArrayList<String> playQueue;

    /*------------------
        Audio Array Variables
        ('word-pool' version of voice audio-feedback: if have individual word/fragments,
        can create new feedback sentences without adding more audio files by simply defining arrays)
    ------------------*/
    private final String[] tts_lola_NetworkConnectionLost = {
            "tts_mp3_lola_warning_warning.mp3",
            "tts_mp3_lola_connection_network.mp3",
            "tts_mp3_lola_connection_connection.mp3",
            "tts_mp3_lola_connection_lost.mp3"
    };

    /*------------------
        Bluetooth Variables
    ------------------*/
    //handles incoming serial messages from bluetooth transmission
    private Handler bluetoothInputHandler;
    private final int handlerState = 0;
    private BluetoothAdapter bluetoothAdapter = null;
    //assembly of received data
    private StringBuilder stringBuilder_input = new StringBuilder();
    // SPP UUID service for Hc05 module - this should work for most devices
    // (replace if able to obtain UUID programmatically)
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
    //indicators
    private WatchedBool indicatorR = new WatchedBool();
    private WatchedBool indicatorL = new WatchedBool();
    //low/high beams
    private WatchedBool headlightL = new WatchedBool();
    private WatchedBool headlightH = new WatchedBool();
    //revCounter is currently always zero: no use implemented as of yet)
    private int revCounter = 0;


    /*----------------------------------------------------------------------------------------------
        LIFECYCLE
    ----------------------------------------------------------------------------------------------*/
    //-called ONCE when service is first created
    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate: ");
        super.onCreate();

        queuePlayback(TTS_LOLA_NOTIFY_START_SERVICE);

        handler = new Handler(Looper.getMainLooper());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //assign listeners to watchedBooleans
        assignWatchedBooleans();

        //assign listeners to watchedIntegers
        assignWatchedIntegers();

        //assign listeners to watchedIntegers
        assignWatchedFloats();

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

        queuePlayback(TTS_LOLA_NOTIFY_BEGIN_LOCATION_UPDATES);
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

        //register network changes
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }


    //-start service with "new" passed intent: repeatable (called on UI thread)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand: ");
        //setup handler to deal with incoming serial data
        createInputHandler();
        //setup bluetooth connection to bike
        setupBluetoothSockets(intent);

        //send user to calling host activity on click of notification
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
        Log.v(TAG, "onDestroy: ");
        queuePlayback(TTS_LOLA_NOTIFY_STOP_SERVICE);
        while (mediaLock.getValue()) {
            //await playback to finish
        }

        Log.v(TAG, "onDestroy: cancelling any active tasks...");
        cancelAsyncTasks();
        Log.v(TAG, "onDestroy: stopping updates...");
        stopUpdates();
        Log.v(TAG, "onDestroy: completing log to file...");
        endLoggingToFile();

        Log.v(TAG, "onDestroy: time difference values:");
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

        Log.v(TAG, "MEAN DELAY: " + meanTime + "\n" +
                "MEDIAN DELAY:" + median + "\n" +
                "number of updates: " + updateCount + "\n" +
                "\tgps total: " + gpsCount + "\n" +
                "\tnetwork total: " + netCount + "\n" +
                "time travel count: " + timeErrCount + "\n" +
                "other error total: " + otherErrCount);
        Log.v(TAG, "_");
        Log.v(TAG, "onDestroy: delay array:\n" +
                medianTime);

        //check if any open oStream still exists
        if (oStream != null) {
            Log.v(TAG, "onDestroy: nullifying oStream.");
            oStream = null;
        } else {
            Log.v(TAG, "onDestroy: no oStream to nullify.");
        }

        //notify user
//        showToastOnUI("stopping service...");

        //release any unreleased mediaPlayers
        stopPlayers();

        //unregister receivers
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceBroadcastReceiver);
        connectivityManager.unregisterNetworkCallback(networkCallback);

        Log.v(TAG, "onDestroy: closing bluetooth sockets:");
        if (bluetoothSocket_bike != null) {
            try {
                bluetoothSocket_bike.close();
            } catch (IOException e) {
                Log.e(TAG, "onDestroy: Error: closing bluetooth sockets");
                //handle?
            }
        }

        Log.v(TAG, "onDestroy: stopping self...");
        stopSelf();

        super.onDestroy();
    }


    /*----------------------------------------------------------------------------------------------
        LISTENERS
    ----------------------------------------------------------------------------------------------*/
    @Override
    public void onLocationChanged(Location location) {
        Log.v(TAG, "onLocationChanged: ");
        //immediately get time since boot as location update time (.getTime() is NOT reliable)
        long locationRealTime = SystemClock.elapsedRealtime();
        long locationRealTimeSeconds = locationRealTime / 1000;
        Log.v(TAG, "onLocationChanged: location updated at " + locationRealTimeSeconds +
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
            Log.v(TAG, "onLocationChanged: GPS location passed");
            gpsCount++;
            //             gpsLocation = location;
        } else if (location.getProvider().equals("network")) {
            Log.v(TAG, "onLocationChanged: NETWORK location passed");
            netCount++;
            //             networkLocation = location;
        } else {
            Log.e(TAG, "onLocationChanged: Error: unknown provider identified");
            logErrorToFile("onLocationChanged: unknown provider.",
                    LOGFILE_LINEBREAK_STAR);
            otherErrCount++;
        }

        //use first location if previous does not exist yet, stop further execution
        if (oldFinalLocation == null) {
            Log.v(TAG, "onLocationChanged: no prior location yet: using first update.");
            //assign updated location and associated time
            oldFinalLocation = location;
            oldFinalLocationRealTime = locationRealTime;
            oldFinalLocationRealTimeSeconds = oldFinalLocationRealTime / 1000;
            diffSeconds_location_oldLocation = 0;
            Log.v(TAG, "onLocationChanged: update number: " + updateCount);
            Log.v(TAG, "onLocationChanged: ----------------------------------------\n_");
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
        Log.v(TAG, "DIFF SECONDS TESTING: \n" +
                "new seconds: " + locationRealTimeSeconds + "\n" +
                "old seconds: " + oldFinalLocationRealTimeSeconds + "\n" +
                "dif: " + diffSeconds_location_oldLocation);

        //ensure new value is more recent than old value
        if (diffSeconds_location_oldLocation > 0) {
            Log.v(TAG, "onLocationChanged: newLocation is [" +
                    (diffSeconds_location_oldLocation + "] seconds newer"));

            //ensure difference between last update is within limit (prioritise accuracy)
            if (diffSeconds_location_oldLocation < LOCATION_DELAY_THRESHOLD) {
                Log.v(TAG, "onLocationChanged: new location within " +
                        LOCATION_DELAY_THRESHOLD + " seconds: " + "prioritise accuracy");
                if (location.getAccuracy() < oldFinalLocation.getAccuracy()) {
                    Log.v(TAG, "onLocationChanged: new location more accurate:\n" +
                            "old: " + oldFinalLocation.getAccuracy() +
                            " --- new: " + location.getAccuracy());
                    finalLocation = location;
                    finalLocationRealTime = locationRealTime;
                    finalLocationRealTimeSeconds = locationRealTimeSeconds;
                } else {
                    Log.v(TAG, "onLocationChanged: old location more accurate and within " +
                            "time window:\n" +
                            "new: " + location.getAccuracy() +
                            " --- old: " + oldFinalLocation.getAccuracy());
                    finalLocation = oldFinalLocation;
                    finalLocationRealTime = oldFinalLocationRealTime;
                    finalLocationRealTimeSeconds = oldFinalLocationRealTimeSeconds;
                }
            } else {
                Log.v(TAG, "onLocationChanged: new location took longer than ten seconds " +
                        "to arrive: prioritise newer value");
                Log.v(TAG, "debug:\n" +
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
            Log.v(TAG, "_");
            Log.v(TAG, "----------------------------------------");
            Log.v(TAG, "final location found:\n" +
                    "Provider: " + finalLocation.getProvider() + "\n" +
                    "Time: " + finalLocationRealTime + "\n" +
                    "Lat: " + finalLocation.getLatitude() + "\n" +
                    "Lon: " + finalLocation.getLongitude() + "\n");
            Log.v(TAG, "onLocationChanged: update number: " + updateCount);
            Log.v(TAG, "----------------------------------------\n_");
        } else {
            Log.w(TAG, "********************");
            Log.e(TAG, "onLocationChanged: Error: finalLocation is null.");
            Log.w(TAG, "********************\n_");
            logErrorToFile("onLocationChanged: finalLocation is NULL",
                    LOGFILE_LINEBREAK_STAR);
        }
        Log.v(TAG, "_");

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
        Log.v(TAG, "onTaskComplete:");
        //copy product to prevent orphan data
        Object localProduct = product;
        String localFlag = flag;

        if (localProduct == null) {
            Log.e(TAG, "onTaskComplete: Error: PRODUCT IS NULL");
            Log.e(TAG, "onTaskComplete: product obtained from: " + localFlag);

            logErrorToFile("onTaskComplete: Error: PRODUCT from [" + localFlag +
                    "]IS NULL", LOGFILE_LINEBREAK_STAR);
            errorReset("onTaskCompleted: localProduct is null");
            return;
        }

        if (localFlag == TASK_COMPLETION_FLAG_HTTP) {
            //timestamp
            httpStopTime = SystemClock.elapsedRealtime();
            //add response time to log
            logObject.setQueryResponseTime(httpStopTime);
            Log.v(TAG, "onTaskComplete: httpTask flag found:");

            //testing:
            Log.v(TAG, "onTaskComplete: returned httpQuery string reads: \n" + localProduct);
            Log.v(TAG, "onTaskComplete: check if task still exists: " + httpTask);

            Log.v(TAG, "onTaskComplete: time taken for httpAsyncTask to complete: \n" +
                    "Start time: " + httpStartTime + "\n" +
                    "Stop time: " + httpStopTime + "\n" +
                    "nanoseconds taken :" + (httpStopTime - httpStartTime) + "\n" +
                    "(" + ((httpStopTime - httpStartTime) / 1000) + " seconds)");

            //check that response is string as expected
            if (localProduct instanceof String) {
                Log.v(TAG, "onTaskComplete: response IS string: continue:");
                //http complete: start parsing of road list
                startAsyncTaskParse((String) localProduct);
            } else {
                logErrorToFile("onTaskComplete: Error: API response is NOT string: " +
                        "cannot continue.", LOGFILE_LINEBREAK_STAR);
                errorReset("onTaskComplete: HTTP flag found: not string: reset.");
            }
        } else if (localFlag == TASK_COMPLETION_FLAG_PARSE) {
            Log.v(TAG, "onTaskComplete: parseTask flag found:");
            //check product is of roadTag as expected
            if (localProduct instanceof RoadTags) {
                Log.v(TAG, "onTaskComplete: rechecking road values:\n" +
                        "road name: " + ((RoadTags) localProduct).getRoadName() + "\n" +
                        "road speed: " + ((RoadTags) localProduct).getRoadSpeed());

                //log road name and limit
                logObject.setRoadName(((RoadTags) localProduct).roadName);
                logObject.setMaxSpeed(((RoadTags) localProduct).roadSpeed);

                //testing:
                currentRoadName = ((RoadTags) localProduct).getRoadName();
                //todo: test this works with watched int
                currentSpeedLimit.set(((RoadTags) localProduct).getRoadSpeed());
                Log.v(TAG, "onTaskComplete: service now has access to:\n" +
                        "road: " + currentRoadName + " with limit: " + currentSpeedLimit);

                //add parse completion time to log
                logObject.setParseCompleteTime(SystemClock.elapsedRealtime());

                //todo: add limit alarm logic
                updateCurrentSpeedInfo((RoadTags) localProduct);
            } else {
                Log.e(TAG, "onTaskComplete: Error: expected RoadTags object, "
                        + localProduct + "received.");
                logErrorToFile("onTaskComplete: Error: expected RoadTags object",
                        LOGFILE_LINEBREAK_STAR);
                errorReset("onTaskComplete: PARSE flag found: localProduct is not a " +
                        "RoadTags object");
                return;
                //todo: handle?
            }
            //repeat async loop (triggered on location update): unlock async && new location check
            asyncLocked = false;

        } else {
            Log.e(TAG, "onTaskComplete: Error: flag not recognised:\n" + flag);
            logErrorToFile("onTaskComplete: Error: flag not recognised:",
                    LOGFILE_LINEBREAK_STAR);
            errorReset("onTaskComplete: flag not recognised: reset.");
            return;
        }
    }


    //-attempt to diagnose if memory is cause of service being killed by logging to file
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.v(TAG, "onLowMemory: ");
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
        Log.v(TAG, "onTrimMemory: ");
        //print memory warning to logfile
        String lowMemoryWarninglog = "****************************************\n" +
                "*************** LOW MEMORY WARNING! **************\n" +
                "******************* onTrimMemory *****************\n" +
                "**************************************************\n";
        logErrorToFile(lowMemoryWarninglog, LOGFILE_LINEBREAK_STAR);
    }


    //-listens for network connection changes (not strictly listener, but provides same function)
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    Log.v(TAG, "onAvailable: CONNECTION");
                    if (!connectivityManager.isActiveNetworkMetered()) {
                        //non-metered doesnt confirm -IS- wifi but for purposes, assume it does.
                        Log.v(TAG, "onAvailable: WIFI");
                        //testing: "spare" clip to differentiate between networks becoming available
                        queuePlayback(TTS_LOLA_NOTIFY_WIFI_ONLINE);
                    } else {
                        Log.v(TAG, "onAvailable: MOBILE");
                        queuePlayback(TTS_LOLA_NOTIFY_MOBILE_ONLINE);
                    }
                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    Log.v(TAG, "losing active connection");
                    queuePlayback(TTS_LOLA_WARNING_NETWORK_LOST);
                }
            };


    //-mediaplayer listener: triggers on currently playing audiofile completion
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.v(TAG, "onCompletion: ");
        play();
    }


    /*----------------------------------------------------------------------------------------------
        CLASSES (AsyncTask and STARTING METHODS)
    ----------------------------------------------------------------------------------------------*/
    //-preparation and triggers execution of http api query asyncTask
    public void startAsyncTaskHTTP() {
        Log.v(TAG, "startAsyncTaskHTTP: ");
        Log.v(TAG, "startAsyncTaskHTTP: location counter = " + updateCount);
        //get current location values
        queryLatitude = oldFinalLocation.getLatitude();
        queryLongitude = oldFinalLocation.getLongitude();
        //replace last check location with current location
        apiCheckDuplicateLocation = finalLocation;

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
            Log.v(TAG, "constructed: AsyncHTTP");
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
            Log.v(TAG, "onPreExecute: AsyncHTTP");
            super.onPreExecute();

            //check if cancelled: do not execute
            if (isCancelled()) {
                Log.v(TAG, "onPreExecute: is cancelled: abort");
                activity.errorReset("AsyncHTTP: onPreExecute: cancelled");
                return;
            }

            //check variables are not null
            //todo: add checks for WHICH is null
            if (weakReference == null | listener == null | lat == null | lon == null) {
                Log.e(TAG, "onPreExecute: HTTP: Error: \n" +
                        "passed variable is null: cannot continue.");

                String error_http_preExecute = "Error: HTTP onPreExecute:\n" +
                        "one or more variables have been found to be NULL:\n" +
                        "weakreference: " + weakReference + "\n" +
                        "listener: " + listener + "\n" +
                        "latitude: " + lat + "\n" +
                        "longitude: " + lon;

                activity.logErrorToFile(error_http_preExecute, LOGFILE_LINEBREAK_STAR);
                activity.errorReset("AsyncHttp: onPreExecute: variable null");
                return;
            } else {
                Log.v(TAG, "onPreExecute: all variables exist");
            }
        }


        /*------------------
            EXECUTE
        ------------------*/
        @Override
        protected String doInBackground(Void... voids) {
            Log.v(TAG, "doInBackground: AsyncHTTP");
            PrimeForegroundService activity = weakReference.get();

            //check if task has been cancelled
            if (isCancelled()) {
                Log.v(TAG, "AsyncHTTP: doInBackground: isCancelled(PRE-execute): exiting");
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
            Log.v(TAG, "doInBackground: beginning request...");

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
                        Log.v(TAG, "onResponse: ");
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
                Log.v(TAG, "doInBackground: API: awaiting response latch");
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
                Log.v(TAG, "AsyncHTTP: doInBackground: isCancelled(POST-execute): exiting");
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
            Log.v(TAG, "onPostExecute: AsyncHTTP");
            PrimeForegroundService activity = weakReference.get();

            //cancel check: exit regardless of string status
            if (isCancelled()) {
                Log.v(TAG, "AsyncHTTP: onPostExecute: cancelled: reset and return null.");
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
                    Log.v(TAG, "onPostExecute: listener value: " + listener);
                    Log.v(TAG, "onPostExecute: flag value: " + TASK_COMPLETION_FLAG_HTTP);
                    listener.onTaskComplete(TASK_COMPLETION_FLAG_HTTP, responseString);
                }
            }
        }


        /*------------------
            CANCEL
        ------------------*/
        @Override
        protected void onCancelled() {
            Log.v(TAG, "onCancelled: AsyncHTTP");
            super.onCancelled();
        }


        /*------------------
            METHODS
        ------------------*/
        //-creates URL string from desired parameters and current lat/lon variables
        public void combineURL() {
            Log.v(TAG, "combineURL: ");

            String url_A = "http://overpass-api.de/api/interpreter?data=[out:json];way[";
            //list of tags required in response from API :
            // todo: investigate if any more are possible/useful to project objective
            String tag = "maxspeed";
            String url_B = "](around:";
            String url_C = ");out tags;";

            //combine url: beginning, tags, radius, latitude, longitude, end
            url = url_A + tag + url_B + API_RADIUS_VALUE + "," + lat + ","
                    + lon + url_C;

            Log.v(TAG, "combineUrl: URL concatenated reads:\n" + url);
        }
    }


    //-preparation and execution of AsyncPARSE
    public void startAsyncTaskParse(String roadList) {
        Log.v(TAG, "startAsyncTaskParse: ");
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

        //create error message by class section
        // todo: check if this is preferred version of error string creation: replace other version
        String errorMessageString = "";
        String errorMessageClass = "Error: AsyncPARSE: ";
        String errorMessageMethod = "";


        /*------------------
            CONSTRUCTOR
        ------------------*/
        //-pass service (activity) reference
        AsyncPARSE(PrimeForegroundService activity, AsyncCompleteListener listener,
                   String response) {
            Log.v(TAG, "constructed: AsyncPARSE:");
            weakReference = new WeakReference<>(activity);
            this.listener = listener;
            this.response = response;
        }


        /*------------------
            PRE EXECUTE
        ------------------*/
        @Override
        protected void onPreExecute() {
            Log.v(TAG, "onPreExecute: AsyncPARSE");
            super.onPreExecute();
            PrimeForegroundService activity = weakReference.get();
            //update error msg
            errorMessageMethod = "onPreExecute: ";

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
            Log.v(TAG, "doInBackground: AsyncPARSE");
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
                Log.v(TAG, "doInBackground: PARSE: elements:\n" +
                        responseArr);

                for (int i = 0; i < responseArr.length(); i++) {
                    //get current element object from string
                    String elementString = responseArr.getString(i);
                    JSONObject elementObj = new JSONObject(elementString);
                    //logcat:
                    Log.v(TAG, "----------------------------------------");

                    //add road name(s) to array list
                    try {
                        //for each element, get string of name
                        roadNames.add(elementObj.getJSONObject("tags").getString("name"));
                        Log.v(TAG, "AsyncPARSE: ROADNAME RETRIEVED:");

                        //testing:
                        Log.v(TAG, "doInBackground: NAME: " + roadNames.get(i));
                    } catch (JSONException e) {
                        Log.w(TAG, "AsyncPARSE: JSON exception occurred: no NAME for road");
                        roadNames.add("No name found");
                    }

                    //add max speed to array list
                    try {
                        //debugging:
                        Log.v(TAG, "doInBackground: debug: speed string contents: ["
                                + elementObj.getJSONObject("tags").getString("maxspeed")
                                + "]");

                        //remove all non numeric from string of speed limit and parse to int
                        String charlessSpeedString = elementObj.getJSONObject("tags")
                                .getString("maxspeed");
                        charlessSpeedString = charlessSpeedString.replaceAll(
                                "[^\\d.]", "");
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
                            Log.v(TAG, "AsyncPARSE: SPEED RETRIEVED:");
                        }

                        //testing:
                        Log.v(TAG, "AsyncPARSE: doInBackground: SPEED: " + roadSpeeds.get(i));
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
                    Log.v(TAG, "----------------------------------------");
                }
            } catch (JSONException e) {
                Log.e(TAG, "AsyncPARSE: doInBackground: Error: parsing response string.");
                //debugging cause of killed service: print error to log
                activity.logErrorToFile("AsyncPARSE: doInBackground: JSON exception " +
                        "occurred: outer JSON try brace.", LOGFILE_LINEBREAK_STAR);
                activity.errorReset("outer JSON parsing exception: reset");
                e.getMessage();
                e.printStackTrace();
            }

            //add number of roads found in radius to logfile
            activity.logObject.setRadiusTotal(roadNames.size());
            //testing:
            Log.v(TAG, "doInBackground: roadnames size = " + roadNames.size());

            //more than one road returned: choose one to use
            if (roadNames.size() > 1) {
                Log.v(TAG, "doInBackground: roadnames.size > 1");
            } else if (roadNames.size() == 1) {
                Log.v(TAG, "doInBackground: roadNames size = 1");
                //todo: potential to skip chooseroad? (ensure not further function called?)
            } else if (roadNames.size() == 0) {
                Log.w(TAG, "doInBackground: Warning: roadsize = 0: check IDE claim this " +
                        "is always true...");
                //no roads added: handle potential errors further in code by substitution of values
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
            Log.v(TAG, "AsyncPARSE: onPostExecute: ");
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
            Log.v(TAG, "--------------------");

            //debug:
            Log.v(TAG, "AsyncPARSE: onPostExecute: task complete: ");
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
            Log.v(TAG, "onCancelled: AsyncPARSE");
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
            Log.v(TAG, "AsyncPARSE: chooseRoad: ");
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
            //TODO: RESEARCH IF ANY WAY TO DETERMINE SELECTION LOGIC (ORDER OF API RESULTS?):

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
                Log.v(TAG, "AsyncPARSE: chooseRoad: road chosen from API returned list:\n"
                        + roadNames.get(0) + " @ " + roadSpeeds.get(0) + "mph");
                returnedRoad = new RoadTags(roadNames.get(0), roadSpeeds.get(0));

                //record total roads found in return for API radius
                activity.logObject.setRadiusTotal(roadNames.size());
                //change number found to zero for log
                if (roadSpeeds.get(0) == -1) {
                    activity.logObject.setRadiusTotal(0);
                } else {
                    //set selected road index position in road array
                    // todo: replace with logic for however road is selected
                    activity.logObject.setRoadArrayIndex(1);
                }

                //testing:
                Log.v(TAG, "chooseRoad: returnedRoad values:\n" +
                        "\troad name: " + returnedRoad.getRoadName() + "\n" +
                        "\troad speed: " + returnedRoad.getRoadSpeed() + "\n");
            }
        }
    }


    /*----------------------------------------------------------------------------------------------
        CONTAINER CLASSES/ THREADS
    ----------------------------------------------------------------------------------------------*/
    //-container for both road name and speed (other tags deemed useful later) back from AsyncPARSE
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
                Log.e(TAG, "ConnectedThread: Error: creating I/O streams for socket");
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
                    Log.e(TAG, "run: Error: failed to obtain message");
                    break;
                }
            }

        }

        //write to remote device (used as test for successful connection)
        public void write(String input) {
            //convert String into bytes
            byte[] msgBuffer = input.getBytes();
            try {
                //write bytes over bluetooth outStream
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                //single instance of audio feedback re various connection stages failing
                queuePlayback(TTS_LOLA_PROMPT_BLUETOOTH_ERROR);
                Log.e(TAG, "write: Error: failed to write to remote device");
                //if you cannot write, close application
                Toast.makeText(getApplicationContext(), "Connection Failure",
                        Toast.LENGTH_LONG).show();
                //todo: testing:
                //prompt user to terminate service and retry from activity (excluding testing)
                sendConnectionError();
            }
        }
    }


    /*----------------------------------------------------------------------------------------------
        METHODS
    ----------------------------------------------------------------------------------------------*/
    //-selects audio array to play (mostly implemented with FULL single files, however kept method
    // in this format in event there is time to debug the delay in 'word-pool' version
    public void playAudio(String playChoice) {
        //clear any existing content
        resourceFilenameArray = new String[1];
        switch (playChoice) {
            //alerts
            case TTS_LOLA_ALERT_SPEEDLIMIT_EXCEEDED:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_alert_speedlimitexceeded.mp3";
                break;
            //notifications
            case TTS_LOLA_NOTIFY_LIMIT_CHANGE_20:
                //todo: revisit idea of fall-through array assignment: ie create array with 2 items, 1st file is update and 2nd file specific number value?
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_notify_20mph.mp3";
                break;
            case TTS_LOLA_NOTIFY_LIMIT_CHANGE_30:
                //todo: test
                resourceFilenameArray[0] = "tts_lola_notify_30mph.mp3";
                break;
            case TTS_LOLA_NOTIFY_BEGIN_LOCATION_UPDATES:
                //todo: test
                resourceFilenameArray[0] = "tts_lola_notify_beginninglocationupdates.mp3";
                break;
            case TTS_LOLA_NOTIFY_BLUETOOTH_ESTABLISHED:
                resourceFilenameArray[0] = "tts_lola_notify_bluetoothestablished.mp3";
                break;
            case TTS_LOLA_NOTIFY_MOBILE_ONLINE:
                resourceFilenameArray[0] = "tts_lola_notify_mobileonline_.mp3";
                break;
            case TTS_LOLA_NOTIFY_NETWORK_ONLINE:
                //todo: implment / test
                //not currently used (as opted for wifi mobile data -specific feedback,
                //kept to possibly reuse later if user-preferred (not much wifi outside)
                resourceFilenameArray[0] = "tts_lola_notify_networkconnectiononline.mp3";
                break;
            case TTS_LOLA_NOTIFY_SPEEDLIMIT_CHANGED:
                //used in conjunction with numeric value: //todo: review combining idea
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_notify_speedlimitchangedto.mp3";
                break;
            case TTS_LOLA_NOTIFY_START_SERVICE:
                resourceFilenameArray[0] = "tts_lola_notify_startingservice.mp3";
                break;
            case TTS_LOLA_NOTIFY_STOP_SERVICE:
                resourceFilenameArray[0] = "tts_lola_notify_stopping_service_.mp3";
                break;
            case TTS_LOLA_NOTIFY_WIFI_ONLINE:
                resourceFilenameArray[0] = "tts_lola_notify_wifionline_.mp3";
                break;
            //prompts
            case TTS_LOLA_PROMPT_BLUETOOTH_ERROR:
                resourceFilenameArray[0] = "tts_lola_prompt_bluetootherror_.mp3";
                break;
            case TTS_LOLA_PROMPT_CHECK_BLUETOOTH:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_prompt_checkbluetoothconnected.mp3";
                break;
            case TTS_LOLA_PROMPT_DEV_VERSION:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_prompt_developmentbuildversion.mp3";
                break;
            case TTS_LOLA_PROMPT_DISCLAIMER:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_prompt_disclaimer.mp3";
                break;
            case TTS_LOLA_PROMPT_HELMET_TEST:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_prompt_helmetbluetoothtest.mp3";
                break;
            case TTS_LOLA_PROMPT_ENABLE_PERMISSIONS:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_prompt_pleaseenablepermissions.mp3";
                break;
            case TTS_LOLA_PROMPT_VOICETEST:
                //currently unused
                resourceFilenameArray[0] = "tts_lola_prompt_voicetest.mp3";
                break;
            //warnings
            case TTS_LOLA_WARNING_CONNECTIONS_REQUIRED:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_warning_appconnectionstofunction.mp3";
                break;
            case TTS_LOLA_WARNING_BLUETOOTH_LOST:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_warning_bluetoothtobikelost.mp3";
                break;
            case TTS_LOLA_WARNING_ERROR_OCCURRED:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_warning_erroroccurred.mp3";
                break;
            case TTS_LOLA_WARNING_GPS_LOST:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_warning_gpssignallost.mp3";
                break;
            case TTS_LOLA_WARNING_NETWORK_LOST:
                //original 'word-pool' implementation (kept unacceptable delay for reference)
                resourceFilenameArray = tts_lola_NetworkConnectionLost;
                break;
            case TTS_LOLA_WARNING_NO_ROAD_DATA:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_warning_noroaddatafound.mp3";
                break;
            case TTS_LOLA_WARNING_NO_SPEED_DATA:
                resourceFilenameArray[0] = "tts_lola_warning_nospeedlimitavailable.mp3";
                break;
            case TTS_LOLA_WARNING_UNABLE_TO_CONTINUE:
                //todo: implment / test
                resourceFilenameArray[0] = "tts_lola_warning_unabletocontinue.mp3";
                break;
        }
        play();
    }


    //-naive attempt to prevent two requests crashing mediaPlayer
    public void queuePlayback(String selection) {
        Log.v(TAG, "queuePlayback: ");
        if (mediaLock.getValue()) {
            //mediaPlayer locked:queue play request
            if (playQueue == null) {
                playQueue = new ArrayList<>();
            }
            Log.v(TAG, "queuePlayback: queued selection");
            playQueue.add(selection);
        } else {
            Log.v(TAG, "queuePlayback: no queue needed: play");
            //directly proceed with playback
            playAudio(selection);
        }
    }


    MediaPlayer interruptMediaPlayer;


    //-pause and resume playback that needs to be interrupted
    public void interruptPlayback(String interruptMessage) {
        Log.d(TAG, "interruptPlayback: ");

        //pause any playing sounds
        if (mediaPlayer_voice != null && mediaPlayer_voice.isPlaying()) {
            mediaPlayer_voice.pause();
        }
        if (mediaPlayer_sfx_indicator != null && mediaPlayer_sfx_indicator.isPlaying()) {
            mediaPlayer_sfx_indicator.pause();
        }

        //play priority file (via new player)
        try {
            interruptMediaPlayer = new MediaPlayer();
            AssetFileDescriptor afd = null;
            afd = getAssets().openFd(interruptMessage);
            interruptMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            interruptMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    //release resources from priority player
                    interruptMediaPlayer.stop();
                    interruptMediaPlayer.reset();
                    interruptMediaPlayer.release();

                    if (mediaPlayer_voice != null && !mediaPlayer_voice.isPlaying()) {
                        mediaPlayer_voice.start();
                    }
                    if (mediaPlayer_sfx_indicator != null && !mediaPlayer_sfx_indicator.isPlaying()) {
                        mediaPlayer_sfx_indicator.start();
                    }
                    //todo: test if needs storage of seek position...?
                }
            });
            interruptMediaPlayer.prepare();
            interruptMediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "interruptPlayback: Error: error on priority player.");
        }
    }

    //storage of playback duration on paused
    private int seekToPosition = -1;
    //storage of intterupted file reference
    private String interruptedFile;


    //-play audio at index in array or cease
    public void play() {
        if (playIndex > resourceFilenameArray.length - 1) {
            Log.v(TAG, "play: playlist finished.");
            //reset counter, array
            playIndex = 0;
            resourceFilenameArray = null;
            //release resources
            stopPlayers();
            //playback complete
            mediaLock.setValue(false);
            return;
        } else {
            //engage lock if not already
            mediaLock.setValue(true);
            //repopulate player
            try {
                try {
                    if (mediaPlayer_voice == null) {
                        //iteration 1
                        mediaPlayer_voice = new MediaPlayer();
                    } else {
                        //iteration 2+
                        mediaPlayer_voice.reset();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "play: Error: creating new mediaplayer");
                    Log.e(TAG, "Cause: " + String.valueOf(e.getCause()));
                    Log.e(TAG, "message: " + e.getMessage());
                    e.printStackTrace();
                }

                AssetFileDescriptor afd = getAssets().openFd(resourceFilenameArray[playIndex]);
                mediaPlayer_voice.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                        afd.getLength());
                mediaPlayer_voice.setOnCompletionListener(this);
                mediaPlayer_voice.setVolume(0.9f, 0.9f);
                mediaPlayer_voice.prepare();
                mediaPlayer_voice.start();
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


    public void playSfx_indicator() {
        Log.v(TAG, "playSfx: ");
        try {
            if (mediaPlayer_sfx_indicator == null) {
                mediaPlayer_sfx_indicator = new MediaPlayer();
            } else {
                mediaPlayer_sfx_indicator.reset();
            }
            AssetFileDescriptor afd = getAssets().openFd(
                    "sfx_car_indicator_interior_twolivesleft.wav");
            mediaPlayer_sfx_indicator.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            mediaPlayer_sfx_indicator.setLooping(true);
            mediaPlayer_sfx_indicator.prepare();
            mediaPlayer_sfx_indicator.start();
        } catch (Exception e) {
            Log.e(TAG, "playSfx: Error: playing from indicator SFX mediaplayer");
            e.printStackTrace();
        }
    }


    //-setup bluetooth adapter and sockets
    public void setupBluetoothSockets(Intent intent) {
        Log.v(TAG, "onStartCommand: obtaining bluetooth adapter");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //todo: check bluetooth state?
        bikeAddress = intent.getStringExtra(BluetoothActions.EXTRA_DEVICE_ADDRESS);
        Log.v(TAG, "onStartCommand: received bluetooth address: " + bikeAddress);

        //todo: fix workaround: hard coded address if intent extra is lost : how can this happen?
        if (bikeAddress == null) {
            Log.e(TAG, "requestConnectDevice: Error: BIKE ADDRESS IS NULL: employing " +
                    "work-around of hard coded value for development.");
            bikeAddress = "FC:A8:9A:00:4A:DF";
        }

        //create device and set the MAC address
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(bikeAddress);
            Log.v(TAG, "onResume: device created: " + device.getName() + " : "
                    + device.getAddress());
            try {
                bluetoothSocket_bike = createBluetoothSocket(device);
                Log.v(TAG, "onResume: create bluetooth socket");
            } catch (IOException e) {
                Log.v(TAG, "onResume: Socket creation failed.");
                Toast.makeText(getApplicationContext(), "Socket creation failed",
                        Toast.LENGTH_LONG).show();
            }
            // Establish the Bluetooth socket connection.
            try {
                bluetoothSocket_bike.connect();
                Log.v(TAG, "onResume: socket connected.");
                queuePlayback(TTS_LOLA_NOTIFY_BLUETOOTH_ESTABLISHED);
                Toast.makeText(getApplicationContext(), "Connected to bike",
                        Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                //todo: specific audio for errors (maybe not applicable to users: dont care re specifics of bluetooth issue)?
                Log.v(TAG, "onResume: socket connection error.");
                try {
                    bluetoothSocket_bike.close();
                    Log.v(TAG, "onResume: socket closed");
                } catch (IOException e2) {
                    Log.v(TAG, "onResume: Error on closing socket during establish failure!");
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


    //-create handler for incoming bluetooth serial data
    // todo: move to static class to prevent leaks (warning suppressed)
    @SuppressLint("HandlerLeak")
    public void createInputHandler() {
        Log.v(TAG, "createInputHandler: ");
        bluetoothInputHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                Log.v(TAG, "handleMessage: ");
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
                        Log.v(TAG, "handleMessage: Data Received = \n" + dataInPrint);
                        //get length of data received (25char initially, grows with triple digits)
                        int dataLength = dataInPrint.length();
                        Log.v(TAG, "handleMessage: String Length = " +
                                String.valueOf(dataLength));

                        //check for beginning character of '#' -signifies desired transmission
                        if (stringBuilder_input.charAt(0) == '#') {
                            //string array for sensor values
                            String[] incomingStatusValues = new String[7];

                            //remove first character ('#') to simplify splitting string
                            stringBuilder_input.deleteCharAt(0);

                            //convert stringBuilder to string array
                            receivedValues = stringBuilder_input.toString().split("\\+");

                            //assign and check result of splitString
                            for (int i = 0; i < receivedValues.length - 1; i++) {
                                //debugging: trying to find cause of index out of bounds exception:
                                // (length = 7; index = 7) shouldnt be possible: guessing something to do with debugger?
                                if (i == incomingStatusValues.length || i == receivedValues.length) {
                                    Log.e(TAG, "handleMessage: Error: INDEX EQUAL TO LENGTH BUG: debugger is the cause?");
                                } else {
                                    incomingStatusValues[i] = receivedValues[i];
                                }
                            }

                            //testing send values to logcat via warning (allows filtering of debug level logs and below)
                            Log.v(TAG, "----------------------------------------");
                            Log.v(TAG, "handleMessage: SEQ No. = " +
                                    incomingStatusValues[0]);
                            Log.v(TAG, "handleMessage: Speed = " +
                                    incomingStatusValues[1] + " mph");
                            Log.v(TAG, "handleMessage: LEFT indicator = " +
                                    incomingStatusValues[2]);
                            Log.v(TAG, "handleMessage: RIGHT indicator = " +
                                    incomingStatusValues[3]);
                            Log.v(TAG, "handleMessage: LOW beams = " +
                                    incomingStatusValues[4]);
                            Log.v(TAG, "handleMessage: HIGHbeams = " +
                                    incomingStatusValues[5]);
                            Log.v(TAG, "handleMessage: REVCOUNTER = " +
                                    incomingStatusValues[6]);
                            Log.v(TAG, "----------------------------------------");

                            //get local copy of speed (that can be monitored)
                            //todo: test if needs valueOf instead?
                            currentSpeed.set(Float.parseFloat(incomingStatusValues[1]));
                            //string copy to display to UI
                            String currentSpeeds = incomingStatusValues[1] + "mph";
                            //bool value obtained from int (if received value == 1 : true otherwise false (ie 0))
                            indicatorL.setValue(incomingStatusValues[2].equals("1"));
                            indicatorR.setValue(incomingStatusValues[3].equals("1"));
                            headlightL.setValue(incomingStatusValues[4].equals("1"));
                            headlightH.setValue(incomingStatusValues[5].equals("1"));

                            sendUiUpdate(currentSpeedLimit.get(), currentSpeeds, indicatorL,
                                    indicatorR, headlightL, headlightH);


                            //todo: better catch for non-sequential data
                            //convert string to int
                            try {
                                seqNew = Integer.parseInt(incomingStatusValues[0]);
                            } catch (Exception e) {
                                Log.e(TAG, "handleMessage: string to int parse error");
                                e.printStackTrace();
                            }

                            Log.v(TAG, "OLD sequence no. = " + seqOld);
                            Log.v(TAG, "NEW sequence no. = " + seqNew);

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
                        stringBuilder_input.delete(0, stringBuilder_input.length());
                    }
                }
            }
        };
    }


    //-reset status in event of error/exception (attempt to continue after logging to file)
    public void errorReset(String methodName) {
        //pass method name that called this method to identify origin of error
        //(local copy in case asyncTask goes out of scope)
        String localMethodName = methodName;

        //log error cause to file
        logErrorToFile(localMethodName, LOGFILE_LINEBREAK_STAR);
        //cancel any async tasks if running:
        cancelAsyncTasks();
        //release lock for awaiting next iteration
        asyncLocked = false;
    }


    //-log error string to file (tidy code: repeated object creation and writing)
    public void logErrorToFile(String errorMessage, String linebreak) {
        Log.v(TAG, "logErrorToFile: ");
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
        Log.v(TAG, "beginLoggingToFile: ");

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Log.v(TAG, "beginLoggingToFile: media mounted");
            if (Build.VERSION.SDK_INT >= 23) {
                Log.v(TAG, "beginLoggingToFile: build version >= 23");
                if (checkWritePermission()) {
                    Log.v(TAG, "beginLoggingToFile: permission granted");
                    File sdcard = Environment.getExternalStorageDirectory();
                    dir = new File(sdcard.getAbsolutePath() + "/logFiles/");
                    Log.v(TAG, "beginLoggingToFile: directory to check/ create: \n" + dir);
                    if (!dir.exists()) {
                        Log.v(TAG, "beginLoggingToFile: directory doesnt exist: creating...");
                        if (dir.mkdir()) {
                            Log.v(TAG, "beginLoggingToFile: directory created");
                        } else {
                            Log.e(TAG, "beginLoggingToFile: Error: directory failed to be " +
                                    "created!");
                            //todo: handle
                        }
                    } else {
                        Log.v(TAG, "beginLoggingToFile: directory exists");
                    }

                    //create file name/path from date
                    String filenameByDate = "gpsRoadLogFile_" + getIsoDate() + ".txt";
                    Log.v(TAG, "beginLoggingToFile: logfile name by date: " + filenameByDate);
                    file = new File(dir, filenameByDate);

                    //debugging
                    Log.v(TAG, "beginLoggingToFile: current date file already exists = " +
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
                        Log.v(TAG, "beginLoggingToFile: Output stream OPENED: log header " +
                                "appended.");
                    } catch (IOException e) {
                        Log.e(TAG, "beginLoggingToFile: error creating outputStream");
                        e.printStackTrace();
                    } finally {
                        try {
                            oStream.close();
                            Log.v(TAG, "beginLoggingToFile: stream closed");
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
        Log.v(TAG, "endLoggingToFile: ");
        //line break at end of current file (multiple same day tests are stored in single file)
        String eof =
                "********************************************************************************";
        try {
            Log.v(TAG, "endLog: reopening final log write");
            oStream = new FileOutputStream(file, true);
            oStream.write(("\n\nEND OF LOG:\n" + eof + "\n\n\n").getBytes());
        } catch (IOException e) {
            Log.e(TAG, "endLog: error writing to log");
            e.printStackTrace();
        } finally {
            try {
                Log.v(TAG, "endLog: closing stream");
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
        Log.v(TAG, "updateCurrentSpeedInfo: ");
        //update speed (in local variable)
        //todo: address assumed duplicate update (additional in onComplete listener)
        currentSpeedLimit.set(product.getRoadSpeed());
        currentRoadName = product.getRoadName();

        //testing:
        Log.v(TAG, "updateCurrentSpeedInfo: Road name: " + currentRoadName);
        Log.v(TAG, "updateCurrentSpeedInfo: Road speed: " + currentSpeedLimit);

        //complete logging this iteration
        logObject.setLimitUpdateTime(SystemClock.elapsedRealtime());
        //use log object to fill out one pass of primary loop logging to file
        logIteration();
    }


    //-print current iteration of primary loop logObject to file
    public void logIteration() {
        Log.v(TAG, "logIteration:");
        if (logObject == null) {
            Log.e(TAG, "logIteration: Error: logObject is null: cannot continue logging.");
            logErrorToFile("logIteration: logObject is null.", LOGFILE_LINEBREAK_STAR);
            return;
        }

        if (logObject.checkIterationComplete()) {
            Log.v(TAG, "logIteration: checkiteration complete.");
            try {
                oStream = new FileOutputStream(file, true);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "logIteration: Error: log file not found!");
                e.getMessage();
                e.printStackTrace();
            }

            //append to log file:
            try {
                Log.v(TAG, "logIteration: logging object contnents to file");
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
                Log.v(TAG, "logIteration: TIME TAKEN TO LOG TO FILE: " +
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
    public void sendUiUpdate(int currentLimit, String actualSpeed, WatchedBool indicateLeft,
                             WatchedBool indicateRight, WatchedBool lightLow,
                             WatchedBool lightHigh) {
        Log.v(TAG, "sendUiUpdate: ");
        //convert current limit
        String currentLimitString = Integer.toString(currentLimit);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent(PrimeForegroundServiceHost.SERVICE_BROADCASTRECEIVER_UI_UPDATE);
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_INDICATOR_LEFT,
                indicateLeft.getValueString());
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_INDICATOR_RIGHT,
                indicateRight.getValueString());
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_LIGHTS_LOW,
                lightLow.getValueString());
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_LIGHTS_HIGH,
                lightHigh.getValueString());
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_SPEED_LIMIT, currentLimitString);
        intent.putExtra(PrimeForegroundServiceHost.UI_UPDATE_SPEED_ACTUAL, actualSpeed);

        lbm.sendBroadcast(intent);
    }


    //-send notification to activity that bluetooth connection has failed (stop self)
    public void sendConnectionError() {
        Log.d(TAG, "sendConnectionError: ");
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent(PrimeForegroundServiceHost.SERVICE_BROADCASTRECEIVER_CONNECTION_ERROR);
        //no extra needed: only signal of error
        lbm.sendBroadcast(intent);
    }


    //-compares current moving speed against current location's speed limit
    public void compareSpeedAndLimit() {
        Log.v(TAG, "checkSpeedAgainstLimit: ");

    }

    /*----------------------------------------------------------------------------------------------
        HELPER METHODS
    ----------------------------------------------------------------------------------------------*/
    //-assigning listeners to watchedBooleans
    public void assignWatchedBooleans() {
        Log.v(TAG, "assignIndicatorLightBools: ");
        //if indicators are on: play indicator SFX
        indicatorL.setBooleanChangeListener(new VariableChangeListener() {
            @Override
            public void onVariableChanged(Object... newValue) {
                if (indicatorL.getValue()) {
                    Log.v(TAG, "onVariableChanged: Start Indicator playback");
                    //testing:
                    Log.e(TAG, "onVariableChanged: VALUE: " + indicatorL.getValue());
                    //play looped SFX
                    playSfx_indicator();
                } else {
                    Log.v(TAG, "onVariableChanged: stop indicator playback");
                    if (mediaPlayer_sfx_indicator != null) {
                        mediaPlayer_sfx_indicator.stop();
                        mediaPlayer_sfx_indicator.reset();
                        mediaPlayer_sfx_indicator.release();
                        mediaPlayer_sfx_indicator = null;
                    }
                }
            }
        });

        //repeat of above
        indicatorR.setBooleanChangeListener(new VariableChangeListener() {
            @Override
            public void onVariableChanged(Object... newValue) {
                if (indicatorR.getValue()) {
                    Log.v(TAG, "onVariableChanged: Start Indicator playback");
                    playSfx_indicator();
                } else {
                    Log.v(TAG, "onVariableChanged: stop indicator playback");
                    if (mediaPlayer_sfx_indicator != null) {
                        mediaPlayer_sfx_indicator.stop();
                        mediaPlayer_sfx_indicator.reset();
                        mediaPlayer_sfx_indicator.release();
                        mediaPlayer_sfx_indicator = null;
                    }
                }
            }
        });

        //mediaPlayer locked/released (multiple playback prevents .isPlaying or conComplete use)
        mediaLock.setBooleanChangeListener(new VariableChangeListener() {
            @Override
            public void onVariableChanged(Object... newValue) {
                if (mediaLock.getValue()) {
                    Log.v(TAG, "onVariableChanged: voice mediaPlayer is now locked");
                } else {
                    Log.v(TAG, "onVariableChanged: voice mediaPlayer is now released");
                    //play next queued item if one exists
                    if (playQueue != null && playQueue.size() > 0) {
                        //assign copy of playing file in case of interruption (to resume)
                        playAudio(playQueue.remove(playQueue.size() - 1));
                        //destroy queue object if empty
                        if (playQueue.size() == 0) {
                            playQueue = null;
                        }
                    }

                }
            }
        });
    }


    public void assignWatchedIntegers() {
        Log.v(TAG, "assignWatchedIntegers: ");
        currentSpeedLimit.setIntChangeListener(new VariableChangeListener() {
            @Override
            public void onVariableChanged(Object... newValue) {
                Log.v(TAG, "onVariableChanged: speed updated to [" +
                        currentSpeedLimit.get() + "]mph");
                //notify user
                //todo: combine these to prevent mitm exceeded warning?
                queuePlayback(TTS_LOLA_NOTIFY_SPEEDLIMIT_CHANGED);
                //specific value:
                switch (currentSpeedLimit.get()) {
                    case 20:
                        queuePlayback(TTS_LOLA_NOTIFY_LIMIT_CHANGE_20);
                        break;
                    case 30:
                        queuePlayback(TTS_LOLA_NOTIFY_LIMIT_CHANGE_30);
                        break;
                    case 40:
                        queuePlayback(TTS_LOLA_NOTIFY_LIMIT_CHANGE_40);
                        break;
                    case 50:
                        queuePlayback(TTS_LOLA_NOTIFY_LIMIT_CHANGE_50);
                        break;
                    case 60:
                        queuePlayback(TTS_LOLA_NOTIFY_LIMIT_CHANGE_60);
                        break;
                    case 70:
                        queuePlayback(TTS_LOLA_NOTIFY_LIMIT_CHANGE_70);
                        break;
                    default:
                        Log.e(TAG, "onVariableChanged: Error: unrecognised speed limit");
                        //unrecognised/no speed value (may need to add other legal variations)
                        queuePlayback(TTS_LOLA_WARNING_NO_SPEED_DATA);
                        break;
                }
            }
        });
    }


    public void assignWatchedFloats() {
        Log.v(TAG, "assignWatchedFloats: ");
        currentSpeed.setFloatChangeListener(new VariableChangeListener() {
            @Override
            public void onVariableChanged(Object... newValue) {
                Log.v(TAG, "onVariableChanged: ");
                //check if exceeding limit (ignore limit of zero: no data)
                if (currentSpeed.get() > currentSpeedLimit.get() && currentSpeedLimit.get() > 0) {
                    //prevent re-triggering every update
                    if (!speedingInProgress) {
                        Log.w(TAG, "onVariableChanged: Warning: speed limit exceeded!");
                        speedingInProgress = true;

                        //repeat timer for playback warning (not looping: too much stimulus?)
                        //todo: check if user testing supports this literature claim
                        warningHandler = new Handler(Looper.getMainLooper());
                        warningHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                interruptPlayback("tts_lola_alert_speedlimitexceeded.mp3");
                                warningHandler.postDelayed(this, 3000);
                            }
                        }, 1000);

                        //begin playback every 3 seconds
                        warningHandler.postDelayed(warningLoop, 0);
                    }
                } else {
                    if (speedingInProgress) {
                        //bool toggle inside check to prevent unnecessary re-assignment
                        speedingInProgress = false;
                        //stop loop (remove ALL pending callbacks and messages (via null key))
                        warningHandler.removeCallbacksAndMessages(null);
                    }

                }
            }
        });
    }


    //creates secure outgoing connection with BT device using UUID
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BtModuleUUID);
    }


    //-cancels both async tasks (if they exist)
    public void cancelAsyncTasks() {
        Log.v(TAG, "cancelAsyncTasks: ");
        //http task check exists
        if (!(httpTask == null)) {
            Log.v(TAG, "cancelAsyncTasks: cancelling http task...");
            httpTask.cancel(true);
        } else {
            Log.v(TAG, "cancelAsyncTasks: no http task to cancel");
        }
        //parse task check exists
        if (!(parseTask == null)) {
            Log.v(TAG, "cancelAsyncTasks: cancelling parse task...");
            parseTask.cancel(true);
        } else {
            Log.v(TAG, "cancelAsyncTasks: no parse task to cancel");
        }
    }


    //-release resources assigned to player if it exists
    public void stopPlayers() {
        if (mediaPlayer_voice != null) {
            Log.v(TAG, "stopPlayers: releasing resources for voice player");
            mediaPlayer_voice.stop();
            mediaPlayer_voice.reset();
            mediaPlayer_voice.release();
            mediaPlayer_voice = null;
            return;
        }

        if (mediaPlayer_sfx_indicator != null) {
            Log.v(TAG, "stopPlayers: releasing resources for sfx player");
            mediaPlayer_sfx_indicator.stop();
            mediaPlayer_sfx_indicator.reset();
            mediaPlayer_sfx_indicator.release();
            mediaPlayer_sfx_indicator = null;
            return;
        }

        if (interruptMediaPlayer != null) {
            interruptMediaPlayer.stop();
            interruptMediaPlayer.reset();
            interruptMediaPlayer.release();
            interruptMediaPlayer = null;
        }

        Log.w(TAG, "stopPlayers: no player exists to stop");
    }


    //-stops updates to gps/network location providers
    public void stopUpdates() {
        Log.v(TAG, "stopUpdates: ");
        //*2 to stop both providers:
        locationManager.removeUpdates(this);
        locationManager.removeUpdates(this);
    }


    //-checks if both conditions are appropriate for next asyncTask loop to begin
    public void checkAsyncLock() {
        if (asyncLocked) {
            //boolean lock in place: do not execute
            Log.w(TAG, "checkAsyncLock: asyncTask(s) in progress, prevent execution");
            return;
        } else {
            //lock async asap (attempt to control conditions)
            //todo: research how to make this lock exclusive
            asyncLocked = true;

            //check previous location matches last query check location record (treat null as same)
            if (apiCheckDuplicateLocation == null) {
                Log.v(TAG, "checkAsyncLock: duplicate location is null:");
                if (finalLocation != null) {
                    Log.v(TAG, "checkAsyncLock: setting current location as duplicate " +
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

            Log.v(TAG, "checkAsyncLock: CONDITIONS MET: begin AsyncHttp");
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


    /*----------------------------------------------------------------------------------------------
        BROADCAST RECEIVERS
    ----------------------------------------------------------------------------------------------*/
    //-receive instructions to trigger service methods from activity
    private BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive: method action broadcast from activity...");
            //get number value of constant (to trigger switch method choice below)
            int broadcastMsg = intent.getIntExtra(PrimeForegroundServiceHost.METHOD_TRIGGER,
                    0);

            switch (broadcastMsg) {
                case METHODTRIGGER_TESTAUDIO:
                    testAudioFromButton();
                    break;
                case METHODTRIGGER_TESTPRINT:
                    Log.v(TAG, "onReceive: SWITCH TEST PRINT!");
                    break;
                default:
                    Log.w(TAG, "onReceive: Warning: unexpected default methodTrigger");
            }
        }
    };


    /*----------------------------------------------------------------------------------------------
        TESTING METHODS
    ----------------------------------------------------------------------------------------------*/
    //-testing of audio warning playback (triggered from activity button)
    public void testAudioFromButton() {
        //initial testing of binding of service:
//        showToastOnUI("binding of service succeeded:\n" +
//                "continue to test audio.");

        queuePlayback(TTS_LOLA_WARNING_NETWORK_LOST);
    }


    /*----------------------------------------------------------------------------------------------
        (UNUSED) LISTENERS
    ----------------------------------------------------------------------------------------------*/
    //-required by superclass
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }


    /*----------------------------------------------------------------------------------------------
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


    //-display toast to UI (remove repeated code from class)
//    public void showToastOnUI(final String toastMessage) {
//        Log.v(TAG, "showToastOnUI: displaying Toast...");
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getApplicationContext(),
//                        toastMessage,
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
//    }


}
