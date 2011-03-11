package gogameoutdoors.app;

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

import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * @author Nicolas Gramlich
 * @since 20:06:15 - 08.07.2010
 */
public class ChangeableTextExample extends BaseExample implements LocationListener{
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
		/*locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation("gps");
		try{
			this.curlat =  location.getLatitude();
			this.curlng =  location.getLongitude();
			Log.e("dev", "got location onLoadEngine");
		} catch (Exception e){
			Log.e("dev", "didn't get location onLoadEngine");
		}*/
		this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera));
	}

	public void onLoadResources() {
		/*locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation("gps");
		try{
			this.curlat = location.getLatitude();
			this.curlng = location.getLongitude();
			Log.e("dev", "got location onLoadResources");
		} catch (Exception e){
			Log.e("dev", "didn't get location onLoadResources");
		}*/
		this.mFontTexture = new Texture(256, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);

		this.mFont = new Font(this.mFontTexture, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 48, true, Color.BLACK);

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
		
		final FPSCounter fpsCounter = new FPSCounter();
		this.mEngine.registerUpdateHandler(fpsCounter);

		final Scene scene = new Scene(1);
		scene.setBackground(new ColorBackground(0.09804f, 0.6274f, 0.8784f));

		final ChangeableText elapsedText = new ChangeableText(100, 160, this.mFont, "Location:", "Seconds elapsed: XXXXX".length());
		final ChangeableText fpsText = new ChangeableText(250, 240, this.mFont, "FPS:", "FPS: XXXXX".length());

		scene.getLastChild().attachChild(elapsedText);
		scene.getLastChild().attachChild(fpsText);

		scene.registerUpdateHandler(new TimerHandler(1 / 20.0f, true, new ITimerCallback() {
			public void onTimePassed(final TimerHandler pTimerHandler) {
				elapsedText.setText("Location: " + curlat + ", " + curlng);
				fpsText.setText("FPS: " + fpsCounter.getFPS());
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

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}