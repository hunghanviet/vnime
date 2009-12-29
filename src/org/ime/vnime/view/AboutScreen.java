package org.ime.vnime.view;

import org.ime.vnime.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.TextView;

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
			String verName;
			try {
				verName = getContext().getPackageManager().getPackageInfo("org.ime.vnime", 0).versionName;
			} catch (NameNotFoundException e) {
				verName = "1.0";    /* Default is version 1.0 */
			}
			TextView txtVersion = (TextView) findViewById(R.id.app_about_txtVersion);
			if (txtVersion != null) {
				txtVersion.setText(txtVersion.getText() + verName);
			}
		}
		
	}
	
}
