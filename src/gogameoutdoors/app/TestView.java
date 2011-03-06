package gogameoutdoors.app;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.LinearLayout;

public class TestView extends MapActivity implements LocationListener{
	LinearLayout linearLayout;
	MapView mapView;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
        setContentView(R.layout.gomapview);
        
		mapView = (MapView) findViewById(R.id.gomapviewholder);
		mapView.setBuiltInZoomControls(true);
		
	}

	public void onLocationChanged(Location arg0) {

		// TODO Auto-generated method stub
		
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

}
