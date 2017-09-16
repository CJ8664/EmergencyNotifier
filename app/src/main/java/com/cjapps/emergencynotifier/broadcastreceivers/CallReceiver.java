package com.cjapps.emergencynotifier.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.cjapps.emergencynotifier.database.DatabaseManager;
import com.cjapps.emergencynotifier.utility.Constants;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * This class extends {@link android.content.BroadcastReceiver} to handle the main functionality
 * of the application
 */
public class CallReceiver extends BroadcastReceiver {

    private AudioManager audioManager ;

    private static int initialRingerMode;          // Static to save the state when call is hung up
    private static boolean ringerModeChanged;

    private DatabaseManager dbManager;

    private ArrayList<String> timeIds = new ArrayList<String>();

    /**
     * The default constructor
     */
    public CallReceiver() {
    }

    /**
     * This method is responsible to handle the broadcast
     *
     * @param context The context in which the broadcast was created
     * @param intent The intent of the received broadcast
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        // Initialize the database object
        dbManager = new DatabaseManager(context);

        // Initialize audio manager
        audioManager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        if (state.equals(Constants.INCOMING_CALL)){
            boolean isAnEmergencyNumber = checkIsAnEmergencyNumber(incomingNumber);

            if(isAnEmergencyNumber) {

                //Log.i("Is stored as EMERGENCY contact", incomingNumber);
                // timeId value will be set in {@link checkIsAnEmergencyNumber} method.
                boolean isInTimeFilter = checkIsCurrentTimeIsInFilter(timeIds);

                if(isInTimeFilter) {
                    // Change the Sound Profile to Ringer.
                    //Log.i("This is an 'EMERGENCY' call from",incomingNumber);
                    changeSoundProfileToRinger();
                    Toast.makeText(context,"EMERGENCY",Toast.LENGTH_LONG).show();
                }
            }
        } else if (Constants.CALL_ENDED.contains(state)) {
            changeSoundProfileToInitial();
            //Log.i("Changing profile","initial mode");
        }
    }

    /**
     * This function checks whether the phone is on Silent Mode
     * @return true if on silent mode else false.
     */
    private boolean checkIfOnSilentMode(){

        initialRingerMode = audioManager.getRingerMode();
        //Log.i("Initial ringer mode",initialRingerMode+"");

        return initialRingerMode == AudioManager.RINGER_MODE_SILENT ||
                initialRingerMode == AudioManager.RINGER_MODE_VIBRATE;
    }

    /**
     * This function changes the Sound profile to Ringer if it is on Silent mode.
     */
    private void changeSoundProfileToRinger(){

        boolean isOnSilentMode = checkIfOnSilentMode();
        if (isOnSilentMode){
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            //Log.i("Changed profile","ringer mode");
            ringerModeChanged = true;
        }
    }

    /**
     * This function resets the Sound Profile to the profile that was before operation
     */
    private void changeSoundProfileToInitial(){
        if (ringerModeChanged) {
            audioManager.setRingerMode(initialRingerMode);
            //Log.i("Changed profile", " to initial mode");
            ringerModeChanged = false; // reset the flag
        }
    }

    /**
     * This function checks whether the phone number is present in the database.
     * @param incomingNumber This is the unprocessed phone number.
     * @return true if the number exists else false.
     */
    private boolean checkIsAnEmergencyNumber(String incomingNumber) {

        incomingNumber = incomingNumber.replace("-","");
        incomingNumber = incomingNumber.replace(" ","");

        int phoneNumberLength = incomingNumber.length();

        if (phoneNumberLength > 10) {
            incomingNumber = incomingNumber.substring(phoneNumberLength - 10, phoneNumberLength);
        }

        //Log.d("Incoming Number",incomingNumber);

        // Store timeId corresponding to that phone number in a string, this will be used to
        // retrieve time filters.

        timeIds = dbManager.getEmergencyNumberTimeIds(incomingNumber);

        return timeIds != null;
    }

    private boolean checkIsCurrentTimeIsInFilter(ArrayList<String> timeIds){

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String currentTime = sdf.format(new Date());

        Time systemTime = Time.valueOf(currentTime);
        //Log.i("SYSTEM_TIME",systemTime.toString());

        return dbManager.checkTimeFilters(timeIds, systemTime);
    }
}
