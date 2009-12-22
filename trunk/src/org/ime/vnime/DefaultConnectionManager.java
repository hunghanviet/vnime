package org.ime.vnime;

import java.security.InvalidParameterException;

import org.ime.vnime.txtproc.MacroManager;
import org.ime.vnime.txtproc.TextChangedListener;
import org.ime.vnime.txtproc.TxtProcFactory;
import org.ime.vnime.txtproc.TxtProcessor;
import org.ime.vnime.txtproc.Word;
import org.ime.vnime.txtproc.TxtProcessor.Marks;
import org.ime.vnime.txtproc.TxtProcessor.Tones;

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
		
		txtProcessor = TxtProcFactory.getInstance(context).createTextProcessor(TxtProcFactory.ID_TXTPROCESSOR_MEMORY);
	}
	
	static private final int MAX_WORD_LEN = 10; /* Maximum length of a Vietnamese word */
	
	static private final int MODIFIER_STRING_LEN = 13;
	static private final int TONES_COUNT = 6;
	
	static private final String MODIFIER_TELEX = "zfsrxjwadeoww";
	
	private static final int MSGID_NOTIFY_TEXT_CHANGED = 20;

	private String modifiers = MODIFIER_TELEX;
	
	private Context context; 
	private InputConnection connection;
	
	private MacroManager macroManager;
	private boolean macroEnabled = true;
	
	private TxtProcessor txtProcessor;
	private Word currentWord;
	private int textLen;
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

		if (!msgHandler.hasMessages(MSGID_NOTIFY_TEXT_CHANGED))
			msgHandler.sendEmptyMessage(MSGID_NOTIFY_TEXT_CHANGED);
		checkNewWord();
	}

	@Override
	public void setModifiers(String modifiers) {
		if (modifiers != null && modifiers.length() == MODIFIER_STRING_LEN) {
			this.modifiers = modifiers;
		} else if (modifiers == null) {
			this.modifiers = MODIFIER_TELEX;	/* Default is Telex */
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
			connection.commitText("" + c, 1);
			checkNewWord();
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
				
				String newText = currentWord.toString();
				connection.deleteSurroundingText(textLen, 0);
				connection.commitText(newText, 1);
				textLen = newText.length();
				revertBuffer += c;
			} else {
				if (txtProcessor.isWordDelimiter(c)) {
					processDelimiter(c);
				} else {
					currentWord.putChar(c, -1);
					connection.commitText("" + c, 1);
					textLen++;
					revertBuffer += c;
				}
			}

			if (!msgHandler.hasMessages(MSGID_NOTIFY_TEXT_CHANGED))
				msgHandler.sendEmptyMessage(MSGID_NOTIFY_TEXT_CHANGED);
			
			return true;
		} else {
			switch (keyCode) {
			case KeyEvent.KEYCODE_SHIFT_LEFT:
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
			case KeyEvent.KEYCODE_ALT_LEFT:
			case KeyEvent.KEYCODE_ALT_RIGHT:
				return true;
			default:
				inDirtyState = true;
				if (connection != null) {
					connection.sendKeyEvent(event);
					// TODO Check if key event is really processed
					if (!msgHandler.hasMessages(MSGID_NOTIFY_TEXT_CHANGED))
						msgHandler.sendEmptyMessage(MSGID_NOTIFY_TEXT_CHANGED);
					checkNewWord();
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
		return false;
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
		textLen = text.length();
		currentWord = new Word(txtProcessor, text);
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
	private void checkNewWord() {
		if (connection == null)
			return;
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
						}
						if (txtProcessor.isPunctuation(ch)) {
							/* We're starting a new sentence */
							textChangedListener.onNewSentence();
						}
						break;
					}
				}
			} else {
				textChangedListener.onNewWord();
				textChangedListener.onNewSentence();
			}
		}
	}

	@Override
	public boolean onText(CharSequence text, TextOrigins origin) {
		switch (origin) {
		case SOFT_KEYBOARD:
			return false;
		case CANDIDATE_VIEW:
			if (connection != null) {
				String curText = getCurrentText();
				int curLen = curText.length();
				connection.deleteSurroundingText(curLen, 0);
				connection.commitText(text + " ", 1);
				inDirtyState = true;
				if (!msgHandler.hasMessages(MSGID_NOTIFY_TEXT_CHANGED))
					msgHandler.sendEmptyMessage(MSGID_NOTIFY_TEXT_CHANGED);
				checkNewWord();
			}
			return true;
		case SELF_PRODUCE:
			if (connection != null) {
				String curText = getCurrentText();
				int curLen = curText.length();
				connection.deleteSurroundingText(curLen, 0);
				connection.commitText(text, 1);
				inDirtyState = true;
				if (!msgHandler.hasMessages(MSGID_NOTIFY_TEXT_CHANGED))
					msgHandler.sendEmptyMessage(MSGID_NOTIFY_TEXT_CHANGED);
				checkNewWord();
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
