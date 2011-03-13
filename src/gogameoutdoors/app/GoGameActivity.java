package gogameoutdoors.app;

import java.util.ArrayList;
import java.util.Vector;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.timer.ITimerCallback;
import org.anddev.andengine.engine.handler.timer.TimerHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.util.FPSCounter;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
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

import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;

/**
 * @author Nicolas Gramlich
 * @since 20:06:15 - 08.07.2010
 */
public class GoGameActivity extends BaseExample implements LocationListener{
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;

	// ===========================================================
	// Fields
	// ===========================================================

	private Camera mCamera;
	private Texture mFontTexture;
	private Font mFont;
	private double curlat;
	private double curlng;
	private LocationManager locationManager;
	private Vector<String> otherPlayers;


	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	public Engine onLoadEngine() {

		this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera));
	}

	public void onLoadResources() {

		this.mFontTexture = new Texture(256, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);

		this.mFont = new Font(this.mFontTexture, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 12, true, Color.BLACK);

		this.mEngine.getTextureManager().loadTexture(this.mFontTexture);
		this.mEngine.getFontManager().loadFont(this.mFont);
	}

	public Scene onLoadScene() {
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		Location location = locationManager.getLastKnownLocation("gps");
		try{
			this.curlat = location.getLatitude();
			this.curlng = location.getLongitude();
			Log.e("dev", "got location onLoadScene");
		} catch (Exception e){
			Log.e("dev", "didn't get location onLoadScene");
		}
		uploadOurLocation(location);

		
		final FPSCounter fpsCounter = new FPSCounter();
		this.mEngine.registerUpdateHandler(fpsCounter);

		final Scene scene = new Scene(1);
		scene.setBackground(new ColorBackground(0.26667f, 0.26275f, 0.26275f));
		final ChangeableText elapsedText = new ChangeableText(20, 20, this.mFont, "Yr Location:", "Yr Location: XXXXX".length());
		final ChangeableText elapsedText2 = new ChangeableText(20, 40, this.mFont, "Othr Location:", "Othr Location: XXXXX".length());

		scene.getLastChild().attachChild(elapsedText);
		scene.getLastChild().attachChild(elapsedText2);

		scene.registerUpdateHandler(new TimerHandler(1 / 20.0f, true, new ITimerCallback() {
			public void onTimePassed(final TimerHandler pTimerHandler) {
				elapsedText.setText("Yr Location: " + curlat + ", " + curlng);
				elapsedText2.setText("Othr Location: " + fpsCounter.getFPS());
			}
		}));

		return scene;
	}

	public void onLoadComplete() {

	}

	public void onLocationChanged(Location location) {
		Log.e("dev", "does it ever reach this");
		this.curlat = location.getLatitude();
		this.curlng = location.getLongitude();	
		uploadOurLocation(location);
	}

	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}

	// ===========================================================
	// Methods
	// ===========================================================
	public void uploadOurLocation(Location location){
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
	        String url = "http://people.ace.ed.ac.uk/dmsp1011/outdoorgaming/handleLocation.php";
	        HttpPost httppost = new HttpPost(url);
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.d("Dev", httppost.getURI().toString()); 

	        ResponseHandler<String> handler = new BasicResponseHandler();
	        String response = httpclient.execute(httppost, handler);
            Log.d("Dev", "Response: " + response);
	        /*HttpEntity entity = response.getEntity();
	        InputStream inputstream = entity.getContent();*/
            result = "["+response+"]";
		}catch(Exception e){
	        Log.e("log_tag", "Error in http connection "+e.toString());
		}
		try{
	        JSONArray jArray = new JSONArray(result);
	        for(int i=0;i<jArray.length();i++){
	                JSONObject json_data = jArray.getJSONObject(i);
	                JSONArray jsonUserArray = json_data.getJSONArray("users");
	                for (int x = 0; x < jsonUserArray.length(); x++){
	                	JSONObject user_data = jsonUserArray.getJSONObject(x);
	                	Log.e("log_tag", user_data.toString());
	                	JSONObject user = user_data.getJSONArray("user").getJSONObject(0);
	                	Log.e("log_tag", user.toString());
	                	Log.e("log_tag", user.getString("id"));
	                	JSONArray user_location = user.getJSONArray("location");
	                	
	                	Log.e("log_tag", user_location.getJSONObject(0).getString("geolat").toString());
	                	
	                }
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
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}