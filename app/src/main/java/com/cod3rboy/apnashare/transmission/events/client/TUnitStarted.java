package com.cod3rboy.apnashare.transmission.events.client;

import com.cod3rboy.apnashare.models.TransmissionFile;

public class TUnitStarted {
    private TransmissionFile file;

    public TUnitStarted(TransmissionFile file) {
        this.file = file;
    }

    public TransmissionFile getFile() {
        return file;
    }
}
