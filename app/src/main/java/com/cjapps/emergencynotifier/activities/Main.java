package com.cjapps.emergencynotifier.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.cjapps.emergencynotifier.R;
import com.cjapps.emergencynotifier.utility.Constants;

import java.util.ArrayList;

public class Main extends ActionBarActivity {

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        final SharedPreferences config = getSharedPreferences("config",MODE_PRIVATE);

        RelativeLayout createFilterOnContact = (RelativeLayout)findViewById(R.id.main_screen_contact_filter_layout);
        RelativeLayout createFilterByTime = (RelativeLayout)findViewById(R.id.main_screen_group_filter_layout);
        RelativeLayout viewFilter = (RelativeLayout)findViewById(R.id.main_screen_view_filter_layout);

        final ToggleButton filterToggle = (ToggleButton)findViewById(R.id.main_screen_filter_toggle);
        final TextView filterState = (TextView)findViewById(R.id.main_screen_filter_toggle_state);

        boolean configFilterState = config.getBoolean("filter_activated",false);
        filterToggle.setChecked(configFilterState);

        if(configFilterState){       // Filter inactive
            filterToggle.setText(getText(R.string.turn_off));
            filterState.setText(getText(R.string.filter_toggle_state_active));
        } else {
            filterToggle.setText(getText(R.string.turn_on));
            filterState.setText(getText(R.string.filter_toggle_state_inactive));
        }

        Log.i("Checked State",filterToggle.isChecked()+"");
        Log.i("Config State",config.getBoolean("filter_activated",false)+"");

        filterToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                SharedPreferences.Editor editor = config.edit();
                editor.putBoolean("filter_activated", isChecked);
                editor.apply();

                if (isChecked) {              // Filter is activated
                    filterState.setText(getText(R.string.filter_toggle_state_active));
                } else {
                    filterState.setText(getText(R.string.filter_toggle_state_inactive));
                }

                Log.i("Checked State", filterToggle.isChecked() + "");
                Log.i("Config State", config.getBoolean("filter_activated", false) + "");
            }
        });

        createFilterOnContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, Constants.PICK_CONTACT);
            }
        });

        createFilterByTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent selectContactActivity = new Intent(context,SelectContact.class);
                startActivity(selectContactActivity);
                endCurrentActivity();
            }
        });

        viewFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewFiltersActivity = new Intent(context,ViewFilters.class);
                startActivity(viewFiltersActivity);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_share) {
            return performShareAction();
        } else if (id == R.id.action_rate_app){
            return performRatingAction();
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean performRatingAction() {
        Intent rateIntent = new Intent(Intent.ACTION_VIEW);
        rateIntent.setData(Uri.parse(getString(R.string.rating_play_store_uri)));
        try{
            // Start Google Play
            startActivity(rateIntent);
            return true;
        }
        catch (ActivityNotFoundException e){
            // If Google Play is not installed open the web page for the app
            rateIntent.setData(Uri.parse(getString(R.string.rating_browser_url)));
            startActivity(rateIntent);
            return false;
        }
    }

    private boolean performShareAction() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.share_subject));
        sharingIntent.putExtra(Intent.EXTRA_TEXT, getText(R.string.share_body));
        startActivity(Intent.createChooser(sharingIntent, getText(R.string.share_dialog_title)));
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.PICK_CONTACT && resultCode == Activity.RESULT_OK){

            Uri selectedContactUri = data.getData();

            String id;
            String name;
            String phone;
            String hasPhoneNumber;

            ArrayList<String> phoneArrayList = new ArrayList<String>();
            ArrayList<String> nameArrayList = new ArrayList<String>();

            ContentResolver contactContentResolver = getContentResolver();

            Cursor contactCursor = contactContentResolver.query(selectedContactUri,
                    new String[]{ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts.HAS_PHONE_NUMBER},
                    null, null, null
            );

            if (contactCursor.moveToFirst()) {

                // CONTACT_ID_COLUMN = 0;
                // DISPLAY_NAME_COLUMN = 1;
                // HAS_PHONE_NUMBER_COLUMN = 2;

                id = contactCursor.getString(0);
                name = contactCursor.getString(1);
                hasPhoneNumber = contactCursor.getString(2);

                if (Integer.parseInt(hasPhoneNumber) > 0) {

                    Cursor phoneCursor = contactContentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null
                    );

                    int phoneCursorCount = phoneCursor.getCount();

                    if (phoneCursorCount > 0) {

                        while (phoneCursor.moveToNext()) {

                            // PHONE_NUMBER_COLUMN = 0;
                            phone = phoneCursor.getString(0);

                            if (!phone.contains("@")) {

                                phone = phone.replace("-", "");
                                phone = phone.replace(" ", "");

                                if (!phoneArrayList.contains(phone)) {
                                    // Add the Phone Number Corresponding to that Contact
                                    phoneArrayList.add(phone);
                                    nameArrayList.add(name);
                                }
                            }
                        }

                        Log.i("Selected Contact info", name + ", " + phoneArrayList.toString()
                                + nameArrayList.toString());
                    }
                    phoneCursor.close();
                }

                // Check if all the contact info is available start activity to set time

                if (phoneArrayList.size() > 0){
                    // Bundle contact data and start activity

                    Uri photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                            Long.parseLong(id));

                    Bundle contactData = new Bundle();

                    contactData.putStringArrayList("phoneArrayList", phoneArrayList);
                    contactData.putStringArrayList("nameArrayList", nameArrayList);

                    contactData.putString("photoUri",photoUri.toString());

                    Intent selectTime = new Intent(context,SelectTime.class);
                    selectTime.putExtras(contactData);
                    endCurrentActivity();
                    startActivity(selectTime);

                } else {
                    Toast.makeText(this,getString(R.string.invalid_phone_number),Toast.LENGTH_LONG)
                            .show();
                }
            }

            contactCursor.close();

        } else {
            Toast.makeText(this,getString(R.string.no_contact_selected),Toast.LENGTH_LONG).show();
        }
    }

    private void endCurrentActivity() {
        this.finish();
    }
}
