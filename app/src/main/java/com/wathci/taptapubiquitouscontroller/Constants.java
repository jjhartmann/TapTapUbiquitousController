package com.wathci.taptapubiquitouscontroller;

/**
 * Created by Lisa on 11/17/2016.
 */

public class Constants {

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
    public static final int RECEIVE_EOF = 4; // using this as EOF when receiving
}
