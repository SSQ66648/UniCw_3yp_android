/*------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        WatchedBool.Java
 *
 * LAYOUT(S):   activity_.xml
 *
 * DESCRIPTION: Object wrapper for boolean variable to trigger listener on update.
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v1.0    200316  Initial implementation.
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
public class WatchedBool {
    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    private static final String TAG = WatchedBool.class.getSimpleName();


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    private boolean value;
    private VariableChangeListener variableChangeListener;


    /*--------------------------------------
        CONSTRUCTOR
    --------------------------------------*/
    public WatchedBool(boolean value) {
        setValue(value);
    }


    /*--------------------------------------
        INTERFACES
    --------------------------------------*/
    public interface VariableChangeListener {
        public void onVariableChanged(Object... variableThatHasChanged);
    }


    /*--------------------------------------
        ACCESSORS
    --------------------------------------*/
    public boolean getValue() {
        return value;
    }


    /*--------------------------------------
        MUTATORS
    --------------------------------------*/
    public void setValue(boolean value) {
        if (value != this.value) {
            this.value = value;
            signalChanged();
        }
    }


    public void setVariableChangeListener(VariableChangeListener variableChangeListener) {
        this.variableChangeListener = variableChangeListener;
    }


    /*--------------------------------------
        METHODS
    --------------------------------------*/
    private void signalChanged() {
        if (variableChangeListener != null) {
            variableChangeListener.onVariableChanged(value);
        }
    }

}
