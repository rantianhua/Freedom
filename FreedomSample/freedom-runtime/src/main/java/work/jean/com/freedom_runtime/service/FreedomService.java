package work.jean.com.freedom_runtime.service;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import work.jean.com.freedom_runtime.receiver.FreedomReceiver;
import work.jean.com.freedom_runtime.server.FreedomServer;
import work.jean.com.freedom_runtime.util.Constant;

/**
 * Created by rantianhua on 17/4/16.
 */

public class FreedomService extends Service {

    private static final int SERVICE_NOTIFICATION_ID = 1001;
    private static final String LOG_TAG = "FreedomService";
    public static final String ACTION_WAKE_UP = "WAKE_FEEDOM_SERVICE_UP";
    public static final String EXTRA_WAKE_UP = "alarmWakeup";

    private AlarmManager mAlarmManager;

    public static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Constant.INTERNAL_CACHE_DIR = getExternalCacheDir().getAbsolutePath();
        sContext = this;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isWakeUpByAlarm = intent != null && intent.getBooleanExtra(EXTRA_WAKE_UP, false);
        if (!isWakeUpByAlarm) {
            //set service running in foreground
            try {
                setServiceForeground();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            Log.d(LOG_TAG, "start service from AlarmManager");
        }
        FreedomServer.startServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            startAlarmWakeup(TimeUnit.SECONDS.toMillis(10));
        }catch (Exception e) {
            e.printStackTrace();
        }
        sContext = null;
        FreedomServer.stopServer();
    }

    private void startAlarmWakeup(long mills) {
        Intent intent = new Intent(this, FreedomReceiver.class);
        intent.setAction(ACTION_WAKE_UP);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1001, intent, 0);

        if (mAlarmManager != null
                && pendingIntent != null) {
            Log.d(LOG_TAG, "have set alarm");
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + mills, pendingIntent);
        }
    }

    /**
     * let this service be a foreground service
     * so that it has a longer life time
     */
    private void setServiceForeground() {
        if (Build.VERSION.SDK_INT < 18) {
            startForeground(SERVICE_NOTIFICATION_ID, new Notification());
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, new Notification());
            Intent innerIntent = new Intent(this, InnerService.class);
            startService(innerIntent);
        }
    }

    public static class InnerService extends Service {
        private static final String LOG_TAG_INNER = LOG_TAG + "$Inner";

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.i(LOG_TAG_INNER, "onStartCommand in InnerService ");
            try{
                startForeground(SERVICE_NOTIFICATION_ID, new Notification());
                stopForeground(true);
                stopSelf();
            } catch (Exception e) {
                Log.e(LOG_TAG_INNER, "InnerService work failed", e);
            }
            return super.onStartCommand(intent, flags, startId);
        }
    }
}
