package com.activities;

import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.common.Coordinate;
import com.common.IntersectionWaypoint;
import com.common.LocationExporter;
import com.common.LocationWaypoint;
import com.common.Place;
import com.common.XmlLocationExporter;

public class MainActivity extends Activity {

	public static final int DIALOG_EXPORTING_GRAPH = 0;

	private boolean mIsBound = false;

	private TextView mCounter;
	private TextView mWaypointCounter;
	private EditText mFileName;
	private EditText mIntersectionName;
	private EditText mPlaceName;
	private Button mButtonPlace;
	private Button mButtonWaypoint;
	private Button mButtonIntersection;

	private ProgressDialog mProgressDialog;

	// TODO: Should not really have this in an activity... should be in the
	// service.
	private LocationExporter mLocationExporter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mCounter = (TextView) findViewById(R.id.counter);
		mWaypointCounter = (TextView) findViewById(R.id.waypoint_counter);
		mFileName = (EditText) findViewById(R.id.file_name);
		mIntersectionName = (EditText) findViewById(R.id.intersection_name);
		mPlaceName = (EditText) findViewById(R.id.place_name);
		mButtonPlace = (Button) findViewById(R.id.button_addplace);
		mButtonWaypoint = (Button) findViewById(R.id.button_addwaypoint);
		mButtonIntersection = (Button) findViewById(R.id.button_addintersection);

		mLocationExporter = new XmlLocationExporter();

		enableButtons(false);

		doBindService();
	}

	/*
	 * Location gathering service.
	 */

	private LocationGathererService mBoundService;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundService = ((LocationGathererService.LocalBinder) service)
					.getService();

			mCounter.setText(mBoundService.getCount() + "");
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
		}
	};

	void doBindService() {
		Intent intent = new Intent(MainActivity.this,
				LocationGathererService.class);

		// Start the service so that it is an on-going background service.
		// startService(intent);

		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		registerReceiver(mReceiver, new IntentFilter(
				LocationGathererService.BROADCAST_LOCATION));
	}

	@Override
	public void onStop() {
		super.onStop();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

	/*
	 * Creates the menu items.
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		return true;
	}

	/*
	 * Handles the menu item selections.
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.exit:
			String filename = mFileName.getText().toString();
			if (!filename.equals("")) {
				new ExportGraphTask().execute(filename);
			} else {
				Toast.makeText(this, "Please enter file name.",
						Toast.LENGTH_LONG);
			}
			break;
		default:
			return false;
		}
		return true;
	}

	/*
	 * Dialog methods.
	 */

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_EXPORTING_GRAPH:
			if (mProgressDialog == null) {
				mProgressDialog = new ProgressDialog(this);
				mProgressDialog
						.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mProgressDialog.setMessage("Exporting graph...");
				mProgressDialog.setCancelable(false);
				mProgressDialog.setMax(100);
			}
			dialog = mProgressDialog;
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private class ExportGraphTask extends AsyncTask<String, Integer, Boolean> {
		protected Boolean doInBackground(String... filename) {
			runOnUiThread(new Runnable() {

				public void run() {
					showDialog(DIALOG_EXPORTING_GRAPH);
				}

			});

			try {
				mLocationExporter.writeToFile(filename[0]);

				return true;
			} catch (IOException e) {
				Toast.makeText(MainActivity.this,
						"Error occured saving to file", Toast.LENGTH_LONG);
			}

			int count = 0;
			long totalSize = 0;
			for (int i = 0; i < count; i++) {
				totalSize += i;
				publishProgress((int) ((i / (float) count) * 100));
			}
			return false;
		}

		public void onProgressUpdate(Integer... progress) {
			mProgressDialog.setProgress(progress[0]);
		}

		protected void onPostExecute(Boolean result) {
			dismissDialog(DIALOG_EXPORTING_GRAPH);

			if (result) {
				MainActivity.this.finish();

				// There should be no bound clients anymore, but we need to
				// call
				// stopService since the service was created with
				// startService.
				stopService(new Intent(MainActivity.this,
						LocationGathererService.class));
			}
		}
	}

	private synchronized void enableButtons(boolean flag) {
		mButtonPlace.setEnabled(flag);
		mButtonWaypoint.setEnabled(flag);
		mButtonIntersection.setEnabled(flag);
	}

	/*
	 * Receives broadcasts from the bound service.
	 */
	protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action != null) {
				if (action.equals(LocationGathererService.BROADCAST_LOCATION)) {
					int count = intent.getIntExtra("count", 0);

					mCounter.setText(count + "");

					enableButtons(true); // we have a location
				}
			}
		}
	};

	private String addWaypoint(LocationWaypoint waypoint) {
		String key = mIntersectionName.getText().toString();
		if (key != null && !key.equals("")) {
			if (mLocationExporter.setCurrentWaypoint(key)) {
				Toast.makeText(this, "Current waypoint: " + key,
						Toast.LENGTH_LONG);
			} else {
				Toast.makeText(this, "Key not found: " + key, Toast.LENGTH_LONG);
			}
		}

		key = mLocationExporter.add(waypoint);

		mWaypointCounter.setText(mLocationExporter.size() + "");

		enableButtons(false);

		return key;
	}

	public void addPlaceClick(View view) {
		Coordinate coord = mBoundService.getLatestCoordinate();

		if (coord != null) {
			Place place = new Place(mPlaceName.getText().toString(), "", "",
					coord);
			LocationWaypoint waypoint = new LocationWaypoint(coord);
			waypoint.setPlace(place);

			addWaypoint(waypoint);
		}
	}

	public void addWaypointClick(View view) {
		Coordinate coord = mBoundService.getLatestCoordinate();

		if (coord != null) {
			LocationWaypoint waypoint = new LocationWaypoint(coord);

			addWaypoint(waypoint);
		}
	}

	public void addIntersectionClick(View view) {
		Coordinate coord = mBoundService.getLatestCoordinate();

		if (coord != null) {
			LocationWaypoint waypoint = new IntersectionWaypoint(coord);

			String key = addWaypoint(waypoint);

			Toast.makeText(this, "Intersection waypoint key: " + key,
					Toast.LENGTH_LONG);
		}
	}

	public void viewMapClick(View view) {
		Intent intent = new Intent(this, WaypointMapActivity.class);
		this.startActivity(intent);
	}

}