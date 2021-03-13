package com.cod3rboy.apnashare.misc;

import com.cod3rboy.apnashare.models.BasicFile;

import java.util.ArrayList;

public final class FileSelectionQueue {
    private static FileSelectionQueue mSingleton = null;

    public static FileSelectionQueue getInstance() {
        if (mSingleton == null) mSingleton = new FileSelectionQueue();
        return mSingleton;
    }

    private ArrayList<BasicFile> mSelectedFiles;

    private FileSelectionQueue() {
        mSelectedFiles = new ArrayList<>();
    }

    public void initialize() {
        mSelectedFiles.clear();
    }

    public int add(BasicFile selectedFile) {
        mSelectedFiles.add(0, selectedFile);
        return 0;
    }

    public int remove(BasicFile deselectedFile) {
        String fileUid = deselectedFile.getUid();
        for (int i = 0; i < mSelectedFiles.size(); i++) {
            BasicFile file = mSelectedFiles.get(i);
            if (file.getUid().equals(fileUid)) {
                mSelectedFiles.remove(i);
                return i;
            }
        }
        return -1;
    }

    public void removeAtPosition(int pos) {
        mSelectedFiles.remove(pos);
    }

    public ArrayList<BasicFile> getAll() {
        return new ArrayList<>(mSelectedFiles);
    }

    public BasicFile getAtPosition(int pos) {
        return mSelectedFiles.get(pos);
    }

    public void clearAll() {
        mSelectedFiles.clear();
    }

    public int count() {
        return mSelectedFiles.size();
    }

    public boolean isEmpty() {
        return mSelectedFiles.isEmpty();
    }
}
