package com.reality;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.common.Coordinate;
import com.common.Place;

public class PlacesListActivity extends ListActivity {

	public static final String TAG = "PlacesListActivity";

	public static final int DIALOG_LOADING = 0;

	private ArrayList<Place> mPlaces = new ArrayList<Place>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.places_list_activity);

		mAdapter.addSection("Places", new PlaceAdapter(this, mPlaces));

		setListAdapter(mAdapter);

		registerForContextMenu(getListView());
	}

	@Override
	public void onResume() {
		super.onResume();

		onRefreshClick(null);
	}

	private class DownloadPlacesTask extends AsyncTask<URL, Void, List<Place>> {
		protected List<Place> doInBackground(URL... url) {
			runOnUiThread(new Runnable() {
				public void run() {
					showDialog(DIALOG_LOADING);
				}
			});

			URL aUrl = url[0];
			if (aUrl != null) {
				return getPlaces(aUrl);
			}

			return null;
		}

		protected void onPostExecute(final List<Place> places) {
			dismissDialog(DIALOG_LOADING);

			if (places != null) {
				synchronized (mPlaces) {
					Location location = getInitialLocation();

					int len = places.size();
					for (int i = 0; i < len; ++i) {
						places.get(i).updateWithLocation(location);
					}

					mPlaces.clear();
					mPlaces.addAll(places);

					mAdapter.notifyDataSetChanged();
				}
			}
		}

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
				String result = convertStreamToString(instream);

				// A Simple JSONObject Creation
				JSONObject json = new JSONObject(result);
				// JSONObject placeObject = json.getJSONObject("posts");
				JSONArray placesArray = json.getJSONArray("posts");

				int len = placesArray.length();
				JSONObject obj;
				for (int i = 0; i < len; i++) {
					obj = placesArray.getJSONObject(i).getJSONObject("post");

					String name = obj.getString("Name");
					String description = obj.getString("Description");

					Coordinate coord;
					try {
						coord = new Coordinate(
								obj.getDouble("Latitude") / 1000000,
								obj.getDouble("Longitude") / 1000000);
					} catch (JSONException e) {
						coord = null;
					}

					places.add(new Place(name, description, null, coord));
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

	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
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
	 * private void getPlaces() { try { Coordinate coordinate = new
	 * Coordinate(45.309485, -75.90909); Coordinate coordinate2 = new
	 * Coordinate(45.309825, -75.94009);
	 * 
	 * mDownloadedBuildings .add(new Place( "Herzberg Laboratories",
	 * "The Herzberg Laboratories for Physics and Computer Science was named for Gerhard Herzberg, Canada’s first Nobel Prize recipient for natural sciences, and Carleton’s former Chancellor.\n\nThe building houses the School of Computer Science, the School of Mathematics and Statistics, the Department of Physics, Department of Earth Sciences (including their $10 million POLARIS project), the Environmental Science program, Integrated Science program, and the Faculty of Science Dean’s Office.\n\nA roof-top observatory features a powerful star-gazing 14″ reflecting Celestron telescope."
	 * , "HP", coordinate2) .setImageResourceId(R.drawable.herzberg));
	 * mDownloadedBuildings .add(new Place( "Mackenzie",
	 * "The Mackenzie Building honours Chalmers Jack Mackenzie who was Carleton’s second chancellor, president of the National Research Council, first president of Atomic Energy of Canada Limited and instrumental in the development of science and engineering education in Canada.\n\nThe building contains facilities to support all engineering disciplines. It’s also home to Carleton’s industrial design program."
	 * , "ME", coordinate) .setImageResourceId(R.drawable.mackenzie));
	 * mDownloadedBuildings .add(new Place( "Loeb Building",
	 * "The Loeb Building recognizes the financial contributions made to Carleton by Ottawa’s Loeb family. It houses offices for the Faculty of Public Affairs, as well as various academic departments, a cafeteria and lounge, classrooms and laboratories.\n\nIt is also home to the University’s Music department with its extensive collection of musical instruments, scores and periodicals."
	 * , "LA", coordinate) .setImageResourceId(R.drawable.loeb));
	 * mDownloadedBuildings .add(new Place( "Architecture Building",
	 * "The Architecture Building was designed specifically for Carleton’s architecture program with its large open studio spaces, flexible classrooms, and fully-equipped work stations. Studios are open 24-hours-a-day."
	 * , "AA", coordinate) .setImageResourceId(R.drawable.architecture));
	 * mDownloadedBuildings .add(new Place( "Tory Building",
	 * "The Tory building was the first building on campus. It was named after the first President of Carleton College, Henry Marshall Tory. The building has been renovated three times since Leslie Frost, then premier of Ontario, first laid its corner stone in 1957.\n\nOnly the mural in the central lecture hall, commonly known as “The Egg”, has remained unaltered. J. Harold Shenkman, a local entrepreneur, commissioned Gerald Trottier to create the piece. The Ottawa artist wanted the mural to depict humanity’s quest for knowledge."
	 * , "TB", coordinate) .setImageResourceId(R.drawable.tory));
	 * mDownloadedBuildings .add(new Place( "Dunton Tower",
	 * "The 22-storey tower houses various schools, institutes, departments, and research centres."
	 * , "DT", coordinate) .setImageResourceId(R.drawable.dunton));
	 * mDownloadedBuildings .add(new Place( "Minto Centre",
	 * "The Minto Centre for Advanced Studies in Engineering, located off the east wing of the Mackenzie Building, houses state-of-the-art facilities for research and studies in engineering, including a flight-simulating wind tunnel. A 400-seat lecture theatre offers the latest in audio-visual equipment. The building is named in honour of Minto Developments Inc., an Ottawa-based real estate development firm."
	 * , "MC", null).setImageResourceId(R.drawable.minto)); mDownloadedBuildings
	 * .add(new Place( "University Centre",
	 * "The University Centre, commonly known as the Unicentre, is a focal point for student life at Carleton.\n\nIt houses student-run CKCU-FM radio; the Bookstore; the University’s student newspaper The Charlatan; Information Carleton; Career Services; International Student Advisory; a variety of student clubs and organizations including the Carleton University Students’ Association; as well as pubs, eateries, banking machines and a variety store that includes a postal outlet."
	 * , "UC", null)); mDownloadedBuildings .add(new Place( "Steacie Building",
	 * "The Steacie Building for Chemistry is named in honour of E.W.R. Steacie, a distinguished chemist who served as chair of Carleton� board of governors and president of the National Research Council."
	 * , "SC", null).setImageResourceId(R.drawable.steacie));
	 * mDownloadedBuildings .add(new Place( "Azrieli Pavilion",
	 * "The new David J. Azrieli Pavilion is located next to the Tory Building and Dunton Tower along Library Road. The 75,000-square-foot pavilion opened in the fall of 2002.\n\nThe pavilion will be home to the newly named David Azrieli Institute for Graduate Studies in Architecture, as well as the new Bachelor of Information Technology program (www.bitdegree.ca). The building will also house the new Azrieli Gallery, dedicated to architecture and allied arts, and the National Capital Institute for Telecommunications — a research consortium of facilities across the region, which will explore the latest telecommunications technologies.\n\nStudents at the new facility have access to four large lecture halls, specialized computer laboratories and workrooms, and state-of-the-art classrooms, teaching studios and seminar rooms."
	 * , "AP", null).setImageResourceId(R.drawable.azrieli));
	 * mDownloadedBuildings.add(new Place("Azrieli Theatre", "", "AT",
	 * null).setImageResourceId(R.drawable.azrieli)); mDownloadedBuildings
	 * .add(new Place( "Life Sciences Research Building",
	 * "The Life Sciences Research Building is a specialized laboratory which supports experimental work in the biological, biochemical, and behavioural sciences."
	 * , "LS", null) .setImageResourceId(R.drawable.lifesciences));
	 * mDownloadedBuildings .add(new Place( "Paterson Hall",
	 * "Paterson Hall is named after Norman Paterson, one of Canada’s longest serving senators whose generous financial support helped to develop Carleton’s world-renowned programs in international affairs.\n\nHere you’ll find the College of Humanities, the Department of History and the School of Linguistics and Applied Language Studies which offers intensive English-language training programs for international students.\n\nA branch of the Bank of Nova Scotia is located on the lower level."
	 * , "PA", null).setImageResourceId(R.drawable.paterson));
	 * mDownloadedBuildings .add(new Place( "MacOdrum Library",
	 * "The MacOdrum Library, named in honour of Carleton’s second president Murdoch Maxwell MacOdrum, contains a collection of more than two million items—books, microfilms, tapes, CDs, government documents, maps, periodicals and archival materials—as well as study space, reading rooms and café.\n\nA computerized catalogue system provides access to the collection."
	 * , "ML", null).setImageResourceId(R.drawable.library));
	 * mDownloadedBuildings .add(new Place( "Southam Hall",
	 * "Southam Hall honours Carleton’s first chancellor and former publisher of The Ottawa Citizen Harry Stevenson Southam, who also donated a significant portion of the land on which the campus is built.\n\nHere you will find the 444-seat Kailash Mital Theatre, Carleton’s largest lecture hall. Two fully-equipped television studios on the sixth floor are used by Carleton’s instructional television service (CUTV), which provides distance education courses throughout the Ottawa Valley via digital cable television, and by students in the University’s highly acclaimed journalism program.\n\nThe Outdoor Amphitheatre between Southam and Paterson halls is used for concerts and classes during fair weather."
	 * , "SA", null).setImageResourceId(R.drawable.southam));
	 * mDownloadedBuildings .add(new Place( "Social Science Research",
	 * "The Social Sciences Research Building houses research centres that support studies in the social sciences. These include the Centre for Immigration and Ethno-Cultural Studies, the Centre for Labour Studies, and the Laboratory of Sleep and Chronopsychology."
	 * , "SR", null)); mDownloadedBuildings.add(new Place(
	 * "Visualization and Simulation Building", "", "", null));
	 * mDownloadedBuildings .add(new Place( "Robertson Hall",
	 * "Robertson Hall is named in honour of former chancellor and chancellor emeritus Gordon Robertson.\n\nIt houses the University Archives, Admissions Services, the Undergraduate Recruitment Office, the Business Office, Awards Office, Computing and Communications Services, Human Resources, University Services, Graphic Services, University Communications, Development and Alumni, meeting rooms and offices for the University Senate and Board of Governors.\n\nA wall in the lobby recognizes major contributors to the University’s Challenge Fund Campaign."
	 * , "RO", null) .setImageResourceId(R.drawable.robertson));
	 * mDownloadedBuildings .add(new Place( "Nesbitt Biology Building",
	 * "The H. H. J. Nesbitt Biology Building (formerly the Environmental Laboratories Biology Annex (ELBA) is made up of climate-controlled greenhouses that contain one of the finest collections of plants for teaching and scientific study in Canada.\n\nThe Nesbitt Building is state of the art with open architecture to foster collaborative work and invite interaction between scholars. Design features such as open laboratories, lounges used by both students and faculty, and faculty offices clustered in a central area, promote an atmosphere of shared learning. This manifests itself in close faculty-student research collaborations, as well as friendships."
	 * , "NB", null).setImageResourceId(R.drawable.nesbitt));
	 * mDownloadedBuildings .add(new Place( "Technology and Training Centre",
	 * "The Carleton Technology and Training Centre is home to the University’s Health Services, Parking Services, the Co-op Office, Environmental Health and Safety, pharmacy, dental clinic, “Treats”, and a variety of Ottawa-based companies and associations."
	 * , "TT", null).setImageResourceId(R.drawable.cttc)); mDownloadedBuildings
	 * .add(new Place( "National Wildlife Research Centre",
	 * "The National Wildlife Research Centre occupies new facilities on the Carleton University campus in Ottawa, Ontario, Canada. This location opens the door to new partnerships, giving government and university scientists new opportunities to collaborate on science that is critical to wildlife conservation."
	 * , "NW", null).setImageResourceId(R.drawable.nwrc)); mDownloadedBuildings
	 * .add(new Place( "Fieldhouse",
	 * "The Fieldhouse is located north of the main Recreation building in the Department of Athletics and Recreation. The Fieldhouse is a popular spot for Carleton students who are keen on staying in shape, and for residents in neighbouring communities as well."
	 * , "FH", null) .setImageResourceId(R.drawable.fieldhouse));
	 * mDownloadedBuildings .add(new Place( "Alumni Hall",
	 * "Carleton University facilities include an air-conditioned triple gymnasium, a double gymnasium, the Ice House with two ice pads, tennis courts, squash courts, multipurpose and combative rooms, a 50-metre L-shaped pool, a fitness centre, a cardio room, a yoga room, excellent outdoor field  and modern locker facilities."
	 * , "AH", null) .setImageResourceId(R.drawable.alumnihall));
	 * mDownloadedBuildings .add(new Place( "Physical Recreation Centre",
	 * "The Fieldhouse is located north of the main Recreation building in the Department of Athletics and Recreation. The Fieldhouse is a popular spot for Carleton students who are keen on staying in shape, and for residents in neighbouring communities as well."
	 * , "AC", null)); mDownloadedBuildings .add(new Place( "Ice House",
	 * "The most comprehensive facility of its kind in central Ottawa, the Ice House at Carleton is located on Bronson Avenue. The $13 million state-of-the-art arena is the cornerstone of the Carleton Athletics Physical Recreation Centre."
	 * , "IC", null) .setImageResourceId(R.drawable.icehousesmall));
	 * mDownloadedBuildings .add(new Place( "Norm Fenn Gymnasium",
	 * "The Fieldhouse is located north of the main Recreation building in the Department of Athletics and Recreation. The Fieldhouse is a popular spot for Carleton students who are keen on staying in shape, and for residents in neighbouring communities as well."
	 * , "GY", null)); mDownloadedBuildings .add(new Place( "Tennis Courts",
	 * "The Fieldhouse is located north of the main Recreation building in the Department of Athletics and Recreation. The Fieldhouse is a popular spot for Carleton students who are keen on staying in shape, and for residents in neighbouring communities as well."
	 * , "TC", null)); mDownloadedBuildings .add(new Place( "Residence Commons",
	 * "University residences, named after Ontario counties in the Ottawa Valley — Glengarry, Grenville, Lanark, Leeds, Prescott, Renfrew, Russell, Stormont and Dundas — provide homes to over 2,600 students in double and single rooms.\n\nSenior residence fellows offer advice and counsel and help new students make a successful transition to university life.\n\nThe University Commons is the hub of residence life. Most students in residence meet here for their meals. The building also contains Fenn Lounge, offices, a convenience store, an arcade, pub and information desk."
	 * , "RC", null) .setImageResourceId(R.drawable.residencecommons));
	 * 
	 * mDownloadedRestaurants.add(new Place("Pizza Pizza", null, null, null));
	 * mDownloadedRestaurants.add(new Place("Subway", null, null, null)); }
	 * catch (Exception e) { e.printStackTrace(); }
	 * 
	 * runOnUiThread(mReturnResults); }
	 */

	public Drawable getDrawable(int id) {
		Drawable picture = null;
		try {
			picture = getResources().getDrawable(id);
		} catch (Resources.NotFoundException e) {

		}
		return picture;
	}

	private SectionedAdapter mAdapter = new SectionedAdapter() {
		protected View getHeaderView(String caption, int index,
				View convertView, ViewGroup parent) {
			TextView result = (TextView) convertView;

			if (convertView == null) {
				result = (TextView) getLayoutInflater().inflate(
						R.layout.list_header, null);
			}

			result.setText(caption);

			return (result);
		}
	};

	private static class ViewHolder {
		ImageView icon;
		TextView top;
		TextView bottom;
		TextView bottomRight;
	}

	private class PlaceAdapter extends ArrayAdapter<Place> {

		private LayoutInflater mInflater;

		private final Bitmap mDefaultBitmap;

		public PlaceAdapter(Context context, ArrayList<Place> places) {
			super(context, R.layout.places_list_item, places);

			mDefaultBitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.ic_menu_gallery);

			// Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
			// R.drawable.ic_menu_gallery), 50,
			// 50, true);

			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
				if (position < mPlaces.size()) {
					place = mPlaces.get(position);
				}
			}

			Bitmap bitmap;
			int resId = place.getImageResourceId();
			if (resId >= 0) {
				bitmap = BitmapFactory.decodeResource(getResources(), resId);
			} else {
				bitmap = mDefaultBitmap;
			}

			holder.icon.setImageBitmap(bitmap);
			holder.top.setText(place.name);
			if (place.distance >= 0) {
				holder.bottom.setText("Distance: "
						+ Place.getDistanceString(place.distance));
			} else {
				holder.bottom.setText("");
			}
			holder.bottomRight.setText(place.buildingCode);

			return convertView;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		onOpenClick((Place) mAdapter.getItem(position));
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == android.R.id.list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

			Place p = (Place) mAdapter.getItem(info.position);

			menu.setHeaderTitle(p.name);
			super.onCreateContextMenu(menu, v, menuInfo);
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.places_context_menu, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		Place place = (Place) mAdapter.getItem(info.position);
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
		try {
			new DownloadPlacesTask()
					.execute(new URL(
							"http://tailoredpages.com/raven/places.php?format=json&limit=10"));
		} catch (MalformedURLException e) {

		}
	}

	public void onOpenClick(Place place) {
		Intent intent = new Intent(this, PlacesActivity.class);
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