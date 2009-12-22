package org.ime.vnime.view;

import java.security.InvalidParameterException;
import java.util.HashMap;

import org.ime.vnime.R;

import android.content.Context;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;

public class KeyboardManager {

	public KeyboardManager(Context ctx) {
		if (ctx == null)
			throw new InvalidParameterException("The context must not be null");
		context = ctx;
	}
	
	private class KeyboardId {
		int xmlLayoutResId;
		int modeId;
		
		KeyboardId() {
			this(KeyboardModes.QWERTY);
		}
		
		KeyboardId(KeyboardModes mode) {
			switch (mode) {
			case QWERTY:
				xmlLayoutResId = R.xml.vkb_common;
				modeId = 0;
				break;
			case QWERTY_NUMBER:
				xmlLayoutResId = R.xml.vkb_common;
				modeId = R.id.kb_mode_qwerty_num;
				break;
			case QWERTY_SYMBOLS:
				xmlLayoutResId = R.xml.vkb_symbols;
				modeId = 0;
				break;
			case QWERTY_SYMBOLS_SHIFTED:
				xmlLayoutResId = R.xml.vkb_symbols_shift;
				modeId = 0;
				break;
			case PHONE:
				xmlLayoutResId = R.xml.vkb_phone;
				modeId = 0;
				break;
			case PHONE_SYMBOLS:
				xmlLayoutResId = R.xml.vkb_phone_symbols;
				modeId = 0;
				break;
			}
		}
	}
	
	private VnKeyboard getKeyboard(KeyboardId id) {
		if (mapKeyboards == null)
			mapKeyboards = new HashMap<KeyboardId, VnKeyboard>();
		VnKeyboard kb = mapKeyboards.get(id);
		if (kb == null) {
			kb = new VnKeyboard(context, id.xmlLayoutResId, id.modeId);
			mapKeyboards.put(id, kb);
		}
		return kb;
	}
	
	public VnKeyboard getKeyboard(EditorInfo info) {
		KeyboardId id = new KeyboardId();
		
		if (info != null) {
			int inputType = info.inputType;
			if ((inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_PHONE) {
				id.xmlLayoutResId = R.xml.vkb_phone;
				id.modeId = 0;
			} else {
				id.xmlLayoutResId = R.xml.vkb_common;
				id.modeId = 0;
			}
		}
		
		return getKeyboard(id);
	}
	
	public VnKeyboard getKeyboard(KeyboardModes mode) {
		if (mode == null)
			mode = KeyboardModes.QWERTY;  /* Default is qwerty keyboard */
		KeyboardId id = new KeyboardId(mode);
		return getKeyboard(id);
	}
	
	public enum KeyboardModes {
		QWERTY,
		QWERTY_NUMBER,
		QWERTY_SYMBOLS,
		QWERTY_SYMBOLS_SHIFTED,
		PHONE,
		PHONE_SYMBOLS,
	}
	
	private HashMap<KeyboardId, VnKeyboard> mapKeyboards;
	private Context context;
}
