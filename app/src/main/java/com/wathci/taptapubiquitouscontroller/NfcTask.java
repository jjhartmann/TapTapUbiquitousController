package com.wathci.taptapubiquitouscontroller;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Lisa on 11/18/2016.
 */

public class NfcTask extends AsyncTask<Integer, Integer, String> {
    private Context appContext;
    private static final int port = 11003;
    private static final String ipAddr = "192.168.1.139";
    private InetAddress addr;
    private Socket socket;

    public NfcTask(Context context){
        appContext = context;
    }

    @Override
    protected String doInBackground(Integer... params) {
        Log.d("background", "start");

        int tagId = params[0];
        String androidId = Settings.Secure.getString(appContext.getContentResolver(),Settings.Secure.ANDROID_ID);

        Log.d("background Device id", androidId);

        // simulating 3 second delay
  /*      long startTime = System.currentTimeMillis();
        long currTime = System.currentTimeMillis();
        long len = 3000;
        while(currTime - startTime < len){
            currTime = System.currentTimeMillis();
        }*/

        // actual stuff
        openSocket();
        String toSend = getStringToSend(tagId, androidId);
        sendString(toSend);
        String result = readResult();
        Log.d("background", "finished");
        try {
            socket.close();
        } catch (Exception e){
            Log.d("background", "error" + e.toString());
        }
        if(socket!=null && socket.isClosed()){
            Log.d("background", "socket is closed");
        } else {
            Log.d("background", "socket problem");
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Intent resultIntent = new Intent(Constants.BROADCAST_ACTION_FROM_SERVICE);
        resultIntent.putExtra(Constants.EXTENDED_DATA_STATUS, result);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(resultIntent);
    }

/* ---------- Socket stuff ---------- */
    /*
    Returns xml string to be sent to server
    Input: params: Array of ints coding param values
        params[0]: tagId
     */
    private String getStringToSend(int tagId, String androidId){
        String result = "<ProtocolFormat>" +
                "<actionType>" + "binarySwitch" + "</actionType>" +
                "<actionValue>" + "0" + "</actionValue>" +
                "<clientID>" + androidId + "</clientID>" +
                "<deviceID>" + Integer.toString(tagId) + "</deviceID>" +
                "</ProtocolFormat>" + "<EOF>";
        return result;
    }

    /*
    Opens socket for read/write.
    Note that async tasks are run sequentially so we will not be interrupted by work
    for another tag
 */
    private void openSocket(){
        try{
            addr = InetAddress.getByName(ipAddr);
            socket = new Socket(addr, port);
        } catch (Exception e){
            Log.d("exceptionOpenSocket", e.toString());
        }
    }

    /*
    Sends toSend to server
     */
    private void sendString(String toSend){
        try {
            OutputStream outputStream= socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream, true);
            printWriter.println(toSend);

            printWriter.close(); // REMOVE THIS WHEN READING
            outputStream.close();
        }
        catch (Exception e){
            Log.d("exceptionWriteSocket", e.toString());
        }
    }

    /*
    Reads resulting message sent back from server
    Returns: Result as XML string.
     */
    private String readResult() {
        try{
            InputStream inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String result;
            // spin on buffer
            while(!bufferedReader.ready()){
                // do nothing
            }
            result = bufferedReader.readLine();
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
            return result;
        } catch(Exception e){
            Log.d("exceptionReadSocket", e.toString());
        }
        return "";
    }
}
