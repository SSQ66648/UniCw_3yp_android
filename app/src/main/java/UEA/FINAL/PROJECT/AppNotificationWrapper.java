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
 *      +   Dates are recorded in YYMMDD notation.
 *--------------------------------------------------------------------------------------------------
 * OUTSTANDING ISSUES:
 *      +
 *--------------------------------------------------------------------------------------------------
 * HISTORY:
 *      v1.0    200314  Initial implementation.
 *--------------------------------------------------------------------------------------------------
 * TO DO:
 *      +
 *------------------------------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;
/*--------------------------------------
    IMPORT LIST
--------------------------------------*/

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

public class AppNotificationWrapper extends Application {
    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    private static final String TAG = AppNotificationWrapper.class.getSimpleName();
    //notification channel for foreground service
    public static final String CHANNEL_3YP = "project_notifiaction_channel";


    /*--------------------------------------
        LIFECYCLE
    --------------------------------------*/

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }


    /*--------------------------------------
        METHODS
    --------------------------------------*/
    public void createNotificationChannels() {
        //check if on oreo (api 26) or above as notification channel class not available lower
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //name here visible to user: describe what its for in real app
            //importance level define how disruptive notifications can be (sound, popup etc)
            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_3YP,
                    "project foreground service notification channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            //remember user has ultimate control over notifications: can turn off at will
            //create channel cant change in hindsight: have to uninstall and reinstall with settings
            //description should describe function of channel
            channel1.setDescription("project foreground service notification channel");
        } else {
            Log.w(TAG, "createNotificationChannels: Warning: SDK below target: ");
        }


    }
}
