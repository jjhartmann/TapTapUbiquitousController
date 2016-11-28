package com.wathci.taptapubiquitouscontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

/**
 * Created by Lisa on 11/28/2016.
 * This is the activity that handles receiving and displaying the results of scanned tags
 */

public class ScanActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        setupToolbar();
        setupBroadcastManager();

        // Created from scanning tag
        if(savedInstanceState == null){
            onNewIntent(getIntent());
        }
    }

    private void setupToolbar(){
        // Add title and home button to toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
    }

    private void setupBroadcastManager(){
        LocalBroadcastManager.getInstance(this).registerReceiver(resultReceiver,
                new IntentFilter(Constants.RETURN_SCAN_FROM_SERVICE));
    }

    // Handles new intents
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int tagId = getTagId(intent);
        Log.d("tagId", Integer.toString(tagId));
        if (tagId != 0) {
            new NfcTask(ScanActivity.this, Constants.ACCEL_TRHESHOLD,
                    Constants.MILLIS_TO_WAIT).execute(tagId, Constants.ACTIVITY_MESSAGE);
        }
    }

    /*
    params: intent is incoming Nfc intent
    returns: id from tag if found as string
             otherwise 0
 */
    private int getTagId(Intent intent){
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Toast.makeText(getApplicationContext(), "Read tag", Toast.LENGTH_SHORT).show();
            Parcelable[] rawMessages =
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {
                NdefMessage[] messages = new NdefMessage[rawMessages.length];
                for (int i = 0; i < rawMessages.length; i++) {
                    messages[i] = (NdefMessage) rawMessages[i];
                }
                // mine
                NdefRecord[] records = messages[0].getRecords(); // get records in first message
                byte[] payload = records[0].getPayload(); // get payload from first record
                String idStr = ""; // id number as str
                try{
                    idStr = new String(payload, "US-ASCII"); // convert payload to str
                } catch (UnsupportedEncodingException e){
                    Log.d("exceptionMain", e.toString());
                }
                if(!idStr.equals("")) {
                    return Integer.parseInt(idStr);
                }
            }
        }
        return 0; // could not get id
    }

    // receives network info from async task
    private BroadcastReceiver resultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ScanResult result = (ScanResult) intent.getSerializableExtra(
                    Constants.EXTENDED_RESULT_FROM_SERVER);
            Log.d("gotResultScan", result.toString());
        }
    };

    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(resultReceiver);
        super.onDestroy();
    }
}
