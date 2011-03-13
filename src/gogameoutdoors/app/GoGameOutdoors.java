package gogameoutdoors.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class GoGameOutdoors extends Activity implements OnClickListener{
    /** Called when the activity is first created. */
	private TextView output;
	private Button button;
	private Button buttonTestLocation;
	private Button buttonByPassTestLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		output = (TextView) findViewById(R.id.output);
		output.append("main view");
		
		button = (Button) findViewById(R.id.start_game);
		button.setOnClickListener(this);
		
		buttonTestLocation = (Button) findViewById(R.id.location_test);
		buttonTestLocation.setOnClickListener(this);

		buttonByPassTestLocation = (Button) findViewById(R.id.single_player);
		buttonByPassTestLocation.setOnClickListener(this);
    }
    
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    
	}
	@Override
	protected void onResume() {
		super.onResume();

	}

	/** Stop the updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
	}
 
	public void onClick(View v) {
		if (v == button){
			Intent intent = new Intent(this, GoGameMapView.class);
			startActivity(intent);
			
		} else if (v==buttonTestLocation){
			Intent intent = new Intent(this,LocationHandle.class);
			startActivity(intent);
		} else if (v == buttonByPassTestLocation){
			Intent intent = new Intent(this,GoGameActivity.class);
			startActivity(intent);
		}

		// TODO Auto-generated method stub
		
	}
}