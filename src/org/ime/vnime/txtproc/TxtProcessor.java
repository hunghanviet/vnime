package org.ime.vnime.txtproc;

public interface TxtProcessor {
	
	public enum Tones {
		NONE,
		GRAVE, /* ` */
		ACUTE, /* ´ */
		HOOK,  /* ̉ */
		TIDLE, /* ~ */
		DOT,   /* . */
		;

		public int toInt() {
			return this.ordinal();
		}
		
		public static Tones fromInt(int n) {
			for (Tones t : Tones.values()) {
				if (t.toInt() == n)
					return t;
			}
			return null;
		}
	}
	
	public enum Marks {
		BREVE,      /* ˘ */
		CIRCUMFLEX, /* ˆ */
		HORN,       /* ' */
		BAR,        /* - */
	}
	
	/**
	 * Check if a character is a <i>monophthong</i> (single vowel)
	 * @param c
	 * @return
	 */
	public boolean isMonophthong(char c);
	
	/**
	 * Check if a character is a single consonant
	 * @param c
	 * @return
	 */
	public boolean isMonoConsonant(char c);
	
	/**
	 * Check if a character is a word separator
	 * @param c
	 * @return
	 */
	public boolean isWordDelimiter(char c);

	/**
	 * Check if a character is a punctuation
	 * @param c
	 * @return
	 */
	public boolean isPunctuation(char c);
	
	/**
	 * Check if the given vowel is valid
	 * @param vowel
	 * @return <b>true</b> if the given text is a valid vowel with no tone
	 */
	public boolean chkValidVowel(String vowel);
	
	/**
	 * Check if the given consonant is a valid consonant that can be placed
	 * before the given vowel.
	 * @param consonant
	 * @param vowel A valid vowel that contains no tone
	 * @return Return <b>true</b> in at least one of the following cases:<br>
	 * - <i>consonant</i> is <b>null</b> or empty<br>
	 * - <i>consonant</i> is valid, <i>vowel</i> is null<br>
	 * - <i>consonant</i> is valid, <i>vowel</i> is valid, and <i>consonant</i>
	 * can be place before <i>vowel</i><br>
	 * Otherwise return <b>false</b>.
	 */
	public boolean chkValidPreConsonant(String consonant, String vowel);
	
	/**
	 * Check if the given consonant is a valid consonant that can be placed
	 * after the given vowel.
	 * @param consonant
	 * @param vowel A valid vowel that contains no tone
	 * @return Return <b>true</b> in at least one of the following cases:<br>
	 * - <i>consonant</i> is <b>null</b> or empty<br>
	 * - <i>consonant</i> is valid, <i>vowel</i> is null, and <i>consonant</i>
	 * can be after at least one (whatever) vowel<br>
	 * - <i>consonant</i> is valid, <i>vowel</i> is valid, and <i>consonant</i>
	 * can be after <i>vowel</i><br>
	 * Otherwise return <b>false</b>.
	 */
	public boolean chkValidSufConsonant(String consonant, String vowel);
	
	/**
	 * Extract the <i>vowel part</i> of the text.<br><br>
	 * The <i>vowel part</i> is a vowel or a <i>vowel candidate</i> that can be turned
	 * to become a valid vowel just by putting mark.<br><br>
	 * This function checks word structure (<i>[con]{vowel}[con]</i>) but does not check
	 * the validity of the consonants.
	 * @param text
	 * @return The vowel part of the text
	 */
	public String extractVowel(String text);
	
	/**
	 * Extracting tone from the text.
	 * The input text must contain exactly one tone. Otherwise this function
	 * shall resurn <b>null</b>.
	 * @param text
	 * @return
	 */
	public Tones extractTone(String text);
	
	/**
	 * Remove all existing tones from the text.
	 * @param text
	 * @return The text which has no tone
	 */
	public String removeTones(String text);
	
	/**
	 * Combine a vowel with a tone.
	 * @param vowel A valid vowel with no tone
	 * @param tone
	 * @return The result text, or <b>null</b> if the input vowel is not a
	 * valid vowel.
	 */
	public String combineVowelWithTone(String vowel, Tones tone);
	
	/**
	 * Transform a vowel or <i>vowel candidate</i> to another vowel 
	 * @param vowel
	 * @param mark The mark of new vowel.
	 * @return Guarantee to be a valid vowel, or null if the tranformation
	 * could not create a valid vowel.
	 */
	public String morphVowel(String vowel, Marks mark);
	
	/**
	 * Remove all vowel mark from given text
	 * @param text
	 * @return
	 */
	public String removeMarks(String text);
}
