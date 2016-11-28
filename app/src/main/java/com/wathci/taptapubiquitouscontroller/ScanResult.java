package com.wathci.taptapubiquitouscontroller;

import java.io.Serializable;

/**
 * Created by Lisa on 11/23/2016.
 * The resulting info received from the server after sending over a scan
 */

public class ScanResult implements Serializable{
    public String actionType; // register, binary, getinfo etc
    public String friendlyName;
    public int state; // use constants from Constants class
    public int status; // success or failure from Constants class

    public ScanResult(String friendlyName, int state, int status){
        this.friendlyName = friendlyName;
        this.state = state;
        this.status = status;
    }

    public String toString() {
        String result = friendlyName + " " + Integer.toString(state) + " " +
                Integer.toString(status);
        return result;
    }
}
