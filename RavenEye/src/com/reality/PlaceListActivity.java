package com.reality;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
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

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.common.Coordinate;
import com.common.HttpBuilder;
import com.common.Place;

public class PlaceListActivity extends ListActivity {

	public static final String TAG = "RealityActivity";

	public static final int DIALOG_LOADING = 0;

	private static final String NULL_STRING = "null";
	private static final String EMPTY_STRING = "";

	private final ArrayList<Place> mPlaces = new ArrayList<Place>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Request progress bar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.places_list_activity);

		setListAdapter(new PlaceAdapter(mPlaces));

		registerForContextMenu(getListView());

	}

	@Override
	public void onResume() {
		super.onResume();
	}
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

	private List<Place> getPlaces(URL url) {
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
				List<Place> places = new ArrayList<Place>();

				// A Simple JSON Response Read
				InputStream instream = entity.getContent();
				String result = HttpBuilder.convertStreamToString(instream);

				// A Simple JSONObject Creation
				JSONObject json = new JSONObject(result);
				// JSONObject placeObject = json.getJSONObject("posts");
				JSONArray placesArray = json.getJSONArray("places");

				int len = placesArray.length();
				JSONObject obj;
				Place place;
				for (int i = 0; i < len; i++) {
					obj = placesArray.getJSONObject(i).getJSONObject("place");

					String id = obj.getString("ID");
					String name = obj.getString("Name");
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
					place.buildingCode = !code.equals(NULL_STRING) ? code
							: null;
					places.add(place);
				}

				// A Simple JSONObject Value Pushing
				json.put("sample key", "sample value");
				Log.i(TAG, "<jsonobject>\n" + json.toString()
						+ "\n</jsonobject>");

				// Closing the input stream will trigger connection release
				instream.close();

				return places;
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

	public Drawable getDrawable(int id) {
		Drawable picture = null;
		try {
			picture = getResources().getDrawable(id);
		} catch (Resources.NotFoundException e) {

		}
		return picture;
	}

	// private SectionedAdapter mAdapter = new SectionedAdapter() {
	// protected View getHeaderView(String caption, int index,
	// View convertView, ViewGroup parent) {
	// TextView result = (TextView) convertView;
	//
	// if (convertView == null) {
	// result = (TextView) getLayoutInflater().inflate(
	// R.layout.list_header, null);
	// }
	//
	// result.setText(caption);
	//
	// return (result);
	// }
	// };

	private static class ViewHolder {
		ImageView icon;
		TextView top;
		TextView bottom;
		TextView bottomRight;
	}

	private class PlaceAdapter extends EndlessAdapter<Place> {

		private RotateAnimation rotate = null;

		private LayoutInflater mInflater;

		private final Bitmap mDefaultBitmap;

		public PlaceAdapter(ArrayList<Place> places) {
			super(new ArrayAdapter<Place>(PlaceListActivity.this,
					R.layout.places_list_item, places));

			mDefaultBitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.ic_menu_gallery);

			// Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
			// R.drawable.ic_menu_gallery), 50,
			// 50, true);

			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF,
					0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			rotate.setDuration(2000);
			rotate.setRepeatMode(Animation.RESTART);
			rotate.setRepeatCount(Animation.INFINITE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = mInflater
						.inflate(R.layout.places_list_item, null);

				holder = new ViewHolder();
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.top = (TextView) convertView.findViewById(R.id.top_text);
				holder.bottom = (TextView) convertView
						.findViewById(R.id.bottom_text);
				holder.bottomRight = (TextView) convertView
						.findViewById(R.id.bottom_right_text);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			Place place = null;

			synchronized (this) {
				if (position < this.getItemCount()) {
					place = (Place) this.getItem(position);
				}
			}

			if (place != null) {
				Bitmap bitmap;
				int resId = place.getImageResourceId();
				if (resId >= 0) {
					bitmap = BitmapFactory
							.decodeResource(getResources(), resId);
				} else {
					bitmap = mDefaultBitmap;
				}

				holder.icon.setImageBitmap(bitmap);
				holder.top.setText(place.name);
				float distance = place.distance;
				if (distance >= 0) {
					holder.bottom.setText("Distance: "
							+ Place.getDistanceString(distance));
				} else {
					holder.bottom.setText(EMPTY_STRING);
				}
				String buildingCode = place.buildingCode;
				if (buildingCode != null) {
					holder.bottomRight.setText(buildingCode);
				} else {
					holder.bottomRight.setText(EMPTY_STRING);
				}
			}

			return super.getView(position, convertView, parent);
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			View row = getLayoutInflater().inflate(R.layout.loading_list_item,
					null);

			row.setClickable(false);
			row.setEnabled(false);

			View child = row.findViewById(R.id.text);

			child = row.findViewById(R.id.throbber);
			child.setVisibility(View.VISIBLE);
			child.startAnimation(rotate);

			return (row);
		}

		@Override
		protected List<Place> cacheInBackground() {
			try {
				runOnUiThread(new Runnable() {
					public void run() {
						setProgressBarIndeterminateVisibility(true);
					}
				});

				URL aUrl = new URL(
						"http://tailoredpages.com/raven/places.php?format=json&offset="
								+ getDownloadOffset() + "&limit="
								+ getDownloadLimit());
				if (aUrl != null) {
					List<Place> data = getPlaces(aUrl);

					return data;
				}
			} catch (MalformedURLException e) {

			}

			return null;
		}

		@Override
		protected void addCachedData(List<Place> data, boolean append) {
			@SuppressWarnings("unchecked")
			ArrayAdapter<Place> adapter = (ArrayAdapter<Place>) getWrappedAdapter();

			if (data != null) {
				if (!append) {
					// A refresh is requested.
					mPlaces.clear();
				}

				Location location = getInitialLocation();

				int len = data.size();
				Place place;
				for (int i = 0; i < len; ++i) {
					place = data.get(i);

					place.updateWithLocation(location);
					adapter.add(place);
				}

				Collections.sort(mPlaces);
			} else {
				// We have possibly reached the end of the data-set from the
				// web-service. Prevent the loading bar from continually
				// showing.

			}

			setProgressBarIndeterminateVisibility(false);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		@SuppressWarnings("unchecked")
		EndlessAdapter<Place> adapter = (EndlessAdapter<Place>) getListAdapter();
		if (position < adapter.getItemCount()) {
			onOpenClick((Place) adapter.getItem(position));
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == android.R.id.list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			@SuppressWarnings("unchecked")
			EndlessAdapter<Place> adapter = (EndlessAdapter<Place>) getListAdapter();

			if (info.position < adapter.getItemCount()) {
				Place p = (Place) adapter.getItem(info.position);

				menu.setHeaderTitle(p.name);
				super.onCreateContextMenu(menu, v, menuInfo);
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.places_context_menu, menu);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		Place place = (Place) getListAdapter().getItem(info.position);
		switch (item.getItemId()) {
		case R.id.open:
			onOpenClick(place);
			return true;
		case R.id.reality:
			onRealityClick(place);
			return true;
		case R.id.map:
			onMapClick(place);
			return true;
		case R.id.directions:
			onDirectionsClick(place);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.places_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refresh:
			onRefreshClick(null);
			return true;
		case R.id.reality:
			onRealityClick(null);
			return true;
		case R.id.map:
			onMapClick(null);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onRefreshClick(View v) {
		((EndlessAdapter<?>) getListAdapter()).refresh();
	}

	public void onOpenClick(Place place) {
		Intent intent = new Intent(this, PlaceActivity.class);
		intent.putExtra(Place.class.toString(), place);

		this.startActivity(intent);
	}

	public void onRealityClick(Place place) {
		Intent intent = new Intent(this, RealityActivity.class);
		if (place != null) {
			intent.putExtra(Place.class.toString(), new Object[] { place });
		}
		this.startActivity(intent);
	}

	public void onMapClick(Place place) {
		Intent intent = new Intent(this, NavigationMapActivity.class);
		intent.putExtra(Place.class.toString(), mPlaces.toArray());

		this.startActivity(intent);
	}

	public void onDirectionsClick(Place place) {

	}

}