package work.jean.com.freedom_runtime.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import work.jean.com.freedom_runtime.service.FreedomService;

/**
 * Created by rantianhua on 17/4/16.
 */

public class FreedomReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        final String action = intent.getAction();
        if (action.equals(FreedomService.ACTION_WAKE_UP)) {
            Intent serviceIntent = new Intent(context, FreedomService.class);
            serviceIntent.putExtra(FreedomService.EXTRA_WAKE_UP, true);
            context.startService(serviceIntent);
        }
    }
}
