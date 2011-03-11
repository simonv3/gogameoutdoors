package gogameoutdoors.app;

import java.util.ArrayList;
import java.util.List;

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
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GoGameMapView extends MapActivity implements LocationListener{
	LinearLayout linearLayout;
	MapView mapView;
	List<Overlay> mapOverlays;
	Drawable drawable;
	private TextView output;

	GoGameMapItemizedOverlay itemizedOverlay;
	private LocationManager locationManager;
	private Location currentBestLocation;

	private MapController mapController;
	private GoGameMapItemizedOverlay itemizedOverlayUser;

	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
        setContentView(R.layout.gomapview);
        
		output = (TextView) findViewById(R.id.location);

        
		mapView = (MapView) findViewById(R.id.gomapviewholder);
		mapView.setBuiltInZoomControls(true);
		mapController = mapView.getController();
		mapController.setZoom(16);
		mapController.animateTo(new GeoPoint((int) (55.946*10e5),(int) (-3.166*10e5)));


		mapOverlays = mapView.getOverlays();
		drawable = this.getResources().getDrawable(R.drawable.androidmarker);//
		itemizedOverlay = new GoGameMapItemizedOverlay(drawable);//
		itemizedOverlayUser = new GoGameMapItemizedOverlay(this.getResources().getDrawable(R.drawable.dot));
		
		//to do with location stuff
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation("gps");
		output.append("Locations (starting with last known):");
		printLocation(location);
		uploadOurLocation(location); //use the local uploadOurLocation function to upload the location to the online
		drawGridLocation(location);
		output.append("what is even going on");
		
	}
	public void onDestroy(){
	    super.onDestroy();
	}
	protected void onResume() {
		super.onResume();
		locationManager.requestLocationUpdates("gps", 20000, 1, this);

	}
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
	}

	public void onLocationChanged(Location location) {
		uploadOurLocation(location);
		printLocation(location);
		try{
			GeoPoint userpoint = new GeoPoint((int) (location.getLatitude()*10e6),(int) (location.getLongitude()*10e6));
			itemizedOverlayUser.addOverlay(new OverlayItem(userpoint,"",""));
			mapOverlays.add(itemizedOverlayUser);
			//TODO process the user location
		} catch(Exception e){
			Log.e("no_gps", "Could not find location information");
		}		// TODO Auto-generated method stub
		
	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void drawGridLocation(Location location){
		GeoPoint userpoint = null;
		output.append("drawing grid");

		try{
			userpoint = new GeoPoint((int) (location.getLatitude()*10e6),(int) (location.getLongitude()*10e6));
			itemizedOverlayUser.addOverlay(new OverlayItem(userpoint,"",""));
			mapOverlays.add(itemizedOverlayUser);
			output.append("added userpoint");
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
	
	private void uploadOurLocation(Location location){
		String result = "";
		//the geolocation to send
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		try{
			nameValuePairs.add(new BasicNameValuePair("geo_lat",Double.toString(location.getLatitude())));
			nameValuePairs.add(new BasicNameValuePair("geo_long",Double.toString(location.getLongitude())));		 
		} catch(Exception e){
			Log.e("no_gps", "Could not find location information");
		}
		 
		String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID); 
		nameValuePairs.add(new BasicNameValuePair("user_id",android_id));

		try{
	        HttpClient httpclient = new DefaultHttpClient();
	        HttpPost httppost = new HttpPost("http://simon.vansintjan.net/outdoorgaming/handleLocation.php");
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.d("Dev", httppost.getURI().toString()); 

	        ResponseHandler<String> handler = new BasicResponseHandler();
	        String response = httpclient.execute(httppost, handler);
            Log.d("Dev", "Response: " + response);
	        /*HttpEntity entity = response.getEntity();
	        InputStream inputstream = entity.getContent();*/
            result = response;
		}catch(Exception e){
	        Log.e("log_tag", "Error in http connection "+e.toString());
		}
		/*try{
	        JSONArray jArray = new JSONArray(result);
	        for(int i=0;i<jArray.length();i++){
	                JSONObject json_data = jArray.getJSONObject(i);
	                /*Log.i("log_tag","id: "+json_data.getInt("id")+
	                        ", name: "+json_data.getString("name")+
	                        ", sex: "+json_data.getInt("sex")+
	                        ", birthyear: "+json_data.getInt("birthyear")
	                );*/
	       /* }
		}catch(JSONException e){
	        Log.e("log_tag", "Error parsing data "+e.toString());
		}*/
		
	}
	private void printLocation(Location location) {
		if (location == null)
			output.append("\nLocation[unknown]\n\n");
		else
			output.append("\n\n" + location.toString());
	}

}
