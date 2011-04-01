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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.common.Coordinate;
import com.common.HttpBuilder;
import com.common.Place;

public class PlaceActivity extends ListActivity {
    
    public static final String TAG = "PlaceActivity";
    
    public static final int DIALOG_LOADING = 0;
    
    private static final String NULL_STRING = "null";
    private static final String EMPTY_STRING = "";
    
    private Place mPlace = null;
    
    private final ArrayList<String> mData = new ArrayList<String>();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Request progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.places_activity);
        
        setListAdapter(new PlaceAdapter(mData));
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Place place = null;
            try {
                place = (Place) extras.getSerializable(Place.class.toString());
                
                setTitle(place.name);
                
                mPlace = place;
            } catch (ClassCastException e) {
                finish();
            } finally {
                
            }
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    private List<String> getPlace(URL url) {
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
                
                List<String> data = new ArrayList<String>();
                Place place = null;
                
                for (int i = 0; i < len; ++i) {
                    obj = placesArray.getJSONObject(i).getJSONObject("place");
                    
                    String id = obj.getString("ID");
                    String name = obj.getString("Name");
                    String description = obj.getString("Description");
                    String code = obj.getString("Code");
                    
                    data.add(name);
                    data.add(description);
                    data.add(code);
                    
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
                    place.buildingCode = !code.equals(NULL_STRING) ? code
                            : null;
                    
                    break;
                }
                
                // A Simple JSONObject Value Pushing
                json.put("sample key", "sample value");
                Log.i(TAG, "<jsonobject>\n" + json.toString()
                        + "\n</jsonobject>");
                
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
    
    /*
     * List adapter.
     */

    private static class ViewHolder {
        TextView title;
        TextView content;
        TextView bottom;
    }
    
    private class PlaceAdapter extends EndlessAdapter<String> {
        
        private RotateAnimation rotate = null;
        
        private LayoutInflater mInflater;
        
        private final Bitmap mDefaultBitmap;
        
        public PlaceAdapter(ArrayList<String> places) {
            super(new ArrayAdapter<String>(PlaceActivity.this,
                    R.layout.place_list_item, places));
            
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
                convertView = mInflater.inflate(R.layout.place_list_item, null);
                
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.content = (TextView) convertView
                        .findViewById(R.id.content);
                holder.bottom = (TextView) convertView
                        .findViewById(R.id.bottom);
                
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            String data = null;
            
            synchronized (this) {
                if (position < this.getItemCount()) {
                    data = (String) this.getItem(position);
                }
            }
            
            if (data != null) {
                // Bitmap bitmap;
                // int resId = place.getImageResourceId();
                // if (resId >= 0) {
                // bitmap = BitmapFactory
                // .decodeResource(getResources(), resId);
                // } else {
                // bitmap = mDefaultBitmap;
                // }
                
                // holder.icon.setImageBitmap(bitmap);
                holder.title.setText("Title");
                holder.content.setText(data);
                // float distance = place.distance;
                // if (distance >= 0) {
                // holder.bottom.setText("Distance: "
                // + Place.getDistanceString(distance));
                // } else {
                // holder.bottom.setText(EMPTY_STRING);
                // }
                // String buildingCode = place.buildingCode;
                // if (buildingCode != null) {
                // holder.bottomRight.setText(buildingCode);
                // } else {
                // holder.bottomRight.setText(EMPTY_STRING);
                // }
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
        protected List<String> cacheInBackground() {
            try {
                runOnUiThread(new Runnable() {
                    public void run() {
                        setProgressBarIndeterminateVisibility(true);
                    }
                });
                
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
        protected void addCachedData(List<String> data, boolean append) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) getWrappedAdapter();
            
            if (data != null) {
                mData.clear();
                
                Location location = getInitialLocation();
                
                int len = data.size();
                for (int i = 0; i < len; ++i) {
                    adapter.add(data.get(i));
                }
                
                Collections.sort(data);
            }
            
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // @SuppressWarnings("unchecked")
        // EndlessAdapter<Place> adapter = (EndlessAdapter<Place>) getListAdapter();
        // if (position < adapter.getItemCount()) {
        // onOpenClick((Place) adapter.getItem(position));
        // }
    }
    
    // @Override
    // public void onCreateContextMenu(ContextMenu menu, View v,
    // ContextMenuInfo menuInfo) {
    // if (v.getId() == android.R.id.list) {
    // AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    // @SuppressWarnings("unchecked")
    // EndlessAdapter<Place> adapter = (EndlessAdapter<Place>) getListAdapter();
    //
    // if (info.position < adapter.getItemCount()) {
    // Place p = (Place) adapter.getItem(info.position);
    //
    // menu.setHeaderTitle(p.name);
    // super.onCreateContextMenu(menu, v, menuInfo);
    // MenuInflater inflater = getMenuInflater();
    // inflater.inflate(R.menu.places_context_menu, menu);
    // }
    // }
    // }
    
    // @Override
    // public boolean onContextItemSelected(MenuItem item) {
    // AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
    // .getMenuInfo();
    // Place place = (Place) getListAdapter().getItem(info.position);
    // switch (item.getItemId()) {
    // case R.id.open:
    // onOpenClick(place);
    // return true;
    // case R.id.reality:
    // onRealityClick(place);
    // return true;
    // case R.id.map:
    // onMapClick(place);
    // return true;
    // case R.id.directions:
    // onDirectionsClick(place);
    // return true;
    // default:
    // return super.onContextItemSelected(item);
    // }
    // }
    
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
