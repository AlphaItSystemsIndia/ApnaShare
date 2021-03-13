package com.cod3rboy.apnashare.transmission.commands;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.exceptions.ProtocolViolationException;
import com.cod3rboy.apnashare.transmission.Protocol;

public class HANDSHAKE {
    private static final String CMD_PREFIX = "HANDSHAKE";
    private static final String DELIMITER = "#";
    private static final int NO_OF_SEGMENTS = 2;

    public static String getRequestString() {
        return CMD_PREFIX + DELIMITER + Protocol.VERSION;
    }

    private int remoteProtocolVersion;

    public static HANDSHAKE decodeCommand(String cmd) throws ProtocolViolationException {
        if (!cmd.startsWith(CMD_PREFIX))
            throw new ProtocolViolationException("Command Mismatch! Expected prefix " + CMD_PREFIX + " but got " + cmd);
        String[] parts = cmd.split(DELIMITER);
        if (parts.length != NO_OF_SEGMENTS)
            throw new ProtocolViolationException("Command args mismatch! Expected args " + NO_OF_SEGMENTS + " but got " + parts.length + " in " + cmd);
        HANDSHAKE handshake = new HANDSHAKE();
        handshake.remoteProtocolVersion = Integer.parseInt(parts[1]);
        return handshake;
    }

    public int getRemoteProtocolVersion() {
        return remoteProtocolVersion;
    }

    @NonNull
    @Override
    public String toString() {
        return getRequestString();
    }
}
