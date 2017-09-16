package com.cjapps.emergencynotifier.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.cjapps.emergencynotifier.R;
import com.cjapps.emergencynotifier.utility.Constants;
import com.cjapps.emergencynotifier.utility.Contact;
import com.cjapps.emergencynotifier.utility.PinnedSectionListView;

import java.util.ArrayList;
import java.util.HashSet;

public class SelectContact extends ActionBarActivity {

    private ListView contactsListView;

    private TextView noContactsTextView;

    private ArrayList<String> selectedContactsIds;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contact);

        context = this;

        contactsListView = (ListView)findViewById(android.R.id.list);
        noContactsTextView = (TextView)findViewById(R.id.no_contact_tv);

        selectedContactsIds = new ArrayList<String>(5);

        Button continueButton = (Button) findViewById(R.id.continue_button);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (selectedContactsIds.size() == 0) {
                    Toast.makeText(context,getString(R.string.no_contact_selected),Toast.LENGTH_SHORT).show();
                } else {
                    Intent selectTimeActivity = new Intent(getApplicationContext()
                            , SelectTime.class);
                    selectTimeActivity.putExtra("selectedContactsIds", selectedContactsIds);
                    startActivity(selectTimeActivity);
                    SelectContact.this.finish();
                }
            }
        });

        initializeAdapter();
    }

    @Override
    public void onBackPressed() {
        endCurrentActivity();
        Toast.makeText(context,getString(R.string.filter_not_saved),Toast.LENGTH_SHORT)
                .show();
        Intent mainActivity = new Intent(this,Main.class);
        startActivity(mainActivity);
    }

    private void endCurrentActivity() {
        this.finish();
    }

    private void initializeAdapter() {

        contactsListView.setFastScrollEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            contactsListView.setFastScrollAlwaysVisible(true);
        }

        FastScrollAdapter fastScrollAdapter = new FastScrollAdapter(this, android.R.layout.simple_list_item_1
                , android.R.id.text1);

        // Set The fast scroll adapter containing the contacts to the (@ListView)
        contactsListView.setAdapter(fastScrollAdapter);
    }

    private class SimpleAdapter extends ArrayAdapter<Contact> implements PinnedSectionListView.PinnedSectionListAdapter {

        public SimpleAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);

            prepareSections();

            //---------------------START OF LOADING CONTACTS FROM PHONE-------------------------//

            ContentResolver contentResolver = getContentResolver();
            Cursor contactCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI
                    , new String[]{ContactsContract.Contacts._ID,
                                   ContactsContract.Contacts.DISPLAY_NAME,
                                   ContactsContract.Contacts.HAS_PHONE_NUMBER}
                    , null, null, ContactsContract.Contacts.DISPLAY_NAME);

            // CONTACT_ID_COLUMN = 0;
            // DISPLAY_NAME_COLUMN = 1;
            // HAS_PHONE_NUMBER_COLUMN = 2;

            ArrayList<Contact> contactArrayList = null;
            HashSet<String> uniqueNamesSet = new HashSet<String>();

            if (contactCursor!=null){

                int currCount = contactCursor.getCount();
                if (currCount > 0){

                    contactArrayList = new ArrayList<Contact>(currCount);

                    while(contactCursor.moveToNext()){

                        if (Integer.parseInt(contactCursor.getString(2)) > 0) {

                            String name = contactCursor.getString(1);

                            if (!uniqueNamesSet.contains(name) && !name.contains("@")) {

                                uniqueNamesSet.add(name);
                                contactArrayList.add(new Contact(Constants.CONTACT,name,contactCursor.getString(0)));
                            }
                        }
                    }

                    contactCursor.close();
                }
            }

            //---------------------END OF LOADING CONTACTS FROM PHONE-------------------------//

            int index = 0;
            int count = 0;
            if (contactArrayList != null) {
                count = contactArrayList.size();
            }

            if(count>0){

                int sectionPosition = 0, listPosition = 0;
                Contact section;
                Contact item;

                char prev = '9';
                char current;

                String name;
                String id;

                while (index<count){

                    name = contactArrayList.get(index).name;
                    id = contactArrayList.get(index).id;

                    current = name.charAt(0);

                    if(prev != current ) {

                        section = new Contact(Constants.SECTION, current+"",null);
                        section.sectionPosition = sectionPosition;
                        section.listPosition = listPosition++;
                        onSectionAdded(section, section.sectionPosition);
                        add(section);
                        sectionPosition++;

                        prev = current;
                    }

                    item = new Contact(Constants.CONTACT, name, id);
                    item.sectionPosition = sectionPosition-1;
                    item.listPosition = listPosition++;
                    add(item);

                    index++;
                }
            } else {
                noContactsTextView.setVisibility(View.VISIBLE);
                contactsListView.setVisibility(View.INVISIBLE);
            }

        }
        protected void prepareSections() { }
        protected void onSectionAdded(Contact section, int sectionPosition) { }

        @Override public View getView(final int position, View convertView, ViewGroup parent) {

            View contactRow;

            final Contact con = getItem(position);

            if(con.type == Constants.SECTION){

                contactRow = View.inflate(context,R.layout.custom_view_section_header,null);

                TextView sectionHeader = (TextView)contactRow.findViewById(R.id.section_header_tv);
                sectionHeader.setText(con.name);

                return contactRow;

            } else {

                contactRow = View.inflate(context,R.layout.custom_view_people_list_item_layout,null);

                TextView name = (TextView)contactRow.findViewById(R.id.contact_list_name);
                final ImageView selectedHighlight = (ImageView)contactRow.findViewById(R.id.selection);

                name.setText(con.name);
                if(con.isSelected){
                    selectedHighlight.setBackgroundResource(R.color.red_light);
                }

                contactRow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (con.isSelected) {

                            selectedContactsIds.remove(con.id);

                            selectedHighlight.setBackgroundResource(android.R.color.white);
                            con.isSelected = false;
                        } else {

                            selectedContactsIds.add(con.id);

                            selectedHighlight.setBackgroundResource(R.color.red_light);
                            con.isSelected = true;
                        }

                    }
                });
            }

            return contactRow;
        }

        @Override public int getViewTypeCount() {
            return 2;
        }

        @Override public int getItemViewType(int position) {
            return getItem(position).type;
        }

        @Override
        public boolean isItemViewTypePinned(int viewType) {
            return viewType == Constants.SECTION;
        }
    }

    private class FastScrollAdapter extends SimpleAdapter implements SectionIndexer {

        private ArrayList<Contact> sections ;

        public FastScrollAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        @Override protected void prepareSections() {
            sections = new ArrayList<Contact>(10);
        }

        @Override protected void onSectionAdded(Contact section, int sectionPosition) {
            sections.add(sectionPosition, section);
        }

        @Override
        public Contact[] getSections() {

            Contact[] temp = new Contact[sections.size()];
            for(int i=0;i<temp.length;i++){
                temp[i] = sections.get(i);
            }
            return temp;
        }

        @Override public int getPositionForSection(int section) {
            if (section >= sections.size()) {
                section = sections.size() - 1;
            }
            return sections.get(section).listPosition;
        }

        @Override public int getSectionForPosition(int position) {
            if (position >= getCount()) {
                position = getCount() - 1;
            }
            return getItem(position).sectionPosition;
        }
    }
}
