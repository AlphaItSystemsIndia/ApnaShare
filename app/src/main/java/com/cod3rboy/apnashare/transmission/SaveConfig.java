package com.cod3rboy.apnashare.transmission;

import com.cod3rboy.apnashare.App;

import java.io.File;

public class SaveConfig {
    private static final String BASE_DIR_NAME = "ApnaShare";
    private static final String DIR_IMAGES = "images";
    private static final String DIR_APPS = "apps";
    private static final String DIR_VIDEOS = "videos";
    private static final String DIR_AUDIO = "audio";
    private static final String DIR_FILES = "files";
    private static final String BASE_DIR_PATH;

    static {
        String extPath = App.getInstance().getExternalFilesDirs(null)[0].getAbsolutePath().toLowerCase();
        BASE_DIR_PATH = extPath.substring(0, extPath.indexOf("android")) + BASE_DIR_NAME;
    }

    public static String getBaseDirPath() {
        return BASE_DIR_PATH;
    }

    public static String getImagesPath() {
        return BASE_DIR_PATH + File.separator + DIR_IMAGES;
    }

    public static String getVideosPath() {
        return BASE_DIR_PATH + File.separator + DIR_VIDEOS;
    }

    public static String getAudioPath() {
        return BASE_DIR_PATH + File.separator + DIR_AUDIO;
    }

    public static String getAppsPath() {
        return BASE_DIR_PATH + File.separator + DIR_APPS;
    }

    public static String getFilesPath() {
        return BASE_DIR_PATH + File.separator + DIR_FILES;
    }

    public static String getRootSavePath(String fileName) {
        return getBaseDirPath() + File.separator + fileName;
    }

    public static String getImageSavePath(String imgNameWithExt) {
        return getImagesPath() + File.separator + imgNameWithExt;
    }

    public static String getAudioSavePath(String audioNameWithExt) {
        return getAudioPath() + File.separator + audioNameWithExt;
    }

    public static String getVideoSavePath(String videoNameWithExt) {
        return getVideosPath() + File.separator + videoNameWithExt;
    }

    public static String getAppSavePath(String appNameWithExt) {
        return getAppsPath() + File.separator + appNameWithExt;
    }

    public static String getFileOrDirSavePath(String fileWithExtOrDirName) {
        return getFilesPath() + File.separator + fileWithExtOrDirName;
    }
}