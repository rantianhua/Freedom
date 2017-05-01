package work.jean.com.freedom_runtime.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by rantianhua on 17/5/1.
 */

public class SPUtil {

    public static void save(Context context, String key, boolean value) {
        SharedPreferences sp = context.getSharedPreferences(Constant.FREEDOM_SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static boolean getBoolean(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(Constant.FREEDOM_SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(key, false);
    }
}
