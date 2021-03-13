package com.cod3rboy.apnashare.transmission.events.client;

public class TransmissionEnded {
    private boolean successful;

    public TransmissionEnded(boolean successful) {
        this.successful = successful;
    }

    public boolean wasSuccessful() {
        return successful;
    }
}
