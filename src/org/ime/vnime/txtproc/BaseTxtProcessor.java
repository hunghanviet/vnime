package org.ime.vnime.txtproc;


abstract class BaseTxtProcessor implements TxtProcessor {

	@Override
	public Tones extractTone(String text) {
		if (text == null)
			return null;
		
		Tones tone = null;
		int len = text.length();
		char c;
		int index;
		
		for (int i = 0; i < len; i++) {
			c = text.charAt(i);
			index = lowerVowels.indexOf(c);
			if (index < 0)
				index = upperVowels.indexOf(c);
			if (index >= 0) {
				index = index%6;
				if (tone == null || (tone == Tones.NONE && index > 0)) {
					tone = Tones.fromInt(index);
				} else if (index > 0) {
					/* More than 1 tone existed in the text */
					tone = null;
					break;
				}
			}
		}
		
		return tone;
	}
	
	@Override
	public String removeTones(String text) {
		if (text == null)
			return null;
		
		String result = "";
		int len = text.length();
		char c;
		int j;
		for (int i = 0; i < len; i++) {
			c = text.charAt(i);
			j = lowerVowels.indexOf(c);
			if (j >= 0) {
				result += lowerVowels.charAt(j/6*6);
			} else {
				j = upperVowels.indexOf(c);
				if (j >= 0) {
					result += upperVowels.charAt(j/6*6);
				} else {
					result += c;
				}
			}
		}
		return result;
	}

	@Override
	public boolean isMonoConsonant(char c) {
		return lowerConsonants.indexOf(c) >= 0 || upperConsonants.indexOf(c) >= 0;
	}

	@Override
	public boolean isMonophthong(char c) {
		return lowerVowels.indexOf(c) >= 0 || upperVowels.indexOf(c) >= 0;
	}

	@Override
	public boolean isWordDelimiter(char c) {
		return wordDelimiters.indexOf(c) >= 0;
	}
	
	@Override
	public boolean isPunctuation(char c) {
		return punctuations.indexOf(c) >= 0;
	}

	@Override
	public String removeMarks(String text) {
		if (text == null)
			return null;
		
		String result = "";
		int len = text.length();
		char c;
		int j;
		String vowelList;
		for (int i = 0; i < len; i++) {
			c = text.charAt(i);
			vowelList = lowerVowels;
			j = vowelList.indexOf(c);
			if (j < 0) {
				vowelList = upperVowels;
				j = vowelList.indexOf(c);
			}
			if (j >= 0) {
				if ((j <= 5) || (j >= 18 && j <= 23) || (j >= 30 && j <= 41) || (j >= 54 && j <= 59) || (j >= 66 && j <= 71)) {
					result += c;
				} else if (j >= 6 && j <= 17) {
					result += vowelList.charAt(j - j/6*6);
				} else if (j >= 34 && j <= 29) {
					result += vowelList.charAt(j - 6);
				} else if (j >= 42 && j <= 53) {
					result += vowelList.charAt(j - (j - 36)/6*6);
				} else if (j >= 60 && j <= 65) {
					result += vowelList.charAt(j - 6);
				}
			} else {
				if (c == 'đ')
					result += 'd';
				else if (c == 'Đ')
					result += 'D';
				else
					result += c;
			}
		}
		
		return result;
	}

	@Override
	public String extractVowel(String text) {
		if (text == null)
			return null;
		
		/* Take out the vowel part */
		int len = text.length();
		char c;
		int i;		/* index of the first vowel */
		int j;		/* index of the last vowel */
		for (i = 0; i < len; i++) {
			c = text.charAt(i);
			if (lowerVowels.indexOf(c) >= 0 || upperVowels.indexOf(c) >= 0)
				break;
		}
		for (j = len - 1; j >= 0; j--) {
			c = text.charAt(j);
			if (lowerVowels.indexOf(c) >= 0 || upperVowels.indexOf(c) >= 0)
				break;
		}
		
		/* Check if there is a vowel in the text */
		if (i > j) {
			return null;
		}
		
		text = removeTones(text);
		
		/* Check for the case of "gi" and "qu" consonant */
		if (i == 1 && (j > i)) {
			String s = text.substring(0, 2).toLowerCase();
			if (s.equals("gi") || s.equals("qu")) {
				i++;
			}
		}
		
		return text.substring(i, j + 1);
	}
	
	protected static final String wordDelimiters = " \t\n\r.,?;:[]{}()<>+-*/=|\\&^%$#@!~";
	protected static final String punctuations = "\n\r.?!";
	protected static final String lowerVowels = "aàáảãạăằắẳẵặâầấẩẫậeèéẻẽẹêềếểễệiìíỉĩịoòóỏõọôồốổỗộơờớởỡợuùúủũụưừứửữựyỳýỷỹỵ";
	protected static final String upperVowels = "AÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬEÈÉẺẼẸÊỀẾỂỄỆIÌÍỈĨỊOÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢUÙÚỦŨỤƯỪỨỬỮỰYỲÝỶỸỴ";
	protected static final String lowerConsonants = "bcdđghklmnpqrstvx";
	protected static final String upperConsonants = "BCDĐGHKLMNPQRSTVX";
	
}
