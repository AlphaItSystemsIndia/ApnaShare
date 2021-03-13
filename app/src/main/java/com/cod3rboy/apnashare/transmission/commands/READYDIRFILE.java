package com.cod3rboy.apnashare.transmission.commands;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.exceptions.ProtocolViolationException;

public class READYDIRFILE {
    private static final String CMD_PREFIX = "READYDIRFILE";
    private static final String DELIMITER = "#";
    private static final int NO_OF_SEGMENTS = 4;

    public static String getResponseString(String dirUUID, String relativePath, long bytesOffset) {
        return CMD_PREFIX + DELIMITER +
                dirUUID + DELIMITER +
                relativePath + DELIMITER +
                bytesOffset;
    }

    private String dirUUID;
    private String relativePath;
    private long bytesOffset;

    public static READYDIRFILE decodeResponse(String response) throws ProtocolViolationException {
        if (!response.startsWith(CMD_PREFIX))
            throw new ProtocolViolationException("Command Mismatch! Expected prefix " + CMD_PREFIX + " but got " + response);
        String[] parts = response.split(DELIMITER);
        if (parts.length != NO_OF_SEGMENTS)
            throw new ProtocolViolationException("Command args mismatch! Expected args " + NO_OF_SEGMENTS + " but got " + parts.length + " in " + response);
        READYDIRFILE cmd = new READYDIRFILE();
        cmd.dirUUID = parts[1];
        cmd.relativePath = parts[2];
        cmd.bytesOffset = Long.parseLong(parts[3]);
        return cmd;
    }

    @NonNull
    @Override
    public String toString() {
        return getResponseString(dirUUID, relativePath, bytesOffset);
    }
}
