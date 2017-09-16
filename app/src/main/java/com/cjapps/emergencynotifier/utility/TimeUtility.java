package com.cjapps.emergencynotifier.utility;


import android.content.Context;
import android.widget.Toast;

import com.cjapps.emergencynotifier.R;

public class TimeUtility {

    private Context context;

    public TimeUtility(Context context){
        this.context = context;
    }

    public boolean isValidTimeFilter (String startTimeForDatabase, String endTimeForDatabase ) {

        if (startTimeForDatabase.equals("")) {
            Toast.makeText(context, context.getString(R.string.invalid_start_time), Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
        if (endTimeForDatabase.equals("")) {
            Toast.makeText(context, context.getString(R.string.invalid_end_time),Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        return true;
    }

    public String formatTime(int hour, int minute, int second, boolean is24HourFormat){

        String hourString;
        String minuteString;
        String secondString;

        String AM_PM = "";

        if (!is24HourFormat) {

            if ( hour == 0 ){               // 12 midnight
                hour = 12;
                AM_PM = " AM";
            } else if ( hour < 12){         // Late night or early morning time
                AM_PM = " AM";
            } else if ( hour == 12) {       // 12 afternoon
                hour = 12;
                AM_PM = " PM";
            } else if(hour > 12){           // Post Afternoon subtract 12 from hours
                hour -= 12;
                AM_PM = " PM";
            }
        }

        if (hour < 10) {
            hourString = "0" + hour;
        } else {
            hourString = "" + hour;
        }

        if ( minute < 10 ){
            minuteString = "0" + minute;
        } else {
            minuteString = "" + minute;
        }

        if ( second < 10 ){
            secondString = "0" + second;
        } else {
            secondString = "" + second;
        }

        if(is24HourFormat){
            return hourString + ":" + minuteString + ":" + secondString;
        } else {
            return hourString + ":" + minuteString + ":" + secondString + AM_PM;
        }
    }
}
