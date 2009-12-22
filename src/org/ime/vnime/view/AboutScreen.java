package org.ime.vnime.view;

import org.ime.vnime.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.util.AttributeSet;

public class AboutScreen extends Preference {

	@Override
	protected void onClick() {
		super.onClick();
		
		AboutDialog dlg = new AboutDialog(getContext());
		dlg.setOnDismissListener(new AboutDialog.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				
			}
			
		});
		dlg.show();
	}

	public AboutScreen(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AboutScreen(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}
	
	private class AboutDialog extends Dialog {
		
		public AboutDialog(Context context) {
			super(context, android.R.style.Theme);
			init();
		}
		
		private void init() {
			setContentView(R.layout.app_about);
			setTitle(R.string.svc_name);
		}
		
	}
	
}
