package work.jean.com.freedom_runtime.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import java.util.Map;

import work.jean.com.freedom_runtime.Freedom;
import work.jean.com.freedom_runtime.service.ReStartAppService;
import work.jean.com.freedom_runtime.util.Constant;
import work.jean.com.freedom_runtime.util.DexUtil;
import work.jean.com.freedom_runtime.util.ResUtil;
import work.jean.com.freedom_runtime.util.SPUtil;

/**
 * Created by rantianhua on 17/4/30.
 */

public class ReStartAppReceiver extends BroadcastReceiver {

    private static final String TAG = "FreedomRestartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean haveRes = intent.getBooleanExtra(Constant.FREEDOM_EXTRA_HAVE_RES, false);
        boolean haveDex = intent.getBooleanExtra(Constant.FREEDOM_EXTRA_HAVE_DEX, false);
        restartTopActivity(context, haveDex, haveRes);
    }

    private void restartTopActivity(Context context, boolean haveDex, boolean haveRes) {

        if (haveRes) {
            Log.d(TAG, "set need reload res patch");
            SPUtil.save(context, Constant.FREEDOM_SP_KEY_NEED_LOAD_RES_PATCH, true);
        }

        Activity top = null;
//        for (Map.Entry<Activity, Integer> activityIntegerEntry : Freedom.sActivitys.entrySet()) {
//            Activity activity = activityIntegerEntry.getKey();
//            if (activity != null && activityIntegerEntry.getValue() == Freedom.ACTIVITY_RESUMED) {
//                top = activity;
//                break;
//            }
//        }

        if (top != null) {
            if (haveDex) {
                DexUtil.loadDex(context);
            }
            if (haveRes) {
                ResUtil.loadResPatch(context);
            }

            top.finish();
            Intent intent = new Intent(context, top.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            Intent intent = new Intent(context, ReStartAppService.class);
            intent.putExtra("package", context.getPackageName());
            context.startService(intent);

            Process.killProcess(Process.myPid());
        }
    }
}
