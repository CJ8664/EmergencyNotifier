package com.cjapps.emergencynotifier.utility;

import android.telephony.TelephonyManager;

/**
 * This class contains all the Constant values and strings
 */
public class Constants {

    public static final String INCOMING_CALL = TelephonyManager.EXTRA_STATE_RINGING;

    public static final String CALL_ENDED = TelephonyManager.EXTRA_STATE_OFFHOOK+":"
            +TelephonyManager.EXTRA_STATE_IDLE;

    // Constants to identify whether the object is a section header or contact
    public static final int CONTACT = 0;
    public static final int SECTION = 1;

    // Constants to identify whether the dialog is a start time or end time dialog
    public static final int START_TIME_DIALOG = 0;
    public static final int END_TIME_DIALOG = 1;

    // Constants to distinguish activityResult
    public static final int PICK_CONTACT = 1;
}
