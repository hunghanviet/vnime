package org.ime.vnime.txtproc;

public interface TextChangedListener {
	
	public void onTextChanged(CharSequence newText);
	
	public void onNewWord();
	
	public void onNewSentence();
}
