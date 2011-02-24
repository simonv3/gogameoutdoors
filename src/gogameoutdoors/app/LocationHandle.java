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

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.TextView;

public class LocationHandle extends Activity implements LocationListener {
	private static final String TAG = "LocationDemo";
	private static final String[] S = { "Out of Service",
			"Temporarily Unavailable", "Available" };

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

		// List all providers:
		List<String> providers = locationManager.getAllProviders();
		for (String provider : providers) {
			printProvider(provider);
		}

		Criteria criteria = new Criteria();
		bestProvider = locationManager.getBestProvider(criteria, false);
		output.append("\n\nBEST Provider:\n");
		printProvider(bestProvider);

		output.append("\n\nLocations (starting with last known):");
		
		
		
		Location location = locationManager.getLastKnownLocation("gps");
		
		/* database stuff */
	    uploadOurLocation(location);
	    /*insertOurLocation(location);*/
	    /*Cursor cursor = getLocations();*/
	    /*showLocations(cursor);*/
		
		printLocation(location);
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

	public void onLocationChanged(Location location) {
			printLocation(location);
			uploadOurLocation(location);


	}

	public void onProviderDisabled(String provider) {
		// let okProvider be bestProvider
		// re-register for updates
		output.append("\n\nProvider Disabled: " + provider);
	}

	public void onProviderEnabled(String provider) {
		// is provider better than bestProvider?
		// is yes, bestProvider = provider
		output.append("\n\nProvider Enabled: " + provider);
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		output.append("\n\nProvider Status Changed: " + provider + ", Status="
				+ S[status] + ", Extras=" + extras);
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
	
	private void uploadOurLocation(Location location){
		String result = "";
		//the geolocation to send
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		try{
			nameValuePairs.add(new BasicNameValuePair("geo_lat",Double.toString(location.getLatitude())));
			nameValuePairs.add(new BasicNameValuePair("geo_long",Double.toString(location.getLongitude())));		 
		} catch(Exception e){
			Log.e("no_gps", "Could not find location information");
			output.append("couldn't find GPS coordinates");
		}
		 
		String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID); 
		output.append(android_id);
		nameValuePairs.add(new BasicNameValuePair("user_id",android_id));

		try{
	        HttpClient httpclient = new DefaultHttpClient();
	        HttpPost httppost = new HttpPost("http://simon.vansintjan.net/outdoorgaming/handleLocation.php");
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.d("Dev", httppost.getURI().toString()); 

	        ResponseHandler<String> handler = new BasicResponseHandler();
	        String response = httpclient.execute(httppost, handler);
	        output.append("response: "+response);
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
		output.append(info.toString() + "\n\n");
	}

	private void printLocation(Location location) {
		if (location == null)
			output.append("\nLocation[unknown]\n\n");
		else
			output.append("\n\n" + location.toString());
	}

}
