package org.ime.vnime;

import org.ime.vnime.txtproc.MacroManager;
import org.ime.vnime.txtproc.TextChangedListener;

import android.view.inputmethod.InputConnection;

public interface ConnectionManager extends SoftKeyboardListener {
	
	/**
	 * Set {@link android.view.inputmethod.InputConnection InputConnection}
	 * for managing
	 * @param conn The {@link android.view.inputmethod.InputConnection InputConnection}
	 * object
	 */
	public void setConnection(InputConnection conn);
	
	/**
	 * Get currently managed {@link android.view.inputmethod.InputConnection InputConnection}
	 * object
	 * @return The currently managed {@link android.view.inputmethod.InputConnection InputConnection}
	 * object
	 */
	public InputConnection getConnection();
	
	public void setMacroManager(MacroManager manager);
	
	public void setMacroEnabled(boolean enabled);
	
	public void setRevertEnabled(boolean enabled);
	
	public void setTextChangedListener(TextChangedListener listener);
	
	/**
	 * Chỉ định các ký tự chuyển đổi, dùng để đặt dấu hoặc chuyển đổi ký tự.<br>
	 * Thí dụ, đối với kiểu gõ Telex: s -> dấu huyền, e -> chuyển e thành ê...<br><br>
	 * 
	 * Thứ tự các ký tự như sau:<br>
	 * <b>bằng-huyền-sắc-hỏi-ngã-nặng-ă-â-đ-ê-ô-ơ-ư</b><br>
	 * Thí dụ, đối với kiểu gõ Telex: <i>zfsrxjwadeoww</i>
	 * 
	 * @param modifiers Chuỗi ký tự chuyển đổi
	 */
	public void setModifiers(String modifiers);
	
	/**
	 * @return The current modifiers sequence. See {@link #setModifiers}
	 * for more details.
	 */
	public String getModifiers();
}
