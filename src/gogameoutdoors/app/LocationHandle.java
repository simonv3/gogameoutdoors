package gogameoutdoors.app;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.TextView;

public class LocationHandle extends MapActivity implements LocationListener {
	private static final String TAG = "LocationDemo";
	private static final String[] S = { "Out of Service",
			"Temporarily Unavailable", "Available" };

	/*map stuff*/
	MapView mapView;
	List<Overlay> mapOverlays;
	GoGameMapItemizedOverlay itemizedOverlay;
	private MapController mapController;
	private GoGameMapItemizedOverlay itemizedOverlayUser;
	Drawable drawable;

	private TextView output;
	private LocationManager locationManager;
	private String bestProvider;
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	private Location currentBestLocation;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location);

		// Get the output UI
		output = (TextView) findViewById(R.id.output);

		// Get the location manager
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		Location location = locationManager.getLastKnownLocation("gps");
		
		/* database stuff */
	    uploadOurLocation(location);

		printLocation(location);
		
		
		/*map stuff*/
		mapView = (MapView) findViewById(R.id.gomapviewholder);
		mapView.setBuiltInZoomControls(true);
		mapController = mapView.getController();
		mapController.setZoom(16);
		mapController.animateTo(new GeoPoint((int) (55.946*10e5),(int) (-3.166*10e5)));
		try{
		mapController.animateTo(new GeoPoint((int) (location.getLatitude()*10e5),(int) (location.getLongitude()*10e5)));
		} catch(Exception e){
			Log.e("no_gps", "Could not find location information");
			output.append("\n\n couldn't find GPS coordinates");
		}
		mapOverlays = mapView.getOverlays();
		drawable = this.getResources().getDrawable(R.drawable.dot_red);//
		itemizedOverlay = new GoGameMapItemizedOverlay(drawable);//
		itemizedOverlayUser = new GoGameMapItemizedOverlay(this.getResources().getDrawable(R.drawable.dot));
		drawGridLocation(location);
		inGameArea(location);
		
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    
	}
	/** Register for the updates when Activity is in foreground */
	@Override
	protected void onResume() {
		super.onResume();
		locationManager.requestLocationUpdates("gps", 20000, 1, this);

	}

	/** Stop the updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
	}

	private void inGameArea(Location location){
		output.append("\n\n");
		try{
			if (location.getLatitude() >= (double) 55.9451 && location.getLatitude() <= (double) 55.94657 && location.getLongitude() <= (double) -3.1650 && location.getLongitude() >= (double) -3.1672){
				output.append("you're in the play area");
				Intent intent = new Intent(this,GoGameActivity.class);
				startActivity(intent);
			} else {
				output.append("you're still not in the play area, use the map to make your way there");
				
			}
		}
		catch(Exception e){
			output.append("you're not in the play area, use the map to make your way there");
		}
	}
	public void onLocationChanged(Location location) {
			printLocation(location);
			uploadOurLocation(location);
			drawGridLocation(location);
			inGameArea(location);
	}
	public void onProviderDisabled(String provider) {
		// let okProvider be bestProvider
		// re-register for updates
		//output.append("\n\nProvider Disabled: " + provider);
	}

	public void onProviderEnabled(String provider) {
		// is provider better than bestProvider?
		// is yes, bestProvider = provider
		//output.append("\n\nProvider Enabled: " + provider);
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		//output.append("\n\nProvider Status Changed: " + provider + ", Status="
		//		+ S[status] + ", Extras=" + extras);
	}
	
	
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	    boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
/*** storage ***/
	
	
	
	/*
	private void insertOurLocation(Location location){
		SQLiteDatabase db = locationStorage.getWritableDatabase();
	    ContentValues values = new ContentValues();
		values.put(LocalStorageHelper.GEO_LAT, location.getLatitude());
	    values.put(LocalStorageHelper.GEO_LONG, location.getLongitude());
	    values.put(LocalStorageHelper.USER_ID, 1);
	    values.put(LocalStorageHelper.USER_LAST_UPDATE, System.currentTimeMillis());
	    db.insert(LocalStorageHelper.TABLE, null, values);
	}
	
	private Cursor getLocations() {
		    SQLiteDatabase db = locationStorage.getReadableDatabase();
		    Cursor cursor = db.query(LocalStorageHelper.TABLE, null, null, null, null,
		        null, null);
		    
		    startManagingCursor(cursor);
		    return cursor;
		  }
*/
	private void showLocations(Cursor cursor) {
		    StringBuilder ret = new StringBuilder("Saved Events:\n\n");
		    while (cursor.moveToNext()) {
		      long id = cursor.getLong(0);
		      long time = cursor.getLong(1);
		      String title = cursor.getString(2);
		      ret.append(id + ": " + time + ": " + title + "\n");
		    }
		    output.setText(ret);
		  }
/***printing ***/
	private void printProvider(String provider) {
		LocationProvider info = locationManager.getProvider(provider);
		//output.append(info.toString() + "\n\n");
	}
	public void uploadOurLocation(Location location){
		String result = "";
		//the geolocation to send
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		try{
			nameValuePairs.add(new BasicNameValuePair("geo_lat",Double.toString(location.getLatitude())));
			nameValuePairs.add(new BasicNameValuePair("geo_long",Double.toString(location.getLongitude())));		 
		} catch(Exception e){
			Log.e("no_gps", "Could not find location information");
			output.append("\n\n couldn't find GPS coordinates");
		}
		 
		String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID); 
		nameValuePairs.add(new BasicNameValuePair("user_id",android_id));

		try{
	        HttpClient httpclient = new DefaultHttpClient();
	        HttpPost httppost = new HttpPost("http://people.ace.ed.ac.uk/dmsp1011/outdoorgaming/handleLocation.php");
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.d("Dev", httppost.getURI().toString()); 

	        ResponseHandler<String> handler = new BasicResponseHandler();
	        String response = httpclient.execute(httppost, handler);
	        output.append("\n \n response: "+response);
            Log.d("Dev", "Response: " + response);
	        /*HttpEntity entity = response.getEntity();
	        InputStream inputstream = entity.getContent();*/
            result = response;
		}catch(Exception e){
	        Log.e("log_tag", "Error in http connection "+e.toString());
		}
		try{
	        JSONArray jArray = new JSONArray(result);
	        for(int i=0;i<jArray.length();i++){
	                JSONObject json_data = jArray.getJSONObject(i);
	                /*Log.i("log_tag","id: "+json_data.getInt("id")+
	                        ", name: "+json_data.getString("name")+
	                        ", sex: "+json_data.getInt("sex")+
	                        ", birthyear: "+json_data.getInt("birthyear")
	                );*/
	        }
		}catch(JSONException e){
	        Log.e("log_tag", "Error parsing data "+e.toString());
		}
		
	}
	private void printLocation(Location location) {
		if (location == null)
		{
			//output.append("\nLocation[unknown]\n\n");
		}
		else
		{
			//output.append("\n\n" + location.toString());
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void drawGridLocation(Location location){
		GeoPoint userpoint = null;
		//output.append("drawing grid");
		try{
			int lat = (int) (location.getLatitude()*10e5);
			int lon = (int) (location.getLongitude()*10e5);
			userpoint = new GeoPoint(lat,lon);
			mapController.setCenter(userpoint);
			//output.append("\n\n location lat: "+Integer.toString(lat)+"   long: "+Integer.toString(lon));

			//output.append("\n\n userpoint lat: "+Integer.toString(userpoint.getLatitudeE6())+"   long: "+Integer.toString(userpoint.getLongitudeE6()));
			itemizedOverlayUser.addOverlay(new OverlayItem(userpoint,"",""));
			mapOverlays.add(itemizedOverlayUser);
			//TODO process the user location
		} catch(Exception e){
			Log.e("no_gps", "Could not find location information");
		}
		GeoPoint centerpoint = new GeoPoint((int) (55.946*10e5),(int) (-3.166*10e5));
		
		GeoPoint bottom_left = new GeoPoint((int) (55.9455*10e5), (int)(-3.1670*10e5));
		GeoPoint bottom_right = new GeoPoint((int) (55.9454*10e5),(int)(-3.1656*10e5));
		GeoPoint top_left = new GeoPoint((int) (55.94655*10e5), (int)(-3.1667*10e5));
		GeoPoint top_right= new GeoPoint((int) (55.9464*10e5), (int)(-3.1653*10e5));

		
		OverlayItem overlayitem = new OverlayItem(centerpoint, "", "");
		OverlayItem overlayitembl = new OverlayItem(bottom_left, "", "");
		OverlayItem overlayitembr = new OverlayItem(bottom_right, "", "");
		OverlayItem overlayitemtl = new OverlayItem(top_left, "", "");
		OverlayItem overlayitemtr = new OverlayItem(top_right, "", "");

		itemizedOverlay.addOverlay(overlayitem);
		itemizedOverlay.addOverlay(overlayitembl);
		itemizedOverlay.addOverlay(overlayitembr);
		itemizedOverlay.addOverlay(overlayitemtl);
		itemizedOverlay.addOverlay(overlayitemtr);
		
		
		mapOverlays.add(itemizedOverlay);
		//
	}

}
