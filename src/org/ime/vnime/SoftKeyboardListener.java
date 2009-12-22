package org.ime.vnime;

import android.view.KeyEvent;

public interface SoftKeyboardListener extends KeyEvent.Callback {

	public enum TextOrigins {
		SOFT_KEYBOARD,
		CANDIDATE_VIEW,
		SELF_PRODUCE,
	}
	
	public boolean onText(CharSequence text, TextOrigins origin);
}
