package org.ime.vnime.view;

import java.security.InvalidParameterException;

import org.ime.vnime.R;
import org.ime.vnime.SoftKeyboardListener;
import org.ime.vnime.VnImeSettings;
import org.ime.vnime.SoftKeyboardListener.TextOrigins;
import org.ime.vnime.view.KeyboardManager.KeyboardModes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.media.AudioManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

public class InputView extends KeyboardView {

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.e("ngnTest", "NewSize = (" + w + ", " + h + "), OldSize = (" + oldw + ", " + oldh + ")");
    }
	
	public enum CapModes {
		NONE,
		SENTENCES,
		WORDS,
		CHARS,
	}
	
	public static final int FEEDBACK_TYPE_VISUAL = 0x1;
	public static final int FEEDBACK_TYPE_SOUND = 0x2;
	public static final int FEEDBACK_TYPE_VIBRATION = 0x4;

	public InputView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public InputView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public void setKeyEventListener(SoftKeyboardListener listener) {
		keyEventListener = listener;
	}
	
	public void setKeyboardManager(KeyboardManager manager) {
		if (manager != null)
			vkbManager = manager;
		else
			throw new InvalidParameterException("KeyboardManager must not be null!");
	}
	
	public void setAutoCapSentences(boolean autoCap) {
		autoCapSentences = autoCap;
	}
	
	public void setFeedbackType(int types) {
		
		setPreviewEnabled((types & FEEDBACK_TYPE_VISUAL) != 0);
		
		feedbackSoundEnabled = (types & FEEDBACK_TYPE_SOUND) != 0;
		feedbackVibrationEnabled = (types & FEEDBACK_TYPE_VIBRATION) != 0;
	}
	
	public void onNewInputTarget(EditorInfo info) {
		changeKeyboardMode(detectKeyboardMode(info));
		
		changeShiftState(MetaKeyStates.OFF);
		
		switch (info.inputType & InputType.TYPE_MASK_CLASS) {
		case InputType.TYPE_CLASS_TEXT:
			switch (info.inputType & InputType.TYPE_MASK_FLAGS) {
			case InputType.TYPE_TEXT_FLAG_CAP_SENTENCES:
				capMode = CapModes.SENTENCES;
				break;
			case InputType.TYPE_TEXT_FLAG_CAP_WORDS:
				capMode = CapModes.WORDS;
				break;
			case InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS:
				capMode = CapModes.CHARS;
				break;
			default:
				capMode = CapModes.NONE;
				break;
			}
			break;
		}
	}
	
	private void init() {
		setOnKeyboardActionListener(vkbActionListener);
	}
	
	public void updateKeyboardCapMode(CapModes mode) {
		if (currentKeyboardMode == KeyboardModes.QWERTY) {
			switch (capMode) {
			case NONE:
				if (autoCapSentences && mode == CapModes.SENTENCES) {
					changeShiftState(MetaKeyStates.ON);
				}
				break;
			case SENTENCES:
			case WORDS:
				if (capMode == mode && stateShift != MetaKeyStates.LOCK) {
					changeShiftState(MetaKeyStates.ON);
				}
				break;
			case CHARS:
				if (capMode == mode) {
					changeShiftState(MetaKeyStates.LOCK);
				}
				break;
			}
		}
	}
	
	private KeyboardModes detectQwertyKeyboardMode() {
		Context ctx = getContext();
		KeyboardModes mode = KeyboardModes.QWERTY;
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		String modifiers = sp.getString(ctx.getString(R.string.vnime_settings_key_typingmethod), null);
		if (modifiers != null) {
			int len = modifiers.length();
			char ch;
			for (int i = 0; i < len; i++) {
				ch = modifiers.charAt(i);
				if (ch >= '0' && ch <= '9') {
					mode = KeyboardModes.QWERTY_NUMBER;
					break;
				}
			}
		}
		return mode;
	}
	
	private KeyboardModes detectKeyboardMode(EditorInfo info) {
		KeyboardModes mode = KeyboardModes.QWERTY;
		if (info != null) {
			switch (info.inputType & InputType.TYPE_MASK_CLASS) {
			case InputType.TYPE_CLASS_PHONE:
			case InputType.TYPE_CLASS_NUMBER:
			case InputType.TYPE_CLASS_DATETIME:
				mode = KeyboardModes.PHONE;
				break;
			case InputType.TYPE_CLASS_TEXT:
				mode = detectQwertyKeyboardMode();
				break;
			}
		}
		return mode;
	}
	
	private void changeKeyboardMode(KeyboardModes newMode) {
		if ((currentKeyboardMode == newMode && currentKeyboardMode != null)
				|| (newMode == null && currentKeyboardMode == KeyboardModes.QWERTY)) {
			return;
		}

		if (newMode == null)
			currentKeyboardMode = KeyboardModes.QWERTY;
		else {
			currentKeyboardMode = newMode;
		}
		setKeyboard(vkbManager.getKeyboard(currentKeyboardMode));
		
		switch (currentKeyboardMode) {
		case QWERTY:
			setShifted(stateShift != MetaKeyStates.OFF);
			break;
		case QWERTY_SYMBOLS_SHIFTED:
			setShifted(true);
			break;
		case QWERTY_SYMBOLS:
			setShifted(false);
			break;
		}
	}
	
	private boolean processShiftKey(KeyEvent event) {
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			switch (currentKeyboardMode) {
			case QWERTY:
			case QWERTY_NUMBER:
				switch (stateShift) {
				case ON:
					changeShiftState(MetaKeyStates.LOCK);
					break;
				case LOCK:
					changeShiftState(MetaKeyStates.OFF);
					break;
				case OFF:
					changeShiftState(MetaKeyStates.ON);
					break;
				}
				break;
			case QWERTY_SYMBOLS:
				changeKeyboardMode(KeyboardModes.QWERTY_SYMBOLS_SHIFTED);
				break;
			case QWERTY_SYMBOLS_SHIFTED:
				changeKeyboardMode(KeyboardModes.QWERTY_SYMBOLS);
				break;
			}
			break;
		default:
			break;
		}
		return keyEventListener.onKeyDown(event.getKeyCode(), event);
	}
	
	private void changeShiftState(MetaKeyStates newState) {
		if (newState == null)
			return;
		
		if (stateShift != newState) {
			stateShift = newState;
			
			if (stateShift != MetaKeyStates.OFF)
				setShifted(true);
			else
				setShifted(false);
	
			Keyboard kb = getKeyboard();
			int shiftKeyIndex = kb.getShiftKeyIndex();
			if (shiftKeyIndex >= 0) {
				Key shiftKey = kb.getKeys().get(kb.getShiftKeyIndex());
				if (shiftKey != null) {
					if (stateShift == MetaKeyStates.LOCK)
						shiftKey.on = true;
					else
						shiftKey.on = false;
					if (isShown())
						invalidateKey(kb.getShiftKeyIndex());
				}
			}
		}
	}
	
	private boolean processAltKey(KeyEvent event) {
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			switch (stateAlt) {
			case ON:
				stateAlt = MetaKeyStates.LOCK;
				break;
			case LOCK:
				stateAlt = MetaKeyStates.OFF;
				break;
			case OFF:
				stateAlt = MetaKeyStates.ON;
				break;
			}
			break;
		default:
			break;
		}
		return keyEventListener.onKeyDown(event.getKeyCode(), event);
	}
	
	private boolean processModeChangeKey(KeyEvent event) {
		switch (event.getKeyCode()) {
		case VnKeyboard.KEYCODE_MODE_CHANGE:
			switch (currentKeyboardMode) {
			case QWERTY:
			case QWERTY_NUMBER:
				changeKeyboardMode(KeyboardModes.QWERTY_SYMBOLS);
				if (stateShift == MetaKeyStates.ON)
					stateShift = MetaKeyStates.OFF;
				stateAlt = MetaKeyStates.OFF;
				break;
			case QWERTY_SYMBOLS:
			case QWERTY_SYMBOLS_SHIFTED:
				changeKeyboardMode(detectQwertyKeyboardMode());
				break;
			case PHONE:
				changeKeyboardMode(KeyboardModes.PHONE_SYMBOLS);
				break;
			case PHONE_SYMBOLS:
				changeKeyboardMode(KeyboardModes.PHONE);
				break;
			}
			break;
		default:
			break;
		}
		return keyEventListener.onKeyDown(event.getKeyCode(), event);
	}
	
	private boolean processModeChangeKeyLong(KeyEvent event) {
		switch (event.getKeyCode()) {
		case VnKeyboard.KEYCODE_MODE_CHANGE:
			Context ctx = getContext();
	        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
	        builder.setTitle(R.string.svc_name);
	        builder.setIcon(R.drawable.sym_keyboard_mode);
	        builder.setCancelable(true);
	        builder.setNegativeButton(android.R.string.cancel, null);
	        CharSequence itemSettings = ctx.getString(org.ime.vnime.R.string.app_name);
	        CharSequence itemInputMethod = ctx.getString(org.ime.vnime.R.string.ime_title);
	        builder.setItems(new CharSequence[] {itemSettings, itemInputMethod},
	                new DialogInterface.OnClickListener() {

	            public void onClick(DialogInterface di, int position) {
	                di.dismiss();
	    			Context ctx = getContext();
	                switch (position) {
	                    case ITEM_POSITION_SETTINGS:
	                        Intent intent = new Intent();
	                        intent.setClass(ctx, VnImeSettings.class);
	                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                        ctx.startActivity(intent);
	                        break;
	                    case ITEM_POSITION_IME:
	                        ((InputMethodManager)ctx.getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
	                        break;
	                }
	            }
	            
	            private static final int ITEM_POSITION_SETTINGS = 0;
	            private static final int ITEM_POSITION_IME = 1;
	        });
	        
	        AlertDialog mOptionsDialog = builder.create();
	        Window window = mOptionsDialog.getWindow();
	        WindowManager.LayoutParams lp = window.getAttributes();
	        lp.token = getWindowToken();
	        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
	        window.setAttributes(lp);
	        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	        mOptionsDialog.show();
			
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyEventListener != null) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_SHIFT_LEFT:
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
				return processShiftKey(event);
			case KeyEvent.KEYCODE_ALT_LEFT:
			case KeyEvent.KEYCODE_ALT_RIGHT:
				return processAltKey(event);
			case VnKeyboard.KEYCODE_MODE_CHANGE:
				return processModeChangeKey(event);
			default:
				int metaState = event.getMetaState();
				if (stateShift == MetaKeyStates.ON || stateShift == MetaKeyStates.LOCK)
					metaState |= KeyEvent.META_SHIFT_ON;
				if (stateAlt == MetaKeyStates.ON || stateAlt == MetaKeyStates.LOCK)
					metaState |= KeyEvent.META_ALT_ON;
				event = new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(),
									 event.getKeyCode(), event.getRepeatCount(), metaState,
									 event.getDeviceId(), event.getScanCode(), event.getFlags());
				if (stateShift == MetaKeyStates.ON) {
					changeShiftState(MetaKeyStates.OFF);
				}
				if (stateAlt == MetaKeyStates.ON)
					stateAlt = MetaKeyStates.OFF;
				return keyEventListener.onKeyDown(keyCode, event);
			}
		}
		else {
			return false;
		}
	}

	@Override
	protected boolean onLongPress(Key popupKey) {
		if (popupKey.codes[0] == VnKeyboard.KEYCODE_MODE_CHANGE) {
			KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, VnKeyboard.KEYCODE_MODE_CHANGE);
			return processModeChangeKeyLong(event);
		}
		return super.onLongPress(popupKey);
	}
	
	private MetaKeyStates stateAlt = MetaKeyStates.OFF;
	private MetaKeyStates stateShift = MetaKeyStates.OFF;
	
	private enum MetaKeyStates {
		OFF,
		ON,
		LOCK,
	}
	
	private boolean feedbackSoundEnabled = false;
	private boolean feedbackVibrationEnabled = false;
	private AudioManager audioManager;
	private float audioVolumeFeedback = -1.0f;
	private Vibrator vibrator;
	private int vibrDurationFeedback = 40;		/* Count in ms */
	
	private boolean autoCapSentences = true;
	private CapModes capMode = CapModes.NONE;
	
	private SoftKeyboardListener keyEventListener;
	private KeyboardManager vkbManager;
	private KeyboardModes currentKeyboardMode;
	
	private OnKeyboardActionListener vkbActionListener = new OnKeyboardActionListener() {
		
		@Override
		public void swipeUp() {
			
		}
		
		@Override
		public void swipeRight() {
			
		}
		
		@Override
		public void swipeLeft() {
			
		}
		
		@Override
		public void swipeDown() {
			
		}
		
		@Override
		public void onText(CharSequence text) {
			keyEventListener.onText(text, TextOrigins.SOFT_KEYBOARD);
		}
		
		@Override
		public void onRelease(int primaryCode) {
			
		}
		
		@Override
		public void onPress(int primaryCode) {
			keyDownTime = SystemClock.uptimeMillis();
			repeatCnt = -1;
			
			Context ctx = getContext();
			
			/* Sound feedback */
			if (feedbackSoundEnabled) {
				if (audioManager == null) {
					audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
				}
				/* Check if sound is enabled in current ringer mode */
				if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
		            int sound = AudioManager.FX_KEYPRESS_STANDARD;
		            switch (primaryCode) {
		                case VnKeyboard.KEYCODE_DELETE:
		                    sound = AudioManager.FX_KEYPRESS_DELETE;
		                    break;
		                case VnKeyboard.KEYCODE_ENTER:
		                    sound = AudioManager.FX_KEYPRESS_RETURN;
		                    break;
		                case ' ':
		                    sound = AudioManager.FX_KEYPRESS_SPACEBAR;
		                    break;
		            }
					audioManager.playSoundEffect(sound, audioVolumeFeedback);
				}
			}
			
			/* Vibration feedback */
			if (feedbackVibrationEnabled) {
				if (vibrator == null) {
					vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
				}
				vibrator.vibrate(vibrDurationFeedback);
			}
		}
		
		@Override
		public void onKey(int primaryCode, int[] keyCodes) {
			KeyEvent event = makeKeyEvent(primaryCode);
			onKeyDown(event.getKeyCode(), event);
		}
		
		private KeyEvent makeKeyEvent(int softKeyCode) {
			int hardKeyCode = VnKeyboard.softCodeToHardCode(softKeyCode);
			long eventTime = SystemClock.uptimeMillis();
			int action = KeyEvent.ACTION_DOWN;
			repeatCnt++;
			int metaState = 0;
			int devId = 0;
			int scanCode = softKeyCode;
			int flags = KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE;
			
			return new KeyEvent(keyDownTime, eventTime, action, hardKeyCode, repeatCnt, metaState, devId, scanCode, flags);
		}
		
		private long keyDownTime = 0;
		private int repeatCnt = 0;
	};

}
