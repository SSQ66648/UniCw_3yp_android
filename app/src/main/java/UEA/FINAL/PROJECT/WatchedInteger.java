/*------------------------------------------------------------------------------
 * TITLE:       3YP Motorcycle Feedback System Mobile Device App
 * FILE:        WatchedInteger.Java
 * DESCRIPTION: Object wrapper for integer variable that requires a listener applied to it.
 *              Triggers VariableChangeListener() on being set().
 *              Required for Android to observe change of a primitive variable
 * AUTHOR:      SSQ16SHU / 100166648
 *-------------------------------------------------------------------------------
 * HISTORY:     v1.0    200108  Initial implementation.
 *              v1.0.1  200110  Tidied code slightly.
 *              v1.1    200320  Added value must have actually changed condition.
 *------------------------------------------------------------------------------
 * TO DO LIST:
 *              todo:   potential to change this into generic object rather than just for int?
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;

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
