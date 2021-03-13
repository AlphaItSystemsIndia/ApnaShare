package com.cod3rboy.apnashare.exceptions;

public class ProtocolVersionMismatch extends Exception {
    private int localProtocolVersion;
    private int remoteProtocolVersion;

    public ProtocolVersionMismatch(int remoteProtocolVersion, int localProtocolVersion) {
        super("Protocol Version Mismatch - " + "Remote:" + remoteProtocolVersion + " LOCAL:" + localProtocolVersion);
        this.localProtocolVersion = localProtocolVersion;
        this.remoteProtocolVersion = remoteProtocolVersion;
    }

    public int getRemoteProtocolVersion() {
        return remoteProtocolVersion;
    }

    public int getLocalProtocolVersion() {
        return localProtocolVersion;
    }
}
