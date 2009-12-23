package org.ime.vnime;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;

/**
 * Manage configuration for VnIme service.
 * @author dtngn
 *
 */
public class VnImeSettings extends PreferenceActivity {

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_layout);
        
        lstTypingMethod = findPreference(getString(R.string.vnime_settings_key_typingmethod));

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		String modifiers = sp.getString(getString(R.string.vnime_settings_key_typingmethod), getString(R.string.modifiers_telex));
		updateTypingMethodSummary(modifiers);
        
        lstTypingMethod.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				updateTypingMethodSummary((String)newValue);
				return true;
			}
		});
    }
    
    private void updateTypingMethodSummary(String modifiers) {
		Resources res = getResources();
		String[] tpNames = res.getStringArray(R.array.typingmethod_name);
		String[] tpModifiers = res.getStringArray(R.array.typingmethod_modifiers);
		for (int i = 0; i < tpModifiers.length; i++) {
			if (tpModifiers[i].equals(modifiers)) {
				lstTypingMethod.setSummary(getString(R.string.vnime_settings_typingmethod_summary) + tpNames[i]);
				break;
			}
		}
    }
    
    private Preference lstTypingMethod;
}