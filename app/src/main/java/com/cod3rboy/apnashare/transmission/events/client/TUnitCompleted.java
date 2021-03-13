package com.cod3rboy.apnashare.transmission.events.client;

import com.cod3rboy.apnashare.models.TransmissionFile;

public class TUnitCompleted {
    private TransmissionFile file;

    public TUnitCompleted(TransmissionFile file) {
        this.file = file;
    }

    public TransmissionFile getFile() {
        return file;
    }
}
