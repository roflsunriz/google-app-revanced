package app.revanced.extension.googleapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class RuntimeHooks implements Application.ActivityLifecycleCallbacks {
    private static final Map<Application, RuntimeHooks> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private RuntimeHooks() {
    }

    public static void install(Application application) {
        synchronized (INSTANCES) {
            if (INSTANCES.containsKey(application)) {
                return;
            }
            RuntimeHooks hooks = new RuntimeHooks();
            INSTANCES.put(application, hooks);
            application.registerActivityLifecycleCallbacks(hooks);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        SettingsInjector.onActivityResumed(activity);
        ViewCollapser.onActivityResumed(activity);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        SettingsInjector.onActivityDestroyed(activity);
        ViewCollapser.onActivityDestroyed(activity);
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) { }
    @Override public void onActivityStarted(Activity activity) { }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivityStopped(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
}
