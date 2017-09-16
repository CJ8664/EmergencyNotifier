package com.cjapps.emergencynotifier.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.security.MessageDigest;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DatabaseManager extends SQLiteOpenHelper {

    public SQLiteDatabase dbWriter;
    public SQLiteDatabase dbReader;

    // Database constants
    public static final String DB_NAME = "emergencynotifier";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_EMERGENCY_CONTACTS = "TABLE_EMERGENCY_CONTACTS";
    public static final String TABLE_FILTER_TIME = "TABLE_FILTER_TIME";

    // EMERGENCY CONTACTS TABLE COLUMNS

    public static final String EMERGENCY_CONTACT_ID = "EMERGENCY_CONTACT_ID";
    public static final String EMERGENCY_CONTACT_TIME_ID = "EMERGENCY_CONTACT_TIME_ID";
    public static final String EMERGENCY_CONTACT_NAME = "EMERGENCY_CONTACT_NAME";
    public static final String EMERGENCY_CONTACT_NUMBER = "EMERGENCY_CONTACT_NUMBER";
    public static final String EMERGENCY_CONTACT_PHOTO_URI = "EMERGENCY_CONTACT_PHOTO_URI";

    // FILTER TIME TABLE COLUMNS

    public static final String FILTER_TIME_ID = "TIME_ID";
    public static final String START_TIME = "START_TIME";
    public static final String END_TIME = "END_TIME";
    public static final String START_TIME_UI = "START_TIME_UI";
    public static final String END_TIME_UI = "END_TIME_UI";

    /**
     * This is the default constructor
     * @param context is the context in which the instance of this class is created
     */
    public DatabaseManager(Context context){
        super(context, DB_NAME, null, DATABASE_VERSION);
    }

    /**
     * This function is called when the database is created.
     * @param sqLiteDatabase is the database object.
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        String CREATE_TABLE_EMERGENCY_CONTACTS = "CREATE TABLE " + TABLE_EMERGENCY_CONTACTS + "("
                + EMERGENCY_CONTACT_NUMBER + " VARCHAR(15)," + EMERGENCY_CONTACT_NAME
                + " VARCHAR(40)," + EMERGENCY_CONTACT_ID + " VARCHAR(40)," + EMERGENCY_CONTACT_PHOTO_URI
                + " VARCHAR(150)," + EMERGENCY_CONTACT_TIME_ID + " VARCHAR(20) REFERENCES "
                + TABLE_FILTER_TIME + "("+FILTER_TIME_ID+"))";

        sqLiteDatabase.execSQL(CREATE_TABLE_EMERGENCY_CONTACTS);
        Log.e("TABLE_EMERGENCY_CONTACTS", "created");

        sqLiteDatabase.execSQL("CREATE INDEX phone_number_index ON "
                +TABLE_EMERGENCY_CONTACTS+" ("+EMERGENCY_CONTACT_NUMBER+")");
        Log.e("TABLE_EMERGENCY_CONTACTS", "index created");

        String CREATE_TABLE_FILTER_TIME = "CREATE TABLE " + TABLE_FILTER_TIME + "("
                + FILTER_TIME_ID + " VARCHAR(20) PRIMARY KEY," + START_TIME + " VARCHAR(20), "
                + END_TIME + " VARCHAR(20)," + START_TIME_UI + " VARCHAR(20), "
                + END_TIME_UI + " VARCHAR(20))";

        sqLiteDatabase.execSQL(CREATE_TABLE_FILTER_TIME);
        Log.e("TABLE_FILTER_TIME", "created");

        Log.e("DATABASE", "created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }

    // ------------------------ METHODS THAT MODIFY DATA OF DATABASE --------------------//

    /**
     * This method takes the following as parameter
     * @param startTime the filter start time in 24 hour format
     * @param endTime the filter end time in 24 hour format
     * @param startTimeUI the filter start time in 12 hour format
     * @param endTimeUI the filter end time in 12 hour format
     * It checks if such a time filter is already created, if so returns the id of that filter or
     * creates a new filter and return it's id
     * @return The time id for given set of parameters
     */
    public int addTimeFilter(String startTime, String endTime,String startTimeUI, String endTimeUI){

        dbReader = this.getReadableDatabase();

        String queryString = "SELECT "+FILTER_TIME_ID+", COUNT("+FILTER_TIME_ID+")" +
                " FROM "+ TABLE_FILTER_TIME +
                " WHERE "+START_TIME+"='"+startTime+"' AND "+END_TIME+"='"+endTime+"'";

        // Check if such filter already exists
        Cursor existingTimeFilters = dbReader.rawQuery(queryString,null);

        if (existingTimeFilters!=null && existingTimeFilters.getCount() > 0){

            existingTimeFilters.moveToFirst();

            int timeId = existingTimeFilters.getInt(0);
            int existingCount = existingTimeFilters.getInt(1);

            if (existingCount > 0){     // The filter doesn't exist
                // Return the existing ID
                Log.i("Existing Filter",timeId+", " + startTime + ", " + endTime);
                return timeId;
            }
        }

        int maxTimeId;

        // GET NEW FILTER_TIME_ID
        queryString = "SELECT MAX("+FILTER_TIME_ID+"), COUNT("+FILTER_TIME_ID+") " +
                "FROM " + TABLE_FILTER_TIME;

        Cursor timeFilters = dbReader.rawQuery(queryString, null);

        if (timeFilters != null && timeFilters.getCount() > 0) {
            timeFilters.moveToFirst();

            int rowCount = timeFilters.getInt(1);
            if (rowCount == 0) {
                // No rows
                maxTimeId = 0;
            } else {
                // Maximum FILTER_TIME_ID value
                maxTimeId = timeFilters.getInt(0);
            }
        } else {
            maxTimeId = 0;
        }

        dbReader.close();

        // INSERT new Time filter

        dbWriter = this.getWritableDatabase();

        ContentValues filterValues = new ContentValues();

        filterValues.put(FILTER_TIME_ID, maxTimeId + 1);
        filterValues.put(START_TIME, startTime);
        filterValues.put(END_TIME, endTime);
        filterValues.put(START_TIME_UI, startTimeUI);
        filterValues.put(END_TIME_UI, endTimeUI);

        Log.i("Inserting filter:",(maxTimeId + 1)+", " + startTime + ", " + endTime);
        dbWriter.insert(TABLE_FILTER_TIME, null, filterValues);

        dbWriter.close();

        return maxTimeId+1;
    }

    /**
     * This method adds the contact name and corresponding phone numbers with the time id
     * @param names {@link java.util.ArrayList} of names
     * @param phoneNumbers {@link java.util.ArrayList} of numbers
     * @param timeId The time id
     * @return The response message after inserting (Eg; error message or success message)
     */
    public String addContactsToDatabase(ArrayList<String> names,ArrayList<String> phoneNumbers
            , ArrayList<String> photoUri, int timeId){

        dbWriter = this.getWritableDatabase();

        dbReader = this.getReadableDatabase();

        String response="";

        int i = 0;
        for (String number : phoneNumbers){

            String query = "SELECT COUNT("+EMERGENCY_CONTACT_NUMBER+") FROM "+TABLE_EMERGENCY_CONTACTS
                    +" WHERE "+EMERGENCY_CONTACT_NUMBER+"='"+number+"' AND "+EMERGENCY_CONTACT_TIME_ID
                    +"='"+timeId+"'";

            Cursor existingFilter = dbReader.rawQuery(query,null);

            existingFilter.moveToFirst();
            int existingCount = existingFilter.getInt(0);

            Log.i("Existing Count",existingCount+"");

            if (existingCount <= 0){ // Same combination of time and contact does not exists

                ContentValues contactDetails = new ContentValues();

                try {

                    String detailsForMD5 = timeId+number+names.get(i);
                    MessageDigest encoder = MessageDigest.getInstance("MD5");
                    encoder.update(detailsForMD5.getBytes());

                    String contactID = "";
                    for (byte hash : encoder.digest()){
                        if ((0xFF & hash) < 0x10){
                            contactID += "0" + Integer.toHexString(0xFF & hash);
                        } else {
                            contactID += Integer.toHexString(0xFF & hash);
                        }
                    }

                    Log.i("Contact ID",contactID);

                    contactDetails.put(EMERGENCY_CONTACT_NAME,names.get(i));
                    contactDetails.put(EMERGENCY_CONTACT_NUMBER,number);
                    contactDetails.put(EMERGENCY_CONTACT_TIME_ID,timeId);
                    contactDetails.put(EMERGENCY_CONTACT_ID,contactID);
                    contactDetails.put(EMERGENCY_CONTACT_PHOTO_URI,photoUri.get(i));

                    dbWriter.insert(TABLE_EMERGENCY_CONTACTS,null,contactDetails);
                } catch (Exception e){
                    Log.e("Failed to insert",number+", "+timeId +" "+e.getMessage());
                    response+="Failed to create filter for "+number+"\n";
                }
            } else {
                Log.e("Filter already exists",number+" "+timeId);
            }

            i++; // Counter to get the corresponding name
        }

        // Optimize Database
        dbWriter.execSQL("VACUUM");

        dbWriter.close();
        return response;
    }

    public boolean deleteFilter(String contactId){

        dbWriter = this.getWritableDatabase();

        try {
            dbWriter.delete(TABLE_EMERGENCY_CONTACTS, EMERGENCY_CONTACT_ID + "=?", new String[]{contactId});
            Log.i("Contact deleted",contactId);
            dbWriter.close();
            return true;
        } catch (Exception e){
            Log.i("Error deleting contact",contactId);
            dbWriter.close();
            return false;
        }

    }

    // ------------------------ METHODS THAT RETRIEVE DATA FROM DATABASE --------------------//

    /**
     * This method returns {@link java.util.ArrayList} of the Time ids for that contact number
     * @param phoneNumber The incoming phone number
     * @return the ids
     */
    public ArrayList<String> getEmergencyNumberTimeIds(String phoneNumber){

        dbReader = this.getReadableDatabase();

        String[] columns = {EMERGENCY_CONTACT_TIME_ID};
        String[] selectionArg = {"%"+phoneNumber};

        Cursor details = dbReader.query(true,TABLE_EMERGENCY_CONTACTS,columns,
                EMERGENCY_CONTACT_NUMBER + " like ?",selectionArg,null,null,null,null);

        if(details!=null && details.getCount() > 0){

            ArrayList<String> timeIds = new ArrayList<String>(details.getCount());

            while (details.moveToNext()) {
                timeIds.add(details.getString(0));
            }
            return timeIds;

        } else {
            return null;
        }
    }

    /**
     * This method checks whether the system time falls between start or end time of any of the
     * filters corresponding to the passed time ids
     * @param timeIds The time ids for a particular phone number
     * @param systemTime The current System Time
     * @return true if the system time is within a filter
     */
    public boolean checkTimeFilters(ArrayList<String> timeIds, Time systemTime){

        dbReader = this.getReadableDatabase();

        String timeIdsAsInRel="";

        for (String id : timeIds){
            timeIdsAsInRel +="'"+id+"',";
        }
        timeIdsAsInRel = timeIdsAsInRel.substring(0,timeIdsAsInRel.length()-1);

        String query = "SELECT "+START_TIME+", "+END_TIME+" FROM "+TABLE_FILTER_TIME + " WHERE "
                +FILTER_TIME_ID +" IN ("+timeIdsAsInRel+")";

        Cursor filters = dbReader.rawQuery(query,null);

        if(filters!=null && filters.getCount() >0){

            while(filters.moveToNext()) {

                long startTimeLong = Time.valueOf(filters.getString(0)).getTime();
                long endTimeLong = Time.valueOf(filters.getString(1)).getTime();

                long midnight = Time.valueOf("23:59:59").getTime();

                long systemTimeLong = systemTime.getTime();

                //Log.i("TIMES",startTimeLong + ", " + systemTimeLong + ", "+ endTimeLong);

                if (endTimeLong < startTimeLong ) {

                    boolean currentDay = systemTimeLong >= startTimeLong && systemTimeLong < midnight;
                    boolean nextDay = systemTimeLong >= midnight && systemTimeLong <= endTimeLong;

                    if (currentDay || nextDay) {
                        // Between Current day and next day
                        // Eg: filter night 8 pm till late morning 4 am
                        //Log.i("Current time","is within a time filter");
                        return true;
                    }
                }
                if (systemTimeLong >= startTimeLong && systemTimeLong <=endTimeLong) {
                    //Log.i("Current time","is within a time filter");
                    return true;
                }
            }
        }

        // Current time is not within a time filter
        return false;
    }

    /**
     * This method returns a complex data structure with details of all the filter and corresponding
     * contact info
     * The HashMap key the name of contact
     * The HashMap value is a HashSet comprising of details like phone number, start time, end time,
     * contact id.
     * @return The complex data structure
     */
    public HashMap<String,HashSet<String>> getAllFilters(){

        HashMap<String,HashSet<String>> filters = null;
        HashSet<String> filterDetailsSet;

        dbReader = this.getReadableDatabase();

        String query = "SELECT E."+EMERGENCY_CONTACT_NAME+", E."+EMERGENCY_CONTACT_NUMBER+", " +
                "E."+EMERGENCY_CONTACT_ID+", "+ " E."+EMERGENCY_CONTACT_PHOTO_URI+", F."+
                START_TIME_UI+", F."+END_TIME_UI+" FROM "+TABLE_EMERGENCY_CONTACTS+" E, "+
                TABLE_FILTER_TIME+" F" + " WHERE E."+EMERGENCY_CONTACT_TIME_ID+" = F."+
                FILTER_TIME_ID+" ORDER BY F."+FILTER_TIME_ID+" DESC";

        Cursor filterCursor = dbReader.rawQuery(query,null);

        if (filterCursor!=null && filterCursor.getCount()>0){

            filters = new HashMap<String, HashSet<String>>();

            while (filterCursor.moveToNext()){

                String name = filterCursor.getString(0);

                if (!filters.containsKey(name)){
                    filterDetailsSet = new HashSet<String>();
                    filters.put(name,filterDetailsSet);
                }

                filterDetailsSet = filters.get(name);

                String tempDetails =  filterCursor.getString(1)+";"+filterCursor.getString(2)+";"
                        +filterCursor.getString(3)+";"+filterCursor.getString(4)+";"+
                        filterCursor.getString(5);
                // 0 number;1 contact_id;2 photo;3 start_time;4 end_time

                filterDetailsSet.add(tempDetails);

                filters.put(name,filterDetailsSet);
                Log.i(name,filters.get(name).toString());
            }
        }

        dbReader.close();

        return filters;
    }
}
