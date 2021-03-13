package com.cod3rboy.apnashare.exceptions;

public class WiFiStateException extends RuntimeException {
    public static final String MSG = "WiFi is not enabled.";

    public WiFiStateException() {
        super(MSG);
    }
}
