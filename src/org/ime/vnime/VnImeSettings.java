package org.ime.vnime;

import android.os.Bundle;
import android.preference.PreferenceActivity;

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
    }
}