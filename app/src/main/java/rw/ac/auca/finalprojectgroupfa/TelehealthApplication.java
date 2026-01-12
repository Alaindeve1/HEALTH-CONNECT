package rw.ac.auca.finalprojectgroupfa;

import android.app.Application;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import rw.ac.auca.finalprojectgroupfa.workers.SyncWorker;

public class TelehealthApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Force light mode regardless of device theme settings
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setupSyncWorker();
    }

    private void setupSyncWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWorkRequest =
                new PeriodicWorkRequest.Builder(SyncWorker.class, 30, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueue(syncWorkRequest);
    }
}