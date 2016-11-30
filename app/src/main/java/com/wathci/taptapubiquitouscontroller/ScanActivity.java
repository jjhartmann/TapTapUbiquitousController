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
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

import static android.R.attr.id;
import static com.wathci.taptapubiquitouscontroller.R.id.textView;


/**
 * Created by Lisa on 11/28/2016.
 * This is the activity that handles receiving and displaying the results of scanned tags
 */

public class ScanActivity extends AppCompatActivity {
    // used to determine swipe
    private float xDown; // location of down touch
    private float xUp; // location of up touch

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        setupToolbar();
        setupBroadcastManager();

        // Created from scanning tag
        if(savedInstanceState == null){
            onNewIntent(getIntent());
        } else {
            updateDisplay();
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
            new NfcTask(ScanActivity.this, true, "").execute(tagId, Constants.ACTIVITY_MESSAGE);
        }
        // started from button press
        else {
            updateDisplay();
        }
    }

    /*
    params: intent is incoming Nfc intent
    returns: id from tag if found as string
             otherwise 0 ie when start from button will be 0
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
            ScanResultsManager.addElem(result);
            updateDisplay();
            Log.d("gotResultScan", result.toString());
        }
    };

    /*
    updates the Content view of this activity to display the current element
     */
    protected void updateDisplay(){
        int size = ScanResultsManager.getSize();
        if(size > 0){
            ScanResult curr = ScanResultsManager.getCurrElem();
            display(curr);
        } else {
            displayBlank();
        }
    }

    /*
    Change the display to have curr's stuff displayed
     */
    protected void display(ScanResult curr){
        setContentView(R.layout.scan_result);
        setupToolbar();

        // setup number
        TextView elemText = (TextView)findViewById(R.id.elemNum);
        String elemNum = Integer.toString(ScanResultsManager.getCurrIndex() + 1);
        elemText.setText(elemNum);

        // setup text
        String stateString = curr.getStateString();
        String statusString = curr.getStatusString();
        String actionString = curr.getActionString();
        String displayText = "Device Name: " + curr.friendlyName + "\n\n" +
                             "Log Type: " + actionString + "\n\n" +
                             "State: " + stateString + "\n\n" +
                             "Status: " + statusString;
        // set text
        TextView textView = (TextView)findViewById(R.id.scan_results);
        textView.setText(displayText);

        // set button
        String buttonText = curr.getButtonText();
        Button button = (Button) findViewById(R.id.resendButton);
        button.setText(buttonText);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resend();
            }
        });
    }

    /*
    Sends new message to server with same tagId and actionType
     */
    protected void resend(){
        ScanResult curr = ScanResultsManager.getCurrElem();
        int tagId = curr.tagId;
        String actionType = curr.actionType;
        new NfcTask(ScanActivity.this, false, actionType).execute(tagId, Constants.ACTIVITY_MESSAGE);
    }

    /*
    Show screen when no saved results
     */
    protected void displayBlank(){
        setContentView(R.layout.activity_scan);
        setupToolbar();

        // setup text
        String displayText = "\n\n\nNo scan results to display.";
        // set text
        TextView textView = (TextView)findViewById(R.id.textView);
        textView.setText(displayText);
    }

    public boolean onTouchEvent(MotionEvent event){
        int action = MotionEventCompat.getActionMasked(event);

        switch(action) {
            case (MotionEvent.ACTION_DOWN) :
                Log.d("OnTouch", "down");
                xDown = event.getX();
                return true;
            case (MotionEvent.ACTION_UP) :
                Log.d("OnTouch", "up");
                xUp = event.getX();
                testSwipe();
                return true;
            default :
                return super.onTouchEvent(event);
        }
    }

    /* will call increment or decrement accordingly if swipe
     */
    protected void testSwipe(){
        Log.d("OnTouch", "in test swipe");
        int swipeThreshold = 100;
        if(Math.abs(xDown - xUp) > swipeThreshold){
            Log.d("OnTouch", Float.toString(Math.abs(xDown - xUp)));
            Log.d("OnTouch", "swipe deteced");
            if(xDown < xUp){
                // swipe right
                ScanResultsManager.decrement();
            } else {
                // swipe left
                ScanResultsManager.increment();
            }
            Toast.makeText(getApplicationContext(), "Swiped!", Toast.LENGTH_SHORT).show();
            updateDisplay();
        }

    }

    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(resultReceiver);
        super.onDestroy();
    }
}
