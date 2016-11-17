package com.wathci.taptapubiquitouscontroller;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onNewIntent(getIntent());
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Toast.makeText(MainActivity.this, "got message", Toast.LENGTH_LONG).show();
            Log.d("logg", "here");
            Parcelable[] rawMessages =
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {
                NdefMessage[] messages = new NdefMessage[rawMessages.length];
                for (int i = 0; i < rawMessages.length; i++) {
                    messages[i] = (NdefMessage) rawMessages[i];
                }
                // mine
                Log.d("messages", messages[0].toString());
                Log.d("messages len", String.valueOf(messages.length));
                NdefRecord[] records = messages[0].getRecords();
                Log.d("records", records[0].toString());
                Log.d("records length", String.valueOf(records.length));
                byte[] payload = records[0].getPayload();
                Log.d("payload", payload.toString());
                String payloadStr = "";
                try{
                    payloadStr = new String(payload, "US-ASCII");
                } catch (UnsupportedEncodingException e){
                    Log.d("unsupported", "exception");
                }
                Log.d("payloadStr", payloadStr);
            }
        }
    }
}
