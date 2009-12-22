package org.ime.vnime.txtproc;

import java.security.InvalidParameterException;

import org.ime.vnime.txtproc.TxtProcessor.Marks;
import org.ime.vnime.txtproc.TxtProcessor.Tones;

public class Word {
	
	public Word(TxtProcessor tp) {
		this(tp, null);
	}
	
	public Word(TxtProcessor tp, String text) {
		if (tp == null)
			throw new InvalidParameterException("TxtProcessor object must not be null");
		txtProcessor = tp;
		
		if (text != null && text.length() > 0) {
			rawText = text;
			syncWithRawText(false);
		}
	}

	String rawText;
	String vowel;
	String preConsonant;
	String sufConsonant;
	Tones tone;
	boolean bValidVowel = false;
	boolean bValidStructure = false;
	boolean bValidMeaning = false;
	
	TxtProcessor txtProcessor;
	
	public boolean hasValidVowel() {
		return bValidVowel;
	}
	
	/**
	 * 
	 * @return <b>true</b> if the word has a valid structure of Vietnamese
	 * word:<br>
	 * <b>[consonant] + {vowel} + [consonant]</b>
	 */
	public boolean hasValidStructure() {
		return bValidStructure;
	}
	
	public boolean hasValidCombination() {
		if (hasValidStructure() && hasValidVowel()) {
			if ((sufConsonant.equals("c") || sufConsonant.equals("ch") || sufConsonant.equals("p") || sufConsonant.equals("t")) && 
					tone != Tones.ACUTE && tone != Tones.DOT)
				return false;
			
			return txtProcessor.chkValidPreConsonant(preConsonant, vowel) && txtProcessor.chkValidSufConsonant(sufConsonant, vowel);
		} else {
			return false;
		}
	}
	
	public boolean putTone(Tones tone) {
		if (hasValidStructure() && hasValidVowel() && tone != this.tone) {
			this.tone = tone;
			syncWithRawText(true);
			return true;
		}
		return false;
	}
	
	public boolean putMark(Marks mark) {
		if (hasValidStructure()) {
			if (mark == Marks.BAR) {
				if (preConsonant != null) {
					if (preConsonant.equals("d")) {
						preConsonant = "đ";
						syncWithRawText(true);
						return true;
					} else if (preConsonant.equals("D")) {
						preConsonant = "Đ";
						syncWithRawText(true);
						return true;
					}
				}
			} else {
				String newVowel = txtProcessor.morphVowel(vowel, mark);
				if (newVowel != null) {
					vowel = newVowel;
					bValidVowel = true;
					syncWithRawText(true);
					return true;
				}
			}
		}
		return false;
	}
	
	public void removeMark() {
		String newVowel = txtProcessor.removeMarks(vowel);
		if (newVowel != null) {
			vowel = newVowel;
			syncWithRawText(true);
		}
	}
	
	public void putChar(char c, int index) {
		if (rawText == null && index == 0)
			rawText = "" + c;
		else if (index >= 0 && index < rawText.length())
			rawText = rawText.substring(0, index) + c + rawText.substring(index);
		else if (rawText == null)
			rawText = "" + c;
		else
			rawText += c;
		syncWithRawText(false);
		
//		if (hasValidStructure()) {
//			if (txtProcessor.isMonophthong(c)) {
//				/* If c is a monophthong */
//				int vowelStartIndex = 0;
//				int vowelLen = vowel.length();
//				if (preConsonant != null)
//					vowelStartIndex = preConsonant.length();
//				if (index >= 0) {
//					if (index >= vowelStartIndex && index <= vowelStartIndex + vowelLen) {
//						/* If c will be insert to vowel part */
//						vowel = vowel.substring(0, index - vowelStartIndex) + c
//								+ vowel.substring(index - vowelStartIndex);
//						bValidVowel = txtProcessor.chkValidVowel(vowel);
//						syncWithRawText(true);
//					} else {
//						/* If c will be insert to consonant part, it breaks the word structure */
//						rawText = rawText.substring(0, index) + c + rawText.substring(index);
//						bValidStructure = false;
//					}
//				} else {
//					/* index < 0 -> append c to the end of the word */
//					if (sufConsonant == null) {
//						vowel += c;
//						bValidVowel = txtProcessor.chkValidVowel(vowel);
//						syncWithRawText(true);
//					} else {
//						rawText += c;
//						bValidStructure = false;
//					}
//				}
//			} else if (txtProcessor.isMonoConsonant(c)){
//				int vowelStartIndex = 0;
//				int vowelLen = vowel.length();
//				if (preConsonant != null)
//					vowelStartIndex = preConsonant.length();
//				if (index >= 0 && index <= vowelStartIndex) {
//					if (preConsonant != null)
//						preConsonant = preConsonant.substring(0, index) + c
//										+ preConsonant.substring(index);
//					else
//						preConsonant = "" + c;
//					bValidStructure = txtProcessor.chkValidPreConsonant(preConsonant, null);
//				} else if (index > vowelStartIndex && index < vowelStartIndex + vowelLen) {
//					rawText = rawText.substring(0, index) + c + rawText.substring(index);
//					bValidStructure = false;
//				} else {
//					if (sufConsonant == null)
//						sufConsonant = "" + c;
//					else {
//						if (index < 0)
//							sufConsonant += c;
//						else
//							sufConsonant = sufConsonant.substring(0, index - (vowelStartIndex + vowelLen))
//											+ c
//											+ sufConsonant.substring(index - (vowelStartIndex + vowelLen));
//						bValidStructure = txtProcessor.chkValidSufConsonant(sufConsonant, null);
//					}
//				}
//			} else {
//				rawText = rawText.substring(0, index) + c + rawText.substring(index);
//				bValidStructure = false;
//			}
//		} else {
//			if (rawText != null)
//				rawText = rawText.substring(0, index) + c + rawText.substring(index);
//			else if (index == 0)
//				rawText = "" + c;
//		}
	}

	@Override
	public String toString() {
		return rawText;
	}
	
	/**
	 * Synchronize word components (consonants, vowel, tone) with raw text
	 * @param updateRawText If <b>true</b>, update raw text. Otherwise update
	 * components.
	 */
	private void syncWithRawText(boolean updateRawText) {
		if (updateRawText) {
			if (hasValidStructure()) {
				rawText = "";
				if (preConsonant != null)
					rawText += preConsonant;
				
				String combinedVowel;
				if (hasValidVowel() && tone != null)
					combinedVowel = txtProcessor.combineVowelWithTone(vowel, tone);
				else
					combinedVowel = vowel;
				if (combinedVowel != null)
					rawText += combinedVowel;

				if (sufConsonant != null)
					rawText += sufConsonant;
			}
		} else {
			if (rawText != null) {
				vowel = txtProcessor.extractVowel(rawText);
				tone = txtProcessor.extractTone(rawText);
				if (vowel != null && tone != null) {
					String rawTextNoTone = txtProcessor.removeTones(rawText);
					int index = rawTextNoTone.indexOf(vowel);
					preConsonant = rawTextNoTone.substring(0, index);
					sufConsonant = rawTextNoTone.substring(index + vowel.length());
					bValidStructure = txtProcessor.chkValidPreConsonant(preConsonant, null)
										&& txtProcessor.chkValidSufConsonant(sufConsonant, vowel);
					bValidVowel = txtProcessor.chkValidVowel(vowel);
				} else {
					if (txtProcessor.chkValidPreConsonant(rawText, null)) {
						preConsonant = rawText;
						bValidStructure = true;
					} else {
						preConsonant = null;
						bValidStructure = false;
					}
					vowel = null;
					tone = null;
					sufConsonant = null;
					bValidVowel = false;
				}
			}
		}
	}
}
