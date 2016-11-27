package com.wathci.taptapubiquitouscontroller;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static android.R.attr.type;
import static android.R.id.message;
import static android.content.Context.SENSOR_SERVICE;
import static org.xmlpull.v1.XmlPullParser.TYPES;

/**
 * Created by Lisa on 11/18/2016.
 */

public class NfcTask extends AsyncTask<Integer, Integer, ScanResult> implements SensorEventListener{
    // sockets
    private Context appContext;
    private static final int port = 11003;
    private static final String ipAddr = "192.168.1.139";
    private InetAddress addr;
    private Socket socket;

    //encryption
    private byte[] key; // encryption key
    private byte[] iv; // iv

    // for accelerometer stuff
    private boolean overThreshold; // will be true if magnitude of accel data passes threshold
    private double threshold; // threshold for magnitude of accel data
    private long millisToWait; // time to wait to detect motion
    private long startTime; // start time of task
    Context myContext;
    SensorManager sm;
    Sensor accelerometer;

    public NfcTask(Context context, double threshold, long millisToWait){
        appContext = context;
        this.threshold = threshold;
        this.millisToWait = millisToWait;
        overThreshold = false;
        startTime = System.currentTimeMillis();

        // placeholder code for key
        for(int i = 0; i < key.length; i++){
            key[i] = (byte)i;
        }
        iv = new byte[Constants.IV_BYTES];
        Random random = new Random();
        random.nextBytes(iv);
    }

    /***********  DO IN BACKGROUND AND HELPERS***********/
    @Override
    protected ScanResult doInBackground(Integer... params) {
        setupAccel();
        return doSocket(params);
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
    private ScanResult doSocket(Integer[] params){

        // socket stuff
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
        waitForAccel();
        Log.d("overThreshold", Boolean.toString(overThreshold));
        String toSend = getStringToSend(tagId, androidId, overThreshold); // get xml string
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
    Spins until millisToWait time has passed since thread started to give user enough
    time to pick up phone and trigger overThreshold
     */
    private void waitForAccel(){
        long currentTime = System.currentTimeMillis();
        while(currentTime - startTime < millisToWait){
            currentTime = System.currentTimeMillis();
        }
    }

    /*
    Returns xml string to be sent to server
    Input: tagId is the id of the tag that was scanned
           androidId is the id of the device
           removedPhone is true if phone was removed from tag, false otherwise
     */
    private String getStringToSend(int tagId, String androidId, Boolean removedPhone){
        String actionType = getActionType(removedPhone);

        String result = "<ProtocolFormat>" +
                "<actionType>" + actionType + "</actionType>" +
                "<actionValue>" + "0" + "</actionValue>" +
                "<clientID>" + androidId + "</clientID>" +
                "<tagID>" + Integer.toString(tagId) + "</tagID>" +
                "</ProtocolFormat>" + "<EOF>";
        return result;
    }

    /*
    Returns actionType associated with the value of removedPhone
     */
    private String getActionType(Boolean removedPhone){
        if(removedPhone){
            return "binarySwitch";
        } else {
            return "getInfo";
        }
    }

    /*
    Sends toSend to server
     */
    private void sendString(String toSend){
        try {
            Cipher cipher = Cipher.getInstance(Constants.CIPHER_TRANSFORMATION);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, Constants.ENCRYPTION_ALGO);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            OutputStream outputStream= socket.getOutputStream();
            sendIv(outputStream); // send iv unencrypted
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            byte[] messageBytes = toSend.getBytes("UTF-8");
            cipherOutputStream.write(messageBytes);
            cipherOutputStream.flush();
            cipherOutputStream.close();
            outputStream.close();
        }
        catch (Exception e){
            Log.d("exceptionWriteSocket", e.toString());
        }
    }

    /*
    Sends iv to stream unencrypted
    MIGHT HAVE TO FORMAT THIS MESSAGE BETTER
     */
    public void sendIv(OutputStream outputStream){
        String ivString = "<iv>" + Arrays.toString(iv) + "</iv><EOF>";
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        printWriter.println(ivString);
        printWriter.close();
    }

    /*
    Reads resulting message sent back from server.
    Returns: ScanResult object containing resulting info
     */
    private ScanResult readResult(){
        ScanResult result = new ScanResult("read error", Constants.DEVICE_OFF, Constants.FAILURE);
        try{
            InputStream inputStream = socket.getInputStream();
            byte[] serverIv = new byte[Constants.IV_BYTES]; // iv server used to encrypt
            serverIv = getIv(inputStream);
            String readStr = readString(inputStream, serverIv); // Entire string sent. Should be Xml string
            inputStream.close();
            result = parseXml(readStr); // parse the Xml string readStr
        } catch (Exception e){
            Log.d("readResult", e.toString());
        }
        return result;
    }

    /*
    reads 16 bytes from server and returns them
     */
    private byte[] getIv(InputStream inputStream){
        byte[] iv = new byte[Constants.IV_BYTES];
        try{
            // reads into iv
            if(inputStream.read(iv) != Constants.IV_BYTES){
                throw(new Exception("incorrect iv length read"));
            }
        } catch(Exception e){
            Log.d("getIv", e.toString());
        }
        return iv;
    }

    /*
    Reads from inputStream until the ASCII end of transmission character is received
    Params: inputStream is stream to read from
            serverIv is the iv that was used to encode the message
    Returns resulting string
     */
    private String readString(InputStream inputStream, byte[] serverIv) throws IOException,
            NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException{
        Cipher cipher = Cipher.getInstance(Constants.CIPHER_TRANSFORMATION);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, Constants.ENCRYPTION_ALGO);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(serverIv);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
        StringBuilder readString = new StringBuilder();
        char nextByte = (char) inputStream.read();
        while (nextByte != (char) Constants.RECEIVE_EOF) {
            readString.append(nextByte);
            nextByte = (char) cipherInputStream.read();
        }
        Log.d("readTestResult", readString.toString());
        return readString.toString();
    }

    /*
    Parses the Xml string read string and returns the resulting ScanResult object
    Input: readString is a properly formatted Xml string
    Returns: Resulting ScanResult object

     */
    private ScanResult parseXml(String readString){
        ScanResult result = new ScanResult("read error", Constants.DEVICE_OFF, Constants.FAILURE);
        try{
            InputStream inputStream = new ByteArrayInputStream(readString.getBytes());

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

    @Override
    protected void onPostExecute(ScanResult result) {
        super.onPostExecute(result);
        sm.unregisterListener(this);
        Intent resultIntent = new Intent(Constants.BROADCAST_ACTION_FROM_SERVICE);
        resultIntent.putExtra(Constants.EXTENDED_RESULT_FROM_SERVER, result);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(resultIntent);
    }

    /********** TO TEST **********/

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
        waitForAccel();
        Log.d("overThreshold", Boolean.toString(overThreshold));
        ScanResult result = readTestResult();
        Log.d("background", "finished");
        return result;
    }

    private ScanResult readTestResult(){
        ScanResult result = new ScanResult("read error", Constants.DEVICE_OFF, Constants.FAILURE);
        // string to be read
        String fakeInput = "<ReturnFormat>" +
                "<friendlyName>outlet</friendlyName>" +
                "<state>2</state>" +
                "<status>2</status>" +
                "</ReturnFormat>" + (char)4;
        try{
/*            InputStream inputStream = new ByteArrayInputStream(fakeInput.getBytes());
            String readStr = readString(inputStream); // Entire string sent. Should be Xml string
            inputStream.close();*/
            result = parseXml(fakeInput.substring(0, fakeInput.length()-1));
        } catch (Exception e){
            Log.d("readResult", e.toString());
        }
        return result;
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
