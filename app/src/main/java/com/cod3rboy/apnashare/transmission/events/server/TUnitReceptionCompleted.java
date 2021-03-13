package com.cod3rboy.apnashare.transmission.events.server;

import com.cod3rboy.apnashare.models.TransmissionFile;

public class TUnitReceptionCompleted {
    private TransmissionFile file;

    public TUnitReceptionCompleted(TransmissionFile file) {
        this.file = file;
    }

    public TransmissionFile getFile() {
        return file;
    }
}
