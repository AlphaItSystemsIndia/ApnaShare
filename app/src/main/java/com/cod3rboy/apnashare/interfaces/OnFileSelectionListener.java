package com.cod3rboy.apnashare.interfaces;

import com.cod3rboy.apnashare.models.BasicFile;

public interface OnFileSelectionListener {
    void onFileSelected(BasicFile selectedFile);

    void onFileDeselected(BasicFile selectedFile);
}
