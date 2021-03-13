package com.cod3rboy.apnashare.transmission.commands;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.exceptions.ProtocolViolationException;

public class FILE {
    public static final String CMD_PREFIX = "FILE";
    private static final String DELIMITER = "#";
    private static final int NO_OF_SEGMENTS = 3;

    public static String getRequestString(String argId, long argSizeInBytes) {
        return CMD_PREFIX + DELIMITER + argId + DELIMITER + argSizeInBytes;
    }

    private String fileUUID;
    private long sizeInBytes;

    public static FILE decodeCommand(String cmd) throws ProtocolViolationException {
        if (!cmd.startsWith(CMD_PREFIX))
            throw new ProtocolViolationException("Command Mismatch! Expected prefix " + CMD_PREFIX + " but got " + cmd);
        String[] parts = cmd.split(DELIMITER);
        if (parts.length != NO_OF_SEGMENTS)
            throw new ProtocolViolationException("Command args mismatch! Expected args " + NO_OF_SEGMENTS + " but got " + parts.length + " in " + cmd);
        FILE fileCmd = new FILE();
        fileCmd.fileUUID = parts[1];
        fileCmd.sizeInBytes = Long.parseLong(parts[2]);
        return fileCmd;
    }

    public String getFileUUID() {
        return fileUUID;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    @NonNull
    @Override
    public String toString() {
        return getRequestString(fileUUID, sizeInBytes);
    }
}
