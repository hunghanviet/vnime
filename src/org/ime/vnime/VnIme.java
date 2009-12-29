package org.ime.vnime;

import org.ime.vnime.txtproc.DictionaryManager;
import org.ime.vnime.txtproc.MacroManager;
import org.ime.vnime.txtproc.TextChangedListener;
import org.ime.vnime.txtproc.TxtProcFactory;
import org.ime.vnime.view.CandidateView;
import org.ime.vnime.view.InputView;
import org.ime.vnime.view.KeyboardManager;
import org.ime.vnime.view.InputView.CapModes;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

/**
 * Main Vietnamese IME service.
 * @author dtngn
 *
 */
public class VnIme extends InputMethodService {

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		/* Invalidate the input view */
		viewInput.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onEvaluateInputViewShown() {
		return (showInputView && super.onEvaluateInputViewShown()) || showInputViewAlways;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		TxtProcFactory factory = TxtProcFactory.getInstance(this);
		
		macroManager = factory.createMacroManager(TxtProcFactory.ID_MACRO_MANAGER_SQLITE);
		dictManager = factory.createDictionaryManager(TxtProcFactory.ID_DICT_MANAGER_MEMORY);
		
		connManager = new DefaultConnectionManager(this);
		connManager.setMacroManager(macroManager);
		connManager.setTextChangedListener(new TextChangedListener() {
			
			@Override
			public void onTextChanged(CharSequence newText) {
		        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
				if (showCandidateView &&
						(display.getHeight()*MAX_OCCUPIED_SCREEN/100 >= viewInput.getHeight() + viewCandidate.getHeight())) {
					String text;
					if (newText != null && (text = newText.toString()).trim().length() > 0) {
						if (dictManager.checkCandidateExist(text, null) || macroManager.checkMacroExist(text)) {
							viewCandidate.updateCandidateList(text);
							setCandidatesViewShown(true);
						} else {
							setCandidatesViewShown(false);
						}
					} else {
						setCandidatesViewShown(false);
					}
				} else {
					setCandidatesViewShown(false);
				}
			}
			
			@Override
			public void onNewWord() {
				if (viewInput != null)
					viewInput.updateKeyboardCapMode(CapModes.WORDS);
			}
			
			@Override
			public void onNewSentence() {
				if (viewInput != null)
					viewInput.updateKeyboardCapMode(CapModes.SENTENCES);
			}
		});

		viewInput = (InputView) getLayoutInflater().inflate(R.layout.svc_input, null);
		viewInput.setKeyEventListener(connManager);
		viewInput.setKeyboardManager(new KeyboardManager(this));

		viewCandidate = (CandidateView) getLayoutInflater().inflate(R.layout.svc_candidate, null);
		viewCandidate.setDictionaryManager(dictManager);
		viewCandidate.setMacroManager(macroManager);
		viewCandidate.setSoftKeyboardListener(connManager);
		
		loadConfiguration();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (super.onKeyDown(keyCode, event))
				return true;
			else
				return viewInput.onKeyDown(keyCode, event);
		}
		if (viewInput.onKeyDown(keyCode, event)) {
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}
	
	private static final int MAX_OCCUPIED_SCREEN = 75;    /* In percentage */
	
	private ConnectionManager connManager;
	
	private DictionaryManager dictManager;
	private MacroManager macroManager;
	
	private CandidateView viewCandidate;
	private InputView viewInput;
	
	private boolean showInputView = true;
	private boolean showInputViewAlways = false;
	private boolean showCandidateView = false;
	
	/**
	 * Load configuration which is managed by {@link VnImeSettings}
	 */
	protected void loadConfiguration() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		connManager.setModifiers(sp.getString(getString(R.string.vnime_settings_key_typingmethod), null));
		connManager.setMacroEnabled(sp.getBoolean(getString(R.string.vnime_settings_key_macro_enable), true));
		connManager.setRevertEnabled(sp.getBoolean(getString(R.string.vnime_settings_key_spellcheck_revert), true));

		showInputView = sp.getBoolean(getString(R.string.vnime_settings_key_showsoftkeyboard), showInputView);
		showInputViewAlways = sp.getBoolean(getString(R.string.vnime_settings_key_showsoftkeyboard_always), showInputViewAlways);
		showCandidateView = sp.getBoolean(getString(R.string.vnime_settings_key_showsuggestion), showCandidateView);
		viewCandidate.setMacroEnabled(sp.getBoolean(getString(R.string.vnime_settings_key_macro_enable), true));
		
		viewInput.setAutoCapSentences(sp.getBoolean(getString(R.string.vnime_settings_key_autocapsentences), true));
		int feedbackType = 0;
		if (sp.getBoolean(getString(R.string.vnime_settings_key_feedback_visual), true)) {
			feedbackType |= InputView.FEEDBACK_TYPE_VISUAL;
		}
		if (sp.getBoolean(getString(R.string.vnime_settings_key_feedback_sound), true)) {
			feedbackType |= InputView.FEEDBACK_TYPE_SOUND;
		}
		if (sp.getBoolean(getString(R.string.vnime_settings_key_feedback_vibration), true)) {
			feedbackType |= InputView.FEEDBACK_TYPE_VIBRATION;
		}
		viewInput.setFeedbackType(feedbackType);
	}

	@Override
	public View onCreateCandidatesView() {
		ViewGroup vg = (ViewGroup) viewCandidate.getParent();
		if (vg != null) {
			vg.removeView(viewCandidate);
		}
		return viewCandidate;
	}

	@Override
	public View onCreateInputView() {
		ViewGroup vg = (ViewGroup) viewInput.getParent();
		if (vg != null) {
			vg.removeView(viewInput);
		}
		
		return viewInput;
	}

	@Override
	public void onStartInput(EditorInfo info, boolean restarting) {
		super.onStartInput(info, restarting);
		
		loadConfiguration();
		
		if (!restarting)
			viewInput.onNewInputTarget(info);
		
		connManager.setConnection(getCurrentInputConnection());
		
		updateInputViewShown();
	}

}
