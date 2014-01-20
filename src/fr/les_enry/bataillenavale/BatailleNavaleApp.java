package fr.les_enry.bataillenavale;

import android.app.Application;
import android.util.Log;

import org.acra.*;
import org.acra.annotation.*;

/**
 * Sets up crash reporting to work even before the activity starts up.
 */
@ReportsCrashes(formKey = "", formUri = "http://les-enry.fr/E/acra.php")
public class BatailleNavaleApp extends Application {
	private static final String TAG = "BatailleNavaleApp";
	
	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(TAG, "BatailleNavaleApp onCreate()");

		ACRA.init(this);
	}
}
