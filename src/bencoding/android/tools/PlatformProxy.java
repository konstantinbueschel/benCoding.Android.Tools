package bencoding.android.tools;

import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.proxy.IntentProxy;

import java.util.List;
import java.util.concurrent.ExecutionException;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.media.AudioManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.Settings;

import bencoding.android.Common;

@Kroll.proxy(creatableInModule=AndroidtoolsModule.class)
public class PlatformProxy  extends KrollProxy {
	
	public PlatformProxy() {
		
		super();
	}


	@SuppressWarnings("deprecation")
	@Kroll.method
	public boolean isAirplaneModeOn() {
		   
		   return Settings.System.getInt(TiApplication.getInstance().getApplicationContext().getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
	}

	@Kroll.method
	public boolean isInForeground() {
		
		try {
	        
	        boolean foreground = new ForegroundCheckTask().execute(TiApplication.getInstance().getApplicationContext()).get();

	        return foreground;

	    } 
	    catch (InterruptedException e) {
	        
	        e.printStackTrace();
	        return false;

	    } 
	    catch (ExecutionException e) {
	        
	        e.printStackTrace();
	        return false;
	    }
	}

	@Kroll.method
	public boolean intentAvailable(IntentProxy intent) {
		
		if (intent == null) {
			return false;
		}

		PackageManager packageManager = TiApplication.getInstance().getPackageManager();

		return (packageManager.queryIntentActivities(intent.getIntent(), PackageManager.MATCH_DEFAULT_ONLY).size() > 0) ;
	}

	private void performExit() {

		android.os.Process.killProcess(android.os.Process.myPid());
	}

	@Kroll.method
	public void startApp() {

        Common.msgLogger("Creating launch intent");

        // get the launch intent for your Ti App
        Intent launchIntent = TiApplication.getInstance().getApplicationContext().getPackageManager()
                    .getLaunchIntentForPackage(TiApplication.getInstance().getApplicationContext().getPackageName());

        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setAction(Intent.ACTION_MAIN);

        Common.msgLogger("Starting launch intent");

		TiApplication.getInstance().getApplicationContext().startActivity(launchIntent);
	}

	@Kroll.method
	public boolean isRingerModeSilent() {

		AudioManager audioManager = (AudioManager)TiApplication.getInstance().getApplicationContext().getSystemService(TiApplication.AUDIO_SERVICE);

		return (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT);
	}

	@Kroll.method
    public boolean isRingerModeVibrate() {

        AudioManager audioManager = (AudioManager)TiApplication.getInstance().getApplicationContext().getSystemService(TiApplication.AUDIO_SERVICE);

        return (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE);
    }

    @Kroll.method
    public boolean isRingerModeNormal() {

        AudioManager audioManager = (AudioManager)TiApplication.getInstance().getApplicationContext().getSystemService(TiApplication.AUDIO_SERVICE);

        return (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL);
    }

	@Kroll.method
	public void restartApp(@Kroll.argument(optional=true) String delay)
	{
		int pendingIntentID = 999123;
		long DELAY_OFFSET = 15000;

		if (delay != null) {
			DELAY_OFFSET = Long.valueOf(delay);
	    }

		if (TiApplication.getInstance().isDebuggerEnabled()) {
			
			Common.msgLogger("App cannot be restarted with debugger enabled");

			throw new IllegalStateException("App cannot be restarted with debugger enabled");
		}

		Common.msgLogger("Creating Start Activity");

		//Get the Start Activity for your Ti App
		Intent iStartActivity = TiApplication.getInstance().getApplicationContext().getPackageManager().getLaunchIntentForPackage( TiApplication.getInstance().getApplicationContext().getPackageName() );

		//Add the flags needed to restart
		iStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		iStartActivity.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		iStartActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		iStartActivity.addCategory(Intent.CATEGORY_LAUNCHER);
		iStartActivity.setAction(Intent.ACTION_MAIN);

		Common.msgLogger("Creating Pending Intent");

		//Create a pending intent for the Start Activity
		PendingIntent pendingIntent = PendingIntent.getActivity(TiApplication.getInstance().getApplicationContext(), pendingIntentID, iStartActivity, PendingIntent.FLAG_UPDATE_CURRENT);

		Common.msgLogger("Scheduling Restart");

		//Schedule an Alarm to restart after a delay
		AlarmManager alarmManager = (AlarmManager)TiApplication.getInstance().getApplicationContext().getSystemService(TiApplication.ALARM_SERVICE);

		if (android.os.Build.VERSION.SDK_INT >= 19) {

			alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + DELAY_OFFSET, pendingIntent);
		}
		else {

			alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + DELAY_OFFSET, pendingIntent);
		}
		
		Common.msgLogger("Clean-up and Exit");

		//Tell Ti to do some clean-up
		TiApplication.getInstance().beforeForcedRestart();

		//Do a force quite
		performExit();
	}

	@Kroll.method
	public void exitApp() {
		
		performExit();
	}

	@Kroll.method
	public void killPackage(String packageName) {
		
		ActivityManager activityManager = (ActivityManager)TiApplication.getInstance().getApplicationContext().getSystemService(TiApplication.ACTIVITY_SERVICE);

	    activityManager.killBackgroundProcesses(packageName);
	}

	@Kroll.method
	public void killProcess(int pid) {

		android.os.Process.killProcess(pid);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Kroll.method
	public Object[] getRunningAppProcesses() {

	    ArrayList appList = new ArrayList();
		
		PackageManager packageManager = TiApplication.getInstance().getApplicationContext().getPackageManager();

		ActivityManager activityManager = (ActivityManager) TiApplication.getInstance().getApplicationContext().getSystemService(TiApplication.ACTIVITY_SERVICE);

	    List<RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();

	    for (int i = 0; i < procInfos.size(); i++) {

	    	HashMap<String, Object> record = new HashMap<String, Object>(6);

	    	record.put("processName",procInfos.get(i).processName);
	    	record.put("pid", procInfos.get(i).pid);
	    	record.put("uid", procInfos.get(i).uid);

			try {

				PackageInfo packageInfo = packageManager.getPackageInfo(procInfos.get(i).processName, PackageManager.GET_META_DATA);

				record.put("packageName", packageInfo.packageName);
		    	record.put("versionCode", packageInfo.versionCode);
		    	record.put("versionName", packageInfo.versionName);
		    	record.put("name", packageInfo.applicationInfo.loadLabel(packageManager).toString());
		    	record.put("isSystemApp",(!isUserApp(packageInfo.applicationInfo)));
			} 
			catch (NameNotFoundException e) {

				Common.msgLogger("Process " + procInfos.get(i).processName + " has not package information available");
			}

			appList.add(record);
	    }

	    Object[] returnObject = appList.toArray(new Object[appList.size()]);

	    return returnObject;
	}

	boolean isUserApp(ApplicationInfo ai) {

	    int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

	    return (ai.flags & mask) == 0;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Kroll.method
	public Object[] getInstalledApps() {

		ArrayList appList = new ArrayList();

		final PackageManager packageManager = TiApplication.getInstance().getApplicationContext().getPackageManager();

		List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);

		for (PackageInfo packageInfo : packages) {
			
			HashMap<String, Object> record = new HashMap<String, Object>(5);
	    	
	    	record.put("packageName",packageInfo.packageName);
	    	record.put("versionCode", packageInfo.versionCode);
	    	record.put("versionName", packageInfo.versionName);
	    	record.put("name", packageInfo.applicationInfo.loadLabel(packageManager).toString());
	    	record.put("isSystemApp",(!isUserApp(packageInfo.applicationInfo)));
			
			appList.add(record);
		}

	    Object[] returnObject = appList.toArray(new Object[appList.size()]);

	    return returnObject;
	}

	@Kroll.method
	public void launchIntentForPackage(String packageName) {

		Intent launchIntent = TiApplication.getInstance().getApplicationContext().getPackageManager().getLaunchIntentForPackage(packageName);

		TiApplication.getInstance().startActivity(launchIntent);
	}
}
