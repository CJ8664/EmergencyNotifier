package com.cjapps.emergencynotifier.activities;

import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.cjapps.emergencynotifier.R;
import com.cjapps.emergencynotifier.database.DatabaseManager;
import com.cjapps.emergencynotifier.utility.Constants;
import com.cjapps.emergencynotifier.utility.TimeUtility;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

public class SelectTime extends ActionBarActivity {

    private String startTimeForDatabase = "00:00:00";
    private String endTimeForDatabase = "23:59:59";

    private String startTime = "Any Time";
    private String endTime = "Any Time";

    private ArrayList<String> phoneArrayList;
    private ArrayList<String> nameArrayList;
    private ArrayList<String> photoUriArrayList;

    private TextView startTimeTextView;
    private TextView endTimeTextView;

    private DatabaseManager databaseManager;

    private Context context;

    private int backPressAttempt = 0;

    private TimeUtility timeUtility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_time);

        context = this;

        databaseManager = new DatabaseManager(context);

        timeUtility = new TimeUtility(context);

        RelativeLayout contactCard = (RelativeLayout)findViewById(R.id.select_time_contact_card);
        final RelativeLayout startTimeCard = (RelativeLayout)findViewById(R.id.select_time_start_time_card);
        final RelativeLayout endTimeCard = (RelativeLayout)findViewById(R.id.select_time_end_time_card);

        Bundle contactData = getIntent().getExtras();

        if(contactData != null){            // Contact info is passed

            // ------------- Multiple Contact info is passed ------------------//

            if (contactData.containsKey("selectedContactsIds")){

                phoneArrayList = new ArrayList<String>();
                nameArrayList = new ArrayList<String>();
                photoUriArrayList = new ArrayList<String>();

                contactCard.setVisibility(View.GONE);

                String contactIds = contactData.getStringArrayList("selectedContactsIds").toString();
                contactIds = contactIds.replace("[","'");
                contactIds = contactIds.replace("]","'");
                contactIds = contactIds.replace(", ","','");

                //---------------------START OF LOADING CONTACTS FROM PHONE-------------------------//


                ContentResolver contentResolver = getContentResolver();

                Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                        ,new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER
                                ,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                                ,ContactsContract.CommonDataKinds.Phone.CONTACT_ID}
                        ,ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" in ("+contactIds+")"
                        , null, null
                );


                if (phoneCursor!=null){

                    int currCount = phoneCursor.getCount();
                    if (currCount > 0){

                        Uri photoUri;

                        while(phoneCursor.moveToNext()){

                            String phone = phoneCursor.getString(0);

                            if (!phone.contains("@")) {

                                phone = phone.replace("-", "");
                                phone = phone.replace(" ", "");

                                if (!phoneArrayList.contains(phone)) {
                                    phoneArrayList.add(phone);
                                    nameArrayList.add(phoneCursor.getString(1)); // get the name and add it to the list

                                    photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI
                                            ,Long.parseLong(phoneCursor.getString(2)));

                                    photoUriArrayList.add(photoUri.toString());
                                }
                            }
                        }

                        phoneCursor.close();
                        Log.i("Selected Contact info",phoneArrayList.toString() + nameArrayList.toString());
                    }
                }

                //---------------------END OF LOADING CONTACTS FROM PHONE-------------------------//

            } else { // Single contact info passed show contactCard

                phoneArrayList = contactData.getStringArrayList("phoneArrayList");
                nameArrayList = contactData.getStringArrayList("nameArrayList");
                photoUriArrayList = new ArrayList<String>();

                String name = nameArrayList.get(0);

                // Set name from the passed contact info
                TextView nameTextView = (TextView) findViewById(R.id.select_time_contact_name);
                nameTextView.setText(name);

                ImageView contactDpImageView = (ImageView) findViewById(R.id.select_time_contact_dp);
                // Get Image from the Uri of the contact
                String photoUri = contactData.getString("photoUri");

                for (String ignored : phoneArrayList) {
                    photoUriArrayList.add(photoUri);
                }

                Log.i("photoArrayList:", photoUriArrayList.toString());

                try {
                    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(
                            getContentResolver(),
                            Uri.parse(photoUri));

                    if (input != null) {
                        Bitmap photo = BitmapFactory.decodeStream(input);
                        contactDpImageView.setImageBitmap(photo);
                        input.close();
                    }
                } catch (IOException e) {
                    Log.e("Contact Photo", "Failed to open image stream.");
                }

                TextView phoneNumberTextView = (TextView) findViewById(R.id.select_time_phone_number_tv);

                if (phoneArrayList.size() > 1) {       // More than one number

                    // Hide single phone number field
                    phoneNumberTextView.setVisibility(View.GONE);

                    ListView phoneNumberListView = (ListView) findViewById(R.id.select_time_phone_number_lv);

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this
                            , R.layout.custom_phone_number_textview, phoneArrayList);
                    phoneNumberListView.setAdapter(adapter);

                    phoneNumberListView.setVisibility(View.VISIBLE);

                } else {                            // Single Phone number
                    phoneNumberTextView.setText(phoneArrayList.get(0));
                }
            }
        }

        CheckBox anyTime = (CheckBox)findViewById(R.id.select_time_any_time_checkbox);
        anyTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){

                    startTimeCard.setVisibility(View.GONE);
                    endTimeCard.setVisibility(View.GONE);

                    // SET START and END TIME for Database
                    startTimeForDatabase = "00:00:00";
                    endTimeForDatabase = "23:59:59";

                } else {

                    startTimeCard.setVisibility(View.VISIBLE);
                    endTimeCard.setVisibility(View.VISIBLE);

                    // SET START and END TIME for Database
                    startTimeForDatabase = "";
                    endTimeForDatabase = "";

                    // Reset Start and End time cards
                    startTimeTextView.setText(getString(R.string.default_time));
                    endTimeTextView.setText(getString(R.string.default_time));
                }
            }
        });

        final Context context = this;

        Button startTimeButton = (Button)findViewById(R.id.start_time_button);
        Button endTimeButton = (Button)findViewById(R.id.button_end_time);
        Button saveButton = (Button)findViewById(R.id.select_time_save_button);

        startTimeTextView = (TextView)findViewById(R.id.start_time_tv);
        endTimeTextView = (TextView)findViewById(R.id.end_time_tv);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                boolean validTimeFilter = timeUtility.isValidTimeFilter(startTimeForDatabase
                        , endTimeForDatabase);

                if (validTimeFilter) {
                    // Insert the Time Filter
                    int timeId = databaseManager.addTimeFilter(startTimeForDatabase
                            , endTimeForDatabase,startTime,endTime);

                    Log.i("TIME ID",timeId+"");

                    // Insert the phone numbers with the time filter id
                    String response = databaseManager.addContactsToDatabase(nameArrayList
                            , phoneArrayList, photoUriArrayList, timeId);
                    if (!response.contains("Failed")){

                        Toast.makeText(context, getString(R.string.filter_created), Toast.LENGTH_SHORT)
                                .show();

                    } else { // Show the error message
                        Toast.makeText(context, response, Toast.LENGTH_LONG).show();
                    }

                    Intent mainActivity = new Intent(context,Main.class);
                    startActivity(mainActivity);

                    // End the current activity
                    SelectTime.this.finish();
                }
            }
        });

        startTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePickerDialog(context, Constants.START_TIME_DIALOG);
            }
        });

        endTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePickerDialog(context, Constants.END_TIME_DIALOG);
            }
        });

    }

    @Override
    public void onBackPressed() {
        if(backPressAttempt == 0){
            Toast.makeText(context,getString(R.string.back_pressed_message),Toast.LENGTH_SHORT)
                    .show();
            backPressAttempt++;
        } else {
            endCurrentActivity();
            Toast.makeText(context,getString(R.string.filter_not_saved),Toast.LENGTH_SHORT)
                    .show();
            Intent mainActivity = new Intent(this,Main.class);
            startActivity(mainActivity);
        }
    }

    private void endCurrentActivity() {
        this.finish();
    }

    private void showTimePickerDialog(Context context, final int type){

        // Process to get Current Time
        Calendar c = Calendar.getInstance();
        final int systemHour = c.get(Calendar.HOUR_OF_DAY);
        final int systemMinute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay,int minute) {

                        if(type == Constants.START_TIME_DIALOG){

                            startTimeForDatabase = timeUtility.formatTime(hourOfDay,minute,0,true);
                            startTime =  timeUtility.formatTime(hourOfDay, minute, 0, false);

                            startTimeTextView.setText(startTime);

                            Log.i("START TIME for database", startTimeForDatabase);

                        } else if (type == Constants.END_TIME_DIALOG){

                            endTimeForDatabase = timeUtility.formatTime(hourOfDay, minute, 0, true);
                            endTime =  timeUtility.formatTime(hourOfDay, minute, 0, false);

                            endTimeTextView.setText(endTime);

                            Log.i("END TIME for database",endTimeForDatabase);
                        }
                    }
                }, systemHour, systemMinute, false);

        timePickerDialog.show();
    }
}
