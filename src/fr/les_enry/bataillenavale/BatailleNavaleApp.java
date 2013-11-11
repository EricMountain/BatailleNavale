package fr.les_enry.bataillenavale;

import android.app.Application;
import android.util.Log;

import org.acra.*;
import org.acra.annotation.*;

//@ReportsCrashes(formKey = "", mailTo = "eric.stephen.mountain@gmail.com", customReportContent = {
//		ReportField.USER_COMMENT, ReportField.APP_VERSION_CODE,
//		ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION,
//		ReportField.BRAND, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA,
//		ReportField.STACK_TRACE, ReportField.LOGCAT }, mode = ReportingInteractionMode.DIALOG, resDialogText = R.string.crash_toast_text)
@ReportsCrashes(formKey = "", formUri = "http://les-enry.fr/E/acra.php")
public class BatailleNavaleApp extends Application {
	private static final String TAG = "BatailleNavaleApp";
	
	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(TAG, "BatailleNavaleApp onCreate()");
		
		// The following line triggers the initialisation of ACRA
		ACRA.init(this);
	}
}
