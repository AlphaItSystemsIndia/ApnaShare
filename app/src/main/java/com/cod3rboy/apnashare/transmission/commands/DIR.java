package com.cod3rboy.apnashare.transmission.commands;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.exceptions.ProtocolViolationException;

public class DIR {
    public static final String CMD_PREFIX = "DIR";
    private static final String DELIMITER = "#";
    private static final int NO_OF_SEGMENTS = 3;

    public static String getRequestString(String argId, long argFilesCount) {
        return CMD_PREFIX + DELIMITER + argId + DELIMITER + argFilesCount;
    }

    private String dirUUID;
    private long filesCount;

    public static DIR decodeCommand(String cmd) throws ProtocolViolationException {
        if (!cmd.startsWith(CMD_PREFIX))
            throw new ProtocolViolationException("Command Mismatch! Expected prefix " + CMD_PREFIX + " but got " + cmd);
        String[] parts = cmd.split(DELIMITER);
        if (parts.length != NO_OF_SEGMENTS)
            throw new ProtocolViolationException("Command args mismatch! Expected args " + NO_OF_SEGMENTS + " but got " + parts.length + " in " + cmd);
        DIR fileCmd = new DIR();
        fileCmd.dirUUID = parts[1];
        fileCmd.filesCount = Long.parseLong(parts[2]);
        return fileCmd;
    }

    public String getDirUUID() {
        return dirUUID;
    }

    public long getFilesCount() {
        return filesCount;
    }

    @NonNull
    @Override
    public String toString() {
        return getRequestString(dirUUID, filesCount);
    }
}
