package com.example.locateu;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.PrivateCredentialPermission;

import com.example.locateu.DBHelper;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.R.integer;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.MapActivity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class MainActivity extends FragmentActivity implements LocationListener,
		OnCancelListener {
	private GoogleMap map;
	private LocationManager locationManager;
	private ProgressDialog mProgressDialog;
	private Location location;
	// private ProgressDialog mProgressDialog;
	public static String URI_API = "http://140.116.39.172/wifi_project/wifi_positioning.php";
	public static final String TEMP_FILE_NAME = "WifiRecord";
	private static final String TUPLENAME = "WifiRecord";
	private static final String TAG = "MyActivity";
	public static final int TIME = 10;
	private Button locateMeButton;
	private TextView locateView;
	public WifiManager wm;
	WifiManager.WifiLock unlock;// avoid wifi falling sleep
	private List<ScanResult> results;
	private SQLiteDatabase db = null;
	public static DBHelper helper = null;
	public static int scanCnt;
	public double lat, lng;
	public String _DBname = "wifiData.db";
	private TelephonyManager telephonyManager;
	int strength;
	int speed;
	int Sequence;
	int level;
	int i;
	int pos;
	String data, apMac, apId;
	String otherwifi, iMEIString, MACString;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(getApplicationContext());
		map = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();
		map.setMyLocationEnabled(true);
		map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

		wm = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		locateMeButton = (Button) findViewById(R.id.locate);
		locateMeButton.setOnClickListener(locateButtonClickListener);
		results = new ArrayList<ScanResult>();
		// start my database class
		this.helper = new DBHelper(this, "wifiData.db", null, 1);

		// calculate how many times the process is executed
		SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
		Sequence = preferences.getInt("Sequence", 0);
		Sequence++;
		getPreferences(Context.MODE_PRIVATE).edit()
				.putInt("Sequence", Sequence).commit();

		telephonyManager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		// String iMEIString = telephonyManager.getDeviceId();
		// Log.d(TAG, "IMEI:" + iMEIString);
		locateView = (TextView) findViewById(R.id.locateView);
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(this);
		mProgressDialog.setMessage("getting current location...");
		mProgressDialog.show();
		for (String provider : locationManager.getProviders(true)) {
			if (LocationManager.GPS_PROVIDER.equals(provider)
					|| LocationManager.NETWORK_PROVIDER.equals(provider)) {

				locationManager.requestLocationUpdates(provider, 0, 0, this);

			}
		}
		/*
		 * LocationListener locationListener = new mylocationListener();
		 * locationManager.getProviders(true);
		 * locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
		 * 0, 0, locationListener); if (location != null) { lat =
		 * location.getLatitude(); lng = location.getLongitude(); Log.d(TAG,
		 * "GPS" + lat + lng); }
		 */
		Log.d(TAG, "onCreate");

	}

	/*
	 * private class mylocationListener implements LocationListener {
	 * 
	 * @Override public void onLocationChanged(Location location) { if (location
	 * != null) { lat = location.getLatitude(); lng = location.getLongitude();
	 * Toast.makeText(getApplicationContext(), lat + "," + lng,
	 * Toast.LENGTH_SHORT).show(); Log.d(TAG, "GPS" + lat + lng); } }
	 * 
	 * @Override public void onProviderDisabled(String provider) { // TODO
	 * Auto-generated method stub
	 * 
	 * }
	 * 
	 * @Override public void onProviderEnabled(String provider) { // TODO
	 * Auto-generated method stub
	 * 
	 * }
	 * 
	 * @Override public void onStatusChanged(String provider, int status, Bundle
	 * extras) { // TODO Auto-generated method stub
	 * 
	 * } }
	 */

	// };

	// click scan button it will post to r1 execute the scan cycle
	View.OnClickListener locateButtonClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// lHandler is manager, r1 is its employee
			// one manager, one employee, one job
			Log.d(TAG, "onLocate");

			Handler lHandler = new Handler();
			Runnable r1 = new InnerRunnable(lHandler);
			lHandler.post(r1);

			// ensure wifi is open
			if (!wm.isWifiEnabled()) {

				wm.setWifiEnabled(true);
			}

			wm.startScan();
			results = wm.getScanResults();
			WifiInfo info = wm.getConnectionInfo();
			strength = info.getRssi();
			speed = info.getLinkSpeed();
			// String MACString = info.getMacAddress();
			// Log.d(TAG, "MAC ADDRESS:" + MACString);

		}

	};

	// mHandler is a manager inside lHndler, it's responsible for counting scan
	// times
	private class InnerRunnable implements Runnable {
		Handler mHandler;

		// int scanCnt = 1;

		public InnerRunnable(Handler handler) {
			mHandler = handler;
		}

		// scan all around wifi APs 0.5second a time till 30 times
		// insert data into sqlite database
		@Override
		public void run() {
			db = helper.getWritableDatabase();
			wm.startScan();
			results = wm.getScanResults();
			data = "";

			for (int i = 0; i < results.size(); i++) {

				data += results.get(i).BSSID + "\n" + results.get(i).SSID
						+ "\n" + results.get(i).level + "\n";
				apMac = results.get(i).BSSID;
				apId = results.get(i).SSID;
				level = results.get(i).level;

				ContentValues values = new ContentValues();
				values.put("Scan_Id", scanCnt);
				values.put("Mac_Address", apMac);
				values.put("Ap_Name", apId);
				values.put("RSS", level);
				values.put("Location", pos);
				values.put("Sequence", Sequence);
				// values.put("Device_Id", MACString);//or iMeiString

				db = helper.getWritableDatabase();
				db.insert(DBHelper._TableName, null, values);

			}
			db.close();

			if (scanCnt < TIME) {
				scanCnt++;
				mHandler.postDelayed(this, 500);
				Log.d(TAG, "scan" + scanCnt + "sequence" + Sequence);
			}
			if (scanCnt == TIME) {
				convert();
				try {
					Thread.sleep(2000); // wait for loading finished
					Log.d(TAG, "sleep()");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				scanCnt = 1; // after sleeping, keep scan and upload cycle
				Sequence = Sequence + 1;
			}

		}

	}

	private void convert() {

		Cursor cursor;
		File file = new File(getFilesDir(), TEMP_FILE_NAME);
		SQLiteDatabase readDatabase = helper.getReadableDatabase();
		cursor = readDatabase.rawQuery(
				String.format("SELECT * FROM %s", DBHelper._TableName), null);
		XmlBuilder mXmlBuilder = new XmlBuilder(cursor, file);
		mXmlBuilder.converToXmlFile();
		upload();

	}

	/*
	 * private void getLocationInfo(Location location) { if (location != null) {
	 * lat = location.getLatitude(); lng = location.getLongitude(); Log.d(TAG,
	 * "GPS:" + lat + " " + lng); } else { Log.d(TAG, "NO LOCATION FOUND"); }
	 * 
	 * }
	 */

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDestroy");
		if (db != null)
			db.close();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		this.locationManager.removeUpdates(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		/*
		 * String provider = this.locationManager.getBestProvider(new
		 * Criteria(), true); if (provider == null) { Log.d(TAG,
		 * "provider = null"); return; }
		 * this.locationManager.requestLocationUpdates(provider, 0, 0, this);
		 * Location location =
		 * this.locationManager.getLastKnownLocation(provider); if (location ==
		 * null) { Log.d(TAG, "location == null"); return; }
		 * this.onLocationChanged(location);
		 */
	}

	// build a file store cursor results
	static class XmlBuilder {

		// private static final String XML_OPENING =
		// "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
		private Cursor mCursor;
		private File mFileXmlFile;

		public XmlBuilder(Cursor cursor, File file) {
			this.mCursor = cursor;
			this.mFileXmlFile = file;
		}

		// the cursor results append tags for xml
		protected void converToXmlFile() {
			Log.d(TAG, "onConvert");
			StringBuilder mBuilder = new StringBuilder();
			// mBuilder.append(XmlBuilder.XML_OPENING);
			mBuilder.append(openTag(DBHelper.WIFIRECORDS));
			mBuilder.append("\n");
			while (mCursor.moveToNext()) {
				// string every column, add tags between them
				String ColumnOne = mCursor.getString(0);
				String ColumnTwo = mCursor.getString(1);
				String ColumnThree = mCursor.getString(2);
				String ColumnFour = mCursor.getString(3);
				String ColumnFive = mCursor.getString(4);
				String ColumnSix = mCursor.getString(5);
				// String columnSeven = mCursor.getString(6);

				mBuilder.append(openTag(TUPLENAME));
				mBuilder.append("\n");

				mBuilder.append("\t" + openTag(DBHelper.SEQUENCE));
				mBuilder.append(ColumnOne);
				mBuilder.append(endTag(DBHelper.SEQUENCE));
				mBuilder.append("\n");

				mBuilder.append("\t" + openTag(DBHelper.SCAN_ID));
				mBuilder.append(ColumnTwo);
				mBuilder.append(endTag(DBHelper.SCAN_ID));
				mBuilder.append("\n");

				mBuilder.append("\t" + openTag(DBHelper.MAC_ADDRESS));
				mBuilder.append(ColumnThree);
				mBuilder.append(endTag(DBHelper.MAC_ADDRESS));
				mBuilder.append("\n");

				mBuilder.append("\t" + openTag(DBHelper.AP_NAME));
				mBuilder.append(ColumnFour);
				mBuilder.append(endTag(DBHelper.AP_NAME));
				mBuilder.append("\n");

				mBuilder.append("\t" + openTag(DBHelper.RSS));
				mBuilder.append(ColumnFive);
				mBuilder.append(endTag(DBHelper.RSS));
				mBuilder.append("\n");

				mBuilder.append("\t" + openTag(DBHelper.LOCATION));
				mBuilder.append(ColumnSix);
				mBuilder.append(endTag(DBHelper.LOCATION));
				mBuilder.append("\n");

				// mBuilder.append("\t" + openTag(DBHelper.DEVICE_ID));
				// mBuilder.append(columnSeven);
				// mBuilder.append(endTag(DBHelper.DEVICE_ID));
				// mBuilder.append("\n");

				mBuilder.append(endTag(TUPLENAME));
				mBuilder.append("\n");
			}
			mBuilder.append(endTag(DBHelper.WIFIRECORDS));
			// Log.v(TAG, mBuilder.toString());
			// append end, results save to file
			saveToFile(mBuilder.toString());
		}

		// write the append results into mFileXmlFile
		private void saveToFile(String dataxml) {
			try {
				FileWriter fileWriter = new FileWriter(mFileXmlFile);
				fileWriter.write(dataxml);
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		static String openTag(String tag) {
			return "<" + tag + ">";
		}

		static String endTag(String tag) {
			return "</" + tag + ">";
		}

	}

	private void upload() {
		Intent intent = new Intent();
		intent.setClass(this, UploadIntentService.class);
		startService(intent);
		Log.d(TAG, "onUpload");

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		this.locateView.setText("" + location.getLatitude() + ","
				+ location.getLongitude());
		mProgressDialog.dismiss();
		lat = location.getLatitude();
		lng = location.getLongitude();
		Log.d(TAG,"GPS:"+String.valueOf(lat)+String.valueOf(lng));
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCancel(DialogInterface dialog) {
		// TODO Auto-generated method stub

	}

}
