package com.cod3rboy.apnashare.events;

import java.net.InetAddress;

public class WiFiReceiverConnected {
    private InetAddress ipAddress;
    private String receiverSSID;

    public WiFiReceiverConnected(InetAddress ipAddress, String receiverSSID) {
        this.ipAddress = ipAddress;
        this.receiverSSID = receiverSSID;
    }

    public InetAddress getIpAddress() {
        return this.ipAddress;
    }

    public String getReceiverSSID() {
        return this.receiverSSID;
    }
}
