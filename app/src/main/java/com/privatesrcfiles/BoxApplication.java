package com.privatesrcfiles;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.util.Log;
import com.sbox.SboxCore;
import com.sbox.app.configuration.AppLifecycleCallback;
import com.sbox.app.configuration.ClientConfiguration;
import com.sbox.core.system.api.BoxActivate;
import java.io.File;
import com.privatesrcfiles.utils.Prefs;  // Changed import


public class BoxApplication extends Application {

static {
        System.loadLibrary("PRIVATE_SRC_FILES"); 
    }
    public static native String getSdkKey();
    private static final String TAG = "BoxApplication";
    private final String[] process_names = {
            "com.pubg.krmobile",   // KOREA - 1
            "com.tencent.ig",      // GLOBAL - 2
            "com.rekoo.pubgm",     // TAIWAN - 3
            "com.vng.pubgmobile",  // VIETNAM - 4
            "com.pubg.imobile"     // BGMI - 5
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "BoxApplication attachBaseContext started");
        Prefs prefs = new Prefs(base);  // Changed from FPrefs.with(base) to new Prefs(base)
        try {
            Log.d(TAG, "Initializing ZCoreCore...");
            SboxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

               @Override
                public boolean isHideRoot() {
                    return true;
                }

               @Override
                public boolean isHideXposed() {
                    return true;
                }

                @Override
                public boolean isEnableDaemonService() {
                    return false;
                }

               @Override
                public boolean requestInstallPackage(File file){
                    PackageInfo packageInfo = base.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),0);
                    return false;
                }
            });
            Log.d(TAG, "ZCoreCore initialization completed");
            Log.d(TAG, "App name check completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in attachBaseContext", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SboxCore.get().doCreate();
        BoxActivate.activateBox(getSdkKey());
        SboxCore.get().addAppLifecycleCallback(new AppLifecycleCallback() {
            @Override
            public void beforeCreateApplication(String packageName, String processName, Context context, int userId) {
                // Handle before application creation
            }

            @Override
                public void beforeApplicationOnCreate(String packageName, String processName, Application application, int userId) {
                    try {
                        for (String pkg : process_names) {
                            if (pkg.equals(packageName) && pkg.equals(processName)) {

                                // BGMI loader
                                if (pkg.equals("com.pubg.imobile")) {
                                    File p1 = new File(getFilesDir(), "loader/libbgmi.so");
                                    if (p1.exists()) {
                                        System.load(p1.getAbsolutePath());
                                        Log.d("App", "Loaded libbgmi.so for BGMI");
                                    } else {
                                        Log.e("App", "libbgmi.so not found!");
                                    }
                                }

                                // PUBG Global loader
                                if (pkg.equals("com.tencent.ig")) {
                                    File p2 = new File(getFilesDir(), "loader/libpubgm.so");
                                    if (p2.exists()) {
                                        System.load(p2.getAbsolutePath());
                                        Log.d("App", "Loaded libpubgm.so for PUBG Global");
                                    } else {
                                        Log.e("App", "libpubgm.so not found!");
                                    }
                                }

                                break;
                            }
                        }
                    } catch (UnsatisfiedLinkError e) {
                        Log.e("App", "Native lib load failed: " + e.getMessage());
                        e.printStackTrace();
                        System.exit(0);
                    } catch (Exception e) {
                        Log.e("App", "Error loading game libs: " + e.getMessage());
                        e.printStackTrace();
                        System.exit(0);
                    }
                }

            @Override
            public void afterApplicationOnCreate(String packageName, String processName, Application application, int userId) {
                // Handle after application onCreate
            }
        });
    }
}