/*------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        WatchedFloat.Java
 *
 * LAYOUT(S):   activity_.xml
 *
 * DESCRIPTION: Object wrapper to allow use of listener to detect change in value of float primitive
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v1.0    200320  Initial implementation.
 *------------------------------------------------------------------------------
 * NOTES:       
 *      +   
 *------------------------------------------------------------------------------
 * TO DO LIST:  
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;

/*--------------------------------------
    IMPORT LIST
--------------------------------------*/
public class WatchedFloat {
    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    private static final String TAG = WatchedFloat.class.getSimpleName();


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    private VariableChangeListener listener;
    private float value;


    /*--------------------------------------
        ACCESSORS
    --------------------------------------*/
    public float get() {
        return value;
    }


    /*--------------------------------------
        MUTATORS
    --------------------------------------*/
    public void set(float value) {
        this.value = value;
        if (listener != null) {
            //no "ignore if value same as current" logic: trigger every time regardless
            listener.onVariableChanged(value);
        }
    }


    /*--------------------------------------
        METHODS
    --------------------------------------*/
    //set listener
    public void setFloatChangeListener(VariableChangeListener listener) {
        this.listener = listener;
    }
}
