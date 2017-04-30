package work.jean.com.freedomsample;

import android.app.Application;

import work.jean.com.freedom_runtime.Freedom;

/**
 * Created by rantianhua on 17/4/30.
 */

public class FreedomApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Freedom.init(this);
    }
}
