/*------------------------------------------------------------------------------
 * TITLE:       3YP Motorcycle Feedback System Mobile Device App
 * FILE:        VariableChangeListener.Java
 * ASSOCIATED:  WatchedInteger.Java, WatchedFloat.java, WatchedBool.java
 * DESCRIPTION: Custom interface listener for changes to a variable in Android app.
 * AUTHOR:      SSQ16SHU / 100166648
 *------------------------------------------------------------------------------
 *  HISTORY:     v1.0    200108  Initial implementation.
 *------------------------------------------------------------------------------
 * NOTES:
 *          +   Does not contain any tag of logging due to being an Interface
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;

public interface VariableChangeListener {

    /*--------------------------------------
        INTERFACE METHODS
    --------------------------------------*/
    //trigger when object has been changed (via object set() method)
    void onVariableChanged(Object... newValue);
}
