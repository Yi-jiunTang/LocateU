package com.example.locateu;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

//import android.content.ContentValues;

public class DBHelper extends SQLiteOpenHelper {

	static final String TAG = DBHelper.class.getSimpleName();

	final static int _DBversion = 1; // database version

	final static String _DBname = "wifiOnline.db";// DBname&tableName r
													// different from off-line

	final static String _TableName = "online";

	// static final String COUNTER = "Counter";

	public static final String SCAN_ID = "Scan_Id";

	public static final String MAC_ADDRESS = "Mac_Address";

	public static final String AP_NAME = "Ap_Name";

	public static final String RSS = "RSS";

	public static final String WIFIRECORDS = "WifiRecords";
	
	public static final String LATITUDE = "Latitude";
	
	public static final String LONGITUDE = "Longitude";

	public static final String SEQUENCE = "Sequence";
	
	public static final String GYRO_X="Gyro_x";
	
	public static final String GYRO_Y="Gyro_y";
	
	public static final String GYRO_Z="Gyro_z";
	
	public static final String STEP_FREQUENCY = "Step_Frequency";
	
	public static final String MOVE_LATITUDE = "Move_Latitude";
	
	public static final String MOVE_LONGITUDE = "Move_Longitude";
	

	// public static final String DEVICE_ID = "Device_Id";

	// private SQLiteDatabase db;

	public DBHelper(Context context) {

		super(context, _DBname, null, _DBversion);

	}

	public DBHelper(Context context, String name, CursorFactory factory,
			int version) {

		super(context, name, factory, version);

	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		// TODO Auto-generated method stub

		final String SQL = "CREATE TABLE "

				+ _TableName
				+ "("

				+ SEQUENCE
				+ " INTEGER NOT NULL,"

				+ SCAN_ID
				+ " INTEGER NOT NULL,"

				+ MAC_ADDRESS
				+ " TEXT NOT NULL,"

				+ AP_NAME
				+ " TEXT NOT NULL,"

				+ RSS
				+ " TEXT NOT NULL,"
				
				+ LATITUDE
				+ " TEXT NOT NULL,"
				
				+ LONGITUDE
				+ " TEXT NOT NULL,"
				
				+ GYRO_X
				+ " TEXT NOT NULL,"
				
				+ GYRO_Y
				+ " TEXT NOT NULL,"
				
				+ GYRO_Z
				+ " TEXT NOT NULL,"
				
				+ STEP_FREQUENCY
				+ " INTEGER NOT NULL,"
				
				+ MOVE_LATITUDE
				+ " TEXT NOT NULL,"
				
				+ MOVE_LONGITUDE
				+ " TEXT NOT NULL,"

				// + DEVICE_ID
				// + " TEXT NOT NULL,"

				+ String.format("PRIMARY KEY(%s,%s,%s)", SEQUENCE, SCAN_ID,
						MAC_ADDRESS) + ");";

		db.execSQL(SQL);

		Log.d(TAG, "creat table succeed!");

	}

	@Override
	public void onOpen(SQLiteDatabase db) {

		// TODO Auto-generated method stub

		Log.d(TAG, "SQLiteHelper onOpen");

		super.onOpen(db);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		// TODO Auto-generated method stub

		db.execSQL("DROP TABLE IF EXISTS");

		onCreate(db);

		Log.d(TAG, "SQLiteHelper onUpgrade!");

	}
	/*
	 * public void clean (){
	 * this.getWritableDatabase().execSQL("DROP TABLE IF EXISTS "+_TableName);
	 * System.out.println("clean��除���);
	 * this.onCreate(this.getWritableDatabase());
	 * this.getWritableDatabase().close(); }
	 */

}
