package org.ime.vnime;

import java.security.InvalidParameterException;

import org.ime.vnime.txtproc.MacroManager;
import org.ime.vnime.txtproc.TextChangedListener;
import org.ime.vnime.txtproc.TxtProcFactory;
import org.ime.vnime.txtproc.TxtProcessor;
import org.ime.vnime.txtproc.Word;
import org.ime.vnime.txtproc.TxtProcessor.Marks;
import org.ime.vnime.txtproc.TxtProcessor.Tones;
import org.ime.vnime.view.VnKeyboard;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

public class DefaultConnectionManager implements ConnectionManager {

	public DefaultConnectionManager(Context ctx) {
		this(ctx, null);
	}
	
	public DefaultConnectionManager(Context ctx, InputConnection conn) {
		if (ctx == null)
			throw new InvalidParameterException("Context must not be null!");
		context = ctx;
		connection = conn;
		
		modifiers = context.getString(R.string.modifiers_telex);
		
		txtProcessor = TxtProcFactory.getInstance(context).createTextProcessor(TxtProcFactory.ID_TXTPROCESSOR_MEMORY);
	}
	
	static private final int MAX_WORD_LEN = 10; /* Maximum length of a Vietnamese word */
	
	static private final int MODIFIER_STRING_LEN = 13;
	static private final int TONES_COUNT = 6;
	
	private static final int MSGID_NOTIFY_TEXT_CHANGED = 20;
	private static final int MSGID_CHECK_NEW_WORD = 30;
	private static final int NOTIFICATION_DELAY = 80;   /* Count in ms */

	private String modifiers;
	
	private Context context; 
	private InputConnection connection;
	
	private MacroManager macroManager;
	private boolean macroEnabled = true;
	
	private TxtProcessor txtProcessor;
	private Word currentWord;
	private boolean inDirtyState = true;
	
	private String revertBuffer = "";
	private boolean revertEnabled = true;
	
	private TextChangedListener textChangedListener;
	
	private Handler msgHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSGID_NOTIFY_TEXT_CHANGED:
				if (textChangedListener != null) {
					String text = getCurrentText();
					textChangedListener.onTextChanged(text);
				}
				break;
			case MSGID_CHECK_NEW_WORD:
				CharSequence text = connection.getTextBeforeCursor(MAX_WORD_LEN, 0);
				if (text != null) {
					int len = text.length();
					if (len > 0) {
						for (int i = len - 1; i >= 0; i--) {
							char ch = text.charAt(i);
							if (!Character.isWhitespace(ch)) {
								if (i < len - 1) {
									/* There is at least one whitespace before the cursor */
									textChangedListener.onNewWord();
									
									if (txtProcessor.isPunctuation(ch)) {
										/* We're starting a new sentence */
										textChangedListener.onNewSentence();
									}
								}
								break;
							}
						}
					} else {
						textChangedListener.onNewWord();
						textChangedListener.onNewSentence();
					}
				}
				break;
			}
		}
	};
	
	@Override
	public InputConnection getConnection() {
		return connection;
	}

	@Override
	public void setConnection(InputConnection conn) {
		connection = conn;
		inDirtyState = true;

		notifyTextChanged();
	}

	@Override
	public void setModifiers(String modifiers) {
		if (modifiers != null && modifiers.length() == MODIFIER_STRING_LEN) {
			this.modifiers = modifiers;
		} else if (modifiers == null) {
			this.modifiers = context.getString(R.string.modifiers_telex);	/* Default is Telex */
		} else {
			throw new InvalidParameterException("Invalid modifiers sequence!");
		}
	}

	@Override
	public String getModifiers() {
		return modifiers;
	}
	
	private boolean isModifier(char c) {
		int index = modifiers.indexOf(Character.toLowerCase(c));
		if (index >= 0 && currentWord.hasValidStructure()) {
			/* Check for valid CIRCUMFLEX */
			String text = currentWord.toString();
			int len = text.length();
			char ch;
			index -= TONES_COUNT;
			int i;
			if (index == 1) {
				/* Check for 'a', 'ă', 'â' */
				for (i = 0; i < len; i++) {
					ch = Character.toLowerCase(text.charAt(i));
					if (ch == 'a' || ch == 'ă' || ch == 'â')
						return true;
				}
				if (i >= len)
					return false;
			} else if (index == 3) {
				/* Check for 'e', 'ê' */
				for (i = 0; i < len; i++) {
					ch = Character.toLowerCase(text.charAt(i));
					if (ch == 'e' || ch == 'ê')
						return true;
				}
				if (i >= len)
					return false;
			} else if (index == 4) {
				/* Check for 'o', 'ô', 'ơ' */
				for (i = 0; i < len; i++) {
					ch = Character.toLowerCase(text.charAt(i));
					if (ch == 'o' || ch == 'ô' || ch == 'ơ')
						return true;
				}
				if (i >= len)
					return false;
			}
			return true;
		}
		return false;
	}
	
	private Tones modifierToTone(char c) {
		int index = modifiers.indexOf(Character.toLowerCase(c));
		if (index >= 0 && index < TONES_COUNT) {
			return Tones.fromInt(index);
		} else {
			return null;
		}
	}
	
	private Marks modifierToMark(char c) {
		int index = modifiers.indexOf(Character.toLowerCase(c)) - TONES_COUNT;
		switch (index) {
		case 0:
		case 5:
		case 6:
			return Marks.BREVE;
		case 1:
		case 3:
		case 4:
			return Marks.CIRCUMFLEX;
		case 2:
			return Marks.BAR;
		default:
			return null;
		}
	}
	
	private void processDelimiter(char c) {
		String macroValue = null;
		if (macroEnabled && macroManager != null && !inDirtyState
				&& (macroValue = macroManager.expandMacro(currentWord.toString(), true)) != null) {
			/* Expand macro if possible */
			onText(macroValue + c, TextOrigins.SELF_PRODUCE);
		} else if (revertEnabled && !currentWord.hasValidCombination() && !inDirtyState) {
			/* Revert the word if it is not valid */
			onText(revertBuffer + c, TextOrigins.SELF_PRODUCE);
		} else {
			/* If nothing special happened, simply put the char in */
			connection.finishComposingText();
			connection.commitText("" + c, 1);
		}

		inDirtyState = true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		char c = 0;
		if (connection != null && (c = keyToChar(event)) > 0) {
			if (inDirtyState) {
				resetState();
			}

			if (isModifier(c)) {
				if (currentWord.hasValidStructure()) {
					Tones tone = modifierToTone(c);
					if (tone != null) {
						if (!currentWord.putTone(tone)) {
							currentWord.putTone(Tones.NONE);
							currentWord.putChar(c, -1);
						}
					} else {
						Marks mark = modifierToMark(c);
						if (!(currentWord.putMark(mark) || (mark == Marks.BREVE && currentWord.putMark(Marks.HORN)))) {
							currentWord.removeMark();
							currentWord.putChar(c, -1);
						}
					}
				} else {
					currentWord.putChar(c, -1);
				}
				
				connection.setComposingText(currentWord.toString(), 1);
				revertBuffer += c;
			} else {
				if (txtProcessor.isWordDelimiter(c)) {
					processDelimiter(c);
				} else {
					currentWord.putChar(c, -1);
					connection.setComposingText(currentWord.toString(), 1);
					revertBuffer += c;
				}
			}

			notifyTextChanged();
			
			return true;
		} else {
			switch (keyCode) {
			case VnKeyboard.KEYCODE_MODE_CHANGE:
				return true;
			case KeyEvent.KEYCODE_SHIFT_LEFT:
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
			case KeyEvent.KEYCODE_ALT_LEFT:
			case KeyEvent.KEYCODE_ALT_RIGHT:
				/* Do nothing */
				return true;
			case KeyEvent.KEYCODE_DEL:
				String text = currentWord.toString();
				int len = 0;
				if (text != null && (len = text.length()) > 0) {
					currentWord = new Word(txtProcessor, text.substring(0, len - 1));
				} else {
					currentWord = new Word(txtProcessor);
				}
				if (connection != null) {
					connection.sendKeyEvent(event);
					notifyTextChanged();
				}
				return true;
			default:
				inDirtyState = true;
				if (connection != null) {
					connection.sendKeyEvent(event);
					notifyTextChanged();
				}
				return true;
			}
		}
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return false;
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			/* Do nothing */
			return true;
		default:
			return false;
		}
	}
	
	private String getCurrentText() {
		String result = "";
		
		if (connection != null) {
			CharSequence text = connection.getTextBeforeCursor(MAX_WORD_LEN, 0);
			if (text != null) {
				result = text.toString();
			} else {
				result = "";
			}

			for (int i = result.length() - 1; i >= 0; i--) {
				if (txtProcessor.isWordDelimiter(result.charAt(i))) {
					result = result.substring(i + 1);
					break;
				}
			}
		}
		
		return result;
	}
	
	private void resetState() {
		String text = getCurrentText();
		int len = text.length();
		currentWord = new Word(txtProcessor, text);
		if (connection != null && len > 0) {
			connection.deleteSurroundingText(len, 0);
			connection.setComposingText(text, 1);
		}
		inDirtyState = false;
		revertBuffer = text;
	}
	
	/**
	 * Convert a key event to appropriate character
	 * @param event
	 * @return Return the Unicode character that can be produced by
	 * the key event, or 0 if the key event can not produce any
	 * character.
	 */
	private char keyToChar(KeyEvent event) {
		int keyCode = event.getKeyCode();
		char result = 0;
		
		if (keyCode > KeyEvent.getMaxKeyCode()) {
			result = (char) (keyCode - KeyEvent.getMaxKeyCode());
			if (event.isShiftPressed()) {
				result = Character.toUpperCase(result);
			}
		} else {
			result = (char) event.getUnicodeChar();
			if (result > 0 && event.isShiftPressed()) {
				result = Character.toUpperCase(result);
			}
		}
		return result;
	}
	
	/**
	 * Check if we're starting a new word and/or a new sentence
	 */
	private void notifyTextChanged() {
		if (connection == null)
			return;
		if (!msgHandler.hasMessages(MSGID_NOTIFY_TEXT_CHANGED))
			msgHandler.sendEmptyMessageDelayed(MSGID_NOTIFY_TEXT_CHANGED, NOTIFICATION_DELAY);
		if (!msgHandler.hasMessages(MSGID_CHECK_NEW_WORD))
			msgHandler.sendEmptyMessageDelayed(MSGID_CHECK_NEW_WORD, NOTIFICATION_DELAY);
	}

	@Override
	public boolean onText(CharSequence text, TextOrigins origin) {
		switch (origin) {
		case SOFT_KEYBOARD:
			return false;
		case CANDIDATE_VIEW:
			if (connection != null) {
				if (inDirtyState) {
					resetState();
				}
				connection.setComposingText(text + " ", 1);
				connection.finishComposingText();
				inDirtyState = true;
				notifyTextChanged();
			}
			return true;
		case SELF_PRODUCE:
			if (connection != null) {
				connection.setComposingText(text, 1);
				connection.finishComposingText();
				inDirtyState = true;
				notifyTextChanged();
			}
			return true;
		}
		return false;
	}

	@Override
	public void setTextChangedListener(TextChangedListener listener) {
		textChangedListener = listener;
	}

	@Override
	public void setMacroManager(MacroManager manager) {
		macroManager = manager;
	}

	@Override
	public void setMacroEnabled(boolean enabled) {
		macroEnabled = enabled;
	}

	@Override
	public void setRevertEnabled(boolean enabled) {
		revertEnabled = enabled;
	}

}
