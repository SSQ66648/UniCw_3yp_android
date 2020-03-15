/*--------------------------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System App (UEA.FINAL.PROJECT)
 *
 * FILE:        MainActivity.Java
 *
 * LAYOUT:      .xml
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * DESCRIPTION: Main activity for app. Hosts main menu for application
 *              (possibly superfluous if no submenus are implemented in time)
 *--------------------------------------------------------------------------------------------------
 * NOTES:
 *      +
 *      +   Dates are recorded in YYMMDD notation.
 *--------------------------------------------------------------------------------------------------
 * OUTSTANDING ISSUES:
 *      +
 *--------------------------------------------------------------------------------------------------
 * HISTORY:
 *      v1.0    200314  Reimplementation from test project.
 *--------------------------------------------------------------------------------------------------
 * TO DO:
 *      todo:   finish assigning buttons
 *      todo:
 *      todo:
 *------------------------------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;
/*--------------------------------------
    IMPORT LIST
--------------------------------------*/

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    //---LAYOUT---
    private Button button_sync;
    private Button button_serviceActivity;
    private Button button_optionsMenu;


    /*--------------------------------------
        LIFECYCLE
    --------------------------------------*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //logcat clarification of relevent content:
        Log.d(TAG, "----------------------------------------------------------------------------------------------------");
        Log.d(TAG, "onCreate: ");

        //---TOOLBAR---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Motorcycle Feedback App");

        //--BUTTONS---
        button_sync = findViewById(R.id.button_main_openbluetooth);
        button_sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BluetoothActions.class));
            }
        });

        button_serviceActivity = findViewById(R.id.button_main_test_serviceactivity);
        button_serviceActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PrimeForegroundServiceHost.class));
            }
        });

        button_optionsMenu = findViewById(R.id.button_main_openoptions);
        button_optionsMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo: change code to class when created...
                startActivity(new Intent(MainActivity.this, SyncDevices.class));
            }
        });

    }
}
