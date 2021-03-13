package com.cod3rboy.apnashare.transmission.events.server;

import com.cod3rboy.apnashare.models.TransmissionFile;

import java.util.ArrayList;

public class MetaDataReceived {
    private ArrayList<TransmissionFile> filesToReceive;

    public MetaDataReceived(ArrayList<TransmissionFile> filesToReceive) {
        this.filesToReceive = filesToReceive;
    }

    public ArrayList<TransmissionFile> getFilesToReceive() {
        return filesToReceive;
    }
}
