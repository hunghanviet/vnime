package org.ime.vnime.view;

import org.ime.vnime.R;
import org.ime.vnime.txtproc.MacroManager;
import org.ime.vnime.txtproc.TxtProcFactory;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;

public class MacroPreferenceScreen extends Preference {

	@Override
	protected void onClick() {
		super.onClick();
		
		MacroManagerDialog dlg = new MacroManagerDialog(getContext());
		dlg.setOnDismissListener(new MacroManagerDialog.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				
			}
			
		});
		dlg.show();
	}

	public MacroPreferenceScreen(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MacroPreferenceScreen(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}
	
	private class MacroManagerDialog extends Dialog {

		private MacroManager macroManager;
		private SimpleCursorAdapter adapter;
		
		private static final int MENU_ITEMID_DELETE = 10;
		
		public MacroManagerDialog(Context context) {
			super(context, android.R.style.Theme);
			init();
		}
		
		private void init() {
			setContentView(R.layout.app_macromanager);
			setTitle(R.string.vnime_settings_macro_manager);

			Context ctx = getContext();
			macroManager = TxtProcFactory.getInstance(getContext()).createMacroManager(TxtProcFactory.ID_MACRO_MANAGER_SQLITE);
			String[] from = new String[2];
			from[0] = "key";
			from[1] = "value";
			int[] to = new int[2];
			to[0] = R.id.macromanager_listitem_lbKey;
			to[1] = R.id.macromanager_listitem_lbValue;
			adapter = new SimpleCursorAdapter(ctx, R.layout.app_macromanager_listitem, macroManager.getAllMacros(), from, to);
			
			ListView lstMacros = (ListView)findViewById(R.id.macromanager_lstMacros);
			lstMacros.setAdapter(adapter);
			
			lstMacros.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
				@Override
				public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {
					menu.add(ContextMenu.NONE, MENU_ITEMID_DELETE, ContextMenu.NONE, R.string.macromanager_label_menuitem_delete);
				}
			});
			
			lstMacros.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					String key = (String) ((TextView)((ViewGroup)view).findViewById(R.id.macromanager_listitem_lbKey)).getText();
					String value = (String) ((TextView)((ViewGroup)view).findViewById(R.id.macromanager_listitem_lbValue)).getText();
					((EditText)findViewById(R.id.macromanager_txtKey)).setText(key);
					((EditText)findViewById(R.id.macromanager_txtValue)).setText(value);
				}
			});
			
			lstMacros.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					String key = (String) ((TextView)((ViewGroup)view).findViewById(R.id.macromanager_listitem_lbKey)).getText();
					String value = (String) ((TextView)((ViewGroup)view).findViewById(R.id.macromanager_listitem_lbValue)).getText();
					((EditText)findViewById(R.id.macromanager_txtKey)).setText(key);
					((EditText)findViewById(R.id.macromanager_txtValue)).setText(value);
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					/* Do nothing */
				}
			});

			Button btnUpdate = (Button)findViewById(R.id.macromanager_btnUpdate);
			btnUpdate.setEnabled(false);
			btnUpdate.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					String key = ((EditText)findViewById(R.id.macromanager_txtKey)).getText().toString();
					String value = ((EditText)findViewById(R.id.macromanager_txtValue)).getText().toString();
					if (key == null || key.trim().length() == 0 || value == null || value.trim().length() == 0)
						return;
					
					boolean changed = false;
					if (macroManager.checkMacroExist(key)) {
						changed = macroManager.updateMacro(key, value);
					} else {
						changed = macroManager.registerMacro(key, value);
					}
					if (changed)
						adapter.changeCursor(macroManager.getAllMacros());
				}
			});

			TextView txtKey = ((TextView)findViewById(R.id.macromanager_txtKey));
			TextView txtValue = ((TextView)findViewById(R.id.macromanager_txtValue));
			TextWatcher textWatcher = new TextWatcher() {
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					
				}
				
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {
					
				}
				
				@Override
				public void afterTextChanged(Editable s) {
					updateButtonState();

					updateListSelection();
				}
			};
			txtKey.addTextChangedListener(textWatcher);
			txtValue.addTextChangedListener(textWatcher);
		}
		
		private void updateButtonState() {
			Button btnUpdate = (Button)findViewById(R.id.macromanager_btnUpdate);
			EditText txtKey = ((EditText)findViewById(R.id.macromanager_txtKey));
			EditText txtValue = ((EditText)findViewById(R.id.macromanager_txtValue));
			String key = txtKey.getText().toString();
			String value = txtValue.getText().toString();

			if (key == null || key.trim().length() == 0 || value == null || value.trim().length() == 0) {
				btnUpdate.setEnabled(false);
				return;
			}
			
			btnUpdate.setEnabled(true);
			if (macroManager.checkMacroExist(key)) {
				btnUpdate.setText(R.string.macromanager_label_update);
			} else {
				btnUpdate.setText(R.string.macromanager_label_add);
			}
		}
		
		private void updateListSelection() {
			EditText txtKey = ((EditText)findViewById(R.id.macromanager_txtKey));
			String key = txtKey.getText().toString();
			
			int count = adapter.getCount();
			int i = 0;
			for (i = 0; i < count; i++) {
				ViewGroup vg = (ViewGroup) adapter.getView(i, null, null);
				TextView lbKey = (TextView) vg.findViewById(R.id.macromanager_listitem_lbKey);
				if (lbKey.getText().toString().startsWith(key)) {
					break;
				}
			}
			ListView lstMacros = (ListView)findViewById(R.id.macromanager_lstMacros);
			if (i <= count) {
				lstMacros.setSelection(i);
			} else {
				lstMacros.setSelection(0);
			}
		}

		@Override
		public boolean onMenuItemSelected(int featureId, MenuItem item) {
			switch (item.getItemId()) {
			case MENU_ITEMID_DELETE:
				ViewGroup viewGroup = (ViewGroup) ((AdapterContextMenuInfo)item.getMenuInfo()).targetView;
				String key = (String) ((TextView)viewGroup.findViewById(R.id.macromanager_listitem_lbKey)).getText();
				macroManager.removeMacro(key);
				adapter.changeCursor(macroManager.getAllMacros());
				return true;
			}
			return super.onMenuItemSelected(featureId, item);
		}
		
	}

}
