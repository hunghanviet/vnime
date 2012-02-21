package org.ime.vnime.txtproc;

import java.security.InvalidParameterException;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserDbOpenHelper extends SQLiteOpenHelper {
	
	public static final String dbName = "userdb.sqlite";
	
	private static UserDbOpenHelper theInstance;
	
	public static UserDbOpenHelper getInstance(Context ctx) {
		if (theInstance == null) {
			if (ctx == null)
				throw new InvalidParameterException("The context must not be null!");

			int dbVer;
			try {
				dbVer = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
			} catch (NameNotFoundException e) {
				dbVer = 1000000; /* Supposedly version 1.0-0000 */
			}
			theInstance = new UserDbOpenHelper(ctx, dbVer);
		}
		return theInstance;
	}
	
	private UserDbOpenHelper(Context ctx, int dbVer) {
		super(ctx, dbName, null, dbVer);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		db.execSQL("CREATE TABLE macros (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL , key TEXT NOT NULL, value TEXT NOT NULL)");
		
		db.execSQL("CREATE TABLE userdict (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL , word TEXT NOT NULL)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		/* Do nothing */
	}

}
