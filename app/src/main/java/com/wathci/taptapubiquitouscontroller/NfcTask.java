package com.wathci.taptapubiquitouscontroller;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.text.LocaleDisplayNames;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import static android.R.attr.key;
import static android.R.attr.type;
import static android.R.id.message;
import static android.content.Context.SENSOR_SERVICE;
import static android.view.View.X;
import static org.xmlpull.v1.XmlPullParser.TYPES;

/**
 * Created by Lisa on 11/18/2016.
 */

public class NfcTask extends AsyncTask<Integer, String, ScanResult> implements SensorEventListener{
    // sockets
    private Context appContext;
    private static final int port = 8080;
    private static final String ipAddr = "192.168.1.139";
    private InetAddress addr;
    private SSLSocket socket;

    // for accelerometer stuff
    private boolean testAccel; // true when acceleration should be tested, false otherwise
    private boolean overThreshold; // will be true if magnitude of accel data passes threshold
    private double threshold; // threshold for magnitude of accel data
    private long millisToWait; // time to wait to detect motion
    private long startTime; // start time of task
    Context myContext;
    SensorManager sm;
    Sensor accelerometer;

    // message type
    private int messageType; // needs to be global in class because needed by onPostExecute

    // For registration message
    AlertDialog alertDialog;

    // result
    ScanResult result;

    /*
        Params: threshold is
     */
    public NfcTask(Context context, boolean testAccel, String actionType){
        appContext = context;
        this.testAccel = testAccel;
        this.threshold = Constants.ACCEL_TRHESHOLD;
        this.millisToWait = Constants.MILLIS_TO_WAIT;
        overThreshold = false;
        startTime = System.currentTimeMillis();
        result = new ScanResult(actionType, "read error", Constants.DEVICE_OFF,
                Constants.FAILURE, 0);
    }

    /***********  DO IN BACKGROUND AND HELPERS***********/
    @Override
    protected ScanResult doInBackground(Integer... params) {
        messageType = params[1];
        int tagId = params[0];
        result.tagId = tagId;
        if(testAccel) {
            setupAccel();
        }
        return doSocket(tagId);
    }

    /*
    Sets up accelerometer stuff
     */
    private void setupAccel(){
        sm = (SensorManager)appContext.getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /*
    Opens socket, sends data, receives data, returns result.
     */
    private ScanResult doSocket(int tagId){

        // socket stuff
        Log.d("background", "start");

        // get androidId
        String androidId = Settings.Secure.getString(
                appContext.getContentResolver(),Settings.Secure.ANDROID_ID);

        // do the thing
        if(Constants.IN_SERVER_MODE) {
            actualScan(tagId, androidId);
        } else {
            testScan(tagId, androidId);
        }
        return result;
    }

    /*
    Opens socket, send data to server, receives data, returns result
     */
    private void actualScan(int tagId, String androidId){
        openSocket();

        if(testAccel) {
            waitForAccel();
        }
        Log.d("overThreshold", Boolean.toString(overThreshold));
        String toSend = getStringToSend(tagId, androidId, overThreshold); // get xml string
        sendString(toSend); // send it
        readResult(); // get result

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
    }

    /*
    Opens an SSL socket for read/write.
    Note that async tasks are run sequentially so we will not be interrupted by work
    for another tag
    */
    private void openSocket(){
        try{
            SSLContext sslContext = getSSLContext();
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            addr = InetAddress.getByName(ipAddr);
            socket = (SSLSocket)socketFactory.createSocket(addr, port);
        } catch (Exception e){
            Log.d("exceptionOpenSocket", e.toString());
        }
    }

    /*
    Returns SSLContext that trusts server's certificate
     */
    private SSLContext getSSLContext() throws CertificateException, IOException, KeyStoreException,
    NoSuchAlgorithmException, KeyManagementException{

        // Load CAs from an InputStream
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = appContext.getResources().openRawResource(R.raw.certificate);
        Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
        } finally {
            caInput.close();
        }

        // Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Create an SSLContext that uses our TrustManager
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(null, tmf.getTrustManagers(), null);

        return context;

    }

    /*
    Spins until millisToWait time has passed since thread started to give user enough
    time to pick up phone and trigger overThreshold
     */
    private void waitForAccel(){
        long currentTime = System.currentTimeMillis();
        while (currentTime - startTime < millisToWait) {
            currentTime = System.currentTimeMillis();
        }
        sm.unregisterListener(this);
    }

    /*
    Returns xml string to be sent to server
    Input: tagId is the id of the tag that was scanned
           androidId is the id of the device
           removedPhone is true if phone was removed from tag, false otherwise
     */
    private String getStringToSend(int tagId, String androidId, Boolean removedPhone){
        String actionType = getActionType(removedPhone);
        String actionValue = getActionValue();
        notifyUser(actionValue);

        String result = "<ProtoTapTap>" +
                "<actionType>" + actionType + "</actionType>" +
                "<actionValue>" + actionValue + "</actionValue>" +
                "<clientID>" + androidId + "</clientID>" +
                "<tagID>" + Integer.toString(tagId) + "</tagID>" +
                "</ProtoTapTap>" + "<EOF>";
        return result;
    }

    /*
    Returns actionType associated with the value of removedPhone and messageType
     */
    private String getActionType(Boolean removedPhone){
        String actionType = result.actionType; // will be this if doesn't get replaced
        if(testAccel) {
            // REGISTRATION MESSAGE
            if (messageType == Constants.REGISTRATION_MESSAGE) {
                actionType = Constants.ADD_DEVICE;
            }

            // ACTIVITY MESSAGE
            else if (messageType == Constants.ACTIVITY_MESSAGE) {
                if (removedPhone) {
                    actionType = Constants.BINARY_SWITCH;
                } else {
                    actionType = Constants.GET_INFO;
                }
            }
            Log.d("actionType", actionType);
            result.actionType = actionType;
        }
        return actionType;
    }

    /*
    returns action value
    Returns: for now returns password if messageType is REGISTRATION_MESSAGE
            Otherwise, returns empty string
     */
    private String getActionValue(){
        if(messageType == Constants.REGISTRATION_MESSAGE){
            String allowed_chars ="0123456789abcdefghijklmnopqrstuvwxyz" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"; // chars allowed in password
            int passwordLen = 8;
            StringBuilder password = new StringBuilder();
            Random random = new Random(); // random number generator
            for(int i = 0; i < passwordLen; i++){
                int randomIndex = random.nextInt(allowed_chars.length()); // char index
                password.append(allowed_chars.charAt(randomIndex));
            }
            return password.toString();
        }
        return "";
    }

    /*
        ACTION: Calls onProgressUpdate to display message to user about entering password
            on server if the message is a REGISTRATION_MESSAGE
     */
    void notifyUser(String password){
        Log.d("notifyUser", "called notify");
        if(messageType == Constants.REGISTRATION_MESSAGE){
            Log.d("notifyUser", "registration message");
            String[] passwordArr = {password};
            publishProgress(passwordArr);
        }
    }

    /*
    Invoked on UI thread after call to publishProgress.
    In this makes popup telling user to enter password on server
     */
    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Log.d("On progress update", "here");
        String password = values[0];
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                new ContextThemeWrapper(appContext, R.style.RegDialogueTheme));
        String alertMessage = "Enter: " + password + "\nin dailogue box on server's screen.\n"
                + "Press ok on server's screen when finished.";
        alertBuilder.setMessage(alertMessage).setCancelable(false);
        alertDialog = alertBuilder.create();
        alertDialog.setTitle("Registration");
        alertDialog.show();
    }

    /*
        Sends toSend to server
         */
    private void sendString(String toSend){
        try {
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream, true);
            printWriter.println(toSend);
            printWriter.close();
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
    private void readResult(){
        Log.d("readResult", "in read result");
        try{
            InputStream inputStream = socket.getInputStream();
            Log.d("readResult", "opened input stream");
            String readStr = readString(inputStream); // Entire string sent. Should be Xml string
            Log.d("rawXmlStr", readStr);
            inputStream.close();
            parseXml(readStr); // parse the Xml string readStr
        } catch (Exception e){
            result.actionType = "readError";
            Log.d("readResult", e.toString());
        }
    }

    /*
    Reads from inputStream until the ASCII end of transmission character is received
    Params: inputStream is stream to read from
    Returns resulting string
     */
    private String readString(InputStream inputStream) throws IOException{
        StringBuilder readString = new StringBuilder();
        char nextByte = (char) inputStream.read();
        while (nextByte != (char)-1) {
            readString.append(nextByte);
            nextByte = (char) inputStream.read();
        }
        return readString.toString();
    }

    /*
    Parses the Xml string read string and returns the resulting ScanResult object
    Input: readString is a properly formatted Xml string
    Returns: Resulting ScanResult object

     */
    private void parseXml(String readString){
        try{
            InputStream inputStream = new ByteArrayInputStream(readString.getBytes());

            // make xml parser
            XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser myParser = xmlPullParserFactory.newPullParser();
            myParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            myParser.setInput(inputStream, null);

            // read from parser
            getResult(myParser);
            inputStream.close();
        } catch (Exception e){
            Log.d("backgroundReadResult", e.toString());
            result.actionType = "parseError";
        }
    }

    /*
     Input: XmlPullParser parser in proper format
     Reads from parser
     Output: Resulting ScanResult object
     */
    private void getResult(XmlPullParser parser) throws XmlPullParserException, IOException{
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
                    result.friendlyName = parser.getText();
                    break;
                case "state":
                    result.state = Integer.parseInt(parser.getText());
                    break;
                case "status":
                    result.status = Integer.parseInt(parser.getText());
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
    }

    @Override
    protected void onPostExecute(ScanResult result) {
        super.onPostExecute(result);
        switch(messageType){
            case Constants.REGISTRATION_MESSAGE:
                returnFromRegistration(result);
                break;
            case Constants.ACTIVITY_MESSAGE:
                returnFromActivity(result);
                break;
        }
    }

    /*
    Handles onPostExecute for REGISTRATION messages.
    Closes the alert Dialogue and (locally) broadcasts a RETURN_REGISTRATION_FROM_SERVICE message
     */
    protected void returnFromRegistration(ScanResult result){
        alertDialog.cancel();
        Intent resultIntent = new Intent(Constants.RETURN_REGISTRATION_FROM_SERVICE);
        resultIntent.putExtra(Constants.EXTENDED_RESULT_FROM_SERVER, result);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(resultIntent);
    }

    /*
    Handles onPostExecute for ACTIVITY messages
    (locally) Broadcasts a  RETURN_SCAN_FROM_SERVICE message
     */
    protected void returnFromActivity(ScanResult result){
        Log.d("returnFromActivity", "called");
        Intent resultIntent = new Intent(Constants.RETURN_SCAN_FROM_SERVICE);
        resultIntent.putExtra(Constants.EXTENDED_RESULT_FROM_SERVER, result);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(resultIntent);
    }

    /********** TO TEST **********/

        /*
    Testing sending and receiving when server is unavailable
     */
    private void testScan(int tagId, String androidId){
        if(testAccel) {
            waitForAccel();
        }
        Log.d("overThreshold", Boolean.toString(overThreshold));
        String toSend = getStringToSend(tagId, androidId, overThreshold);
        if(messageType == Constants.REGISTRATION_MESSAGE) {
            // simulating delay
            long startTime = System.currentTimeMillis();
            long currTime = System.currentTimeMillis();
            long len = 5000;
            while(currTime - startTime < len){
                currTime = System.currentTimeMillis();
            }
        }
        readTestResult(tagId, overThreshold);
        Log.d("background", "finished");
    }

    private void readTestResult(int tagId, boolean overThreshold){
        String friendlyName = "";
        if(messageType == Constants.REGISTRATION_MESSAGE){
            friendlyName = "Registered";
        } else {
            if(tagId == 1){
                friendlyName = "Outlet";
            } else if(tagId == 2){
                friendlyName = "Arduino";
            }
        }

        int status = 1;
        if(messageType == Constants.REGISTRATION_MESSAGE){
            Random random = new Random();
            status = random.nextInt(2); // will return 0 or 1
        }

        // string to be read
        String fakeInput = "<ReturnFormat>" +
                "<friendlyName>" + friendlyName + "</friendlyName>" +
                "<state>1</state>" +
                "<status>" + Integer.toString(status) + "</status>" +
                "</ReturnFormat>";
        try{
            InputStream inputStream = new ByteArrayInputStream(fakeInput.getBytes());
            String readStr = readString(inputStream); // Entire string sent. Should be Xml string
            inputStream.close();
            parseXml(readStr);
        } catch (Exception e){
            Log.d("readResult", e.toString());
        }
    }

    /********** ACCELEROMETER **********/

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        double scalar = Math.sqrt(x*x + y*y + z*z);
        Log.d("scalar", Double.toString(scalar));
        if(scalar > threshold){
            overThreshold = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
