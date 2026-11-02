package com.privatesrcfiles;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.pm.PackageManager;
import com.privatesrcfiles.utils.AppManager;


import org.lsposed.lsparanoid.Obfuscate;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.CRC32;
import com.sbox.SboxCore;
import com.sbox.core.env.BEnvironment;
import com.sbox.entity.pm.InstallResult;



@Obfuscate
public class MAct extends AppCompatActivity {

    static {
        try {
            System.loadLibrary("PRIVATE_SRC_FILES");
        } catch (UnsatisfiedLinkError ignored) {}
    }
    public static native String apkcrc(); // returns the expected CRC32 as string
    private static final String PKG_BGMI = "com.pubg.imobile";
    private static final int USER_ID = 0;

    private ProgressBar progressBar;
    private TextView progressText;
    private boolean isObbCopied = false;
    private AppManager appManager;
    public static native String exdate();
    // Declare the native method in Java
    


    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       

        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(getColor(R.color.background));
        appManager = new AppManager(this);
        doCountTimerAccout();

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);

        // BGMI START
        Button startBgmi = findViewById(R.id.startBgmi);
        startBgmi.setOnClickListener(v -> {

            showStartOptions();
        });        
    }

    private void showStartOptions() {
        new AlertDialog.Builder(this)
                .setTitle("Start Options")
                .setItems(new CharSequence[]{"Launch", "Clear Login"}, (dialog, which) -> {
                    if (which == 0) {
                        handleLaunchFlow();
                    } else if (which == 1) {
                        handleClearLogin();
                    }
                })
                .show();
    }

    private void handleLaunchFlow() {
        // APK is verified, proceed with BGMI installation or launch
        if (!SboxCore.get().isInstalled(PKG_BGMI, USER_ID)) {
            InstallResult result = SboxCore.get().installPackageAsUser(PKG_BGMI, USER_ID);
            if (result.success) {
                Toast.makeText(this, "BGMI installed. Please wait...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Install failed: " + result.msg, Toast.LENGTH_SHORT).show();
            }
            return; // Return after attempting installation
        }

        // Check if OBB file already exists in the target location
        try {
            PackageInfo info = getPackageManager().getPackageInfo(PKG_BGMI, 0);
            String gameObb = "main." + info.versionCode + "." + info.packageName + ".obb";
            File obbDestDir = BEnvironment.getExternalObbDir(PKG_BGMI);
            File obbDestFile = new File(obbDestDir, gameObb);

            if (obbDestFile.exists()) {
                launchGame(); // OBB exists, launch the game directly
                return;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "Could not get package info.", Toast.LENGTH_SHORT).show();
        }

        // If OBB doesn't exist, start the copy process
        if (!isObbCopied) {
            copyObbFiles();
        } else {
            launchGame();
        }
    }

    private void handleClearLogin() {
        try {
            // Stop and clear package data for BGMI user 0
            SboxCore.get().stopPackage(PKG_BGMI, USER_ID);
            SboxCore.get().clearPackage(PKG_BGMI, USER_ID);
            Toast.makeText(this, "Login data cleared", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to clear login: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    // This method periodically checks the APK's CRC32 to detect any tampering
   
    private void doCountTimerAccout() {
        android.os.Handler handler = new android.os.Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    handler.postDelayed(this, 1000);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date expiryDate = dateFormat.parse(exdate());
                    long now = System.currentTimeMillis();
                    long distance = expiryDate.getTime() - now;

                    long d = distance / (24 * 60 * 60 * 1000);
                    long h = (distance / (60 * 60 * 1000)) % 24;
                    long m = (distance / (60 * 1000)) % 60;
                    long s = (distance / 1000) % 60;

                    if (distance < 0) {
                        Toast.makeText(MAct.this, "License expired!", Toast.LENGTH_SHORT).show();
                    } else {
                        ((TextView) findViewById(R.id.tvD)).setText(String.format("%02d", d));
                        ((TextView) findViewById(R.id.tvH)).setText(String.format("%02d", h));
                        ((TextView) findViewById(R.id.tvM)).setText(String.format("%02d", m));
                        ((TextView) findViewById(R.id.tvS)).setText(String.format("%02d", s));
                    }
                } catch (Exception ignored) {}
            }
        };
        handler.postDelayed(runnable, 0);
    }

    private void copyObbFiles() {
        findViewById(R.id.progressCard).setVisibility(android.view.View.VISIBLE);
        progressText.setText("Copying OBB...");
        progressBar.setProgress(0);

        new Thread(() -> {
            try {
                PackageInfo info = getPackageManager().getPackageInfo(PKG_BGMI, 0);
                String gameObb = "main." + info.versionCode + "." + info.packageName + ".obb";

                File obbDestDir = BEnvironment.getExternalObbDir(PKG_BGMI);
                if (!obbDestDir.exists()) obbDestDir.mkdirs();

                File obbSource = null;
                StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);
                for (StorageVolume storageVolume : sm.getStorageVolumes()) {
                    File obbDir;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        obbDir = storageVolume.getDirectory();
                    } else {
                        // Use reflection to call the hidden getPathFile() method for older Android versions
                        try {
                            Method getPathFile = StorageVolume.class.getMethod("getPathFile");
                            obbDir = (File) getPathFile.invoke(storageVolume);
                        } catch (Exception e) {
                            Log.e("MAct", "Failed to get path from StorageVolume via reflection", e);
                            obbDir = null;
                        }
                    }

                    if (obbDir != null) {
                        File obbSubDir = new File(obbDir, "Android/obb/" + PKG_BGMI);
                        File obbFile = new File(obbSubDir, gameObb);
                        if (obbFile.exists() && obbFile.canRead()) {
                            obbSource = obbFile;
                            break;
                        }
                    }
                }

                File obbDestFile = new File(obbDestDir, gameObb);
                if (obbDestFile.exists()) {
                    runOnUiThread(() -> {
                        isObbCopied = true;

                        launchGame();
                    });
                    return;
                }

                final File finalObbSource = obbSource;
                if (obbSource == null || !obbSource.exists() || !obbSource.canRead()) {
                    runOnUiThread(() -> Toast.makeText(this, "OBB source file not found or is not readable: " + (finalObbSource != null ? finalObbSource.getAbsolutePath() : "null"), Toast.LENGTH_LONG).show());
                    return;
                }


                java.io.FileInputStream fis = new java.io.FileInputStream(obbSource);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(obbDestFile);

                long total = obbSource.length();
                long copied = 0;
                byte[] buf = new byte[8192];
                int len;
                while ((len = fis.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    copied += len;
                    int prog = (int) ((copied * 100) / total);
                    runOnUiThread(() -> {
                        progressBar.setProgress(prog);
                        progressText.setText("Copying OBB: " + prog + "%");
                    });
                }
                fis.close();
                fos.close();

                runOnUiThread(() -> {
                    isObbCopied = true;
                    progressText.setText("Ready to launch");
                    launchGame();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "OBB copy failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /** Launch BGMI */
    private void launchGame() {
        // Perform CRC check before launching the game

        try {
            SboxCore.get().launchApk(PKG_BGMI, USER_ID);
            Toast.makeText(this, "Launching BGMI...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Launch failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
