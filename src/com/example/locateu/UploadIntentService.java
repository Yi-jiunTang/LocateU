package com.example.locateu;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class UploadIntentService extends IntentService {
	// public static Boolean fromServer = false;
	private static final String TAG = UploadIntentService.class.getSimpleName();

	public UploadIntentService(String name) {
		super(name);
	}

	public UploadIntentService() {
		super(null);
	}

	@Override
	protected void onHandleIntent(Intent arg0) {
		upload();

	}

	public static String tempString = null;
	public static String wifiLatString = "";
	public static String wifiLngString = "";
	public static double wifiLat;
	public static double wifiLng;
	String temp1, temp2;

	void upload() {

		try {
			Log.d(TAG, "onUpload");

			HttpURLConnection httpUrlConnection = (HttpURLConnection) new URL(
					MainActivity.URI_API).openConnection();
			httpUrlConnection.setDoOutput(true);
			httpUrlConnection.setRequestMethod("POST");
			OutputStream os = httpUrlConnection.getOutputStream();
			// Thread.sleep(1000);

			BufferedInputStream fis = new BufferedInputStream(
					openFileInput("WifiRecord"));

			byte[] temp = new byte[1024 * 4]; // the common size of Internet
												// transmission
			int count;
			while ((count = fis.read(temp)) != -1) { // if the xmlFile is read
														// over, return-1
				os.write(temp, 0, count);
				// Log.d(TAG, "uploadService");
				// Log.v(TAG, new String(temp, 0, count));
			}

			fis.close();
			os.close();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					httpUrlConnection.getInputStream()));

			// accept the messages returned from server
			while ((tempString = in.readLine()) != null) {
				// fromServer = true;

				System.out.println(tempString);

				Matcher m = Pattern.compile("(\\d+.\\d+)").matcher(tempString);

				if (m.find())
					wifiLatString = m.group();
				if (m.find())
					wifiLngString = m.group();
				try {

					wifiLat = Double.parseDouble(wifiLatString);
					wifiLng = Double.parseDouble(wifiLngString);
					Log.d(TAG, wifiLat + "," + wifiLng);

				} catch (Exception e) {
					Log.e(TAG, "parseDouble failed");
				}

				initializeDb();

			}
			Handler msgHandler = new Handler(getMainLooper());
			msgHandler.post(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(getApplicationContext(),
							wifiLat + "," + wifiLng, Toast.LENGTH_LONG).show();
				}
			});
			in.close();
			fis.close();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {

		}
	}

	private void initializeDb() {
		// TODO Auto-generated method stub
		MainActivity.helper.getWritableDatabase().delete(DBHelper._TableName,
				null, null);
	}

}
