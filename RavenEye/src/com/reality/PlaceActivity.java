package com.reality;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.common.Coordinate;
import com.common.HttpBuilder;
import com.common.Place;

public class PlaceActivity extends Activity {

	public static final String TAG = "PlaceActivity";

	public static final int DIALOG_LOADING = 0;

	private Place mPlace = null;

	private TextView mName;
	private TextView mDescription;
	private TextView mBuildingCode;
	private TextView mDistance;
	private ImageView mImage;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.places_activity);

		mName = (TextView) findViewById(R.id.name);
		mDescription = (TextView) findViewById(R.id.description);
		mBuildingCode = (TextView) findViewById(R.id.building_code);
		mDistance = (TextView) findViewById(R.id.distance);
		mImage = (ImageView) findViewById(R.id.picture);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			Place place = null;
			try {
				place = (Place) extras.getSerializable(Place.class.toString());

				mPlace = place;
			} catch (ClassCastException e) {
				finish();
			} finally {
				if (place != null) {
					mName.setText(place.name);
					String description = place.description;
					if (description != null) {
						mDescription.setText(description);
					}
					mBuildingCode.setText(place.buildingCode);
					if (place.distance >= 0) {
						mDistance.setText(Place
								.getDistanceString(place.distance));
					} else {
						mDistance.setText("Unknown");
					}

					Drawable picture = place.getImageResource();
					if (picture == null) {
						try {
							picture = getResources().getDrawable(
									place.getImageResourceId());
						} catch (Resources.NotFoundException e) {
							picture = getResources().getDrawable(
									R.drawable.ic_menu_gallery);
						}
					}

					mImage.setImageDrawable(picture);

					refresh();
				}
			}
		}
	}

	public void refresh() {
		try {
			new DownloadPlaceTask().execute(new URL(
					"http://tailoredpages.com/raven/places.php?format=json&place="
							+ mPlace.id + "&content=extended"));
		} catch (MalformedURLException e) {

		}
	}

	private class DownloadPlaceTask extends AsyncTask<URL, Void, Place> {
		protected Place doInBackground(URL... url) {
			runOnUiThread(new Runnable() {
				public void run() {
					showDialog(DIALOG_LOADING);
				}
			});

			URL aUrl = url[0];
			if (aUrl != null) {
				return getPlace(aUrl);
			}

			return null;
		}

		protected void onPostExecute(final Place place) {
			try {
				dismissDialog(DIALOG_LOADING);
			} catch (IllegalArgumentException e) {

			}

			if (place != null) {
				synchronized (this) {
					Location location = getInitialLocation();

					place.updateWithLocation(location);

					mName.setText(place.name);
					String description = place.description;
					if (description != null) {
						mDescription.setText(description);
					}
					mBuildingCode.setText(place.buildingCode);
					if (place.distance >= 0) {
						mDistance.setText(Place
								.getDistanceString(place.distance));
					} else {
						mDistance.setText("Unknown");
					}
				}
			}
		}

	}

	private Place getPlace(URL url) {
		final HttpClient httpclient = new DefaultHttpClient();
		final HttpGet httpget = new HttpGet(url.toString());
		HttpResponse response;

		try {
			response = httpclient.execute(httpget);

			// Examine the response status.
			Log.i(TAG, response.getStatusLine().toString());

			// Get hold of the response entity.
			HttpEntity entity = response.getEntity();
			// If the response does not enclose an entity, there is no need
			// to worry about connection release.

			if (entity != null) {
				// A Simple JSON Response Read
				InputStream instream = entity.getContent();
				String result = HttpBuilder.convertStreamToString(instream);

				// A Simple JSONObject Creation
				JSONObject json = new JSONObject(result);
				// JSONObject placeObject = json.getJSONObject("posts");
				JSONArray placesArray = json.getJSONArray("places");

				int len = placesArray.length();
				JSONObject obj;
				Place place = null;

				for (int i = 0; i < len; ++i) {
					obj = placesArray.getJSONObject(i).getJSONObject("place");

					String id = obj.getString("ID");
					String name = obj.getString("Name");
					String description = obj.getString("Description");
					String code = obj.getString("Code");

					Coordinate coord;
					try {
						coord = new Coordinate(
								obj.getDouble("Latitude") / 1000000,
								obj.getDouble("Longitude") / 1000000);
					} catch (JSONException e) {
						coord = null;
					}

					place = new Place(Integer.parseInt(id), name, coord);
					place.description = description;
					place.buildingCode = code;

					break;
				}

				// A Simple JSONObject Value Pushing
				json.put("sample key", "sample value");
				Log.i(TAG, "<jsonobject>\n" + json.toString()
						+ "\n</jsonobject>");

				// Closing the input stream will trigger connection release
				instream.close();

				return place;
			}

		} catch (ClientProtocolException e) {
			Log.e(TAG, e.toString());
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		} catch (final JSONException e) {
			Log.e(TAG, e.toString());
		}

		return null;
	}

	/**
	 * Get last known location.
	 */
	private Location getInitialLocation() {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = lm.getProviders(true);
		Location location = null;

		for (int i = providers.size() - 1; i >= 0; i--) {
			location = lm.getLastKnownLocation(providers.get(i));
			if (location != null) {
				break;
			}
		}

		return location;
	}

	/*
	 * Dialog methods.
	 */

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_LOADING:
			dialog = ProgressDialog.show(this, null, "Loading. Please wait...");
			dialog.setCancelable(true);
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.places, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.directory:
			onDirectoryClick(null);
			return true;
		case R.id.gallery:
			onPictureClick(null);
			return true;
		case R.id.review:
			onWriteReviewClick(null);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onDirectoryClick(View v) {
		this.startActivity(new Intent(this, PlaceListActivity.class));
	}

	public void onViewRealityClick(View v) {
		Intent intent = new Intent(this, RealityActivity.class);
		intent.putExtra(Place.class.toString(), new Object[] { mPlace });

		this.startActivity(intent);
	}

	public void onViewMapClick(View v) {
		final Place[] list = new Place[] { mPlace };

		Intent intent = new Intent(this, NavigationMapActivity.class);
		intent.putExtra(Place.class.toString(), list);

		this.startActivity(intent);
	}

	public void onGetDirectionsClick(View v) {

	}

	public void onAddToFavouritesClick(View v) {

	}

	public void onWriteReviewClick(View v) {
		Intent intent = new Intent(this, ReviewListActivity.class);
		intent.putExtra(Place.class.toString(), mPlace);

		this.startActivity(intent);
	}

	public void onPictureClick(View v) {
		Intent intent = new Intent(this, PictureFrameActivity.class);
		intent.putExtra(Place.class.toString(), mPlace);

		this.startActivity(intent);
	}

}
