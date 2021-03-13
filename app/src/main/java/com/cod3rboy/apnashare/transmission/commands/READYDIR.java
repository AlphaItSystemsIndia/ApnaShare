package com.cod3rboy.apnashare.transmission.commands;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.exceptions.ProtocolViolationException;

public class READYDIR {
    private static final String CMD_PREFIX = "READYDIR";
    private static final String DELIMITER = "#";
    private static final int NO_OF_SEGMENTS = 2;

    public static String getResponseString(String dirUUID) {
        return CMD_PREFIX + DELIMITER + dirUUID;
    }

    private String dirUUID;

    public static READYDIR decodeResponse(String response) throws ProtocolViolationException {
        if (!response.startsWith(CMD_PREFIX))
            throw new ProtocolViolationException("Command Mismatch! Expected prefix " + CMD_PREFIX + " but got " + response);
        String[] parts = response.split(DELIMITER);
        if (parts.length != NO_OF_SEGMENTS)
            throw new ProtocolViolationException("Command args mismatch! Expected args " + NO_OF_SEGMENTS + " but got " + parts.length + " in " + response);
        READYDIR cmd = new READYDIR();
        cmd.dirUUID = parts[1];
        return cmd;
    }

    @NonNull
    @Override
    public String toString() {
        return getResponseString(dirUUID);
    }
}
