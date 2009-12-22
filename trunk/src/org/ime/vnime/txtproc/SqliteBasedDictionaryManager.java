package org.ime.vnime.txtproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Vector;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


class SqliteBasedDictionaryManager implements DictionaryManager {
	
	private UserDbOpenHelper userDbOpenHelper;
	private DictDbOpenHelper dictDbOpenHelper;
	
	public SqliteBasedDictionaryManager(Context ctx) {
		userDbOpenHelper = UserDbOpenHelper.getInstance(ctx);

		int dbVer;
		try {
			dbVer = ctx.getPackageManager().getPackageInfo("org.ime.vnime", 0).versionCode;
		} catch (NameNotFoundException e) {
			dbVer = 100; /* Supposedly version 1.0 */
		}
		dictDbOpenHelper = new DictDbOpenHelper(ctx, dbVer);
	}

	@Override
	public boolean addUserWord(String word) {
		if (word == null || word.trim().length() == 0)
			return false;
		
		/* Check if the word existed in the standard dictionary */
		boolean wordExisted = false;
		SQLiteDatabase dbDict = dictDbOpenHelper.getReadableDatabase();
		String sql = "SELECT word FROM vndict_singleword WHERE word = ?";
		String[] params = new String[] {word};
		Cursor cursor = dbDict.rawQuery(sql, params);
		if (cursor != null) {
			if (cursor.getCount() > 0)
				wordExisted = true;
			cursor.close();
		}
		
		if (!wordExisted) {
			SQLiteDatabase dbUser = userDbOpenHelper.getWritableDatabase();
			sql = "INSERT INTO userdict(word) VALUES(?)";
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
			query = "SELECT word FROM vndict_singleword WHERE word LIKE ? ORDER BY word LIMIT " + maxCount ;
			params = new String[] {filter};
			SQLiteDatabase dbDict = dictDbOpenHelper.getReadableDatabase();
			cursor = dbDict.rawQuery(query, params);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					result.add(cursor.getString(0));
					while (cursor.moveToNext()) {
						result.add(cursor.getString(0));
					}
				}
				cursor.close();
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
			
			
			String[] sResult = new String[result.size()];
			Iterator<String> itrt = result.iterator();
			int i = 0;
			String sTmp;
			while (itrt.hasNext()) {
				sTmp = itrt.next();
				if (capitalize)
					sTmp = "" + Character.toUpperCase(sTmp.charAt(0)) + sTmp.substring(1);
				else if (allUpper)
					sTmp = sTmp.toUpperCase();
				sResult[i] = sTmp;
				i++;
			}
			return sResult;
		} else {
			return null;
		}
	}

	@Override
	public Cursor getAllUserWord() {
		SQLiteDatabase db = userDbOpenHelper.getWritableDatabase();
		String sql = "SELECT id as _id, word FROM userdict ORDER BY word";
		return db.rawQuery(sql, null);
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
	
	private class DictDbOpenHelper extends SQLiteOpenHelper {
		
		private static final String dbName = "vnime_dict.sqlite";
		
		public DictDbOpenHelper(Context ctx, int dbVer) {
			super(ctx, dbName, null, dbVer);
			
			context = ctx;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			
			InputStream is = context.getResources().openRawResource(org.ime.vnime.R.raw.wordlist74k_lowercase_single);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String sql;
			try {
				while((sql = br.readLine()) != null) {
					db.execSQL(sql);
				}
				br.close();
				isr.close();
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			/* Do nothing */
		}
		
		private Context context;
	}

}
