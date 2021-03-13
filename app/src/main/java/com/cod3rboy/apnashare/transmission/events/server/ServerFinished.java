package com.cod3rboy.apnashare.transmission.events.server;

public class ServerFinished {
    private boolean success;

    public ServerFinished(boolean success) {
        this.success = success;
    }

    public boolean wasSuccessful() {
        return success;
    }
}
