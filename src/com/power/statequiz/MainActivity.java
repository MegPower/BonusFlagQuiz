package com.power.statequiz;

import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	//keys for reading data from SharedPreferences
	public static final String CHOICES = "pref_numberOfChoices";
	public static final String REGIONS = "pref_regionsToInclude";
	
	//forces portrait mode
	private boolean phoneDevice = true;
	//checks if preference changed
	private boolean preferencesChanged = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//set default values in SharedPreferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		//register listener for SharedPreferences changes
		PreferenceManager.getDefaultSharedPreferences(this).
			registerOnSharedPreferenceChangeListener(preferenceChangeListener);
		
		//determine screen size
		int screenSize = getResources().getConfiguration().screenLayout & 
				Configuration.SCREENLAYOUT_SIZE_MASK;
		
		//if on a tablet, set phoneDevice to false
		if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || 
				screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
			phoneDevice = false;
		
		//if on phone, allow only portrait orientation
		if (phoneDevice)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}//end on onCreate
	
	@Override
	protected void onStart()
	{
		super.onStart();
		
		if (preferencesChanged)
		{
			//initialize QuizFragment and start quiz
			QuizFragment quizFragment = (QuizFragment)
					getFragmentManager().findFragmentById(R.id.quizFragment);
			quizFragment.updateGuessRows(
					PreferenceManager.getDefaultSharedPreferences(this));
			quizFragment.updateRegions(
					PreferenceManager.getDefaultSharedPreferences(this));
			quizFragment.resetQuiz();
			preferencesChanged = false;
		}
	}//end onStart

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		//get default display object representing the screen
		Display display = ((WindowManager)
				getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		//initializes screen size
		Point screenSize = new Point();
		//stores size 
		display.getRealSize(screenSize);
	
		//display app's menu only in portrait orientation
		if (screenSize.x < screenSize.y)
		{
			//inflate the menu
			getMenuInflater().inflate(R.menu.main, menu);
			return true;
		}
		else 
			return false;
	}//end onCreateMenuOptionsMenu

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent preferencesIntent = new Intent(this, SettingsActivity.class);
		startActivity(preferencesIntent);
		return super.onOptionsItemSelected(item);
	}
	
	//listener for changes to SharedPreferences
	private OnSharedPreferenceChangeListener preferenceChangeListener = 
			new OnSharedPreferenceChangeListener()
	{
		//called when app's preferences are changed
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key)
			{
				//use changed settings
				preferencesChanged = true;
				
				QuizFragment quizFragment = (QuizFragment)
						getFragmentManager().findFragmentById(R.id.quizFragment);
				
				//if # of choices to display changed
				if (key.equals(CHOICES))
				{
					quizFragment.updateGuessRows(sharedPreferences);
					quizFragment.resetQuiz();
				}
				//if regions to include changed
				else if (key.equals(REGIONS))
				{
					Set<String> regions =
							sharedPreferences.getStringSet(REGIONS, null);
					
					if (regions != null && regions.size() > 0)
					{
						quizFragment.updateRegions(sharedPreferences);
						quizFragment.resetQuiz();
					}
					//one region must be selected. if not North America is default
					else
					{
						SharedPreferences.Editor editor = sharedPreferences.edit();
						regions.add(
							getResources().getString(R.string.default_region));
						editor.putStringSet(REGIONS, regions);
						editor.commit();
						Toast.makeText(MainActivity.this, R.string.default_region_message, 
								Toast.LENGTH_SHORT).show();
					}
				}
				
				Toast.makeText(MainActivity.this, R.string.restarting_quiz, 
						Toast.LENGTH_SHORT).show();
			}//end OnSharedPreferencesChanged
				
	};//end OnSharedPreferenceChangedListener
}//end MainActivity class
