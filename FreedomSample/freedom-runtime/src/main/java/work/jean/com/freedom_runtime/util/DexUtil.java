package work.jean.com.freedom_runtime.util;

import android.content.Context;
import android.util.Log;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * Created by rantianhua on 17/4/30.
 */

public class DexUtil {

    private static final String TAG = "FreedomDexUtil";

    private static final String DEX_ELEMENTS_FILED = "dexElements";

    public static void loadDex(Context context) {
        if (context == null) {
            Log.d(TAG, "load dex skip because context is null");
            return;
        }

        File patchDir = new File(context.getCacheDir(), Constant.FREEDOM_DEX_PATCH_DIR);
        File[] dexs = patchDir.listFiles();
        if (dexs == null || dexs.length == 0) {
            Log.d(TAG, "no dex to load in " + patchDir.getPath());
            return;
        }

        File optDir = null;
        try {
            optDir = context.getDir(Constant.FREEDOM_OPT_DEX_DIR, Context.MODE_PRIVATE);
            if (!optDir.exists()) {
                optDir.mkdirs();
            }
        }catch (Exception e) {
            Log.d(TAG, "error in create opt dir", e);
        }


        for (File dex : dexs) {
            try {

                mergeDex(context, dex, optDir);
                Log.d(TAG, "inject " + dex.getName() + " success");
            } catch (Exception e) {
                Log.e(TAG, "error in load dex", e);
                break;
            }
        }

        for (File dex : dexs) {
            Log.d(TAG, "delete dex file " + dex.getName());
            dex.delete();
        }
    }

    private static void mergeDex(Context context, File dex, File opt) throws Exception {

        //加载app原有dex的classloader
        PathClassLoader pathClassLoader = (PathClassLoader) context.getClassLoader();
        //创建加载补丁dex的classloader
        DexClassLoader dexClassLoader = new DexClassLoader(dex.getAbsolutePath(), opt.getAbsolutePath(), null, pathClassLoader);

        //获取自身app的BaseDexClassLoader的pathList
        Object appPathList = reflectDexPathList(pathClassLoader);
        //补丁BaseDexClassLoader的pathList
        Object patchPathList = reflectDexPathList(dexClassLoader);

        //获取自身app的dexElements
        Object appDexElements = reflectDexElements(appPathList);
        //获取自身补丁的dexElements
        Object patchDexElements = reflectDexElements(patchPathList);

        //合并两个dexElements
        Object dexElements = combineElements(appDexElements, patchDexElements);

        injectFieldValue(appPathList, appPathList.getClass(), DEX_ELEMENTS_FILED, dexElements);
    }

    private static void injectFieldValue(Object o, Class<?> aClass,
            String fieldName, Object value) throws Exception {
        Field field = aClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(o, value);
    }

    private static Object combineElements(Object appDexElements, Object patchDexElements) throws Exception {
        Class<?> clz = appDexElements.getClass().getComponentType();
        int appLen = Array.getLength(appDexElements);
        int patchLen = Array.getLength(patchDexElements);

        Object result = Array.newInstance(clz, appLen + patchLen);
        for (int i = 0; i < appLen + patchLen; i++) {
            if (i < patchLen) {
                Array.set(result, i, Array.get(patchDexElements, i));
            }else {
                Array.set(result, i, Array.get(appDexElements, i - patchLen));
            }
        }

        return result;
    }

    private static Object reflectDexElements(Object patchPathList) throws Exception {
        return reflectField(patchPathList, patchPathList.getClass(), DEX_ELEMENTS_FILED);
    }

    private static Object reflectDexPathList(BaseDexClassLoader classLoader) throws Exception {
        return reflectField(classLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    private static Object reflectField(Object o, Class<?> clz, String fieldName) throws Exception {
        Field field = clz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(o);
    }
}
