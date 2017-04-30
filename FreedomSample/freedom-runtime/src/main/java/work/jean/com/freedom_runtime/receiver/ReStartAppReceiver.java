package work.jean.com.freedom_runtime.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;

import java.util.Map;

import work.jean.com.freedom_runtime.Freedom;
import work.jean.com.freedom_runtime.service.ReStartAppService;
import work.jean.com.freedom_runtime.util.DexUtil;

/**
 * Created by rantianhua on 17/4/30.
 */

public class ReStartAppReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        restartTopActivity(context);
    }

    private void restartTopActivity(Context context) {
        Activity top = null;
        for (Map.Entry<Activity, Integer> activityIntegerEntry : Freedom.sActivitys.entrySet()) {
            Activity activity = activityIntegerEntry.getKey();
            if (activity != null && activityIntegerEntry.getValue() == Freedom.ACTIVITY_RESUMED) {
                top = activity;
                break;
            }
        }

        if (top != null) {
            DexUtil.loadDex(context);

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
