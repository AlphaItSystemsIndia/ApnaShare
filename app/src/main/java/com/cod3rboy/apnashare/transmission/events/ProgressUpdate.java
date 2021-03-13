package com.cod3rboy.apnashare.transmission.events;

import com.cod3rboy.apnashare.models.TransmissionFile;

public class ProgressUpdate {
    private TransmissionFile file;
    private long totalBytesOrFiles;
    private long bytesOrFilesProcessed;
    private int progress;
    private long bytesProcessed;

    public ProgressUpdate(TransmissionFile file, long totalBytesOrFiles, long bytesOrFilesProcessed, long bytesProcessed) {
        this.file = file;
        this.totalBytesOrFiles = totalBytesOrFiles;
        this.bytesOrFilesProcessed = bytesOrFilesProcessed;
        this.bytesProcessed = bytesProcessed;
        this.progress = (int) (bytesOrFilesProcessed * 100f / totalBytesOrFiles);
    }

    public TransmissionFile getFile() {
        return file;
    }

    public long getTotalBytesOrFiles() {
        return totalBytesOrFiles;
    }

    public long getBytesOrFilesProcessed() {
        return bytesOrFilesProcessed;
    }

    public int getProgress() {
        return progress;
    }

    public long getTotalBytesProcessed() {
        return bytesProcessed;
    }
}
