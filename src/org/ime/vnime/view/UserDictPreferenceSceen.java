package org.ime.vnime.view;

import org.ime.vnime.R;
import org.ime.vnime.txtproc.DictionaryManager;
import org.ime.vnime.txtproc.TxtProcFactory;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class UserDictPreferenceSceen extends Preference {

	@Override
	protected void onClick() {
		super.onClick();
		
		UserDictManagerDialog dlg = new UserDictManagerDialog(getContext());
		dlg.setOnDismissListener(new UserDictManagerDialog.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				
			}
			
		});
		dlg.show();
	}

	public UserDictPreferenceSceen(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public UserDictPreferenceSceen(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}
	
	private class UserDictManagerDialog extends Dialog {

		private DictionaryManager dictManager;
		private SimpleCursorAdapter adapter;
		
		private boolean isButtonAdd = true;
		
		private Button btnAddDelete;
		private EditText edtWord;
		ListView lstWords;
		
		public UserDictManagerDialog(Context context) {
			super(context, android.R.style.Theme);
			init();
		}
		
		private void init() {
			setContentView(R.layout.app_dictmanager);
			setTitle(R.string.vnime_settings_dictionary_manager);

			btnAddDelete = (Button)findViewById(R.id.dictmanager_btnAddDelete);
			edtWord = (EditText)findViewById(R.id.dictmanager_edtWord);
			lstWords = (ListView)findViewById(R.id.dictmanager_lstWords);

			Context ctx = getContext();
			dictManager = TxtProcFactory.getInstance(getContext()).createDictionaryManager(TxtProcFactory.ID_DICT_MANAGER_MEMORY);
			String[] from = new String[1];
			from[0] = "word";
			int[] to = new int[1];
			to[0] = R.id.dictmanager_listitem_txtWord;
			adapter = new SimpleCursorAdapter(ctx, R.layout.app_dictmanager_listitem, dictManager.getAllUserWord(), from, to);
			
			lstWords.setAdapter(adapter);
			lstWords.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					String word = (String) ((TextView)((ViewGroup)view).findViewById(R.id.dictmanager_listitem_txtWord)).getText();
					edtWord.setText(word);
				}
			});
			
			lstWords.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					String word = (String) ((TextView)((ViewGroup)view).findViewById(R.id.dictmanager_listitem_txtWord)).getText();
					edtWord.setText(word);
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					/* Do nothing */
				}
			});

			btnAddDelete.setEnabled(false);
			btnAddDelete.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					String word = edtWord.getText().toString().trim();
					if (word == null || word.trim().length() == 0)
						return;
					
					boolean changed = true;
					if (isButtonAdd) {
						changed = dictManager.addUserWord(word);
					} else {
						dictManager.removeUserWord(word);
					}
					if (changed) {
						isButtonAdd = !isButtonAdd;
						adapter.changeCursor(dictManager.getAllUserWord());
						if (isButtonAdd) {
							btnAddDelete.setText(R.string.dictmanager_label_add);
						} else {
							btnAddDelete.setText(R.string.dictmanager_label_delete);
						}
					}
				}
			});

			edtWord.addTextChangedListener(new TextWatcher() {
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					
				}
				
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {
					
				}
				
				@Override
				public void afterTextChanged(Editable s) {
					String word = edtWord.getText().toString().trim();

					if (word == null || word.trim().length() == 0) {
						btnAddDelete.setEnabled(false);
						lstWords.setSelection(0);
						return;
					}
					
					btnAddDelete.setEnabled(true);

					isButtonAdd = true;
					int count = adapter.getCount();
					int i = 0;
					for (i = 0; i < count; i++) {
						ViewGroup vg = (ViewGroup) adapter.getView(i, null, null);
						TextView txtWord = (TextView) vg.findViewById(R.id.dictmanager_listitem_txtWord);
						String item = txtWord.getText().toString();
						if (item.equals(word)) {
							isButtonAdd = false;
							break;
						} else if (item.startsWith(word)) {
							break;
						}
					}
					
					if (isButtonAdd) {
						btnAddDelete.setText(R.string.dictmanager_label_add);
					} else {
						btnAddDelete.setText(R.string.dictmanager_label_delete);
					}
					if (i <= count) {
						lstWords.setSelection(i);
					} else {
						lstWords.setSelection(0);
					}
				}
			});
		}
		
	}

}
