/*--------------------------------------------------------------------------------------------------
 * TITLE:       3YP Motorcycle Feedback System Mobile Device App
 * FILE:        AsyncCompleteListener.Java
 * DESCRIPTION: interface to be used for custom listener for asyncTask completion.
 * AUTHOR:      SSQ16SHU / 100166648
 * -------------------------------------------------------------------------------------------------
 * HISTORY:     v1.0    200224  Initial implementation.
 *              v1.1    200314  Copied from test project.
 * -------------------------------------------------------------------------------------------------
 * NOTES:
 *          +   flag is used as identifier of which task has completed (assign string constant)
 *              and product is any object to be returned from postExecute() (use null if not needed)
 *              any primitives used are automatically wrapped.
 -------------------------------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;

public interface AsyncCompleteListener {
    void onTaskComplete(String flag, Object product);
}
