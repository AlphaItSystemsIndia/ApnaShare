package com.cod3rboy.apnashare.transmission.commands;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.exceptions.ProtocolViolationException;

public class OK {
    private static final String CMD_PREFIX = "OK";

    public static String getResponseString() {
        return CMD_PREFIX;
    }

    public static OK decodeResponse(String response) throws ProtocolViolationException {
        if (!response.startsWith(CMD_PREFIX))
            throw new ProtocolViolationException("Command Mismatch! Expected prefix " + CMD_PREFIX + " but got " + response);
        return new OK();
    }

    @NonNull
    @Override
    public String toString() {
        return getResponseString();
    }
}
