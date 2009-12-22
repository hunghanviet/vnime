package org.ime.vnime.txtproc;

import android.database.Cursor;

public interface MacroManager {
	
	public String expandMacro(String macro, boolean changeCase);
	
	public boolean registerMacro(String key, String value);
	
	public boolean updateMacro(String key, String value);
	
	public boolean checkMacroExist(String key);
	
	public void removeMacro(String key);
	
	public Cursor getAllMacros();
}
