package com.cod3rboy.apnashare.transmission.commands;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.exceptions.ProtocolViolationException;

public class META {
    private static final String CMD_PREFIX = "META";
    private static final String DELIMITER = "#";
    private static final int NO_OF_SEGMENTS = 2;

    public static String getRequestString(long argMetaSizeBytes) {
        return CMD_PREFIX + DELIMITER + argMetaSizeBytes;
    }

    private long sizeInBytes;

    public static META decodeCommand(String cmd) throws ProtocolViolationException {
        if (!cmd.startsWith(CMD_PREFIX))
            throw new ProtocolViolationException("Command Mismatch! Expected prefix " + CMD_PREFIX + " but got " + cmd);
        String[] parts = cmd.split(DELIMITER);
        if (parts.length != NO_OF_SEGMENTS)
            throw new ProtocolViolationException("Command args mismatch! Expected args " + NO_OF_SEGMENTS + " but got " + parts.length + " in " + cmd);
        META meta = new META();
        meta.sizeInBytes = Long.parseLong(parts[1]);
        return meta;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    @NonNull
    @Override
    public String toString() {
        return getRequestString(sizeInBytes);
    }
}
