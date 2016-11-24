package com.wathci.taptapubiquitouscontroller;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import static android.R.attr.type;
import static org.xmlpull.v1.XmlPullParser.TYPES;

/**
 * Created by Lisa on 11/18/2016.
 */

public class NfcTask extends AsyncTask<Integer, Integer, ScanResult> {
    private Context appContext;
    private static final int port = 11003;
    private static final String ipAddr = "192.168.1.139";
    private InetAddress addr;
    private Socket socket;
    public NfcTask(Context context){
        appContext = context;
    }

    @Override
    protected ScanResult doInBackground(Integer... params) {
        ScanResult result;
        Log.d("background", "start");

        // actual params
        int tagId = params[0];
        String androidId = Settings.Secure.getString(
                appContext.getContentResolver(),Settings.Secure.ANDROID_ID);

        // do the thing
        //result = actualScan(tagId, androidId);
        result = testScan(tagId, androidId);
        return result;
    }

    /*
    Opens socket, send data to server, receives data, returns result
     */
    private ScanResult actualScan(int tagId, String androidId){
        openSocket();
        String toSend = getStringToSend(tagId, androidId); // get xml string
        sendString(toSend); // send it
        ScanResult result = readResult(); // get result

        // close socket
        try {
            socket.close();
        } catch (Exception e){
            Log.d("background", "error" + e.toString());
        }

        // make sure it's really closed
        if(socket!=null && socket.isClosed()){
            Log.d("background", "socket is closed");
        } else {
            Log.d("background", "socket problem");
        }

        return result;
    }

    /*
    Testing sending and receiving when server is unavailable
     */
    private ScanResult testScan(int tagId, String androidId){

/*        // simulating 3 second delay
        long startTime = System.currentTimeMillis();
        long currTime = System.currentTimeMillis();
        long len = 3000;
        while(currTime - startTime < len){
            currTime = System.currentTimeMillis();
        }*/

        ScanResult result = readTestResult();
        Log.d("background", "finished");
        return result;
    }

    @Override
    protected void onPostExecute(ScanResult result) {
        super.onPostExecute(result);
        Intent resultIntent = new Intent(Constants.BROADCAST_ACTION_FROM_SERVICE);
        resultIntent.putExtra(Constants.EXTENDED_RESULT_FROM_SERVER, result);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(resultIntent);
    }

    /*
    Returns xml string to be sent to server
    Input: tagId is the id of the tag that was scanned
           androidId is the id of the device
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
    Reads resulting message sent back from server.
    Returns: ScanResult object containing resulting info
     */
    private ScanResult readResult(){
        ScanResult result = new ScanResult("read error", Constants.DEVICE_OFF, Constants.FAILURE);
        try{
            InputStream inputStream = socket.getInputStream(); // input stream from socket
            // Spin until stream is ready to be read
            while(inputStream.available() == 0){
            }

            // make xml parser
            XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser myParser = xmlPullParserFactory.newPullParser();
            myParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            myParser.setInput(inputStream, null);

            // read from parser
            result = getResult(myParser);
            inputStream.close();
        } catch (Exception e){
            Log.d("backgroundReadResult", e.toString());
        }
        return result;
    }

    /*
    Fakes functionality of readResult for testing purposes when server is unavailable.
    Creates an xml string to be read and a parser that reads it
    Returns: ScanResult object with resulting info
     */
    private ScanResult readTestResult(){
        ScanResult result = new ScanResult("read error", Constants.DEVICE_OFF, Constants.FAILURE);

        // string to be read
        String fakeInput = "<ReturnFormat>" +
                "<friendlyName>outlet</friendlyName>" +
                "<state>2</state>" +
                "<status>2</status>" +
                "</ReturnFormat>";

        try {
            // make string stream
            InputStream inputStream = new ByteArrayInputStream(fakeInput.getBytes());

            // make xml parser
            XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser myParser = xmlPullParserFactory.newPullParser();
            myParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false); // no namespace
            myParser.setInput(inputStream, null);

            // read from parser
            result = getResult(myParser);
            inputStream.close();
        } catch (Exception e){
            Log.d("backgroundFake", e.toString());
        }
        return result;
    }

    /*
     Input: XmlPullParser parser in proper format
     Reads from parser
     Output: Resulting ScanResult object
     */
    private ScanResult getResult(XmlPullParser parser) throws XmlPullParserException, IOException{
        String friendlyName = null;
        int state = -1;
        int status = -1;

        parser.nextTag(); // move past start of doc event
        parser.require(XmlPullParser.START_TAG, null, Constants.XML_START_TAG); // start correctly
        while(parser.next() != XmlPullParser.END_TAG){
            // parses one entry <name>info</name>

            // make sure we're at start tag
            int eventType = parser.getEventType();
            if(eventType != XmlPullParser.START_TAG){
                throw new XmlPullParserException( "expected "+
                        TYPES[ XmlPullParser.START_TAG ] + " got " + TYPES[eventType]);

            }

            String tagName = parser.getName();
            parser.next();
            switch(tagName){
                case "friendlyName":
                    friendlyName = parser.getText();
                    break;
                case "state":
                    state = Integer.parseInt(parser.getText());
                    break;
                case "status":
                    status = Integer.parseInt(parser.getText());
                    break;
                default:
                    throw new XmlPullParserException( "unexpected tag name: " + tagName);
            }

            parser.nextTag(); // advance to end tag
            // must be on end tag
            if(parser.getEventType() != XmlPullParser.END_TAG){
                throw new XmlPullParserException("expected " + TYPES[XmlPullParser.END_TAG] +
                " got " + TYPES[parser.getEventType()]);
            }
            // must have same name as start tag
            if(parser.getName() != tagName){
                throw new XmlPullParserException("expected end tag " + tagName +
                        "got end tag " + parser.getName());
            }
        }
        parser.require(XmlPullParser.END_TAG, null, Constants.XML_START_TAG); // correct end name
        ScanResult result = new ScanResult(friendlyName, state, status);
        return result;
    }
}
