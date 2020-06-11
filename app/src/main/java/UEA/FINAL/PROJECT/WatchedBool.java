/*------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System Mobile Device App
 * FILE:        WatchedBool.Java
 * DESCRIPTION: Object wrapper for boolean variable to trigger listener on update.
 *              Required for Android to observe change of a primitive variable
 * AUTHOR:      SSQ16SHU / 100166648
 * HISTORY:     v1.0    200316  Initial implementation.
 *              v1.1    200316  Returned to version previously tested as is known working.
 *              v1.2    200319  Added string accessor for value.
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;

public class WatchedBool {
    private static final String TAG = WatchedBool.class.getSimpleName();


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    private boolean value;
    private VariableChangeListener variableChangeListener;


    /*--------------------------------------
        CONSTRUCTOR
    --------------------------------------*/
//    public WatchedBool(boolean value) {
//        setValue(value);
//    }


    /*--------------------------------------
        INTERFACES
    --------------------------------------*/
//    public interface VariableChangeListener {
//        public void onVariableChanged(Object... variableThatHasChanged);
//    }


    /*--------------------------------------
        ACCESSORS
    --------------------------------------*/
    public boolean getValue() {
        return value;
    }


    public String getValueString() {
        return Boolean.toString(value);
    }


    /*--------------------------------------
        MUTATORS
    --------------------------------------*/
    public void setValue(boolean value) {
        if (value != this.value) {
            this.value = value;

            if (variableChangeListener != null) {
                variableChangeListener.onVariableChanged();
            }
        }
    }


    public void setBooleanChangeListener(VariableChangeListener variableChangeListener) {
        this.variableChangeListener = variableChangeListener;
    }


    /*--------------------------------------
        METHODS
    --------------------------------------*/
//    private void signalChanged() {
//        if (variableChangeListener != null) {
//            variableChangeListener.onVariableChanged(value);
//        }
//    }

}
