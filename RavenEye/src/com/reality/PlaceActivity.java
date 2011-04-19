package com.reality;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.common.Coordinate;
import com.common.HttpBuilder;
import com.common.Place;

public class PlaceActivity extends ListActivity {

	public static final String TAG = "PlaceActivity";

	public static final int DIALOG_LOADING = 0;

	public static final String REMOTE_PLACE_IMAGE_DIR = "http://tailoredpages.com/raven/images/";

	private static final String NULL_STRING = "null";
	private static final String EMPTY_STRING = "";

	private static final int TYPE_ICON = 0;
	private static final int TYPE_NAME = 1;
	private static final int TYPE_DISTANCE = 2;
	private static final int TYPE_DESCRIPTION = 3;
	private static final int TYPE_BCODE = 4;
	private static final int TYPE_COORDINATES = 5;
	private static final int TYPE_ADDRESS = 6;
	private static final int TYPE_REVIEWS = 7;
	private static final int TYPE_RATING = 8;

	private Place mPlace = null;

	private final ArrayList<Data> mData = new ArrayList<Data>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.place_activity);

		final TextView title = (TextView) findViewById(R.id.title);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			Place place = null;
			try {
				place = (Place) extras.getSerializable(Place.class.toString());

				title.setText(place.name);

				mPlace = place;
			} catch (ClassCastException e) {
				finish();
			} finally {

			}
		}

		setListAdapter(new PlaceAdapter(mData));
		registerForContextMenu(getListView());
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	private List<Data> getPlace(URL url) {
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

				List<Data> data = new ArrayList<Data>();
				Place place = null;

				// This should only run once.
				for (int i = 0; i < len; ++i) {
					obj = placesArray.getJSONObject(i).getJSONObject("place");

					String id = obj.getString("ID");
					String name = obj.getString("Name");
					String address = obj.getString("Street") + "\n"
							+ obj.getString("City") + ", "
							+ obj.getString("PCode") + " "
							+ obj.getString("PostalCode") + "\n"
							+ obj.getString("Country");
					String description = obj.getString("Description");
					String code = obj.getString("BCode");
					String fileName = obj.getString("FileName");
					String reviewCount = obj.getString("ReviewCount");
					String rating = obj.getString("Rating");

					Coordinate coord;
					try {
						coord = new Coordinate(
								obj.getDouble("Latitude") / 1000000,
								obj.getDouble("Longitude") / 1000000);
					} catch (JSONException e) {
						coord = null;
					}

					place = new Place(Integer.parseInt(id), name, address,
							coord);
					place.updateWithLocation(getInitialLocation());

					place.description = description;
					code = !code.equals(NULL_STRING) ? code : null;
					place.buildingCode = code;
					if (!reviewCount.equals(NULL_STRING)) {
						place.setReviewCount(Integer.parseInt(reviewCount));
					}
					place.setRating(Float.parseFloat(rating));

					/*
					 * Add to data.
					 */
					if (!fileName.equals(NULL_STRING)) {
						place.setRemoteImageFileName(fileName);
						data.add(new Data(null, null, TYPE_ICON));
					} else if (place.getRemoteImageFileName() != null) {
						data.add(new Data(null, null, TYPE_ICON));
					}
					data.add(new Data("Distance", Place
							.getDistanceString(place.distance),
							R.drawable.ic_menu_directions, TYPE_DISTANCE));
					data.add(new Data("Rating", null, TYPE_RATING));
					data.add(new Data("Description", description,
							TYPE_DESCRIPTION));
					data.add(new Data("Address", address,
							R.drawable.ic_menu_mapmode, TYPE_ADDRESS));
					if (code != null) {
						data.add(new Data("Building Code", code, TYPE_BCODE));
					}
					if (coord != null) {
						data.add(new Data("Coordinates", coord.toString(),
								R.drawable.ic_menu_compass, TYPE_COORDINATES));
					}
					int reviews = place.getReviewCount();
					data.add(new Data("Reviews", reviews
							+ ((reviews == 1) ? " review" : " reviews"),
							R.drawable.ic_menu_edit, TYPE_REVIEWS));
				}

				// Closing the input stream will trigger connection release
				instream.close();

				return data;
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

	private class DownloadImageTask extends AsyncTask<Place, Void, Void> {

		protected Void doInBackground(Place... p) {
			ListView listview = PlaceActivity.this.getListView();

			Place place = p[0];
			if (place != null) {
				String remoteFileName = place.getRemoteImageFileName();
				if (remoteFileName != null) {
					BitmapDrawable bitmapDrawable = null;
					try {
						bitmapDrawable = new BitmapDrawable(
								BitmapFactory.decodeStream(new URL(
										"http://tailoredpages.com/raven/images/"
												+ remoteFileName).openStream()));
					} catch (MalformedURLException e) {

					} catch (IOException e) {

					} finally {
						if (bitmapDrawable != null) {
							place.setImageResource(bitmapDrawable);
						}
					}
				}
			}
			return null;
		}
	}

	/*
	 * List adapter.
	 */

	private static class TextViewHolder {
		ImageView icon;
		TextView title;
		TextView content;
		TextView bottom;
	}

	private static class IconViewHolder {
		ImageView icon;
	}

	private static class RatingViewHolder {
		ImageView icon;
		TextView title;
		RatingBar rating;
		TextView bottom;
	}

	private class PlaceAdapter extends EndlessAdapter<Data> {

		private RotateAnimation rotate = null;

		private LayoutInflater mInflater;

		private final Drawable mDefaultDrawable;

		private ImageLoader mImageLoader;

		public PlaceAdapter(ArrayList<Data> places) {
			super(new ArrayAdapter<Data>(PlaceActivity.this,
					R.layout.place_list_item, places));

			mDefaultDrawable = getResources().getDrawable(
					R.drawable.ic_popup_sync);

			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			rotate = new RotateAnimation(0f, 3600f, Animation.RELATIVE_TO_SELF,
					0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			rotate.setDuration(10000);
			rotate.setRepeatMode(Animation.RESTART);
			rotate.setRepeatCount(Animation.INFINITE);

			mImageLoader = new ImageLoader(getApplicationContext(),
					REMOTE_PLACE_IMAGE_DIR, mDefaultDrawable);

			new DownloadImageTask().execute(mPlace);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Data data = null;

			synchronized (this) {
				if (position < getItemCount()) {
					data = (Data) getItem(position);
				}
			}

			if (data != null) {
				switch (data.type) {
				case TYPE_ICON:
					IconViewHolder iconHolder;
					if (convertView == null
							|| !(convertView.getTag() instanceof IconViewHolder)) {
						iconHolder = new IconViewHolder();

						convertView = mInflater.inflate(
								R.layout.icon_list_item, null);

						iconHolder.icon = (ImageView) convertView
								.findViewById(R.id.icon);

						convertView.setTag(iconHolder);
					} else {
						iconHolder = (IconViewHolder) convertView.getTag();
					}

					mImageLoader.DisplayImage(mPlace.getRemoteImageFileName(),
							iconHolder.icon);
					break;
				case TYPE_RATING:
					RatingViewHolder ratingHolder;
					if (convertView == null
							|| !(convertView.getTag() instanceof RatingViewHolder)) {
						ratingHolder = new RatingViewHolder();

						convertView = mInflater.inflate(
								R.layout.rating_list_item, null);

						ratingHolder.title = (TextView) convertView
								.findViewById(R.id.title);
						ratingHolder.rating = (RatingBar) convertView
								.findViewById(R.id.rating);

						convertView.setTag(ratingHolder);
					} else {
						ratingHolder = (RatingViewHolder) convertView.getTag();
					}

					ratingHolder.title.setText(data.title);
					ratingHolder.rating.setRating(mPlace.getRating());
					break;
				default:
					TextViewHolder textHolder;
					if (convertView == null
							|| !(convertView.getTag() instanceof TextViewHolder)) {
						textHolder = new TextViewHolder();

						convertView = mInflater.inflate(
								R.layout.place_list_item, null);

						textHolder.icon = (ImageView) convertView
								.findViewById(R.id.icon);
						textHolder.title = (TextView) convertView
								.findViewById(R.id.title);
						textHolder.content = (TextView) convertView
								.findViewById(R.id.content);
						textHolder.bottom = (TextView) convertView
								.findViewById(R.id.bottom);

						convertView.setTag(textHolder);
					} else {
						textHolder = (TextViewHolder) convertView.getTag();
					}

					// Set the holder values.
					int visibility;
					if (data.drawable == 0) {
						visibility = View.GONE;
					} else {
						visibility = View.VISIBLE;

						textHolder.icon.setBackgroundResource(data.drawable);
					}
					textHolder.icon.setVisibility(visibility);
					textHolder.title.setText(data.title);
					textHolder.content.setText(data.content);
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
		protected List<Data> cacheInBackground() {
			try {
				// runOnUiThread(new Runnable() {
				// public void run() {
				// setProgressBarIndeterminateVisibility(true);
				// }
				// });

				URL aUrl = new URL(
						"http://tailoredpages.com/raven/places.php?format=json&place="
								+ mPlace.id + "&content=extended");
				if (aUrl != null) {
					return getPlace(aUrl);
				}
			} catch (MalformedURLException e) {

			}

			return null;
		}

		@Override
		protected void addCachedData(List<Data> data, boolean append) {
			@SuppressWarnings("unchecked")
			ArrayAdapter<Data> adapter = (ArrayAdapter<Data>) getWrappedAdapter();

			if (data != null) {
				mData.clear();

				int len = data.size();
				for (int i = 0; i < len; ++i) {
					adapter.add(data.get(i));
				}
			}

			// setProgressBarIndeterminateVisibility(false);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		@SuppressWarnings("unchecked")
		EndlessAdapter<Data> adapter = (EndlessAdapter<Data>) getListAdapter();
		if (position < adapter.getItemCount()) {
			Data data = (Data) adapter.getItem(position);
			switch (data.type) {
			case TYPE_ICON:
				if (((Data) adapter.getItem(position)).drawable != 0) {
					onPictureClick(null);
				} else {
					Toast.makeText(this, "No images available",
							Toast.LENGTH_SHORT).show();
				}
				break;
			case TYPE_DISTANCE:
				onDirectionsClick(null);
				break;
			case TYPE_ADDRESS:
			case TYPE_COORDINATES:
				onViewMapClick(null);
				break;
			case TYPE_REVIEWS:
				onWriteReviewClick(null);
				break;
			default:
			}
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == android.R.id.list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

			EndlessAdapter<Data> adapter = (EndlessAdapter<Data>) getListAdapter();
			Data data = (Data) adapter.getItem(info.position);

			if (data.type == TYPE_DISTANCE) {
				menu.setHeaderTitle("Directions");
				super.onCreateContextMenu(menu, v, menuInfo);
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.place_context_menu, menu);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.reality:
			onViewRealityClick(null);
			return true;
		case R.id.map:
			onViewMapClick(null);
			return true;
		case R.id.r_directions:
			onDirectionsClick(null);
			return true;
		case R.id.t_directions:
			onTraditionalDirectionsClick(null);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
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
		inflater.inflate(R.menu.place, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.directory:
			onDirectoryClick(null);
			return true;
		case R.id.reality:
			onViewRealityClick(null);
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

	public void onDirectionsClick(View v) {
		Intent intent = new Intent(getApplicationContext(),
				RealityActivity.class);

		intent.putExtra("type", RealityActivity.TYPE_DIRECTIONS);
		intent.putExtra(Place.class.toString(), new Object[] { mPlace });

		this.startActivity(intent);
	}

	public void onTraditionalDirectionsClick(View v) {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = lm.getProviders(true);
		Location location = null;

		for (int i = providers.size() - 1; i >= 0; i--) {
			location = lm.getLastKnownLocation(providers.get(i));
			if (location != null) {
				break;
			}
		}

		if (location != null) {
			StringBuilder url = new StringBuilder();
			url.append("http://maps.google.com/maps?f=d&hl=en");
			url.append("&daddr=");
			url.append(Double.toString(mPlace.coordinate.latitude));
			url.append(",");
			url.append(Double.toString(mPlace.coordinate.longitude));
			url.append("&dirflg=d&nav=1");
			url.append("&mode=walking");
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setData(Uri.parse(url.toString()));
			intent.addFlags(0x10800000);
			intent.setClassName("com.google.android.apps.m4ps",
					"com.google.android.maps.driveabout.app.NavigationActivity");
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				intent.setClassName("com.google.android.apps.maps",
						"com.google.android.maps.driveabout.app.NavigationActivity");
				try {
					startActivity(intent);
				} catch (ActivityNotFoundException e2) {

				}
			}
		}
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

	private class Data implements Comparable<Data> {

		public final String title;
		public final String content;
		public final int drawable;
		public final int type;

		public Data(String title, String content, int type) {
			this(title, content, 0, type);
		}

		public Data(String title, String content, int drawableId, int type) {
			this.title = title;
			this.content = content;
			this.drawable = drawableId;

			this.type = type;
		}

		public int compareTo(Data data) {
			return title.compareTo(data.title);
		}

	}

}
