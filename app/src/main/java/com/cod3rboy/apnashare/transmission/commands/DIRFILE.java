package com.cod3rboy.apnashare.transmission.commands;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.exceptions.ProtocolViolationException;

public class DIRFILE {
    private static final String CMD_PREFIX = "DIRFILE";
    private static final String DELIMITER = "#";
    private static final int NO_OF_SEGMENTS = 4;

    public static String getRequestString(String argDirId, String argRelativePath, long argSizeInBytes) {
        return CMD_PREFIX + DELIMITER +
                argDirId + DELIMITER +
                argRelativePath + DELIMITER +
                argSizeInBytes;
    }

    private String dirUUID;
    private String relativePath;
    private long sizeInBytes;

    public static DIRFILE decodeCommand(String cmd) throws ProtocolViolationException {
        if (!cmd.startsWith(CMD_PREFIX))
            throw new ProtocolViolationException("Command Mismatch! Expected prefix " + CMD_PREFIX + " but got " + cmd);
        String[] parts = cmd.split(DELIMITER);
        if (parts.length != NO_OF_SEGMENTS)
            throw new ProtocolViolationException("Command args mismatch! Expected args " + NO_OF_SEGMENTS + " but got " + parts.length + " in " + cmd);
        DIRFILE dirFileCmd = new DIRFILE();
        dirFileCmd.dirUUID = parts[1];
        dirFileCmd.relativePath = parts[2];
        dirFileCmd.sizeInBytes = Long.parseLong(parts[3]);
        return dirFileCmd;
    }

    public String getDirUUID() {
        return dirUUID;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    @NonNull
    @Override
    public String toString() {
        return getRequestString(dirUUID, relativePath, sizeInBytes);
    }
}
