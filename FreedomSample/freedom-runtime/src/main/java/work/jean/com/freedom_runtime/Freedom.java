package work.jean.com.freedom_runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.WeakHashMap;

import work.jean.com.freedom_runtime.service.FreedomService;
import work.jean.com.freedom_runtime.util.Constant;
import work.jean.com.freedom_runtime.util.DexUtil;
import work.jean.com.freedom_runtime.util.ResUtil;


/**
 * Created by rantianhua on 17/4/16.
 */

public class Freedom {

    public static final int ACTIVITY_CREATED = 1;
    public static final int ACTIVITY_STARTED = 2;
    public static final int ACTIVITY_RESUMED = 3;
    public static final int ACTIVITY_PAUSED = 4;

    public static void init(Application application) {

        addHackDexIfNeeded(application);
        DexUtil.loadDex(application);
        ResUtil.loadResPatch(application);

        Intent intent = new Intent(application, FreedomService.class);
        application.startService(intent);

        application.registerActivityLifecycleCallbacks(lifeCycleCallback);
    }

    private static void addHackDexIfNeeded(Application application) {
        //将freedomhack.dex放到dex的patch目录里
        try {
            File dir = new File(application.getCacheDir(), Constant.FREEDOM_DEX_PATCH_DIR);
            File hackDex = new File(dir, "freedomhack.dex");
            if (hackDex.exists()) return;
            if (!dir.exists() || dir.listFiles().length <= 0) return;
            InputStream inputStream = application.getAssets().open("freedomhack.dex");
            FileOutputStream fos = new FileOutputStream(hackDex);
            byte[] buff = new  byte[1024];
            int len = 0;
            while ((len = inputStream.read(buff)) != -1) {
                fos.write(buff, 0, len);
            }
            inputStream.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final Application.ActivityLifecycleCallbacks lifeCycleCallback = new Application.ActivityLifecycleCallbacks() {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            sActivitys.put(activity, ACTIVITY_CREATED);
        }

        @Override
        public void onActivityStarted(Activity activity) {
            sActivitys.put(activity, ACTIVITY_STARTED);
        }

        @Override
        public void onActivityResumed(Activity activity) {
            sActivitys.put(activity, ACTIVITY_RESUMED);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            sActivitys.put(activity, ACTIVITY_PAUSED);
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            sActivitys.remove(activity);
        }
    };

    public static WeakHashMap<Activity, Integer> sActivitys = new WeakHashMap<>();
}
