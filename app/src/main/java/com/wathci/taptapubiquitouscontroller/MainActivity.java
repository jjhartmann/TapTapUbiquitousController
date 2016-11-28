package com.wathci.taptapubiquitouscontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

import static android.R.attr.id;
import static android.R.attr.password;

public class MainActivity extends AppCompatActivity {

    /********** ON CREATE **********/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("onCreate", "called on create");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar();
        setupBroadcastManager();
        setupButton();
    }

    private void setupToolbar(){
        // toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
    }

    private void setupBroadcastManager(){
        LocalBroadcastManager.getInstance(this).registerReceiver(resultReceiver,
                new IntentFilter(Constants.RETURN_REGISTRATION_FROM_SERVICE));
    }

    private void setupButton(){
        // button stuff
        Button registerButton = (Button) findViewById(R.id.registration);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start async task to register device
                new NfcTask(MainActivity.this, Constants.ACCEL_TRHESHOLD,
                        Constants.MILLIS_TO_WAIT).execute(
                        Constants.NO_TAG_ID, Constants.REGISTRATION_MESSAGE);

                // popup message with password
            }
        });
    }

    /********** END ON CREATE **********/

    @Override
    protected void onPostResume(){
        super.onPostResume();
        Log.d("onPostResume", "called on post resume");
    }

    // receives network info from async task
    private BroadcastReceiver resultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ScanResult result = (ScanResult) intent.getSerializableExtra(
                    Constants.EXTENDED_RESULT_FROM_SERVER);
            displayRegistrationConfirmation(result);
            Log.d("gotResultMain", result.toString());
        }
    };

    /*
    Displays alertDialogue communicating the result of the registration to the user
     */
    private void displayRegistrationConfirmation(ScanResult result){
        // set message
        int status = result.status;
        String alertMessage = "";
        if(status == Constants.SUCCESS){
            alertMessage = "Your phone is now registered.";
        } else {
            alertMessage = "An error occurred.\nPlesae try again.";
        }
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                new ContextThemeWrapper(MainActivity.this, R.style.RegDialogueTheme));
        alertBuilder.setMessage(alertMessage).setCancelable(false)
                    .setNeutralButton("Got it", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.setTitle("Registration");
        alertDialog.show();

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(resultReceiver);
        super.onDestroy();
    }
}
