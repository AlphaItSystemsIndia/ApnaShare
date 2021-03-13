package com.cod3rboy.apnashare.models;

import java.io.File;

public class AppFile extends BasicFile {
    private String mAppName;
    private String mPackageName;

    public AppFile(String appName, String packageName, File apkFile) {
        super(appName + ".apk", apkFile.getAbsolutePath(), apkFile.length());
        this.mAppName = appName;
        this.mPackageName = packageName;
    }

    public String getAppName() {
        return mAppName;
    }

    public String getPackageName() {
        return mPackageName;
    }
}
