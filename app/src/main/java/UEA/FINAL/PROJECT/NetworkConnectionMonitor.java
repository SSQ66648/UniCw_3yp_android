/*------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        NetworkConnectionMonitor.Java
 *
 * LAYOUT(S):   activity_.xml
 *
 * DESCRIPTION: MIGHT BE OBSOLETE AND DEPRECIATED AKA USELESS
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v1.0    200319  Initial implementation.
 *------------------------------------------------------------------------------
 * NOTES:       
 *      +   
 *------------------------------------------------------------------------------
 * TO DO LIST:  
 *      todo:
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/*--------------------------------------
    IMPORT LIST
--------------------------------------*/
public class NetworkConnectionMonitor {
    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    private static final String TAG = NetworkConnectionMonitor.class.getSimpleName();


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    public static final int TYPE_NOT_CONNECTED = 0;
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE = 2;
    public static final int NETWORK_NOT_CONNECTED = 0;
    public static final int NETWORK_WIFI = 1;
    public static final int NETWORK_MOBILE = 2;


    /*--------------------------------------
        STATIC METHODS
    --------------------------------------*/
    //-returns the current connection state
    public static int getConnectionStatus(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return TYPE_WIFI;
            }
            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return TYPE_MOBILE;
            }
        }
        return TYPE_NOT_CONNECTED;
    }


}
