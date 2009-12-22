package org.ime.vnime.txtproc;

class MemoryBasedProcessor extends BaseTxtProcessor {

	@Override
	public boolean chkValidPreConsonant(String consonant, String vowel) {
		if (consonant == null || consonant.trim().length() == 0)
			return true;

		boolean result = false;
		String lowerConsonant = consonant.toLowerCase();
		
		for (int i = 0; i < consonants.length; i++) {
			if (lowerConsonant.equals(consonants[i])) {
				result = true;
				break;
			}
		}
		
		return result;
	}

	@Override
	public boolean chkValidSufConsonant(String consonant, String vowel) {
		if (consonant == null || consonant.trim().length() == 0)
			return true;

		boolean result = false;
		String lowerConsonant = consonant.toLowerCase();
		
		if (vowel == null) {
			for (int i = 0; i < consonants.length; i++) {
				if (consonantSuffixValid[i] && lowerConsonant.equals(consonants[i])) {
					result = true;
					break;
				}
			}
		} else {
			String lowerVowel = vowel.toLowerCase();
			int vowelIndex = 0;
			for (int i = 0; i < vowels.length; i++) {
				if (vowels[i][0].equals(lowerVowel)) {
					vowelIndex = i + 1;
					break;
				}
			}
			if (vowelIndex > 0) {
				int[] candidates = new int[8];
				int candidateCount = 0;
				for (int i = 0; i < vcpairs.length; i++) {
					if (vcpairs[i][0] == vowelIndex) {
						candidates[candidateCount] = vcpairs[i][1];
						candidateCount++;
					} else if (vcpairs[i][0] > vowelIndex) {
						break;
					}
				}
				if (candidateCount > 0) {
					for (int i = 0; i < candidateCount; i++) {
						if (consonants[candidates[i] - 1].equals(lowerConsonant)) {
							result = true;
							break;
						}
					}
				}
			}
		}
		
		return result;
	}

	@Override
	public boolean chkValidVowel(String vowel) {
		return chkValidVowel(vowel, false);
	}

	@Override
	public String combineVowelWithTone(String vowel, Tones tone) {
		if (tone == null || tone == Tones.NONE || vowel == null)
			return vowel;
		
		int toneIndex = tone.toInt();
		String result = null;
		String lowerVowel = vowel.toLowerCase();
		
		for (int i = 0; i < vowels.length; i++) {
			if (vowels[i][0].equals(lowerVowel)) {
				result = matchCase(vowels[i][toneIndex], vowel);
				break;
			}
		}
		
		return result;
	}

	@Override
	public String morphVowel(String vowel, Marks mark) {
		if (vowel == null || mark == null || mark == Marks.BAR)
			return null;
		
		int markIndex = 0;
		switch (mark) {
		case BREVE:
			markIndex = 1;
			break;
		case CIRCUMFLEX:
			markIndex = 2;
			break;
		case HORN:
			markIndex = 3;
			break;
		}
		
		String result = null;
		String lowerVowel = vowel.toLowerCase();
		int vowelIndex = 0;
		for (int i = 0; i < vowels.length; i++) {
			if (vowels[i][0].equals(lowerVowel)) {
				vowelIndex = i + 1;
				break;
			}
		}
		if (vowelIndex > 0) {
			int resultIndex = 0;
			for (int i = 0; i < crossmorph.length; i++) {
				if (crossmorph[i][0] == vowelIndex) {
					resultIndex = crossmorph[i][markIndex];
					break;
				}
			}
			if (resultIndex > 0) {
				result = matchCase(vowels[resultIndex - 1][0], vowel);
			}
		}
		
		return result;
	}

	@Override
	public String extractVowel(String text) {
		String vowel = super.extractVowel(text);
		if (chkValidVowel(vowel, false))
			return vowel;
		else
			return null;
	}
	
	private boolean chkValidVowel(String vowel, boolean checkCandidates) {
		if (vowel == null)
			return false;
		
		boolean result = false;
		String lowerVowel = vowel.toLowerCase();
		
		for (int i = 0; i < vowels.length; i++) {
			if (vowels[i][0].equals(lowerVowel)) {
				if (checkCandidates) {
					result = vowelValid[i];
				} else {
					result = true;
				}
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Convert case of character in s1 to match those in s2.
	 * @param s1
	 * @param s2
	 * @return Result of the conversion.
	 */
	private String matchCase(String s1, String s2) {
		if (s1 == null)
			return null;
		if (s2 == null)
			return s1;
		int len1 = s1.length();
		int len2 = s2.length();
		if (len2 < len1)
			len1 = len2;
		
		String result = "";
		char c1;
		char c2;
		for (int i = 0; i < len1; i ++) {
			c1 = s1.charAt(i);
			c2 = s2.charAt(i);
			if (Character.isUpperCase(c2)) {
				result += Character.toUpperCase(c1);
			} else {
				result += Character.toLowerCase(c1);
			}
		}
		return result;
	}

	private static final String[][] vowels = new String[][] {
			{"a", "à", "á", "ả", "ã", "ạ"},
			{"ă", "ằ", "ắ", "ẳ", "ẵ", "ặ"},
			{"â", "ầ", "ấ", "ẩ", "ẫ", "ậ"},
			{"e", "è", "é", "ẻ", "ẽ", "ẹ"},
			{"ê", "ề", "ế", "ể", "ễ", "ệ"},
			{"i", "ì", "í", "ỉ", "ĩ", "ị"},
			{"o", "ò", "ó", "ỏ", "õ", "ọ"},
			{"ô", "ồ", "ố", "ổ", "ỗ", "ộ"},
			{"ơ", "ờ", "ớ", "ở", "ỡ", "ợ"},
			{"u", "ù", "ú", "ủ", "ũ", "ụ"},
			{"ư", "ừ", "ứ", "ử", "ữ", "ự"},
			{"y", "ỳ", "ý", "ỷ", "ỹ", "ỵ"},
			{"ai", "ài", "ái", "ải", "ãi", "ại"},
			{"ao", "ào", "áo", "ảo", "ão", "ạo"},
			{"au", "àu", "áu", "ảu", "ãu", "ạu"},
			{"ay", "ày", "áy", "ảy", "ãy", "ạy"},
			{"âu", "ầu", "ấu", "ẩu", "ẫu", "ậu"},
			{"ây", "ầy", "ấy", "ẩy", "ẫy", "ậy"},
			{"eo", "èo", "éo", "ẻo", "ẽo", "ẹo"},
			{"êu", "ều", "ếu", "ểu", "ễu", "ệu"},
			{"ia", "ìa", "ía", "ỉa", "ĩa", "ịa"},
			{"iê", "iề", "iế", "iể", "iễ", "iệ"},
			{"iu", "ìu", "íu", "ỉu", "ĩu", "ịu"},
			{"oa", "oà", "oá", "oả", "oã", "oạ"},
			{"oă", "oằ", "oắ", "oẳ", "oẵ", "oặ"},
			{"oe", "oè", "oé", "oẻ", "oẽ", "oẹ"},
			{"oi", "òi", "ói", "ỏi", "õi", "ọi"},
			{"oo", "oò", "oó", "oỏ", "oõ", "oọ"},
			{"ôi", "ồi", "ối", "ổi", "ỗi", "ội"},
			{"ơi", "ời", "ới", "ởi", "ỡi", "ợi"},
			{"ua", "ùa", "úa", "ủa", "ũa", "ụa"},
			{"uâ", "uầ", "uấ", "uẩ", "uẫ", "uậ"},
			{"ue", "uè", "ué", "uẻ", "uẽ", "uẹ"},
			{"uê", "uề", "uế", "uể", "uễ", "uệ"},
			{"ui", "ùi", "úi", "ủi", "ũi", "ụi"},
			{"uô", "uồ", "uố", "uổ", "uỗ", "uộ"},
			{"uy", "uỳ", "uý", "uỷ", "uỹ", "uỵ"},
			{"ưa", "ừa", "ứa", "ửa", "ữa", "ựa"},
			{"ưi", "ừi", "ứi", "ửi", "ữi", "ựi"},
			{"ươ", "ườ", "ướ", "ưở", "ưỡ", "ượ"},
			{"ưu", "ừu", "ứu", "ửu", "ữu", "ựu"},
			{"yê", "yề", "yế", "yể", "yễ", "yệ"},
			{"iêu", "iều", "iếu", "iểu", "iễu", "iệu"},
			{"uay", "uày", "uáy", "uảy", "uãy", "uạy"},
			{"uây", "uầy", "uấy", "uẩy", "uẫy", "uậy"},
			{"uôi", "uồi", "uối", "uổi", "uỗi", "uội"},
			{"uya", "uỳa", "uýa", "uỷa", "uỹa", "uỵa"},
			{"uyê", "uyề", "uyế", "uyể", "uyễ", "uyệ"},
			{"ươi", "ười", "ưới", "ưởi", "ưỡi", "ượi"},
			{"ươu", "ườu", "ướu", "ưởu", "ưỡu", "ượu"},
			{"yêu", "yều", "yếu", "yểu", "yễu", "yệu"},
			{"ueo", "uèo", "uéo", "uẻo", "uẽo", "uẹo"},
			{"oai", "oài", "oái", "oải", "oãi", "oại"},
			{"oay", "oày", "oáy", "oảy", "oãy", "oạy"},
			{"eu", "èu", "éu", "ẻu", "ẽu", "ẹu"},
			{"ie", "iè", "ié", "iẻ", "iẽ", "iẹ"},
			{"uo", "uò", "uó", "uỏ", "uõ", "uọ"},
			{"ưo", "ưò", "ưó", "ưỏ", "ưõ", "ưọ"},
			{"uu", "ùu", "úu", "ủu", "ũu", "ụu"},
			{"ye", "yè", "yé", "yẻ", "yẽ", "yẹ"},
			{"ieu", "ièu", "iéu", "iẻu", "iẽu", "iẹu"},
			{"uoi", "uòi", "uói", "uỏi", "uõi", "uọi"},
			{"uye", "uyè", "uyé", "uyẻ", "uyẽ", "uyẹ"},
			{"uou", "uòu", "uóu", "uỏu", "uõu", "uọu"},
			{"ưou", "ưòu", "ưóu", "ưỏu", "ưõu", "ưọu"},
			{"yeu", "yèu", "yéu", "yẻu", "yẽu", "yẹu"}
	};

	private static final boolean[] vowelValid = new boolean[] {
		true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
		true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
		true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
		true, true, true, true, true, true, false, false, false, false, false, false, false, false,
		false, false, false, false
	};

	private static final String[] consonants = new String[] {
			"b", "c", "ch", "d", "đ", "g", "gh", "h", "k", "kh", "l", "m", "n", "ng", "ngh", "nh", "p", "ph",
			"q", "r", "s", "t", "th", "tr", "v", "x", "gi", "qu"
	};
	
	private static final boolean[] consonantSuffixValid = new boolean[] {
			false, true, true, false, false, false, false, false, false, false, false, true, true, true, false, true, true, false,
			false, false, false, true, false, false, false, false, false, false
	};

	private static final int[][] crossmorph = new int[][] {
		{1, 2, 3, -1},
		{2, -2, 3, -1},
		{3, 2, -2, -1},
		{4, -1, 5, -1},
		{5, -1, -2, -1},
		{7, -1, 8, 9},
		{8, -1, -2, 9},
		{9, -1, 8, -2},
		{10, -1, -1, 11},
		{11, -1, -1, -2},
		{15, -1, 17, -1},
		{16, -1, 18, -1},
		{17, -1, -2, -1},
		{18, -1, -2, -1},
		{24, 25, -1, -1},
		{25, -2, -1, -1},
		{27, -1, 29, 30},
		{29, -1, -2, 30},
		{30, -1, 29, -2},
		{31, -1, 32, 38},
		{32, -1, -2, 38},
		{33, -1, 34, -1},
		{34, -1, -2, -1},
		{35, -1, -1, 39},
		{36, -1, -2, 40},
		{38, -1, 32, -2},
		{39, -1, -1, -2},
		{40, -1, 36, -2},
		{44, -1, 45, -1},
		{45, -1, -2, -1},
		{46, -1, -2, 49},
		{49, -1, 46, -2},
		{55, -1, 20, -1},
		{56, -1, 22, -1},
		{57, -1, 36, 40},
		{58, -1, 36, 40},
		{59, -1, -1, 41},
		{60, -1, 42, -1},
		{61, -1, 43, -1},
		{62, -1, 46, 49},
		{63, -1, 48, -1},
		{64, -1, -1, 50},
		{65, -1, -1, 50},
		{66, -1, 51, -1}
	};

	private static final int[][] vcpairs = new int[][] {
		{1, 2},
		{1, 3},
		{1, 12},
		{1, 13},
		{1, 14},
		{1, 16},
		{1, 17},
		{1, 22},
		{2, 2},
		{2, 12},
		{2, 13},
		{2, 14},
		{2, 17},
		{2, 22},
		{3, 2},
		{3, 12},
		{3, 13},
		{3, 14},
		{3, 17},
		{3, 22},
		{4, 2},
		{4, 3},
		{4, 12},
		{4, 13},
		{4, 14},
		{4, 16},
		{4, 17},
		{4, 22},
		{5, 2},
		{5, 3},
		{5, 12},
		{5, 13},
		{5, 16},
		{5, 17},
		{5, 22},
		{6, 2},
		{6, 3},
		{6, 12},
		{6, 13},
		{6, 16},
		{6, 17},
		{6, 22},
		{7, 2},
		{7, 12},
		{7, 13},
		{7, 14},
		{7, 17},
		{7, 22},
		{8, 2},
		{8, 12},
		{8, 13},
		{8, 14},
		{8, 17},
		{8, 22},
		{9, 12},
		{9, 13},
		{9, 17},
		{9, 22},
		{10, 2},
		{10, 12},
		{10, 13},
		{10, 14},
		{10, 17},
		{10, 22},
		{11, 2},
		{11, 12},
		{11, 13},
		{11, 14},
		{11, 22},
		{22, 2},
		{22, 12},
		{22, 13},
		{22, 14},
		{22, 17},
		{22, 22},
		{24, 2},
		{24, 3},
		{24, 12},
		{24, 13},
		{24, 14},
		{24, 16},
		{24, 17},
		{24, 22},
		{25, 2},
		{25, 12},
		{25, 13},
		{25, 14},
		{25, 22},
		{26, 13},
		{26, 22},
		{31, 13},
		{31, 14},
		{31, 22},
		{32, 13},
		{32, 14},
		{32, 22},
		{33, 2},
		{33, 3},
		{33, 13},
		{33, 16},
		{34, 13},
		{34, 16},
		{34, 2},
		{34, 3},
		{36, 2},
		{36, 12},
		{36, 13},
		{36, 14},
		{36, 22},
		{37, 2},
		{37, 3},
		{37, 13},
		{37, 16},
		{37, 17},
		{37, 22},
		{40, 2},
		{40, 12},
		{40, 13},
		{40, 14},
		{40, 17},
		{40, 22},
		{42, 12},
		{42, 13},
		{42, 14},
		{42, 22},
		{48, 13},
		{48, 22},
		{56, 2},
		{56, 12},
		{56, 13},
		{56, 14},
		{56, 17},
		{56, 22},
		{57, 2},
		{57, 12},
		{57, 13},
		{57, 14},
		{57, 17},
		{57, 22},
		{58, 2},
		{58, 12},
		{58, 13},
		{58, 14},
		{58, 17},
		{58, 22},
		{60, 12},
		{60, 13},
		{60, 14},
		{60, 17},
		{60, 22},
		{63, 13},
		{63, 22}
	};
}
