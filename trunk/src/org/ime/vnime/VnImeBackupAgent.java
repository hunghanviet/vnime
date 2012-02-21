package org.ime.vnime;

import java.io.IOException;

import org.ime.vnime.txtproc.UserDbOpenHelper;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

public class VnImeBackupAgent extends BackupAgentHelper {

	public VnImeBackupAgent() {

	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {
		String defSharedPrefsName = getPackageName() + DEF_SHARED_PREFS_SUFFIX;
		addHelper(defSharedPrefsName, new SharedPreferencesBackupHelper(this, defSharedPrefsName));
		
		addHelper(USER_DB_FILE, new FileBackupHelper(this, USER_DB_FILE));
		
		super.onBackup(oldState, data, newState);
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {
		super.onRestore(data, appVersionCode, newState);
	}
	
	private final String DEF_SHARED_PREFS_SUFFIX = "org.ime.vnime_preferences";
	private final String USER_DB_FILE = "../databases/" + UserDbOpenHelper.dbName;

}
