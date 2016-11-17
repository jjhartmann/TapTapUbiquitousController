package com.wathci.taptapubiquitouscontroller;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by Lisa on 11/17/2016.
 */

public class NfcService extends IntentService {

    public NfcService(){
        super("NfcService");
        Log.d("service", "fake started");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("service", "started");
        int tagId = intent.getIntExtra(Constants.EXTENDED_DATA_STATUS, -1);
        Log.d("service", Integer.toString(tagId));

        //String result = sendData(); // Todo send over network
        String result = "result";
        reportResult(result);
    }

    // reports results using local broadcast
    private void reportResult(String result){
        Intent resultIntent = new Intent(Constants.BROADCAST_ACTION_FROM_SERVICE);
        resultIntent.putExtra(Constants.EXTENDED_DATA_STATUS, result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
    }
}
