package com.cod3rboy.apnashare.transmission.commands;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.exceptions.ProtocolViolationException;

public class READYFILE {
    private static final String CMD_PREFIX = "READYFILE";
    private static final String DELIMITER = "#";
    private static final int NO_OF_SEGMENTS = 3;

    private String fileUUID;
    private long bytesOffset;

    public static String getResponseString(String fileUUID, long bytesOffset) {
        return CMD_PREFIX + DELIMITER + fileUUID + DELIMITER + bytesOffset;
    }

    public static READYFILE decodeResponse(String response) throws ProtocolViolationException {
        if (!response.startsWith(CMD_PREFIX))
            throw new ProtocolViolationException("Command Mismatch! Expected prefix " + CMD_PREFIX + " but got " + response);
        String[] parts = response.split(DELIMITER);
        if (parts.length != NO_OF_SEGMENTS)
            throw new ProtocolViolationException("Command args mismatch! Expected args " + NO_OF_SEGMENTS + " but got " + parts.length + " in " + response);
        READYFILE cmd = new READYFILE();
        cmd.fileUUID = parts[1];
        cmd.bytesOffset = Long.parseLong(parts[2]);
        return cmd;
    }

    @NonNull
    @Override
    public String toString() {
        return getResponseString(fileUUID, bytesOffset);
    }
}
