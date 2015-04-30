//SettingsFragment.java
//subclass of preferenceFragment for settings
package com.power.statequiz;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment
{	
	//creates preferences GUI from preferences.xml file in res/xml
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//load from xml
		addPreferencesFromResource(R.xml.preferences);
	}

}
