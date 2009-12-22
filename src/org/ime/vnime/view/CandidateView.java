package org.ime.vnime.view;

import org.ime.vnime.R;
import org.ime.vnime.SoftKeyboardListener;
import org.ime.vnime.SoftKeyboardListener.TextOrigins;
import org.ime.vnime.txtproc.DictionaryManager;
import org.ime.vnime.txtproc.MacroManager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CandidateView extends LinearLayout {

	public CandidateView(Context context) {
		this(context, null);
	}

	public CandidateView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setDictionaryManager(DictionaryManager manager) {
		dictManager = manager;
	}
	
	public void setMacroManager(MacroManager manager) {
		macroManager = manager;
	}
	
	public void setMacroEnabled(boolean enabled) {
		macroEnabled = enabled;
	}
	
	public void setSoftKeyboardListener(SoftKeyboardListener listener) {
		keyEventListener = listener;
	}
	
	public void updateCandidateList(String seed) {
		clearContent();
		
		int candidateViewWidth = getWidth();
		
		if (macroEnabled) {
			String macroValue = macroManager.expandMacro(seed, true);
			if (macroValue != null && macroValue.length() > 0) {
				macroResult.setText(macroValue);
				addView(macroResult);
				candidateViewWidth -= macroResult.getWidth();
			}
		}
		
		if (candidateViewWidth <= 0)
			return;
		
		String[] words = dictManager.getCandidates(seed, null, MAX_SEARCH_RESULT);
		int i = 0;
		if (words != null && words.length > 0) {
			while (candidateViewWidth > 0 && i < words.length) {
				TextView txtWord = new TextView(getContext());
				txtWord.setTextColor(getContext().getResources().getColor(R.color.svc_candidate_foreground));
				txtWord.setBackgroundResource(R.drawable.img_candidate_item_background);
				txtWord.setPadding(8, 8, 8, 8);
				txtWord.setLines(1);
				txtWord.setOnClickListener(itemClickListener);
				txtWord.setText(words[i]);
				addView(txtWord);
				i++;
				candidateViewWidth -= txtWord.getWidth();
			}
		}
	}
	
	private void clearContent() {
		if (macroResult == null) {
			macroResult = (TextView) findViewWithTag("txtMacro");
			macroResult.setOnClickListener(itemClickListener);
		}
		if (dictResult == null) {
			dictResult = (TextView) findViewWithTag("txtWord");
			dictResult.setOnClickListener(itemClickListener);
		}

		removeAllViews();
	}
	
	private OnClickListener itemClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String text = ((TextView)v).getText().toString();
			if (keyEventListener != null)
				keyEventListener.onText(text, TextOrigins.CANDIDATE_VIEW);
		}
	};

	private DictionaryManager dictManager;
	private MacroManager macroManager;
	private boolean macroEnabled = true;
	private SoftKeyboardListener keyEventListener;
	
	private TextView macroResult;
	private TextView dictResult;

	private static final int MAX_SEARCH_RESULT = 10;
}
