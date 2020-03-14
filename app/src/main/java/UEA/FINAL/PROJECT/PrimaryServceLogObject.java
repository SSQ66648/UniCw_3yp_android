/*------------------------------------------------------------------------------
 *
 * TITLE:       3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        PrimaryServceLogObject.Java
 *
 * LAYOUT(S):   n/a
 *
 * DESCRIPTION: class to contain (& de-clutter) the timestamps required for debugging/log file in
 *              GpsForegroundService.
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v1.0    200302  Initial implementation.
 *              v1.1    200302  added lat/lon/name/speed/index of array to variables & get/set.
 *              v1.2    200302  added lat/lon/accuracy/radiusTotal/provider to log object to ease log to file method
 *              v1.3    200309  added check for null variables method, changed primitives to object for null check.
 *              v1.4    200314  copied from test project.
 *
 * NOTES:       
 *          +   separate method to calculate total time may not be needed: if use subtraction in
 *              accessor, method is made obsolete.
 *
 *              date notation: YYMMDD
 *              comment format:
 *                  //---GROUP---
 *                  //-purpose of method etc-
 *                  //explanation
 *
 *------------------------------------------------------------------------------
 * TO DO LIST:  
 *              todo:
 *              todo:   tidy code
 *
 * TO DO TODAY:
 *              TODO:
 *
 -----------------------------------------------------------------------------*/
package UEA.FINAL.PROJECT;

import android.util.Log;

public class PrimaryServceLogObject {
    /*--------------------------------------
        CONSTANTS
    --------------------------------------*/
    private static final String TAG = PrimaryServceLogObject.class.getSimpleName();


    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    //time of lat lon fix by provider
    private Long locationFixTime;
    //time of lat/lon USE by API code
    private Long locationUseTime;
    //time of api query sent to API
    private Long querySentTime;
    //time of api query response received
    private Long queryResponseTime;
    //time of parse completion
    private Long parseCompleteTime;
    //time of updated currentSpeedLimit from parsed values
    private Long limitUpdateTime;
    //total time taken to perform 'primary-loop'(updated speedLimit - lat/lon fix USE time)
    private Long totalTimeRequired;

    private String roadName;
    private Integer maxSpeed;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Integer radiusTotal;
    private String provider;

    //TODO: still to add to service logging procedure
    private int roadArrayIndex;

    //debugging log items (
    // might not use log object for low memory)
    private String lowMemoryWarning = "LOW MEMORY LISTENER HAS TRIGGERED: " +
            "could be cause of process death?";


    /*--------------------------------------
        CONSTRUCTOR
    --------------------------------------*/
    public PrimaryServceLogObject(double latitude, double longitude, double accuracy, long locationFixTime, long locationUseTime) {
        //may only need empty constructor for object (set variables as proceed)
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.locationFixTime = locationFixTime;
        this.locationUseTime = locationUseTime;
    }


    /*--------------------------------------
        ACCESSORS
    --------------------------------------*/
    public long getLocationFixTime() {
        return locationFixTime;
    }

    public long getLocationUseTime() {
        return locationUseTime;
    }

    public long getQuerySentTime() {
        return querySentTime;
    }

    public long getQueryResponseTime() {
        return queryResponseTime;
    }

    public long getParseCompleteTime() {
        return parseCompleteTime;
    }

    public long getLimitUpdateTime() {
        return limitUpdateTime;
    }

    public long getTotalTimeRequired() {
        //if not already manually called:
        totalTimeRequired = (limitUpdateTime - locationUseTime);
        return totalTimeRequired;
    }

    public String getRoadName() {
        return roadName;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getRoadArrayIndex() {
        return roadArrayIndex;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public int getRadiusTotal() {
        return radiusTotal;
    }

    public String getProvider() {
        return provider;
    }

    /*--------------------------------------
            MUTATORS
        --------------------------------------*/
    public void setLocationFixTime(long locationFixTime) {
        this.locationFixTime = locationFixTime;
    }

    public void setLocationUseTime(long locationUseTime) {
        this.locationUseTime = locationUseTime;
    }

    public void setQuerySentTime(long querySentTime) {
        this.querySentTime = querySentTime;
    }

    public void setQueryResponseTime(long queryResponseTime) {
        this.queryResponseTime = queryResponseTime;
    }

    public void setParseCompleteTime(long parseCompleteTime) {
        this.parseCompleteTime = parseCompleteTime;
    }

    public void setLimitUpdateTime(long limitUpdateTime) {
        this.limitUpdateTime = limitUpdateTime;
    }

    public void setTotalTimeRequired(long totalTimeRequired) {
        this.totalTimeRequired = totalTimeRequired;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setRoadArrayIndex(int roadArrayIndex) {
        this.roadArrayIndex = roadArrayIndex;
    }

    public void setRadiusTotal(int radiusTotal) {
        this.radiusTotal = radiusTotal;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    /*--------------------------------------
                    METHODS
                --------------------------------------*/
    //-separate method for this variable in case access to it after creation is required
    public long calculateDuration() {
        totalTimeRequired = (limitUpdateTime - locationUseTime);
        return totalTimeRequired;
    }


    //-check that all relevant variables are populated (prevent null crashing on logging)
    public boolean checkIterationComplete() {
        if (latitude == null | longitude == null | accuracy == null | provider == null | radiusTotal == null | roadName == null | maxSpeed == null | locationFixTime == null | locationUseTime == null | querySentTime == null | queryResponseTime == null | parseCompleteTime == null | limitUpdateTime == null) {
            Log.e(TAG, "checkIterationComplete: Error: required loggging variable is NULL:");
            //todo: add handling here or where used (currently on use)
            return false;
        } else {
            Log.d(TAG, "checkIterationComplete: all variables accounted for:");
            return true;
        }
    }
}
