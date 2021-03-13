package com.cod3rboy.apnashare.background;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cod3rboy.apnashare.models.AppFile;

public class AppsAdapterDataLoader extends AsyncTaskLoader<ArrayList<AppFile>> {

    public AppsAdapterDataLoader(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Nullable
    @Override
    public ArrayList<AppFile> loadInBackground() {
        ArrayList<AppFile> installedAppsData = new ArrayList<>();
        // Get all Installed Apps on the device
        List<PackageInfo> allInstalledPackages = getContext().getPackageManager().getInstalledPackages(0);
        // Filter user installed apps
        ArrayList<PackageInfo> userApps = new ArrayList<>();
        for (PackageInfo pkgInfo : allInstalledPackages)
            if (!isSystemPackage(pkgInfo)) userApps.add(pkgInfo);
        for (int i = 0; i < userApps.size(); i++) {
            PackageInfo p = userApps.get(i);
            String appName = p.applicationInfo.loadLabel(getContext().getPackageManager()).toString();
            String packageName = p.applicationInfo.packageName;
            File apkFile = new File(p.applicationInfo.publicSourceDir);
            installedAppsData.add(new AppFile(appName, packageName, apkFile));
        }
        Collections.sort(installedAppsData, (a, b) -> a.getAppName().compareTo(b.getAppName()));
        return installedAppsData;
    }

    private static boolean isSystemPackage(PackageInfo pkgInfo) {
        return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

}
