package com.cod3rboy.apnashare.events;

import java.util.ArrayList;

public class WiFiScanComplete {
    private ArrayList<String> receiverSSIDs;

    public WiFiScanComplete(ArrayList<String> receiverSSIDs) {
        this.receiverSSIDs = receiverSSIDs;
    }

    public ArrayList<String> getReceiverSSIDs() {
        return this.receiverSSIDs;
    }
}
