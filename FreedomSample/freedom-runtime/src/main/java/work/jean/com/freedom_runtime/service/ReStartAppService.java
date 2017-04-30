package work.jean.com.freedom_runtime.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

/**
 * Created by rantianhua on 17/4/30.
 */

public class ReStartAppService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        if (intent != null) {
            long delay = intent.getLongExtra("delay", 2000);
            final String packageName = intent.getStringExtra("package");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
                    startActivity(launch);
                    ReStartAppService.this.stopSelf();
                }
            }, delay);
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
