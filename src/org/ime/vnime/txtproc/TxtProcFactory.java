package org.ime.vnime.txtproc;

import java.security.InvalidParameterException;

import android.content.Context;

public class TxtProcFactory {
	
	public static final int ID_DICT_MANAGER_MEMORY = 11;
	public static final int ID_DICT_MANAGER_SQLITE = 12;
	public static final int ID_MACRO_MANAGER_SQLITE = 21;
	public static final int ID_TXTPROCESSOR_MEMORY = 41;
	public static final int ID_TXTPROCESSOR_SQLITE = 42;
	
	public static TxtProcFactory getInstance(Context ctx) {
		if (theInstance == null) {
			theInstance = new TxtProcFactory(ctx);
		}
		return theInstance;
	}
	
	public DictionaryManager createDictionaryManager(int id) {
		switch (id) {
		case ID_DICT_MANAGER_MEMORY:
			return (dictManagerMemory != null) ?
					dictManagerMemory :
					(dictManagerMemory = new MemoryBasedDictionaryManager(context));
		case ID_DICT_MANAGER_SQLITE:
			return (dictManagerSqlite != null) ?
					dictManagerSqlite :
					(dictManagerSqlite = new SqliteBasedDictionaryManager(context));
		}
		return null;
	}
	
	public MacroManager createMacroManager(int id) {
		switch (id) {
		case ID_MACRO_MANAGER_SQLITE:
			return (macroManagerSqlite != null) ?
					macroManagerSqlite :
					(macroManagerSqlite = new SqliteBasedMacroManager(context));
		}
		return null;
	}
	
	public TxtProcessor createTextProcessor(int id) {
		switch (id) {
		case ID_TXTPROCESSOR_MEMORY:
			return (txtProcessorMemory != null) ?
					txtProcessorMemory :
					(txtProcessorMemory = new MemoryBasedProcessor());
		case ID_TXTPROCESSOR_SQLITE:
			return (txtProcessorSqlite != null) ?
					txtProcessorSqlite :
					(txtProcessorSqlite = new SqliteBasedProcessor(context));
		}
		return null;
	}
	
	private TxtProcFactory(Context ctx) {
		if (ctx == null)
			throw new InvalidParameterException("The context must not be null!");
		context = ctx;
	}
	
	private static TxtProcFactory theInstance;
	private Context context;
	
	private MemoryBasedDictionaryManager dictManagerMemory;
	private SqliteBasedDictionaryManager dictManagerSqlite;
	
	private SqliteBasedMacroManager macroManagerSqlite;
	
	private SqliteBasedProcessor txtProcessorSqlite;
	private MemoryBasedProcessor txtProcessorMemory;
}
