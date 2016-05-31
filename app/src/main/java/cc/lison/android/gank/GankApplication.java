package cc.lison.android.gank;

import android.app.Application;
import android.content.Context;

import com.orhanobut.logger.Logger;
import com.umeng.update.UmengUpdateAgent;

public class GankApplication extends Application {

    private static Context context;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        initLogger();

        UmengUpdateAgent.update(this);
    }

    private void initLogger() {
        Logger.init("lison").hideThreadInfo().setLogValve(BuildConfig.LOG_DEBUG).methodCount(1);
    }
}
