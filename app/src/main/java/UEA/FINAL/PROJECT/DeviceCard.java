/*------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        DeviceCard.Java
 *
 * LAYOUT(S):   card_device.xml
 *
 * DESCRIPTION: Card representing Bluetooth devices.
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v1.0    200315  Initial implementation.
 *------------------------------------------------------------------------------
 * NOTES:       
 *      +   
 *------------------------------------------------------------------------------
 * TO DO LIST:  
 *      todo:
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;


/*--------------------------------------
    IMPORT LIST
--------------------------------------*/
public class DeviceCard {
    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    private static final String TAG = DeviceCard.class.getSimpleName();


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    //---VARIABLES---
    private int deviceIconResource;
    private String deviceName;


    /*--------------------------------------
        CONSTRUCTOR
    --------------------------------------*/
    public DeviceCard(int deviceIconResource, String deviceName) {
        this.deviceIconResource = deviceIconResource;
        this.deviceName = deviceName;
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


    /*--------------------------------------
        METHODS
    --------------------------------------*/
    @Override
    public String toString() {
        return "Device card contents:\n" +
                "Device name: " + deviceName + "\n";
        //todo: add more info items as needed
    }
}
