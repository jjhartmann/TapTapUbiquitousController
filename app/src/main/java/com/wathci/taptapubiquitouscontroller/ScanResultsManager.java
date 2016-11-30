package com.wathci.taptapubiquitouscontroller;

/**
 * Created by Lisa on 11/28/2016.
 * Manages scan results that are displayed in scan acitivity
 * It's a modified stack - new elems go on the top and the last elem to be accessed is removed
 * If there are too many
 * If trying to add an elem that's already there, instead of adding it to the end, the old one is
 * moved to the top of the stack with the state and status updated to the new result
 */

public class ScanResultsManager {
    private static int currIndex = 0; // current index
    private static int size = 0; // number of elems actually on stack
    private static int maxSize = 3; // max number of elems allowed
    private static ScanResult[] resultArr = new ScanResult[maxSize];

    /*
    Adds newElem to end of array.
    Should only be called if newElem is not already in the array
     */
    private static void push(ScanResult newElem){
        // just append to end
        if(size < maxSize){
            resultArr[size] = newElem;
            size++;
        }
        // remove oldest thing
        else {
            for(int i = 0; i < size - 1; i++){
                resultArr[i] = resultArr[i+1];
            }
            resultArr[size-1] = newElem;
        }
    }

    /*
    If newElem is already in the array, returns its index
    Otherwise returns -1
     */
    private static int getElemIndex(ScanResult newElem){
        int index = -1;
        for(int i = 0; i < size; i++){
            ScanResult currElem = resultArr[i];
            if (currElem.equals(newElem)){
                index = i;
                break;
            }
        }
        return index;
    }

    /*
    updates state and status of elem at oldIndex to that of newElem
     */
    private static void updateElem(ScanResult newElem, int oldIndex){
        ScanResult actualElem = resultArr[oldIndex];
        actualElem.state = newElem.state;
        actualElem.status = newElem.status;
    }

    // moves element at elemIndex to top of resultArr
    private static void moveToTop(int elemIndex){
        ScanResult currElem = resultArr[elemIndex];
        for(int i = elemIndex; i < size - 1; i++){
            resultArr[i] = resultArr[i+1];
        }
        resultArr[size-1] = currElem;
    }

    // public method to add elem to end of arr
    public static void addElem(ScanResult newElem){
        int elemIndex = getElemIndex(newElem); // getElemIndex returns -1 if newElem not in array
        if(elemIndex!=-1){
            updateElem(newElem, elemIndex);
            moveToTop(elemIndex);
        } else {
            push(newElem);
        }
        currIndex = size-1;

    }

    /*
    Returns number of elems in array
     */
    public static int getSize(){
        return size;
    }

    /*
    Returns element at current index
     */
    public static ScanResult getCurrElem(){
        return resultArr[currIndex];
    }

    public static int getCurrIndex(){
        return currIndex;
    }

    /*
    If possible, increase index
     */
    public static void increment(){
        if(currIndex < size-1){
            currIndex++;
        }
    }

    /*
    If possible, decrease index
     */
    public static void decrement(){
        if(currIndex > 0){
            currIndex--;
        }
    }
}
