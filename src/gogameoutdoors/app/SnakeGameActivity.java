package gogameoutdoors.app;

import gogameoutdoors.app.adt.Direction;
import gogameoutdoors.app.adt.SnakeSuicideException;
import gogameoutdoors.app.constants.GameConstants;
import gogameoutdoors.app.entity.Frog;
import gogameoutdoors.app.entity.Snake;
import gogameoutdoors.app.entity.SnakeHead;

import java.io.IOException;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.audio.sound.Sound;
import org.anddev.andengine.audio.sound.SoundFactory;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.anddev.andengine.engine.camera.hud.controls.BaseOnScreenControl.IOnScreenControlListener;
import org.anddev.andengine.engine.camera.hud.controls.DigitalOnScreenControl;
import org.anddev.andengine.engine.handler.timer.ITimerCallback;
import org.anddev.andengine.engine.handler.timer.TimerHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.modifier.RotationModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.font.FontFactory;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.HorizontalAlign;
import org.anddev.andengine.util.MathUtils;

import com.google.android.maps.GeoPoint;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * @author Nicolas Gramlich
 * @since 02:26:05 - 08.07.2010
 */
public class SnakeGameActivity extends BaseGameActivity implements GameConstants, LocationListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = CELLS_HORIZONTAL * CELL_WIDTH; // 640
	private static final int CAMERA_HEIGHT = CELLS_VERTICAL * CELL_HEIGHT; // 480

	private static final int LAYER_BACKGROUND = 0;
	private static final int LAYER_FOOD = LAYER_BACKGROUND + 1;
	private static final int LAYER_SNAKE = LAYER_FOOD + 1;
	private static final int LAYER_SCORE = LAYER_SNAKE + 1;

	// ===========================================================
	// Fields
	// ===========================================================

	private Camera mCamera;

	private DigitalOnScreenControl mDigitalOnScreenControl;

	private Texture mFontTexture;
	private Font mFont;

	private Texture mTexture;
	private TextureRegion mTailPartTextureRegion;
	private TiledTextureRegion mHeadTextureRegion;
	private TiledTextureRegion mFrogTextureRegion;

	private Texture mBackgroundTexture;
	private TextureRegion mBackgroundTextureRegion;

	private Texture mOnScreenControlTexture;
	private TextureRegion mOnScreenControlBaseTextureRegion;
	private TextureRegion mOnScreenControlKnobTextureRegion;
	private Snake mSnake;
	private Frog mFrog;

	private String mLocation = "0,0";
	private ChangeableText mLocationText;

	private Sound mGameOverSound;
	private Sound mMunchSound;
	protected boolean mGameRunning;
	private Text mGameOverText;
	private LocationManager locationManager;
	private long curlat;
	private long curlng;

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
		/* Load the font we are going to use. */
		FontFactory.setAssetBasePath("font/");
		this.mFontTexture = new Texture(512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		this.mFont = FontFactory.createFromAsset(this.mFontTexture, this, "Plok.ttf", 32, true, Color.WHITE);

		this.mEngine.getTextureManager().loadTexture(this.mFontTexture);
		this.mEngine.getFontManager().loadFont(this.mFont);

		/* Load all the textures this game needs. */
		this.mTexture = new Texture(128, 128, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		TextureRegionFactory.setAssetBasePath("gfx/");
		this.mHeadTextureRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "snake_head.png", 0, 0, 3, 1);
		this.mTailPartTextureRegion = TextureRegionFactory.createFromAsset(this.mTexture, this, "snake_tailpart.png", 96, 0);
		this.mFrogTextureRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "frog.png", 0, 64, 3, 1);

		this.mBackgroundTexture = new Texture(1024, 512, TextureOptions.DEFAULT);
		this.mBackgroundTextureRegion = TextureRegionFactory.createFromAsset(this.mBackgroundTexture, this, "snake_background.png", 0, 0);

		this.mOnScreenControlTexture = new Texture(256, 128, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		this.mOnScreenControlBaseTextureRegion = TextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = TextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_knob.png", 128, 0);

		this.mEngine.getTextureManager().loadTextures(this.mBackgroundTexture, this.mTexture, this.mOnScreenControlTexture);

		/* Load all the sounds this game needs. */
		try {
			SoundFactory.setAssetBasePath("mfx/");
			this.mGameOverSound = SoundFactory.createSoundFromAsset(this.getSoundManager(), this, "game_over.ogg");
			this.mMunchSound = SoundFactory.createSoundFromAsset(this.getSoundManager(), this, "munch.ogg");
		} catch (final IOException e) {
			Debug.e(e);
		}
	}

	public Scene onLoadScene() {
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		Location location = locationManager.getLastKnownLocation("gps");
			// Define a listener that responds to location updates
		
		this.mEngine.registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene(4);
		/* No background color needed as we have a fullscreen background sprite. */
		scene.setBackgroundEnabled(false);
		scene.getChild(LAYER_BACKGROUND).attachChild(new Sprite(0, 0, this.mBackgroundTextureRegion));

		/* The ScoreText showing how many points the pEntity scored. */
		this.mLocationText = new ChangeableText(5, 5, this.mFont, "Score: 0", "Score: XXXX".length());
		this.mLocationText.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		this.mLocationText.setAlpha(0.5f);
		scene.getChild(LAYER_SCORE).attachChild(this.mLocationText);

		/* The Snake. */
		this.mSnake = new Snake(Direction.RIGHT, 0, CELLS_VERTICAL / 2, this.mHeadTextureRegion, this.mTailPartTextureRegion);
		this.mSnake.getHead().animate(200);
		/* Snake starts with one tail. */
		this.mSnake.grow();
		scene.getChild(LAYER_SNAKE).attachChild(this.mSnake);

		/* A frog to approach and eat. */
		this.mFrog = new Frog(0, 0, this.mFrogTextureRegion);
		this.mFrog.animate(1000);
		this.setFrogToRandomCell();
		scene.getChild(LAYER_FOOD).attachChild(this.mFrog);

		/* The On-Screen Controls to control the direction of the snake. */
		this.mDigitalOnScreenControl = new DigitalOnScreenControl(0, CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight(), this.mCamera, this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, new IOnScreenControlListener() {
			public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {
				if(pValueX == 1) {
					SnakeGameActivity.this.mSnake.setDirection(Direction.RIGHT);
				} else if(pValueX == -1) {
					SnakeGameActivity.this.mSnake.setDirection(Direction.LEFT);
				} else if(pValueY == 1) {
					SnakeGameActivity.this.mSnake.setDirection(Direction.DOWN);
				} else if(pValueY == -1) {
					SnakeGameActivity.this.mSnake.setDirection(Direction.UP);
				}
			}
		});
		/* Make the controls semi-transparent. */
		this.mDigitalOnScreenControl.getControlBase().setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		this.mDigitalOnScreenControl.getControlBase().setAlpha(0.5f);

		scene.setChildScene(this.mDigitalOnScreenControl);

		/* Make the Snake move every 0.5 seconds. */
		scene.registerUpdateHandler(new TimerHandler(0.5f, true, new ITimerCallback() {
			public void onTimePassed(final TimerHandler pTimerHandler) {
				if(SnakeGameActivity.this.mGameRunning) {
					try {
						SnakeGameActivity.this.mSnake.move();
					} catch (final SnakeSuicideException e) {
						SnakeGameActivity.this.onGameOver();
					}

					SnakeGameActivity.this.handleNewUserPosition();
				}
			}
		}));

		/* The title-text. */
		final Text titleText = new Text(0, 0, this.mFont, "Snake\non a Phone!", HorizontalAlign.CENTER);
		titleText.setPosition((CAMERA_WIDTH - titleText.getWidth()) * 0.5f, (CAMERA_HEIGHT - titleText.getHeight()) * 0.5f);
		titleText.setScale(0.0f);
		titleText.registerEntityModifier(new ScaleModifier(2, 0.0f, 1.0f));
		scene.getChild(LAYER_SCORE).attachChild(titleText);

		/* The handler that removes the title-text and starst the game. */
		scene.registerUpdateHandler(new TimerHandler(3.0f, new ITimerCallback() {
			public void onTimePassed(final TimerHandler pTimerHandler) {
				scene.unregisterUpdateHandler(pTimerHandler);
				scene.getChild(LAYER_SCORE).detachChild(titleText);
				SnakeGameActivity.this.mGameRunning = true;
			}
		}));

		/* The game-over text. */
		this.mGameOverText = new Text(0, 0, this.mFont, "You left the game area", HorizontalAlign.CENTER);
		this.mGameOverText.setPosition((CAMERA_WIDTH - this.mGameOverText.getWidth()) * 0.5f, (CAMERA_HEIGHT - this.mGameOverText.getHeight()) * 0.5f);
		this.mGameOverText.registerEntityModifier(new ScaleModifier(3, 0.1f, 2.0f));
		this.mGameOverText.registerEntityModifier(new RotationModifier(3, 0, 720));

		return scene;
	}
	/*public void onCreate(Bundle savedInstanceState){
		        super.onCreate(savedInstanceState);

		
	}*/
	public void onLoadComplete() {

	}

	// ===========================================================
	// Methods
	// ===========================================================

	private void setFrogToRandomCell() {
		this.mFrog.setCell(MathUtils.random(1, CELLS_HORIZONTAL - 2), MathUtils.random(1, CELLS_VERTICAL - 2));
	}

	private void handleNewUserPosition() {
		final SnakeHead snakeHead = this.mSnake.getHead();
		this.mLocation = curlat+", "+curlng;
		this.mLocationText.setText("Score: " + this.mLocation);
		if(snakeHead.getCellX() < 0 || snakeHead.getCellX() >= CELLS_HORIZONTAL || snakeHead.getCellY() < 0 || snakeHead.getCellY() >= CELLS_VERTICAL) {
			this.onGameOver();
		} else if(snakeHead.isInSameCell(this.mFrog)) {

			this.mSnake.grow();
			this.mMunchSound.play();
			this.setFrogToRandomCell();
		}
	}

	private void onGameOver() {
		//this.mGameOverSound.play();
		this.mEngine.getScene().getChild(LAYER_SCORE).attachChild(this.mGameOverText);
		this.mGameRunning = false;
	}

	public void onLocationChanged(Location location) {
		Log.e("Dev","accessing Location");
		curlat = (long) (location.getLatitude()*1E6);
		curlng = (long) (location.getLongitude()*1E6);
		this.mLocation = curlat+", "+curlng;
		this.mLocationText.setText("Score: " + this.mLocation);

		// TODO Auto-generated method stub
		
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
	// Inner and Anonymous Classes
	// ===========================================================
}