package com.wathci.taptapubiquitouscontroller;

import java.io.Serializable;

/**
 * Created by Lisa on 11/23/2016.
 * The resulting info received from the server after sending over a scan
 */

public class ScanResult implements Serializable{
    public int tagId; // needed to for resending info
    public String actionType; // register, binary, getinfo etc
    public String friendlyName;
    public int state; // use constants from Constants class
    public int status; // success or failure from Constants class

    public ScanResult(String actionType, String friendlyName, int state, int status, int tagId){
        this.actionType = actionType;
        this.friendlyName = friendlyName;
        this.state = state;
        this.status = status;
        this.tagId = tagId;
    }

    public String toString() {
        String result = actionType + " " + friendlyName + " " + Integer.toString(state) + " " +
                Integer.toString(status);
        return result;
    }

    /*
    Returns true if same actionType, friendlyName, and status
    State can be different.
     */
    public boolean equals(ScanResult result){
        return(this.actionType.equals(result.actionType) &&
        this.friendlyName.equals(result.friendlyName) &&
        this.status == result.status);
    }

    /*
    Returns status as a string
     */
    public String getStatusString(){
        switch(status){
            case Constants.SUCCESS:
                return "Success";
            case Constants.FAILURE:
                return "Failure";
        }
        return "Error";
    }

    /*
    Returns state as a string
     */
    public String getStateString(){
        switch(state){
            case Constants.DEVICE_OFF:
                return "Off";
            case Constants.DEVICE_ON:
                return "On";
        }
        return "Error";
    }

    /*
    Returns the text that should be displayed on the button by the ScanResultsManager
     */
    public String getButtonText(){
        switch(actionType){
            case Constants.BINARY_SWITCH:
                return "Toggle";
            case Constants.GET_INFO:
                return "Update";
        }
        return "Error";
    }

    public String getActionString(){
        switch(actionType){
            case Constants.BINARY_SWITCH:
                return "Toggle";
            case Constants.GET_INFO:
                return "Information";
        }
        return "Error";
    }
}
