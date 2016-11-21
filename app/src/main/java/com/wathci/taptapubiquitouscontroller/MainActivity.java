package com.wathci.taptapubiquitouscontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    private boolean screenTouched = false; // true when user is touching screen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LocalBroadcastManager.getInstance(this).registerReceiver(resultReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION_FROM_SERVICE));

        // Created from scanning tag
        if(savedInstanceState == null){
            onNewIntent(getIntent());
        }
    }

    // Handles new intents
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int tagId = getTagId(intent);
        Log.d("tagId", Integer.toString(tagId));
        if(tagId!=0) {
            new NfcTask(getApplicationContext()).execute(tagId);
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

    /*
    Sets screenTouched to true when user is touching screen
    Sets screenTouched to false when user is not touching screen
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        switch(action){
            case(MotionEvent.ACTION_DOWN):
                Log.d("motionEvent", "down");
                screenTouched = true;
                return true;
            case(MotionEvent.ACTION_UP):
                Log.d("motionEvent", "up");
                screenTouched = false;
                return true;
            default:
                Log.d("motionEvent", "other motion event");
                return super.onTouchEvent(event);
        }

    }

    // receives network info from async task
    private BroadcastReceiver resultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra(Constants.EXTENDED_DATA_STATUS);
            Log.d("gotResult", result);
        }
    };

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(resultReceiver);
        super.onDestroy();
    }
}
