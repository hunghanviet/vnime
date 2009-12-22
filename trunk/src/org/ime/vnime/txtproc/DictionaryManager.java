package org.ime.vnime.txtproc;

import android.database.Cursor;

public interface DictionaryManager {
	
	/**
	 * Find all Vietnamese word that begins and ends with given prefix and
	 * suffix.
	 * @param prefix
	 * @param suffix
	 * @param maxCount Maximum number of result
	 * @return
	 */
	public String[] getCandidates(String prefix, String suffix, int maxCount);
	
	public boolean checkCandidateExist(String prefix, String suffix);
	
	public Cursor getAllUserWord();
	
	public boolean addUserWord(String word);
	
	public void removeUserWord(String word);
	
	public void clearUserDict();
}
