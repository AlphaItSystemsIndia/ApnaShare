package com.cod3rboy.apnashare.transmission.events.server;

import com.cod3rboy.apnashare.models.TransmissionFile;

public class TUnitReceptionStarted {
    private TransmissionFile file;
    private long filesOrSizeInBytes;

    public TUnitReceptionStarted(TransmissionFile file, long filesOrSizeInBytes) {
        this.file = file;
        this.filesOrSizeInBytes = filesOrSizeInBytes;
    }

    public TransmissionFile getFile() {
        return file;
    }

    public long getFilesCountOrSizeInBytes() {
        return filesOrSizeInBytes;
    }
}
