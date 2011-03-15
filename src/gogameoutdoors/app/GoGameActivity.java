package gogameoutdoors.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.anddev.andengine.audio.sound.Sound;
import org.anddev.andengine.audio.sound.SoundFactory;
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
import org.anddev.andengine.util.Debug;
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
import android.os.Handler;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

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
	private HashMap<String,Map<String,Double>> otherPlayers = new HashMap<String, Map<String,Double>>();
	private Sound mInSquare1Sound;
	private long timer = 0;
	protected long initialTime = System.currentTimeMillis();
	private Sound mInSquare2Sound;



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
		return new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera).setNeedsSound(true));
	}

	public void onLoadResources() {

		this.mFontTexture = new Texture(256, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);

		this.mFont = new Font(this.mFontTexture, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 12, true, Color.BLACK);

		this.mEngine.getTextureManager().loadTexture(this.mFontTexture);
		this.mEngine.getFontManager().loadFont(this.mFont);
		try {
			SoundFactory.setAssetBasePath("mfx/");
			this.mInSquare1Sound = SoundFactory.createSoundFromAsset(this.getSoundManager(), this, "daft_punk_fall.ogg");
			this.mInSquare2Sound = SoundFactory.createSoundFromAsset(this.getSoundManager(), this, "all_of_the_lights.ogg");

		} catch (final IOException e) {
			Log.e("import_sound", e.toString());
		}
	}

	public Scene onLoadScene() {
		final Handler mHandler = new Handler();

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		Location location = locationManager.getLastKnownLocation("gps");		
		uploadOurLocation(location);

		try{
			this.curlat = location.getLatitude();
			this.curlng = location.getLongitude();
			Log.e("dev", "got location onLoadScene");
		} catch (Exception e){
			Log.e("dev", "didn't get location onLoadScene");
		}

		final FPSCounter fpsCounter = new FPSCounter();
		this.mEngine.registerUpdateHandler(fpsCounter);

		final Scene scene = new Scene(1);
		scene.setBackground(new ColorBackground(0.26667f, 0.26275f, 0.26275f));
		final ChangeableText yrlocation = new ChangeableText(20, 20, this.mFont, "Yr Location:", "Yr Location: 00.0000,00.0000".length());
		final Vector<ChangeableText> othertexts= new Vector<ChangeableText>();
        final ChangeableText fpsText = new ChangeableText(20, 40, this.mFont, "FPS:", "FPS: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".length());
        final ChangeableText fpsTimer = new ChangeableText(20, 80, this.mFont, "FPS:", "Timer: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".length());
        final ChangeableText fpsInitialTime = new ChangeableText(20, 60, this.mFont, "FPS:", "Timer: XXXXXXXXXXXXXXXXXXXXXXXXXXX".length());

		scene.getLastChild().attachChild(yrlocation);
		scene.getLastChild().attachChild(fpsText);
		scene.getLastChild().attachChild(fpsInitialTime);
		scene.getLastChild().attachChild(fpsTimer);
		scene.registerUpdateHandler(new TimerHandler(1 / 20.0f, true, new ITimerCallback() {
			public void onTimePassed(final TimerHandler pTimerHandler) {
				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				long var = System.currentTimeMillis();
				//getOthersLocation(nameValuePairs);
				int height = 100;
				for (int x = 0; x< otherPlayers.size(); x++){
					othertexts.add(new ChangeableText(20,height,mFont, "Othr Location:", "Othr Location: 00.0000,00.0000".length()));
					height+=20;
					scene.getLastChild().attachChild(othertexts.get(x));
				}
            	fpsText.setText("FPS: " + System.currentTimeMillis());
				fpsTimer.setText("Timer: " + timer);
				fpsInitialTime.setText("Timer: " + initialTime );

				yrlocation.setText("Yr Location: " + Double.toString(curlat).substring(0, Math.min(Double.toString(curlat).length(), 7)) + ", " + Double.toString(curlng).substring(0, Math.min(Double.toString(curlng).length(), 7)));
				Iterator<Map<String, Double>> player_values = otherPlayers.values().iterator();
				Log.e("size", Integer.toString(otherPlayers.size()));
				for (int x = 0; x< otherPlayers.size();x++){
					
					if (player_values.hasNext()){
						Map<String, Double> next_value = player_values.next();
						Log.e("log_tag", next_value.toString());
						if (othertexts.size() != 0){
							othertexts.get(x).setText("Othr Location: " + next_value.get("geolat").toString().substring(0, Math.min(next_value.get("geolat").toString().length(), 7)) +","+ next_value.get("geolong").toString().substring(0, Math.min(next_value.get("geolat").toString().length(), 7)));
							//Log.e("log_tag", "here it isn't though");
					        Log.e("log_tag", next_value.get("geolat").toString());

						} else {
							Log.e("log_tag", "the array is size 0");
						}
					}
				}

					playCorrectSound(curlat, curlng);
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
		//uploadOurLocation(location);
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
	
	public void playCorrectSound(double lat, double lng){
		long currentTime = System.currentTimeMillis();
		if(timer == 0){
			this.mInSquare1Sound.play();
			timer = currentTime;
		} else if (timer + 5000 < currentTime){//temporary note: know its 5700
			this.mInSquare2Sound.play();
			this.mInSquare1Sound.play();
			timer = currentTime;
			
		}

	}
	
	public void getOthersLocation(ArrayList<NameValuePair> nameValuePairs){
		Log.e("logger", "getting locations");
		String result = "";
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
	                	JSONObject user = user_data.getJSONArray("user").getJSONObject(0);

	                	JSONObject user_location = user.getJSONArray("location").getJSONObject(0);
	                	
	                	HashMap<String, Double> locationMap = new HashMap<String, Double>(); 
	                	locationMap.put("geolat", user_location.getDouble("geolat"));
	                	locationMap.put("geolong", user_location.getDouble("geolong"));// user_location.getString("geolat"));
	                	
		                otherPlayers.put(user.getString("id"), locationMap);

	                	
	                }
	        }
	        Log.e("log_tag", otherPlayers.toString());
		}catch(JSONException e){
	        Log.e("log_tag", "Error parsing data "+e.toString());
		}
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
		}
		 
		String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID); 
		nameValuePairs.add(new BasicNameValuePair("user_id",android_id));
		
		getOthersLocation(nameValuePairs);
		
		
	}
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}