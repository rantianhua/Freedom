package work.jean.com.freedom_runtime.util;

import static work.jean.com.freedom_runtime.util.HackPlus.*;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by rantianhua on 17/5/1.
 */

public class AndroidHack {

    private static final String TAG = "AndroidHack";

    //exception
    public static AssertionArrayException exceptionArray;

    //resources
    public static HackedClass<android.content.res.AssetManager> AssetManager;
    public static HackedMethod0<android.content.res.AssetManager, Void, HackPlus.Unchecked, HackPlus.Unchecked, HackPlus.Unchecked> AssetManager_construct;
    public static HackedMethod1<Integer, android.content.res.AssetManager, HackPlus.Unchecked, HackPlus.Unchecked, HackPlus.Unchecked, String> AssetManager_addAssetPath;
    public static HackedMethod0<Void, android.content.res.AssetManager, HackPlus.Unchecked, HackPlus.Unchecked, HackPlus.Unchecked>
            AssetManager_ensureStringBlocks;


    //>=19
    public static HackedClass<Object> ResourcesManager;
    public static HackedMethod0<Object, Void, HackPlus.Unchecked, HackPlus.Unchecked, HackPlus.Unchecked> ResourcesManager_getInstance;
    public static HackedField<Object, ArrayMap> ResourcesManager_mActiveResources;
    //>=24
    public static HackedField<Object, ArrayList> ResourcesManager_mResourceReferences;

    //<19
    public static HackedClass<Object> ActivityThread;
    public static HackedMethod0<Void, Void, HackPlus.Unchecked, HackPlus.Unchecked, HackPlus.Unchecked> ActivityThread_currentActivityThread;
    public static HackedField<Object, HashMap> ActivityThread_mActiveResources;


    //>=24
    public static HackedField<Resources, Object> Resources_ResourcesImpl;
    public static HackedField<Object, Object> ResourcesImpl_mAssets;
    //<24
    public static HackedField<Resources, Object> Resources_mAssets;


    public static boolean sIsIgnoreFailure;
    public static boolean sIsReflectAvailable;
    public static boolean sIsReflectChecked;


    public static boolean defineAndVerify() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return false;
        }
        if (sIsReflectChecked) {
            return sIsReflectAvailable;
        }
        long startHack = System.currentTimeMillis();
        try {
            initAssertion();
            hackResources();
            if (exceptionArray != null) {
                Log.e(TAG, "HackPlus error:" + AndroidHack.exceptionArray);
                sIsReflectAvailable = false;
                return sIsReflectAvailable;
            }
            sIsReflectAvailable = true;
            return sIsReflectAvailable;
        } catch (Throwable e) {
            sIsReflectAvailable = false;
            Log.d(TAG, e.getMessage());
        } finally {
            sIsReflectChecked = true;
            long stopHack = System.currentTimeMillis();
            Log.e(TAG, "HackPlus spend time: " + (stopHack - startHack) + " ms");
        }
        return sIsReflectAvailable;
    }


    private static void initAssertion() {
        HackPlus.setAssertionFailureHandler(new AssertionFailureHandler() {
            @Override
            public void onAssertionFailure(final AssertionException failure) {
                if (!sIsIgnoreFailure) {
                    if (exceptionArray == null) {
                        exceptionArray = new AssertionArrayException("HackPlus assert failed");
                    }
                    exceptionArray.addException(failure);
                }
            }
        });
    }


    private static void hackResources() {
        //HackPlus AssetManager
        AssetManager = HackPlus.into(AssetManager.class);
        AssetManager_construct = AssetManager.constructor().withoutParams();
        AssetManager_addAssetPath = AssetManager.method("addAssetPath").returning(int.class).withParam(String.class);
        AssetManager_ensureStringBlocks = AssetManager.method("ensureStringBlocks").withoutParams();

        //大于19时，开始有ResourcesManager这个类，通过这个类去替换内存中的AssetManager对象
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ResourcesManager = HackPlus.into("android.app.ResourcesManager");
            ResourcesManager_getInstance = ResourcesManager.staticMethod("getInstance").returning(ResourcesManager.getClazz()).withoutParams();
            //Android N的时候，将Resources对象移到了mResourceReferences中
            if (Build.VERSION.SDK_INT >= 24) {
                // N moved the resources to mResourceReferences
                ResourcesManager_mResourceReferences = ResourcesManager.field("mResourceReferences").ofType(ArrayList.class);
            } else {
                //Android N之前的版本，Resources随心则在mActiveResources对象中
                // Pre-N
                ResourcesManager_mActiveResources = ResourcesManager.field("mActiveResources").ofType(ArrayMap.class);
            }
        } else {
            //在Andorid 19之前，没有ResourcesManager对象，通过ActivityThread去操作，但是通过ActivityThread操作有个坑，在早期的版本中，ActivityThread是保存在ThreadLocal对象中的，如果你要在子线程中去拿，就会出问题，所以这里也需要Hack一下。
            ActivityThread = HackPlus.into("android.app.ActivityThread");
            ActivityThread_currentActivityThread = ActivityThread.staticMethod("currentActivityThread").withoutParams();
            ActivityThread_mActiveResources = ActivityThread.field("mActiveResources").ofType(HashMap.class);
        }
        //在Android N中，AssetManager对象从Resources对象中的mAssets成员变量转移到了mResourcesImpl成员变量中mAssets成员 变量
        if (Build.VERSION.SDK_INT >= 24) {
            // N moved the mAssets inside an mResourcesImpl field
            Resources_ResourcesImpl = HackPlus.into(Resources.class).field("mResourcesImpl");
            ResourcesImpl_mAssets = HackPlus.into(Resources_ResourcesImpl.getType()).field("mAssets");
        } else {
            // Pre-N
            Resources_mAssets = HackPlus.into(Resources.class).field("mAssets");
        }
    }


    private static Object _sActivityThread;

    static class ActivityThreadGetter implements Runnable {
        ActivityThreadGetter() {
        }

        public void run() {
            try {
                _sActivityThread = AndroidHack.ActivityThread_currentActivityThread.invoke().statically();
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (AndroidHack.ActivityThread_currentActivityThread) {
                AndroidHack.ActivityThread_currentActivityThread.notify();
            }
        }
    }
    //获取ActivityThread的Hack方式，通过判断是否是主线程，如果不是主线程，在阻塞当前线程，切换到主线程去拿
    public static Object getActivityThread() throws Exception {
        if (_sActivityThread == null) {
            if (Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId()) {
                _sActivityThread = AndroidHack.ActivityThread_currentActivityThread.invoke().statically();
            } else {
                // In older versions of Android (prior to frameworks/base 66a017b63461a22842)
                // the currentActivityThread was built on thread locals, so we'll need to try
                // even harder
                Handler handler = new Handler(Looper.getMainLooper());
                synchronized (AndroidHack.ActivityThread_currentActivityThread) {
                    handler.post(new ActivityThreadGetter());
                    AndroidHack.ActivityThread_currentActivityThread.wait();
                }
            }
        }
        return _sActivityThread;
    }
}
