package genius.mohammad.accelerometer.mouse;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import genius.mohammad.accelerometer.mouse.R;

public class Preferences extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		getPreferenceScreen().findPreference("serverIP").setOnPreferenceClickListener(serverIPClickListener);
		getPreferenceScreen().findPreference("serverPort").setOnPreferenceClickListener(serverPortClickListener);
	}
	
	Preference.OnPreferenceClickListener serverIPClickListener = new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
			Intent i = new Intent(Preferences.this, DefaultServerIPActivity.class);
			startActivity(i);
			return true;
		}		
	};
	
	Preference.OnPreferenceClickListener serverPortClickListener = new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
			Intent i = new Intent(Preferences.this, DefaultServerPortActivity.class);
			startActivity(i);
			return true;
		}		
	};
}