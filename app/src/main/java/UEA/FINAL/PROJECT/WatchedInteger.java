/*------------------------------------------------------------------------------
 *
 * TITLE:       3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        WatchedInteger.Java
 *
 * ASSOCIATED:  VariableChangeListener.Java
 *
 * DESCRIPTION: Object wrapper for integer variable that requires a listener applied to it.
 *              Triggers VariableChangeListener() on being set().
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v1.0    200108  Initial implementation.
 *              v1.0.1  200110  Tidied code slightly.
 *              v1.1    200320  Added value must have actually changed condition.
 *
 * NOTES:       date notation: YYMMDD
 *              comment format:
 *                  //---GROUP---
 *                  //explanation
 *
 *------------------------------------------------------------------------------
 * TO DO LIST:
 *              todo:
 *              todo:   potential to change this into generic object rather than just for int?
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;
/*--------------------------------------
    IMPORT LIST
--------------------------------------*/
//log import IS used: just commented out to de-clutter logcat

public class WatchedInteger {
    private static final String TAG = "WatchedInteger";

    /*--------------------------------------
        CLASS VARIABLES
    --------------------------------------*/
    private VariableChangeListener listener;
    private int value;


    /*--------------------------------------
        ACCESSOR(S)
    --------------------------------------*/
    //get value of object
    public int get() {
        return value;
    }


    /*--------------------------------------
        MUTATOR(S)
    --------------------------------------*/
    //set value of object and trigger listener
    public void set(int value) {
        //only notify changed if value has actually changed
        if (value != this.value) {
            this.value = value;
            if (listener != null) {
                //no "ignore if value same as current" logic: trigger every time regardless
                listener.onVariableChanged(value);
            }
        }
    }


    /*--------------------------------------
        METHODS
    --------------------------------------*/
    //set listener
    public void setIntChangeListener(VariableChangeListener listener) {
        this.listener = listener;
    }
}
