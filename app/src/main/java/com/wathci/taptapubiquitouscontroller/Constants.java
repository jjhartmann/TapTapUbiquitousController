package com.wathci.taptapubiquitouscontroller;

/**
 * Created by Lisa on 11/17/2016.
 */

public class Constants {

    /*
    Testing
     */
    // true when actually connecting to server
    public static final boolean IN_SERVER_MODE = false;
    // true when testing, false when not. Use for logs.
    public static final boolean IN_TEST_MODE = true;

    /*
    Service to activity
     */
    public static final String BROADCAST_ACTION_FROM_SERVICE =
            "com.wathci.taptapubiquitouscontroller.BROADCAST_FROM_SERVICE";
    public static final String EXTENDED_RESULT_FROM_SERVER =
            "com.wathci.taptapubiquitouscontroller.RESULT_FROM_SERVER";

    /*
    Accelerometer
     */
    // threshold for magnitude of acceleration
    public static final double ACCEL_TRHESHOLD = 0.75;
    // time to wait from scanning tag for movement
    public static final long MILLIS_TO_WAIT = 1000;

    /*
    Device state
     */
    public static final int DEVICE_ON = 1;
    public static final int DEVICE_OFF = 0;

    /*
    Device status
     */
    public static final int SUCCESS = 1;
    public static final int FAILURE = 0;

    /*
    XML
     */
    public static final String XML_START_TAG = "ReturnFormat";
    public static final byte RECEIVE_EOF = 4; // using this as EOF when receiving

    /*
    Encryption
     */
    public static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"; // AES 128
    public static final String ENCRYPTION_ALGO = "AES";
    public static final int IV_BYTES = 16;

    /*
    Different messages
     */
    public static final int NO_TAG_ID = 0;
    // chars allowed in registration password

    // MESAGE TYPES
    public static final int ACTIVITY_MESSAGE = 0; // scanned a tag
    public static final int REGISTRATION_MESSAGE = 1; // registering device
}
