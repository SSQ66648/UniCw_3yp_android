/*------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System Mobile Device App
 * FILE:        DeviceCard.Java
 * LAYOUT(S):   card_device.xml
 * DESCRIPTION: Cusom CardView representing Bluetooth devices: used to populate the RecyclerViews in
 *              BluetoothActions activity
 * AUTHOR:      SSQ16SHU / 100166648
 * HISTORY:     v1.0    200315  Initial implementation.
 *              v1.1    200316  Added status boolean and storage of device itself within card,
 *                              connection type const.
 *------------------------------------------------------------------------------
 * NOTES:       
 *      +   Storage of device in card is most likely very inefficient and is only used as an
 *          attempted workaround due to time limitations remaining for project.
 *------------------------------------------------------------------------------
 * FUTURE IMPROVEMENTS:
 *      +   add checking to connection types (ie. error if connection types are > 2 or < 0)
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;

import android.bluetooth.BluetoothDevice;

/*--------------------------------------
    IMPORT LIST
--------------------------------------*/
public class DeviceCard {
    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    private static final String TAG = DeviceCard.class.getSimpleName();

    //device types (for monitoring BOTH are connected)
    public static final String CONNECTION_HELMET = "HELMET";
    public static final String CONNECTION_BIKE = "MOTORCYCLE";
    public static final String CONNECTION_NONE = "NONE";


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    //---VARIABLES---
    private int deviceIconResource;
    private String deviceName;
    //testing:
    private boolean isConnected;
    //type of connection made
    private String connectionType = CONNECTION_NONE;
    //contain copy of each device within card itself (allows device to be accessed from card directly)-experimental
    private BluetoothDevice cardDeviceReference;


    /*--------------------------------------
        CONSTRUCTOR
    --------------------------------------*/
    public DeviceCard(int deviceIconResource, String deviceName, BluetoothDevice device) {
        this.deviceIconResource = deviceIconResource;
        this.deviceName = deviceName;
        this.cardDeviceReference = device;
    }


    /*--------------------------------------
        ACCESSORS
    --------------------------------------*/
    public int getImageResource() {
        return deviceIconResource;
    }


    public String getDeviceName() {
        return deviceName;
    }

    public boolean getConnectionStatus() {
        return isConnected;
    }

    public BluetoothDevice getDevice() {
        return cardDeviceReference;
    }

    public String getConnectionType() {
        return connectionType;
    }


    /*--------------------------------------
        MUTATORS
    --------------------------------------*/
    //-set boolean of if device has been connected or not (experimental)
    public void setConnectionStatus(boolean status, String type) {
        this.connectionType = type;
        this.isConnected = status;
    }


    /*--------------------------------------
        METHODS
    --------------------------------------*/
    @Override
    public String toString() {
        return "Device card contents:\n" +
                "Device name: " + deviceName + "\n";
        //todo: add more info items as needed eg device class, address  etc
    }
}
