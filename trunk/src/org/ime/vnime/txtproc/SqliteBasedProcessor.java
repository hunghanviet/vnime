package org.ime.vnime.txtproc;

import java.security.InvalidParameterException;

import org.ime.vnime.R;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class SqliteBasedProcessor extends BaseTxtProcessor {
	
	private DbOpenHelper dbOpenHelper;
	
	public SqliteBasedProcessor(Context ctx) {
		int dbVer; 
		try {
			dbVer = ctx.getPackageManager().getPackageInfo("org.ime.vnime", 0).versionCode;
		} catch (NameNotFoundException e) {
			dbVer = 1000000; /* Supposedly version 1.0-0000 */
		}
		dbOpenHelper = new DbOpenHelper(ctx, dbVer);
	}

	@Override
	public boolean chkValidPreConsonant(String consonant, String vowel) {
		if (consonant == null || consonant.trim().length() == 0)
			return true;

		boolean result = false;
		
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		String query = "SELECT id FROM consonants WHERE text = ?";
		String[] queryArgs = new String[1];
		queryArgs[0] = consonant.toLowerCase();
		
		Cursor cursor = db.rawQuery(query, queryArgs);
		result = (cursor != null) && (cursor.getCount() > 0);
		if (cursor != null)
			cursor.close();
		return result;
	}

	@Override
	public boolean chkValidSufConsonant(String consonant, String vowel) {
		if (consonant == null || consonant.trim().length() == 0)
			return true;

		boolean result = false;
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		
		if (vowel == null) {
			String query = "SELECT id FROM consonants WHERE text = ? AND suffix = 1";
			String[] queryArgs = new String[1];
			queryArgs[0] = consonant.toLowerCase();
			
			Cursor cursor = db.rawQuery(query, queryArgs);
			result = (cursor != null) && (cursor.getCount() > 0);
			if (cursor != null)
				cursor.close();
			return result;
		} else {
			String query = "SELECT tbl2.id FROM ((consonants INNER JOIN vcpairs ON consonants.id = vcpairs.consonantid) AS tbl1 INNER JOIN vowels ON tbl1.vowelid = vowels.id) AS tbl2 WHERE tbl2.text = ? AND tbl2.root = ?";
			String[] queryArgs = new String[2];
			queryArgs[0] = consonant.toLowerCase();
			queryArgs[1] = vowel.toLowerCase();
			
			Cursor cursor = db.rawQuery(query, queryArgs);
			result = (cursor != null) && (cursor.getCount() > 0);
			if (cursor != null)
				cursor.close();
			return result;
		}
	}

	@Override
	public boolean chkValidVowel(String vowel) {
		return chkValidVowel(vowel, false);
	}

	@Override
	public String combineVowelWithTone(String vowel, Tones tone) {
		if (tone != Tones.NONE) {
			SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
			String query = "SELECT " + tone + " FROM vowels WHERE root = ?";
			String[] queryArgs = new String[1];
			queryArgs[0] = vowel.toLowerCase();
			
			Cursor cursor = db.rawQuery(query, queryArgs);
			if (cursor != null && cursor.moveToFirst()) {
				String out = cursor.getString(0);
				cursor.close();
				return matchCase(out, vowel);
			}
			if (cursor != null)
				cursor.close();
			return null;
		} else {
			return vowel;
		}
	}

	@Override
	public String extractVowel(String text) {
		String vowel = super.extractVowel(text);
		if (chkValidVowel(vowel, true))
			return vowel;
		else
			return null;
	}

	@Override
	public String morphVowel(String vowel, Marks mark) {
		if (vowel == null || mark == null || mark == Marks.BAR)
			return null;

		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		String query = "SELECT root FROM vowels WHERE id = (SELECT " + mark + " FROM crossmorph WHERE vowelid = (SELECT id FROM vowels WHERE root = ?))";
		String[] queryArgs = new String[1];
		queryArgs[0] = vowel.toLowerCase();
		
		Cursor cursor = db.rawQuery(query, queryArgs);
		if (cursor != null && cursor.moveToFirst()) {
			String out = cursor.getString(0);
			cursor.close();
			return matchCase(out, vowel);
		}

		if (cursor != null)
			cursor.close();
		return null;
	}
	
	private boolean chkValidVowel(String vowel, boolean checkCandidates) {
		if (vowel == null)
			return false;
		
		boolean result = false;
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		String query = "SELECT id FROM vowels WHERE root = ?";
		if (!checkCandidates)
			query += " AND valid = 1";
		String[] queryArgs = new String[1];
		queryArgs[0] = vowel.toLowerCase();
		
		Cursor cursor = db.rawQuery(query, queryArgs);
		result = (cursor != null) && (cursor.getCount() > 0);
		if (cursor != null)
			cursor.close();
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
	
	private class DbOpenHelper extends SQLiteOpenHelper {
		
		private static final String dbName = "vnime.sqlite";
		
		public DbOpenHelper(Context ctx, int dbVer) {
			super(ctx, dbName, null, dbVer);
			
			if (ctx == null)
				throw new InvalidParameterException("The context must not be null");
			context = ctx;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Resources res = context.getResources();
			String[] statements;
			
			statements = res.getString(R.string.sql_vowels).split(";");
			for (int i = 0; i < statements.length; i++) {
				db.execSQL(statements[i]);
			}
			
			statements = res.getString(R.string.sql_consonants).split(";");
			for (int i = 0; i < statements.length; i++) {
				db.execSQL(statements[i]);
			}
			
			statements = res.getString(R.string.sql_crossmorph).split(";");
			for (int i = 0; i < statements.length; i++) {
				db.execSQL(statements[i]);
			}
			
			statements = res.getString(R.string.sql_vcpairs).split(";");
			for (int i = 0; i < statements.length; i++) {
				db.execSQL(statements[i]);
			}
			
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			/* Do nothing */
		}

		private Context context;
	}
}
