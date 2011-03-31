package com.reality;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.common.HttpBuilder;
import com.common.Place;
import com.common.Review;

public class ReviewListActivity extends ListActivity {

	public static final String TAG = "ReviewListActivity";

	public static final int DIALOG_LOADING = 0;

	private EditText mReviewText;
	private Button mSubmitButton;

	private Place mPlace;
	private List<Review> mReviews;

	private ReviewAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.review_list_activity);

		mReviewText = (EditText) findViewById(R.id.review_text);
		mSubmitButton = (Button) findViewById(R.id.submit);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			try {
				mPlace = (Place) extras.getSerializable(Place.class.toString());
				if (mPlace != null) {
					mReviews = mPlace.getReviews(4);

					setTitle(getTitle() + " - " + mPlace.name);
				}
			} catch (ClassCastException e) {
				finish();
				return;
			}
		}

		if (mReviews == null) {
			mReviews = new ArrayList<Review>();
		}

		setListAdapter(new ReviewAdapter(mReviews));

		final ListView listView = getListView();
		listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

		registerForContextMenu(listView);
	}

	public void refresh() {
		try {
			new DownloadReviewsTask().execute(new URL(
					"http://tailoredpages.com/raven/reviews.php?format=json&limit=10&place="
							+ mPlace.id + "&content=basic"));
		} catch (MalformedURLException e) {

		}
	}

	private class DownloadReviewsTask extends
			AsyncTask<URL, Void, List<Review>> {
		protected List<Review> doInBackground(URL... url) {
			runOnUiThread(new Runnable() {
				public void run() {
					showDialog(DIALOG_LOADING);
				}
			});

			URL aUrl = url[0];
			if (aUrl != null) {
				return getReviews(aUrl);
			}

			return null;
		}

		protected void onPostExecute(final List<Review> reviews) {
			dismissDialog(DIALOG_LOADING);

			if (reviews != null) {
				synchronized (mReviews) {
					mReviews.clear();
					mReviews.addAll(reviews);

					mAdapter.notifyDataSetChanged();
				}
			}
		}

	}

	private List<Review> getReviews(URL url) {
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
				List<Review> items = new ArrayList<Review>();

				// A Simple JSON Response Read
				InputStream instream = entity.getContent();
				String result = HttpBuilder.convertStreamToString(instream);

				// A Simple JSONObject Creation
				JSONObject json = new JSONObject(result);
				// JSONObject placeObject = json.getJSONObject("posts");
				JSONArray placesArray = json.getJSONArray("reviews");

				int len = placesArray.length();
				JSONObject obj;
				for (int i = 0; i < len; i++) {
					obj = placesArray.getJSONObject(i).getJSONObject("review");

					String name = obj.getString("Name");
					String date = obj.getString("Date");
					String text = obj.getString("Text");

					items.add(new Review(name, text, date));
				}

				// A Simple JSONObject Value Pushing
				json.put("sample key", "sample value");
				Log.i(TAG, "<jsonobject>\n" + json.toString()
						+ "\n</jsonobject>");

				// Closing the input stream will trigger connection release
				instream.close();

				return items;
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
			// dialog.setCancelable(true);
			break;
		default:
			dialog = null;
		}
		return dialog;
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

	private class ReviewAdapter extends EndlessAdapter<Review> {

		private RotateAnimation rotate = null;
		private LayoutInflater mInflater;

		public ReviewAdapter(List<Review> items) {
			super(new ArrayAdapter<Review>(ReviewListActivity.this,
					R.layout.review_list_item, items));

			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF,
					0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			rotate.setDuration(2000);
			rotate.setRepeatMode(Animation.RESTART);
			rotate.setRepeatCount(Animation.INFINITE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			String name = null;
			String content = null;
			String date = null;

			Review review = null;

			synchronized (this) {
				if (position < mReviews.size()) {
					review = mReviews.get(position);
				}
			}

			if (review != null) {
				name = review.title;
				content = review.content;
				date = review.date;
			}

			if (v == null) {
				v = mInflater.inflate(R.layout.review_list_item, null);
			}

			TextView titleText = (TextView) v.findViewById(R.id.title);
			TextView contentText = (TextView) v.findViewById(R.id.content);
			TextView dateText = (TextView) v.findViewById(R.id.date);

			if (titleText != null)
				titleText.setText(name);
			if (contentText != null)
				contentText.setText(content);
			if (dateText != null)
				dateText.setText(date);

			return v;
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
		protected List<Review> cacheInBackground() {
			try {
				int offset = this.getDownloadOffset();
				int limit = offset + 10;

				URL aUrl = new URL(
						"http://tailoredpages.com/raven/places.php?format=json&offset="
								+ offset + "limit=" + limit);
				if (aUrl != null) {
					List<Review> data = getReviews(aUrl);

					return data;
				}
			} catch (MalformedURLException e) {

			}

			return null;
		}

		@Override
		protected void addCachedData(List<Review> data, boolean append) {
			@SuppressWarnings("unchecked")
			ArrayAdapter<Review> adapter = (ArrayAdapter<Review>) getWrappedAdapter();

			if (data != null) {
				if (!append) {
					// A refresh is requested.
					mReviews.clear();
				}

				int len = data.size();
				Review item;
				for (int i = 0; i < len; ++i) {
					item = data.get(i);

					adapter.add(item);
				}
			} else {
				// We have possibly reached the end of the data-set from the
				// web-service. Prevent the loading bar from continually
				// showing.
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.review, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refresh:
			refresh();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == android.R.id.list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

			Review r = (Review) mAdapter.getItem(info.position);

			menu.setHeaderTitle(r.title);
			super.onCreateContextMenu(menu, v, menuInfo);
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.review_context_menu, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		Review review = (Review) mAdapter.getItem(info.position);
		switch (item.getItemId()) {
		case R.id.report_review:
			onReportClick(review);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public void onReportClick(final Review review) {
		runOnUiThread(new Runnable() {

			public void run() {
				synchronized (this) {
					mReviews.remove(review);
				}

				mAdapter.notifyDataSetChanged();
			}

		});
	}

	public void onSubmitClick(final View v) {
		runOnUiThread(new Runnable() {

			public void run() {
				String reviewBody = mReviewText.getText().toString().trim();
				if (reviewBody.length() > 0) {
					Review review = new Review("Review X", reviewBody,
							new Date().toString());

					synchronized (this) {
						mReviews.add(review);
						mPlace.addReview(review);
					}

					mAdapter.notifyDataSetChanged();
					mReviewText.setText("");
					mReviewText.clearFocus();
					// mSubmitButton.setEnabled(false);
					// getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mReviewText.getWindowToken(), 0);

					// listView.scrollTo(0, listView..getBaseline());
				}
			}

		});
	}
}
