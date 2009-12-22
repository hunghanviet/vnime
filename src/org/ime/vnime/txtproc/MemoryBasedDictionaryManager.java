package org.ime.vnime.txtproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class MemoryBasedDictionaryManager implements DictionaryManager {
	
	public MemoryBasedDictionaryManager(Context ctx) {
		userDbOpenHelper = UserDbOpenHelper.getInstance(ctx);
		
		loadDictionary(ctx);
	}

	private void loadDictionary(Context ctx) {
		InputStream is = ctx.getResources().openRawResource(org.ime.vnime.R.raw.wordlist74k_lowercase_single_raw);
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		try {
			String line = br.readLine();
			/* Detect dictionary size */
			int len = line.length();
			int i, j;
			for (i = 0; i < len && line.charAt(i) != '#'; i++);
			i++;
			for (j = i; j < len && line.charAt(j) != '#'; j++);
			int size = Integer.parseInt(line.substring(i, j));
			
			dictSingleWords = new String[size];
			
			i = 0;
			while ((line = br.readLine()) != null && i < size) {
				dictSingleWords[i] = line;
				i++;
			}
			
			br.close();
			isr.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean addUserWord(String word) {
		if (word == null || word.trim().length() == 0)
			return false;
		
		/* Check if the word existed in the standard dictionary */
		boolean wordExisted = checkWordExistInStandardDict(word);
		
		
		if (!wordExisted) {
			SQLiteDatabase dbUser = userDbOpenHelper.getWritableDatabase();
			String sql = "INSERT INTO userdict(word) VALUES(?)";
			String[] params = new String[] {word};
			dbUser.execSQL(sql, params);
		}
		
		return !wordExisted;
	}

	@Override
	public boolean checkCandidateExist(String prefix, String suffix) {
		String[] ar = getCandidates(prefix, suffix, 1);
		return ar != null && ar.length > 0;
	}

	@Override
	public void clearUserDict() {
		SQLiteDatabase db = userDbOpenHelper.getWritableDatabase();
		String sql = "DELETE FROM userdict";
		db.execSQL(sql);
	}

	@Override
	public Cursor getAllUserWord() {
		SQLiteDatabase db = userDbOpenHelper.getWritableDatabase();
		String sql = "SELECT id as _id, word FROM userdict ORDER BY word";
		return db.rawQuery(sql, null);
	}

	@Override
	public String[] getCandidates(String prefix, String suffix, int maxCount) {
		if ((prefix == null && suffix == null) || maxCount <= 0)
			return null;
		
		Vector<String> result = new Vector<String>();
		
		maxCount++;
		
		if (prefix == null)
			prefix = "";
		if (suffix == null)
			suffix = "";
		String filter = prefix.toLowerCase() + "%" + suffix.toLowerCase();

		SQLiteDatabase dbUser = userDbOpenHelper.getReadableDatabase();
		
		String query = "SELECT word FROM userdict WHERE word LIKE ? ORDER BY word LIMIT " + maxCount ;
		String[] params = new String[] {filter};
		Cursor cursor = dbUser.rawQuery(query, params);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				result.add(cursor.getString(0));
				while (cursor.moveToNext()) {
					result.add(cursor.getString(0));
				}
			}
			cursor.close();
		}
		
		int count = result.size();
		if (count < maxCount) {
			maxCount -= count;
			Vector<String> standardDictResult = getCandidatesFromStandardDict(prefix, suffix, maxCount);
			Iterator<String> itrt = standardDictResult.iterator();
			while (itrt.hasNext()) {
				result.add(itrt.next());
			}
		}
		
		count = result.size();
		
		if (count > 0) {
			if (result.get(0).equals(prefix.toLowerCase() + suffix.toLowerCase())) {
				result.remove(0);
				count--;
			}
		}
		
		if (count > 0) {
			int firstUpper = -1;
			int lastUpper = -1;
			int len = prefix.length();
			for (int i = 0; i < len; i++) {
				if (Character.isUpperCase(prefix.charAt(i))) {
					if (firstUpper < 0)
						firstUpper = i;
					lastUpper = i;
				}
			}
			boolean capitalize = (firstUpper == 0) && (lastUpper == 0);
			boolean allUpper = (firstUpper == 0) && (lastUpper == len - 1);
			
			
			String[] arResult = new String[result.size()];
			Iterator<String> itrt = result.iterator();
			int i = 0;
			String sTmp;
			while (itrt.hasNext()) {
				sTmp = itrt.next();
				if (capitalize)
					sTmp = "" + Character.toUpperCase(sTmp.charAt(0)) + sTmp.substring(1);
				else if (allUpper)
					sTmp = sTmp.toUpperCase();
				arResult[i] = sTmp;
				i++;
			}
			return arResult;
		} else {
			return null;
		}
	}

	@Override
	public void removeUserWord(String word) {
		if (word == null || word.trim().length() == 0)
			return;
		
		SQLiteDatabase db = userDbOpenHelper.getWritableDatabase();
		String sql = "DELETE FROM userdict WHERE word = ?";
		String[] params = new String[] {word};
		db.execSQL(sql, params);
	}
	
	private boolean checkWordExistInStandardDict(String word) {
		int begin = 0;
		int end = dictSingleWords.length - 1;
		int middle = (end - begin)/2;
		int compareResult;
		while ((compareResult = word.compareToIgnoreCase(dictSingleWords[middle = (begin + end)/2])) != 0) {
			if (compareResult > 0) {
				begin = middle + 1;
			}
			else {
				end = middle - 1;
			}
			if (begin > end)
				break;
		}
		
		return compareResult == 0;
	}
	
	private Vector<String> getCandidatesFromStandardDict(String prefix, String suffix, int maxCount) {
		Vector<String> result = new Vector<String>();

		prefix = prefix.toLowerCase();
		suffix = suffix.toLowerCase();
		
		int count = 0;
		int begin = 0;
		int end = dictSingleWords.length - 1;
		int middle;
		int compareResult;
		boolean foundPrefix = false;
		
		/* Find prefix */
		if (prefix.trim().length() > 0) {
			while (!(foundPrefix = dictSingleWords[middle = (begin + end)/2].startsWith(prefix))) {
				compareResult = prefix.compareToIgnoreCase(dictSingleWords[middle]);
				if (compareResult > 0) {
					begin = middle + 1;
				}
				else {
					end = middle - 1;
				}
				if (begin > end)
					break;
			}
			if (foundPrefix) {
				for (begin = middle; begin >= 0 && dictSingleWords[begin].startsWith(prefix); begin--);
				begin++;
				for (end = middle; end < dictSingleWords.length && dictSingleWords[end].startsWith(prefix); end++);
				end--;
			}
		} else {
			foundPrefix = true;
		}
		
		if (foundPrefix) {
			if (suffix.trim().length() > 0) {
				/* Find suffix */
				for (int i = begin; i <= end && count < maxCount; i++) {
					if (dictSingleWords[i].endsWith(suffix)) {
						result.add(dictSingleWords[i]);
						count++;
					}
				}
			} else {
				for (int i = begin; i <= end && count < maxCount; i++) {
					result.add(dictSingleWords[i]);
					count++;
				}
			}
		}
		
		return result;
	}

	private UserDbOpenHelper userDbOpenHelper;
	private String[] dictSingleWords;
}
