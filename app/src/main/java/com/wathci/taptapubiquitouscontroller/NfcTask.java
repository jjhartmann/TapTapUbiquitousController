package com.wathci.taptapubiquitouscontroller;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by Lisa on 11/18/2016.
 */

public class NfcTask extends AsyncTask<Integer, Integer, String> {
    private Context appContext;

    public NfcTask(Context context){
        appContext = context;
    }

    @Override
    protected String doInBackground(Integer... params) {
        int tagId = params[0];
        // int touchType = params[1];
        long startTime = System.currentTimeMillis();
        long currTime = System.currentTimeMillis();
        long len = 3000;
        while(currTime - startTime < len){
            currTime = System.currentTimeMillis();
        }
        Log.d("task", "done");
        String result = "finished";
        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Intent resultIntent = new Intent(Constants.BROADCAST_ACTION_FROM_SERVICE);
        resultIntent.putExtra(Constants.EXTENDED_DATA_STATUS, result);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(resultIntent);
    }
}
