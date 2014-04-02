package com.example.locateu;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.PrivateCredentialPermission;

import android.R.integer;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import static java.lang.Double.isNaN;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.lang.Math.round;
import java.text.NumberFormat;

public class MainActivity extends FragmentActivity implements LocationListener,
		OnCancelListener, SensorEventListener {
	private static GoogleMap map;
	private LocationManager locationManager;
	Location location;
	// private ProgressDialog mProgressDialog;
	public static String URI_API = "http://140.116.179.11/wifi_project/wifi_positioning.php";
	public static final String TEMP_FILE_NAME = "WifiRecord";
	private static final String TUPLENAME = "WifiRecord";
	private static final String TAG = "MyActivity";
	public static final int TIME = 10;

	private Button locateMeButton;
	private EditText initialPos;
	// private TextView locateView;
	public WifiManager wm;
	private SensorManager mSensorManager;
	private SensorManager mSensorManager2;
	private Sensor accSensor;
	private Sensor gyroSensor;
	private boolean mInitialized;
	private float gravity[] = new float[3];
	private float gyro[] = new float[3];
	private float angle[] = new float[3];
	WifiManager.WifiLock unlock;// avoid wifi falling sleep
	private List<ScanResult> results;
	private SQLiteDatabase db = null;
	public static DBHelper helper = null;
	public static int scanCnt;
	// Noise higher, step counter is less sensitive
	private final float NOISE = (float) 1.0;
	private static final float NS2S = 1.0f / 1000000000.0f;
	private final float[] deltaRotationVector = new float[4];
	private float timestamp;
	public int stepsCount; // distance
	public double mLastX;
	public double mLastY;
	public double mLastZ;
	public float axisX, axisY, axisZ;
	// boolean POS = false;
	double lat, lng;
	public double calLat, calLng, traceLat, traceLng;
	public double gpslat, gpslng, azimuth;
	public static double a = 6378137;
	public static double b = 6356752.3142;
	public static double f = 1 / 298.257223563; // WGS-84 ellipsiod
	public double alpha1, sinAlpha1, cosAlpha1, tanU1, cosU1, sinU1, sigma1,
			sinAlpha, cosSqAlpha, uSq, A, B, cos2SigmaM, sinSigma, cosSigma,
			deltaSigma, sigmaP, sigma;
	public String _DBname = "wifiData.db";
	private TelephonyManager telephonyManager;
	int strength;
	int speed;
	int Sequence;
	int level;
	int i;
	public static int pos;
	String data, apMac, apId;
	String otherwifi, iMEIString, MACString;
	Thread mapLayout;
	private HandlerThread markerThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		addGoogleMap();

		wm = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		locateMeButton = (Button) findViewById(R.id.locate);
		locateMeButton.setOnClickListener(locateButtonClickListener);
		initialPos = (EditText) findViewById(R.id.initialPos);
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
		mInitialized = false;
		mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
		gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Log.d(TAG, "onCreate");

	}

	private void addGoogleMap() {
		// TODO Auto-generated method stub
		int status = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(getBaseContext());
		if (status != ConnectionResult.SUCCESS) {
			int requestCode = 10;
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this,
					requestCode);
			dialog.show();
		} else {
			map = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			map.setMyLocationEnabled(true);
			map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

			locationManager = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);
			// Criteria criteria = new Criteria();
			// String provider = locationManager.getBestProvider(criteria,
			// true);
			Location location = locationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (location != null) {
				onLocationChanged(location);
			}
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, this);

		}

	}

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
			pos = Integer.parseInt(initialPos.getText().toString());
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

				if (UploadIntentService.wifiLat != 0) {
					traceLat = UploadIntentService.wifiLat;
					traceLng = UploadIntentService.wifiLng;
				} else {
					traceLat = gpslat;
					traceLng = gpslng;
				}
				alpha1 = Math.toRadians(azimuth);
				sinAlpha1 = Math.sin(alpha1);
				cosAlpha1 = Math.cos(alpha1);
				tanU1 = (1 - f) * Math.tan(Math.toRadians(traceLat));
				cosU1 = 1 / Math.sqrt((1 + tanU1 * tanU1));
				sinU1 = tanU1 * cosU1;
				sigma1 = Math.atan2(tanU1, cosAlpha1);
				sinAlpha = cosU1 * sinAlpha1;
				cosSqAlpha = 1 - sinAlpha * sinAlpha;
				uSq = cosSqAlpha * (a * a - b * b) / (b * b);
				A = 1 + uSq / 16384
						* (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
				B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
				double sigma = stepsCount * 0.2 / (b * a);
				double sigmaP = 2 * Math.PI;
				while (Math.abs(sigma - sigmaP) > 1e-12) {
					cos2SigmaM = Math.cos(2 * sigma1 + sigma);
					sinSigma = Math.sin(sigma);
					cosSigma = Math.cos(sigma);
					deltaSigma = B
							* sinSigma
							* (cos2SigmaM + B
									/ 4
									* (cosSigma
											* (-1 + 2 * cos2SigmaM * cos2SigmaM) - B
											/ 6
											* cos2SigmaM
											* (-3 + 4 * sinSigma * sinSigma)
											* (-3 + 4 * cos2SigmaM * cos2SigmaM)));
					sigmaP = sigma;
					sigma = stepsCount * 0.2 / (b * A) + deltaSigma;
				}
				double tmp = sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1;
				calLat = Math.atan2(sinU1 * cosSigma + cosU1 * sinSigma
						* cosAlpha1,
						(1 - f) * Math.sqrt(sinAlpha * sinAlpha + tmp * tmp));
				double lambda = Math.atan2(sinSigma * sinAlpha1, cosU1
						* cosSigma - sinU1 * sinSigma * cosAlpha1);
				double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
				double L = lambda
						- (1 - C)
						* f
						* sinAlpha
						* (sigma + C
								* sinSigma
								* (cos2SigmaM + C * cosSigma
										* (-1 + 2 * cos2SigmaM * cos2SigmaM)));
				calLng = (Math.toRadians(traceLng) + L + 3 * Math.PI)
						% (2 * Math.PI) - Math.PI;
				calLat = Math.toDegrees(calLat);
				calLng = Math.toDegrees(calLng);
				stepsCount = 0; // initialize the distance
				ContentValues values = new ContentValues();
				values.put("Scan_Id", scanCnt);
				values.put("Mac_Address", apMac);
				values.put("Ap_Name", apId);
				values.put("RSS", level);
				values.put("Sequence", Sequence);
				values.put("Latitude", gpslat);
				values.put("Longitude", gpslng);
				values.put("Gyro_x", axisX);
				values.put("Gyro_y", axisY);
				values.put("Gyro_z", axisZ);
				values.put("Step_Frequency", 0.5/stepsCount);
				values.put("Move_Latitude", calLat);
				values.put("Move_Longitude", calLng);
				
				// values.put("Device_Id", MACString);//or iMeiString

				db = helper.getWritableDatabase();
				db.insert(DBHelper._TableName, null, values);

			}
			db.close();

			if (scanCnt < TIME) {
				scanCnt++;
				mHandler.postDelayed(this, 500);
				Log.d(TAG, "scan" + scanCnt + "sequence" + Sequence + " "+pos);
//						+ calLat + calLng + ";;" + gpslat + gpslng);

			}
			if (scanCnt == TIME) {
				convert();
				try {
					Thread.sleep(15000); // wait for loading finished
					Log.d(TAG, "sleep()");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				scanCnt = 0; // after sleeping, keep scan and upload cycle
				Sequence = Sequence + 1;

			}
			mHandler.post(marker2);
		}

	}

	private Runnable marker2 = new Runnable() {

		private ArrayList<LatLng> traceMe;

		@Override
		public void run() {
			if (UploadIntentService.wifiLat != 0.0) {
				lat = UploadIntentService.wifiLat;
				lng = UploadIntentService.wifiLng;

				map.addMarker(new MarkerOptions()
						.position(new LatLng(lat, lng))
						.icon(BitmapDescriptorFactory
								.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
				if (traceMe == null) {
					traceMe = new ArrayList<LatLng>();
				}

				traceMe.add(new LatLng(lat, lng));
				PolylineOptions polylineOptions = new PolylineOptions();
				for (LatLng latlng : traceMe) {
					polylineOptions.add(latlng);
				}
				polylineOptions.color(Color.YELLOW);
				Polyline line = map.addPolyline(polylineOptions);
				line.setWidth(3);
			}
			// if (!POS) {
			// if (UploadIntentService.wifiLat == 0.0) {
			// POS = false;
			// Log.d(TAG, "FROM SERVER 0");
			// } else {
			// Log.d(TAG, "FROM SERVER FIRST");
			// p1 = UploadIntentService.wifiLat;
			// q1 = UploadIntentService.wifiLng;
			// POS = true;
			// map.addMarker(new MarkerOptions()
			// .position(new LatLng(p1, q1))
			// .icon(BitmapDescriptorFactory
			// .defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
			// }
			// } if (POS) {
			// Log.d(TAG, "FROM SERVER NEW");
			// p2 = UploadIntentService.wifiLat;
			// q2 = UploadIntentService.wifiLng;
			// // map.addMarker(new MarkerOptions()
			// // .position(new LatLng(p2, q2))
			// // .icon(BitmapDescriptorFactory
			// // .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
			//
			// map.addPolyline(new PolylineOptions()
			// .add(new LatLng(p1, q1), new LatLng(p2, q2)).width(2)
			// .color(Color.BLUE));
			// }
		}
	};

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

		if (markerThread != null) {
			markerThread.quit();
		}
	}

	@Override
	protected void onPause() {
		this.locationManager.removeUpdates(this);
		mSensorManager.unregisterListener(this);

		super.onPause();
		Log.d(TAG, "onPause");

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
		mSensorManager.registerListener(mSensorListener, accSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(mSensorListener, gyroSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		Log.d(TAG, "onResume");

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
			
			mBuilder.append("\t" + openTag("First_RP_ID"));
			mBuilder.append(pos);
			mBuilder.append(endTag("First_RP_ID"));
			mBuilder.append("\n");
			
			while (mCursor.moveToNext()) {
				// string every column, add tags between them
				String ColumnOne = mCursor.getString(0);
				String ColumnTwo = mCursor.getString(1);
				String ColumnThree = mCursor.getString(2);
				String ColumnFour = mCursor.getString(3);
				String ColumnFive = mCursor.getString(4);
				String ColumnSix = mCursor.getString(5);
				String ColumnSeven = mCursor.getString(6);
				String ColumnNine = mCursor.getString(7);
				String ColumnTen = mCursor.getString(8);
				String ColumnEleven = mCursor.getString(9);
				String ColumnTwelve = mCursor.getString(10);
				String ColumnThirteen = mCursor.getString(11);
				String ColumnFourteen = mCursor.getString(12);
				

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

				mBuilder.append("\t" + openTag(DBHelper.LATITUDE));
				mBuilder.append(ColumnSix);
				mBuilder.append(endTag(DBHelper.LATITUDE));
				mBuilder.append("\n");

				mBuilder.append("\t" + openTag(DBHelper.LONGITUDE));
				mBuilder.append(ColumnSeven);
				mBuilder.append(endTag(DBHelper.LONGITUDE));
				mBuilder.append("\n");

				mBuilder.append("\t" + openTag(DBHelper.GYRO_X));
				mBuilder.append(ColumnNine);
				mBuilder.append(endTag(DBHelper.GYRO_X));
				mBuilder.append("\n");

				mBuilder.append("\t" + openTag(DBHelper.GYRO_Y));
				mBuilder.append(ColumnTen);
				mBuilder.append(endTag(DBHelper.GYRO_Y));
				mBuilder.append("\n");

				mBuilder.append("\t" + openTag(DBHelper.GYRO_Z));
				mBuilder.append(ColumnEleven);
				mBuilder.append(endTag(DBHelper.GYRO_Z));
				mBuilder.append("\n");
				
				mBuilder.append("\t" + openTag(DBHelper.STEP_FREQUENCY));
				mBuilder.append(ColumnTwelve);
				mBuilder.append(endTag(DBHelper.STEP_FREQUENCY));
				mBuilder.append("\n");
				
				mBuilder.append("\t" + openTag(DBHelper.MOVE_LATITUDE));
				mBuilder.append(ColumnThirteen);
				mBuilder.append(endTag(DBHelper.MOVE_LATITUDE));
				mBuilder.append("\n");
				
				mBuilder.append("\t" + openTag(DBHelper.MOVE_LONGITUDE));
				mBuilder.append(ColumnThirteen);
				mBuilder.append(endTag(DBHelper.MOVE_LONGITUDE));
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

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub

		// this.locateView.setText("" + location.getLatitude() + ","
		// + location.getLongitude());
		// mProgressDialog.dismiss();
		Log.d(TAG, "onChange()");

		gpslat = location.getLatitude();
		gpslng = location.getLongitude();
		LatLng latLng = new LatLng(gpslat, gpslng);
		map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
		map.animateCamera(CameraUpdateFactory.zoomTo(18));

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

	// mSensorListener = new SensorEventListener() {
	private SensorEventListener mSensorListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			synchronized (this) {
				float[] values = event.values;
				if (event.sensor == gyroSensor) {
					// Log.d(TAG, "GYROSCOPE");
					if (timestamp != 0) {
						final float dT = (event.timestamp - timestamp) * NS2S;
						// Axis of the rotation sample, not normalized yet.
						axisX = event.values[0];
						axisY = event.values[1];
						axisZ = event.values[2];

						// Calculate the angular speed of the sample
						float omegaMagnitude = (float) Math.sqrt(axisX * axisX
								+ axisY * axisY + axisZ * axisZ);

						float EPSILON = 1;
						// Normalize the rotation vector if it's big enough to
						// get the axis
						// (that is, EPSILON should represent your maximum
						// allowable margin of error)
						/*
						 * if (omegaMagnitude > EPSILON) { axisX /=
						 * omegaMagnitude; axisY /= omegaMagnitude; axisZ /=
						 * omegaMagnitude; }
						 */

						// Integrate around this axis with the angular speed by
						// the timestep
						// in order to get a delta rotation from this sample
						// over the timestep
						// We will convert this axis-angle representation of the
						// delta rotation
						// into a quaternion before turning it into the rotation
						// matrix.
						float thetaOverTwo = omegaMagnitude * dT / 2.0f;
						float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
						float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
						deltaRotationVector[0] = sinThetaOverTwo * axisX;
						deltaRotationVector[1] = sinThetaOverTwo * axisY;
						deltaRotationVector[2] = sinThetaOverTwo * axisZ;
						deltaRotationVector[3] = cosThetaOverTwo;

					}
					timestamp = event.timestamp;
					float[] deltaRotationMatrix = new float[9];
					SensorManager.getRotationMatrixFromVector(
							deltaRotationMatrix, deltaRotationVector);

					// Log.d(TAG, "gyro:" + axisX + " , " + axisY + " , " +
					// axisZ);

					// Log.d(TAG, "Ori" + angle[0] + "," + angle[1] + ","
					// + angle[2]);
				}
				if (event.sensor == accSensor) {
					// Log.d(TAG, "ACCELERATE");
					// event object contains values of acceleration, read those
					double x = event.values[0];
					double y = event.values[1];
					double z = event.values[2];

					/*
					 * determine the orientation here
					 */
					if (x <= 1 & x >= 0.10) {
						// Log.d(TAG, "0~45 left");
						azimuth = -22.5;
					}
					if (x < -0.20 & x >= -1) {
						// Log.d(TAG, "0~45 right");
						azimuth = 22.5;
					}
					if (x < -1 & x >= -2) {
						// Log.d(TAG, "45~90 right");
						azimuth = 50;
					}
					if (x <= 2 & x > 1) {
						// Log.d(TAG, "45~90 left");
						azimuth = -50;
					}
					// Log.d(TAG, "X-axis: " + event.values[0]);
					// alpha is calculated as t / (t + dT)
					// with t, the low-pass filter's time-constant
					// and dT, the event delivery rate

					final double alpha = 0.8; // constant for our filter below

					double[] gravity = { 0, 0, 0 };

					// Isolate the force of gravity with the low-pass filter.
					gravity[0] = alpha * gravity[0] + (1 - alpha)
							* event.values[0];
					gravity[1] = alpha * gravity[1] + (1 - alpha)
							* event.values[1];
					gravity[2] = alpha * gravity[2] + (1 - alpha)
							* event.values[2];

					// Remove the gravity contribution with the high-pass
					// filter.
					x = event.values[0] - gravity[0];
					y = event.values[1] - gravity[1];
					z = event.values[2] - gravity[2];
					if (!mInitialized) {
						// sensor is used for the first time, initialize the
						// last read
						// values
						mLastX = x;
						mLastY = y;
						mLastZ = z;
						mInitialized = true;
					} else {
						// sensor is already initialized, and we have previously
						// read
						// values.
						// take difference of past and current values and decide
						// which
						// axis acceleration was detected by comparing values

						double deltaX = Math.abs(mLastX - x);
						double deltaY = Math.abs(mLastY - y);
						double deltaZ = Math.abs(mLastZ - z);

						if (deltaX < NOISE)
							deltaX = (float) 0.0;
						if (deltaY < NOISE)
							deltaY = (float) 0.0;
						if (deltaZ < NOISE)
							deltaZ = (float) 0.0;
						mLastX = x;
						mLastY = y;
						mLastZ = z;
						if (deltaX > deltaY) {
							// Horizontal shake
							// do something here if you like

						} else if (deltaY > deltaX) {
							// Vertical shake
							// do something here if you like

						} else if ((deltaZ > deltaX) && (deltaZ > deltaY)) {
							// Z shake
							stepsCount = stepsCount + 1;
							if (stepsCount > 0) {

								Log.d(TAG, String.valueOf(stepsCount));
							}
						}
					}
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	};

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

	}

}
