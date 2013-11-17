package com.widjaja.hendra.geolocation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

@SuppressWarnings("unused")
public class MySQLiteHelper extends SQLiteOpenHelper {
	
	// Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "MyLocation.DB";
    private static SQLiteDatabase db;
	private Cursor cursor;
	
	// Table name
    private static final String TABLE_LOCATIONS = "locations";
    
    // Table Columns names
    private static final String KEY_ID = "_id";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";    
    private static final String[] COLUMNS = {KEY_ID, KEY_LATITUDE, KEY_LONGITUDE};   
   
    // SQL statement to create table
 	private static final String CREATE_LOCATION_TABLE = "CREATE TABLE " + TABLE_LOCATIONS + 
 			                                            " (" + KEY_ID + 
 			                                            " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_LATITUDE + 
 			                                            " TEXT NOT NULL, " + KEY_LONGITUDE + 
 			                                            " TEXT NOT NULL)";
    
	protected MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);	
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("onCreate Database get called", "" + db.getVersion());
		// create table
		db.execSQL(CREATE_LOCATION_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d("onUpdate get called", "" + db.getVersion() + " " + Integer.toString(oldVersion));
		// Drop table if existed
        db.execSQL("DROP TABLE IF EXISTS");
        
        // create table
        this.onCreate(db);
	}
	
	/**
     * CRUD operations (create "add", read "get", update, delete)  + get all + delete all 
     */
	public void addPosition(MyPosition position){
		Log.d("addPosition get Called", position.toString());
		
		// 1. get reference to writable DB
		db = this.getWritableDatabase();
		 
		// 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_LATITUDE, position.getLatitude()); 
        values.put(KEY_LONGITUDE, position.getLongitude());
 
        // 3. insert
        db.insert(TABLE_LOCATIONS, null, values); // key/value -> keys = column names/ values = column values
        
        // 4. close
        db.close(); 
	}
	
	public MyPosition getPosition(int id){
		Log.d("getPosition get called", Integer.toString(id));
		
		// 1. get reference to readable DB
		db = this.getReadableDatabase();
		 
		// 2. build query
        Cursor cursor = db.query(TABLE_LOCATIONS, COLUMNS, " id = ? ", new String[] { String.valueOf(id) }, null, null, null, null);
        
        // 3. if we got results get the first one
        if (cursor != null) cursor.moveToFirst();
 
        // 4. build   object
        MyPosition position = new MyPosition();
        position.setId(Integer.parseInt(cursor.getString(0)));
        position.setLatitude(cursor.getString(1));
        position.setLongitude(cursor.getString(2));

		Log.d("Read from MySQL", position.toString());
        // 5. return  
        return position;
	}
	
	// Get All 
    public ArrayList<MyPosition> getAllPositions() {
    	Log.d("getAllPositions get called", "");
    	
        ArrayList<MyPosition> positions = new ArrayList<MyPosition>();

        // 1. build the query
        String query = "SELECT  * FROM " + TABLE_LOCATIONS;
 
    	// 2. get reference to writable DB
        db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
 
        // 3. go over each row, build and add it to list
        MyPosition position = null;
        if (cursor.moveToFirst()) {
            do {
            	position = new MyPosition();
                position.setId(Integer.parseInt(cursor.getString(0)));
                position.setLatitude(cursor.getString(1));
                position.setLongitude(cursor.getString(2));

                // Add 
                positions.add(position);
            } while (cursor.moveToNext());
        }
        return positions;
    }
    
	// Updating single  
    public int updatePosition(MyPosition position) {
    	Log.d("updatePosition get called", "" + position.getId());
    	
    	// 1. get reference to writable DB
        db = this.getWritableDatabase();
 
		// 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put("latitude", position.getLatitude()); 
        values.put("longitude", position.getLongitude());
 
        // 3. updating row
        int i = db.update(TABLE_LOCATIONS, //table
        		values, // column/value
        		KEY_ID+" = ?", // selections
                new String[] { String.valueOf(position.getId()) }); //selection args
        
        // 4. close
        db.close();       
        return i;       
    }

    // Deleting single  
    public void deletePosition(MyPosition position) {
    	Log.d("deletePosition get called", "" + position.getId());
    	
    	// 1. get reference to writable DB
        db = this.getWritableDatabase();
        
        // 2. delete
        db.delete(TABLE_LOCATIONS, KEY_ID + " = ?", new String[] { String.valueOf(position.getId()) });
        
        // 3. close
        db.close();     
//		Log.d("deleteLocation", position.toString());
    }
    
    // Get size of element of records
    public int getLocationCount(MyPosition position) {
    	Log.d("getLocationCount get called", "" + position.getId());
    	String countQuery = "SELECT  * FROM " + TABLE_LOCATIONS;
    	db = this.getWritableDatabase();
        cursor = db.rawQuery(countQuery, null);
        cursor.close();
     
        // return count
        return cursor.getCount();
     }
}