package org.ime.vnime.txtproc;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class SqliteBasedMacroManager implements MacroManager {
	
	private UserDbOpenHelper dbOpenHelper;

	public SqliteBasedMacroManager(Context ctx) {
		dbOpenHelper = UserDbOpenHelper.getInstance(ctx);
	}

	@Override
	public String expandMacro(String key, boolean changeCase) {
		String result = null;
		if (key == null || key.trim().length() == 0)
			return result;
		
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		String query = "SELECT value FROM macros WHERE key = ?";
		String[] queryArgs = new String[1];
		queryArgs[0] = key.toLowerCase();
		
		Cursor cursor = db.rawQuery(query, queryArgs);
		if (cursor != null && cursor.moveToFirst()) {
			result = cursor.getString(0);
			cursor.close();
			if (changeCase) {
				int firstCapitalizedChar = -1;
				int lastCapitalizedChar = -1;
				int len = key.length();
				for (int i = 0; i < len; i++) {
					if (Character.isUpperCase(key.charAt(i))) {
						if (firstCapitalizedChar < 0) {
							firstCapitalizedChar = i;
						}
						lastCapitalizedChar = i;
					}
				}
				if (firstCapitalizedChar == 0 && lastCapitalizedChar == len - 1) {
					/* Convert all sequence to uppercase */
					result = result.toUpperCase();
				} else if (firstCapitalizedChar < 0 && lastCapitalizedChar < 0) {
					/* Convert all sequence to lowercase */
					result = result.toLowerCase();
				}
			}
		} else if (cursor != null) {
			cursor.close();
		}
		
		return result;
	}

	@Override
	public Cursor getAllMacros() {
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		String query = "SELECT id AS _id, key, value FROM macros ORDER BY key";
		
		return db.rawQuery(query, null);
	}

	@Override
	public boolean registerMacro(String key, String value) {
		if (key == null || key.trim().length() == 0 || value == null || value.trim().length() == 0)
			return false;
		if (!checkMacroExist(key)) {
			SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
			String query = "INSERT INTO macros(key, value) VALUES (?, ?)";
			String[] queryArgs = new String[2];
			queryArgs[0] = key.toLowerCase();
			queryArgs[1] = value;

			db.execSQL(query, queryArgs);
			
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void removeMacro(String key) {
		if (key == null || key.trim().length() == 0)
			return;

		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		String query = "DELETE FROM macros WHERE key = ?";
		String[] queryArgs = new String[1];
		queryArgs[0] = key.toLowerCase();

		db.execSQL(query, queryArgs);
	}

	@Override
	public boolean updateMacro(String key, String value) {
		if (key == null || key.trim().length() == 0 || value == null || value.trim().length() == 0)
			return false;
		if (checkMacroExist(key)) {
			SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
			String query = "UPDATE macros SET value = ? WHERE key = ?";
			String[] queryArgs = new String[2];
			queryArgs[0] = value;
			queryArgs[1] = key.toLowerCase();

			db.execSQL(query, queryArgs);
			
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean checkMacroExist(String key) {
		boolean result = false;
		if (key == null || key.trim().length() == 0)
			return result;
		
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		String query = "SELECT value FROM macros WHERE key = ?";
		String[] queryArgs = new String[1];
		queryArgs[0] = key.toLowerCase();
		
		Cursor cursor = db.rawQuery(query, queryArgs);
		if (cursor != null) {
			if (cursor.getCount() > 0)
				result = true;
			cursor.close();
		}
		
		return result;
	}

}
