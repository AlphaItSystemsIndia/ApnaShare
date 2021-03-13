package com.cod3rboy.apnashare.transmission.events;

public class ProtocolMismatch {
    private int remoteProtocolVersion;
    private int localProtocolVersion;

    public ProtocolMismatch(int remoteProtocolVersion, int localProtocolVersion) {
        this.remoteProtocolVersion = remoteProtocolVersion;
        this.localProtocolVersion = localProtocolVersion;
    }

    public int getRemoteProtocolVersion() {
        return this.remoteProtocolVersion;
    }

    public int getLocalProtocolVersion() {
        return this.localProtocolVersion;
    }
}
