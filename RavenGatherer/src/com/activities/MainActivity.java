package com.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private boolean mIsBound = false;

	private TextView mCounter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mCounter = (TextView) findViewById(R.id.counter);

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

			// Tell the user about this for our demo.
			Toast.makeText(MainActivity.this, R.string.local_service_connected,
					Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
			Toast.makeText(MainActivity.this,
					R.string.local_service_disconnected, Toast.LENGTH_SHORT)
					.show();
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
			this.finish();

			// There should be no bound clients anymore, but we need to call
			// stopService since the service was created with
			// startService.
			stopService(new Intent(this, LocationGathererService.class));

			break;
		default:
			return false;
		}
		return true;
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
					// int id = intent.getIntExtra("id", -1);

					// Alarm alarm = (Alarm) intent
					// .getSerializableExtra(AlarmItem.class.toString());

					if (mCounter != null) {
						mCounter.setText(count + "");
					}
				}
			}
		}
	};

}