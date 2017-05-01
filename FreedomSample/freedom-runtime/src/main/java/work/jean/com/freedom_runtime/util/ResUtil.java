package work.jean.com.freedom_runtime.util;


import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by rantianhua on 17/5/1.
 */

public class ResUtil {
    private static final String TAG = "FreedomResUtil";

    public static void loadResPatch(Context context) {
        if (context == null) {
            return;
        }

        boolean needLoad = SPUtil.getBoolean(context, Constant.FREEDOM_SP_KEY_NEED_LOAD_RES_PATCH);
        if (!needLoad) {
            Log.d(TAG, "no need to load res patch");
            return;
        }

        File file = new File(context.getCacheDir(), Constant.FREEDOM_RES_PATCH_DIR);
        File[] patches = file.listFiles();;
        if (patches == null || patches.length == 0) {
            Log.d(TAG, "want to load res patch, but file not found");
            return;
        }

        File patchFile = patches[0];
        try {
            injectResources(context, patchFile);
            SPUtil.save(context, Constant.FREEDOM_SP_KEY_NEED_LOAD_RES_PATCH, false);
            Log.d(TAG, "inject resource success");
        } catch (Exception e) {
            Log.e(TAG, "error in inject resource", e);
        }
    }

    private static void injectResources(Context context, File patchFile) throws Exception {
        if (!AndroidHack.defineAndVerify()) {
            Log.d(TAG, "un support load resource patch in this device");
            return;
        }

        //通过构造函数new一个AssetManager对象
        AssetManager newAssetManager = AndroidHack.AssetManager_construct.invoke().statically();
        //调用AssetManager对象的addAssetPath方法添加patch资源
        int cookie = AndroidHack.AssetManager_addAssetPath.invokeWithParam(patchFile.getAbsolutePath()).on(newAssetManager);
        //添加成功时cookie必然大于0
        if (cookie == 0) {
            Log.e(TAG, "Could not create new AssetManager");
            return;
        }
        // 在Android 19以前需要调用这个方法，但是Android L后不需要，实际情况Andorid L上调用也不会有问题，因此这里不区分版本
        // Kitkat needs this method call, Lollipop doesn't. However, it doesn't seem to cause any harm
        // in L, so we do it unconditionally.
        AndroidHack.AssetManager_ensureStringBlocks.invoke().on(newAssetManager);

        //获取内存中的Resource对象的弱引用
        Collection<WeakReference<Resources>> references;

        if (Build.VERSION.SDK_INT >= 24) {
            // Android N，获取的是一个ArrayList，直接赋值给references对象
            // Find the singleton instance of ResourcesManager
            Object resourcesManager = AndroidHack.ResourcesManager_getInstance.invoke().statically();
            //noinspection unchecked
            references = (Collection<WeakReference<Resources>>) AndroidHack.ResourcesManager_mResourceReferences.on(resourcesManager).get();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //Android 19以上 获得的是一个ArrayMap，调用其values方法后赋值给references
            // Find the singleton instance of ResourcesManager
            Object resourcesManager = AndroidHack.ResourcesManager_getInstance.invoke().statically();
            @SuppressWarnings("unchecked")
            ArrayMap<?, WeakReference<Resources>> arrayMap = AndroidHack.ResourcesManager_mActiveResources.on(resourcesManager).get();
            references = arrayMap.values();
        } else {
            //Android 19以下，通过ActivityThread获取得到的是一个HashMap对象，通过其values方法获得对象赋值给references
            Object activityThread = AndroidHack.getActivityThread();
            @SuppressWarnings("unchecked")
            HashMap<?, WeakReference<Resources>> map = (HashMap<?, WeakReference<Resources>>) AndroidHack.ActivityThread_mActiveResources.on(activityThread).get();
            references = map.values();
        }

        //遍历获取到的Ressources对象的弱引用，将其AssetManager对象替换为我们的patch的AssetManager
        for (WeakReference<Resources> wr : references) {
            Resources resources = wr.get();
            // Set the AssetManager of the Resources instance to our brand new one
            if (resources != null) {
                if (Build.VERSION.SDK_INT >= 24) {
                    Object resourceImpl = AndroidHack.Resources_ResourcesImpl.get(resources);
                    AndroidHack.ResourcesImpl_mAssets.set(resourceImpl, newAssetManager);
                } else {
                    AndroidHack.Resources_mAssets.set(resources, newAssetManager);
                }
                resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
            }
        }

    }
}
