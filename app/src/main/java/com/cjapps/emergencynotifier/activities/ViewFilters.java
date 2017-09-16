package com.cjapps.emergencynotifier.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cjapps.emergencynotifier.R;
import com.cjapps.emergencynotifier.database.DatabaseManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

public class ViewFilters extends Activity {

    private Context context;

    private HashMap<String,HashSet<String>> filters;

    private Object[] filterKeys;

    private DatabaseManager databaseManager;

    private FilterExpandableListViewAdapter filterAdapter;

    private boolean delete = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vew_filters);

        context = this;

        databaseManager = new DatabaseManager(context);

        ExpandableListView filterExpandableListView = (ExpandableListView)
                findViewById(R.id.expandableListView);

        filters = databaseManager.getAllFilters();

        if (filters == null){ // No filters are created
            TextView noFilters = (TextView)findViewById(R.id.no_filters_textview);
            noFilters.setVisibility(View.VISIBLE);
            filterExpandableListView.setVisibility(View.GONE);
        } else {
            filterKeys = filters.keySet().toArray();
            filterAdapter = new FilterExpandableListViewAdapter();
            filterExpandableListView.setAdapter(filterAdapter);
        }
    }

    private class FilterExpandableListViewAdapter extends BaseExpandableListAdapter {

        String[] details;

        @Override
        public int getGroupCount() {
            return filterKeys.length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return filters.get(getGroup(groupPosition)).size();
        }

        @Override
        public String getGroup(int groupPosition) {
            return filterKeys[groupPosition].toString();
        }

        @Override
        public String getChild(int groupPosition, int childPosition) {
            return filters.get(getGroup(groupPosition)).toArray()[childPosition].toString();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded
                                    , View view, ViewGroup viewGroup) {

            View parentView = View.inflate(context,R.layout.custom_filter_group,null);

            TextView name = (TextView)parentView.findViewById(R.id.nameTextView);
            String nameKey = filterKeys[groupPosition].toString();
            name.setText(nameKey);

            TextView filterCount = (TextView)parentView.findViewById(R.id.filterCountTextView);

            int childrenCount = getChildrenCount(groupPosition);
            if (childrenCount > 1) {
                filterCount.setText(childrenCount + " filters set");
            } else {
                filterCount.setText(childrenCount + " filter set");
            }

            ImageView contactDpImageView = (ImageView)parentView.findViewById(R.id.photoImageView);

            String photoUri = filters.get(nameKey).toString().split(";")[2];

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

            return parentView;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            View parentView = View.inflate(context,R.layout.custom_filter_child,null);

            details = getChild(groupPosition,childPosition).split(";");

            TextView number = (TextView)parentView.findViewById(R.id.numberTextView);
            TextView time = (TextView)parentView.findViewById(R.id.timeTextView);

            number.setText("From:" + details[0]);
            if (details[3].contains("Any Time")){
                time.setText("Any Time");
            } else {
                time.setText("Time: "+details[3] + " - " + details[4]);
            }

            ImageView deleteButton = (ImageView)parentView.findViewById(R.id.deleteImageView);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO remove test
                    delete = true;
                    removeFilter(groupPosition,childPosition,details[1]);
                }
            });

            return parentView;
        }

        @Override
        public boolean isChildSelectable(int i, int i2) {
            return true;
        }

        private void removeFilter(int groupPosition, int childPosition, final String id){

            Log.e("ID",id);

            final RelativeLayout undoLayout = (RelativeLayout)findViewById(R.id.view_filters_undo_layout);

            undoLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    delete = false;
                    filterAdapter.notifyDataSetChanged();
                    undoLayout.setVisibility(View.GONE);
                }
            });

            Animation slideInBottom = AnimationUtils.loadAnimation(context, R.anim.abc_slide_in_bottom);
            undoLayout.setVisibility(View.VISIBLE);
            undoLayout.startAnimation(slideInBottom);

            slideInBottom.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // After the animation ends start the @Main activity
                    // Create a delay for the logo to be seen

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            undoLayout.setVisibility(View.GONE);
                            if (delete){
                                if ( !databaseManager.deleteFilter(id)){
                                    Toast.makeText(context,"Error removing filter",Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }

                            filterAdapter.notifyDataSetChanged();
                        }
                    }, 2500);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });


            String key = getGroup(groupPosition);
            String filterToBeRemoved = getChild(groupPosition,childPosition);
            filters.get(key).remove(filterToBeRemoved);

            // if there are no filters under that group remove that group also
            if (filters.get(key).size() == 0){
                filters.remove(key);
            }

            // Reset the keys
            filterKeys = filters.keySet().toArray();

            // Notify that the data has changed
            filterAdapter.notifyDataSetChanged();
        }
    }
}
