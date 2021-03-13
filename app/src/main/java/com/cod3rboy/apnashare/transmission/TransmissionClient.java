package com.cod3rboy.apnashare.transmission;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.cod3rboy.apnashare.exceptions.ProtocolVersionMismatch;
import com.cod3rboy.apnashare.exceptions.ProtocolViolationException;
import com.cod3rboy.apnashare.models.TransmissionFile;
import com.cod3rboy.apnashare.transmission.commands.DIR;
import com.cod3rboy.apnashare.transmission.commands.DIRFILE;
import com.cod3rboy.apnashare.transmission.commands.FILE;
import com.cod3rboy.apnashare.transmission.commands.HANDSHAKE;
import com.cod3rboy.apnashare.transmission.commands.META;
import com.cod3rboy.apnashare.transmission.commands.OK;
import com.cod3rboy.apnashare.transmission.commands.READYDIR;
import com.cod3rboy.apnashare.transmission.commands.READYDIRFILE;
import com.cod3rboy.apnashare.transmission.commands.READYFILE;
import com.cod3rboy.apnashare.transmission.commands.READYMETA;
import com.cod3rboy.apnashare.transmission.events.ProtocolMismatch;
import com.cod3rboy.apnashare.transmission.events.ProgressUpdate;
import com.cod3rboy.apnashare.transmission.events.client.TUnitCompleted;
import com.cod3rboy.apnashare.transmission.events.client.TUnitStarted;
import com.cod3rboy.apnashare.transmission.events.client.TransmissionEnded;
import com.cod3rboy.apnashare.transmission.events.client.TransmissionStarted;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

public class TransmissionClient {
    private static final String LOG_TAG = TransmissionClient.class.getSimpleName();
    private static final int FILE_BUFFER_SIZE = 10 * 1024; // 10KB of buffer

    private ExecutorService mExecutionService;
    private ArrayList<TransmissionFile> mFilesToTransmit;
    private Context mContext;
    private boolean mRunning;
    private long mBytesTransmitted;

    public TransmissionClient(Context appContext, ArrayList<TransmissionFile> filesToTransmit) {
        mContext = appContext;
        mExecutionService = null;
        mFilesToTransmit = new ArrayList<>();
        mFilesToTransmit.addAll(filesToTransmit);
        mRunning = false;
        mBytesTransmitted = 0;
    }

    public synchronized boolean isRunning() {
        return mRunning;
    }

    public synchronized void setRunning(boolean running) {
        mRunning = running;
    }

    public void beginTransmission(InetAddress destHostAddress, int destPort) {
        if (mFilesToTransmit.isEmpty()) return;
        mExecutionService = Executors.newCachedThreadPool();
        mExecutionService.execute(new Runnable() {
            @Override
            public void run() {
                // 4s delay before connecting to server. It is working now!
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                setRunning(true);
                notifyTransmissionStarted();
                InputStream inStream = null;
                OutputStream outStream = null;
                Socket socket = null;
                try {
                    InetSocketAddress serverAddress = new InetSocketAddress(destHostAddress, destPort);
                    Log.d(LOG_TAG, "Connecting to server at socket address : " + serverAddress.toString());
                    socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
                    Log.d(LOG_TAG, "Connection Successful!");
                    // Get Streams
                    inStream = new BufferedInputStream(socket.getInputStream());
                    outStream = new BufferedOutputStream(socket.getOutputStream());
                    // Create Buffered Reader and Writer
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8), true);

                    // Handshaking
                    String handshakeCmd = HANDSHAKE.getRequestString();
                    writer.println(handshakeCmd);
                    Log.d(LOG_TAG, "Command Sent - " + handshakeCmd);
                    HANDSHAKE handshakeResponse = HANDSHAKE.decodeCommand(reader.readLine());
                    Log.d(LOG_TAG, "Command Received - " + handshakeResponse);
                    if (handshakeResponse.getRemoteProtocolVersion() != Protocol.VERSION) {
                        // Protocol version mismatch
                        throw new ProtocolVersionMismatch(handshakeResponse.getRemoteProtocolVersion(), Protocol.VERSION);
                    }

                    // Encode meta of Transmission File Collection into JSON
                    Gson gson = new Gson();
                    byte[] metaData = gson.toJson(mFilesToTransmit).getBytes();

                    // Send META Request
                    String metaCmd = META.getRequestString(metaData.length);
                    writer.println(metaCmd);
                    Log.d(LOG_TAG, "Command Sent - " + metaCmd);

                    // Wait for READYMETA response
                    READYMETA response = READYMETA.decodeCommand(reader.readLine());
                    Log.d(LOG_TAG, "Command Received - " + response);

                    // Send META data
                    outStream.write(metaData);
                    outStream.flush();

                    // Get META acknowledgement
                    OK metaAckgmt = OK.decodeResponse(reader.readLine());
                    Log.d(LOG_TAG, "Command Received - " + metaAckgmt);

                    for (int i = 0; isRunning() && i < mFilesToTransmit.size(); i++) {
                        if (!mFilesToTransmit.get(i).isDirectory()) {
                            // Handle File Transfer Here
                            handleFileTransfer(mFilesToTransmit.get(i), outStream, reader, writer);
                        } else {
                            // Handle Directory Transfer Here
                            handleDirectoryTransfer(mFilesToTransmit.get(i), outStream, reader, writer);
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    interruptTransmission();
                } catch (ProtocolViolationException ex) {
                    ex.printStackTrace();
                    interruptTransmission();
                } catch (ProtocolVersionMismatch ex) {
                    ex.printStackTrace();
                    interruptTransmission();
                    notifyProtocolVersionMismatch(ex.getRemoteProtocolVersion(), ex.getLocalProtocolVersion());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    interruptTransmission();
                } finally {
                    mExecutionService.shutdown();
                    mExecutionService = null;
                    if (isRunning()) {
                        // Transmission Completed Successfully
                        setRunning(false);
                        notifyTransmissionEnded(true);
                    } else {
                        // Transmission is either manually stopped or some exception occurred.
                        notifyTransmissionEnded(false);
                    }
                    try {
                        if (inStream != null) inStream.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    try {
                        if (outStream != null) outStream.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    try {
                        if (socket != null) socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void interruptTransmission() {
        setRunning(false);
    }

    private void handleFileTransfer(TransmissionFile transmissionFile, OutputStream outStream, BufferedReader reader, PrintWriter writer) {
        InputStream inStream = null;
        long bytesSent = 0;
        try {
            if (transmissionFile.getFilePathType() == TransmissionFile.FilePathType.PATH_TYPE_ABSOLUTE) {
                // File has an absolute path
                inStream = new FileInputStream(transmissionFile.getFilePath());
            } else {
                // File has content uri
                inStream = mContext.getContentResolver().openInputStream(Uri.parse(transmissionFile.getFilePath()));
            }
            // Send FILE request to receiver
            String cmdFile = FILE.getRequestString(transmissionFile.getFileUUID(), transmissionFile.getFileItemsOrSizeInBytes());
            writer.println(cmdFile);
            Log.d(LOG_TAG, "Command Sent - " + cmdFile);
            // Wait for READYFILE response
            // @todo In future, consider response value for verification and file-resume support.
            READYFILE response = READYFILE.decodeResponse(reader.readLine());
            Log.d(LOG_TAG, "Command Received - " + response);
            // Notify Transmission Started
            notifyTransmissionUnitStarted(transmissionFile);
            // Initiate File Transfer
            byte[] buffer = new byte[FILE_BUFFER_SIZE];
            int len = 0;
            while (isRunning() && (len = inStream.read(buffer, 0, buffer.length)) != -1) {
                outStream.write(buffer, 0, len);
                outStream.flush();
                bytesSent += len;
                mBytesTransmitted += len;
                // Publish Progress Update
                publishProgressUpdate(transmissionFile, transmissionFile.getFileItemsOrSizeInBytes(), bytesSent);
            }
            if (isRunning()) { // Only wait for Acknowledgement when there is no interruption
                // Wait for acknowledgement
                OK ackgmt = OK.decodeResponse(reader.readLine());
                Log.d(LOG_TAG, "Command Received - " + ackgmt);
                // Notify Transmission Completed
                notifyTransmissionUnitCompleted(transmissionFile);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            interruptTransmission();
        } catch (ProtocolViolationException ex) {
            ex.printStackTrace();
            interruptTransmission();
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void handleDirectoryTransfer(TransmissionFile transmissionFile, OutputStream outStream, BufferedReader reader, PrintWriter writer) {
        // Retrieve files from Directory Structure
        File directory = new File(transmissionFile.getFilePath());
        Stack<File> dirsToProcess = new Stack<>();
        ArrayList<File> dirFiles = new ArrayList<>();
        dirsToProcess.push(directory);
        while (!dirsToProcess.isEmpty()) {
            File d = dirsToProcess.pop();
            File[] files = d.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (f.isDirectory()) dirsToProcess.push(f);
                else if (f.isFile()) dirFiles.add(f);
            }
        }

        long filesSent = 0;
        try {
            // Send DIR request to receiver
            String cmdDir = DIR.getRequestString(transmissionFile.getFileUUID(), dirFiles.size());
            writer.println(cmdDir);
            Log.d(LOG_TAG, "Command Sent - " + cmdDir);
            // Wait for READYDIR response
            // @todo In future, consider response value for verification purpose.
            READYDIR dirResponse = READYDIR.decodeResponse(reader.readLine());
            Log.d(LOG_TAG, "Command Received - " + dirResponse);
            // Notify Transmission Started
            notifyTransmissionUnitStarted(transmissionFile);
            // Send Directory Files
            for (int i = 0; isRunning() && i < dirFiles.size(); i++) {
                File file = dirFiles.get(i);
                InputStream inStream = null;
                try {
                    // Create input stream for file
                    inStream = new FileInputStream(file);
                    // Send DIRFILE request to receiver
                    String relFilePath = file.getAbsolutePath().substring(directory.getAbsolutePath().length());
                    String cmdDirFile = DIRFILE.getRequestString(transmissionFile.getFileUUID(), relFilePath, file.length());
                    writer.println(cmdDirFile);
                    Log.d(LOG_TAG, "Command Sent - " + cmdDirFile);
                    // Wait for READYDIRFILE response
                    // @todo In future, consider response value for verification and file-resume support.
                    READYDIRFILE dirFileResponse = READYDIRFILE.decodeResponse(reader.readLine());
                    Log.d(LOG_TAG, "Command Received - " + dirFileResponse);
                    // Initiate File Transfer
                    byte[] buffer = new byte[FILE_BUFFER_SIZE];
                    int len = 0;
                    while (isRunning() && (len = inStream.read(buffer, 0, buffer.length)) != -1) {
                        outStream.write(buffer, 0, len);
                        outStream.flush();
                        mBytesTransmitted += len;
                    }
                    if (isRunning()) { // Only wait for Acknowledgement when there is no interruption
                        // Wait for acknowledgement
                        OK ackgmt = OK.decodeResponse(reader.readLine());
                        Log.d(LOG_TAG, "Command Received - " + ackgmt);
                        filesSent++;
                        publishProgressUpdate(transmissionFile, dirFiles.size(), filesSent);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    interruptTransmission();
                } catch (ProtocolViolationException ex) {
                    ex.printStackTrace();
                    interruptTransmission();
                } finally {
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            if (isRunning()) {// Only wait for Acknowledgement when there is no interruption
                // Update UI progress if directory was empty
                if (dirFiles.isEmpty()) publishProgressUpdate(transmissionFile, 1, 1);

                // Wait for acknowledgement
                OK ackgmt = OK.decodeResponse(reader.readLine());
                Log.d(LOG_TAG, "Command Received - " + ackgmt);
                // Notify Transmission Completed
                notifyTransmissionUnitCompleted(transmissionFile);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            interruptTransmission();
        } catch (ProtocolViolationException ex) {
            ex.printStackTrace();
            interruptTransmission();
        }
    }

    private void notifyTransmissionStarted() {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(TransmissionStarted.class))
            bus.post(new TransmissionStarted());
    }

    private void notifyTransmissionEnded(boolean success) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(TransmissionEnded.class))
            bus.post(new TransmissionEnded(success));
    }

    private void notifyTransmissionUnitStarted(TransmissionFile file) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(TUnitStarted.class))
            bus.post(new TUnitStarted(file));
    }

    private void notifyTransmissionUnitCompleted(TransmissionFile file) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(TUnitCompleted.class))
            bus.post(new TUnitCompleted(file));
    }

    private void publishProgressUpdate(TransmissionFile file, long totalBytesOrFiles, long bytesOrFilesSent) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(ProgressUpdate.class))
            bus.post(new ProgressUpdate(file, totalBytesOrFiles, bytesOrFilesSent, mBytesTransmitted));
    }

    public void notifyProtocolVersionMismatch(int remoteVersion, int localVersion) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(ProtocolMismatch.class))
            bus.post(new ProtocolMismatch(remoteVersion, localVersion));
    }
}