package com.cod3rboy.apnashare.transmission;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.PowerManager;
import android.util.Log;

import com.cod3rboy.apnashare.App;
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
import com.cod3rboy.apnashare.transmission.events.server.ClientConnected;
import com.cod3rboy.apnashare.transmission.events.server.MetaDataReceived;
import com.cod3rboy.apnashare.transmission.events.ProgressUpdate;
import com.cod3rboy.apnashare.transmission.events.server.ServerFinished;
import com.cod3rboy.apnashare.transmission.events.server.TUnitReceptionCompleted;
import com.cod3rboy.apnashare.transmission.events.server.TUnitReceptionStarted;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransmissionServer {
    private static final String LOG_TAG = TransmissionServer.class.getSimpleName();

    public static final int SERVER_PORT = 11024;
    private static final int FILE_BUFFER_SIZE = 10 * 1024; // 10KB of buffer

    private ExecutorService mExecutionService;
    private ServerSocket mServerSocket;
    private int mPort;

    private boolean running;
    private long mBytesReceived;

    private PowerManager.WakeLock mWakeLock;

    public TransmissionServer(Context appContext) {
        mExecutionService = null;
        running = false;
        mPort = SERVER_PORT;
        PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TransmissionServer.class.getSimpleName());
        mWakeLock.setReferenceCounted(false);
    }

    private synchronized void setRunning(boolean running) {
        this.running = running;
    }

    public synchronized boolean isRunning() {
        return this.running;
    }

    public void shutdown() {
        if (mWakeLock.isHeld()) mWakeLock.release();
        if (isRunning()) {// Server is serving client
            setRunning(false);
        } else if (mExecutionService != null) { // Server is either waiting for client to connect
            try {
                if (mServerSocket != null) {
                    mServerSocket.close();
                    mServerSocket = null;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void interruptServer() {
        setRunning(false);
    }

    public void listen() {
        // Acquire wakelock̥
        if (!mWakeLock.isHeld()) mWakeLock.acquire();
        mExecutionService = Executors.newCachedThreadPool();
        mExecutionService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mBytesReceived = 0;
                    mServerSocket = new ServerSocket(mPort);
                    Log.d(LOG_TAG, "Server listening : " + mServerSocket.getInetAddress().getHostName() + ":" + mServerSocket.getLocalPort());
                    // Wait for client to connect
                    Socket clientSoc = mServerSocket.accept();
                    // Client is connected so get client input/output streams
                    setRunning(true);
                    Log.d(LOG_TAG, "Client is connected to server at address " + clientSoc.getRemoteSocketAddress().toString());
                    // Notify that client is connected
                    notifyClientConnected();
                    serveClient(clientSoc);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    interruptServer();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    interruptServer();
                } finally {
                    mExecutionService.shutdown();
                    mExecutionService = null;
                    if (isRunning()) {
                        // Server finished successfully
                        setRunning(false);
                        notifyServerFinished(true);
                    } else {
                        // Server was interrupted
                        notifyServerFinished(false);
                    }
                    if (mServerSocket != null) {
                        try {
                            mServerSocket.close();
                            mServerSocket = null;
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void serveClient(Socket clientSocket) {
        InputStream inStream = null;
        OutputStream outStream = null;
        try {
            // Get Streams
            inStream = new BufferedInputStream(clientSocket.getInputStream());
            outStream = new BufferedOutputStream(clientSocket.getOutputStream());

            // Create Buffered Reader and Writer
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8), true);

            // Handshaking
            HANDSHAKE cmdHandshake = HANDSHAKE.decodeCommand(reader.readLine());
            Log.d(LOG_TAG, "Command Received - " + cmdHandshake);
            String handshakeRes = HANDSHAKE.getRequestString();
            writer.println(handshakeRes);
            Log.d(LOG_TAG, "Command Sent - " + handshakeRes);
            if (cmdHandshake.getRemoteProtocolVersion() != Protocol.VERSION) {
                // Protocol version mismatch
                throw new ProtocolVersionMismatch(cmdHandshake.getRemoteProtocolVersion(), Protocol.VERSION);
            }

            // Get Hash Map of Meta Data
            LinkedHashMap<String, TransmissionFile> filesInTransmission = retrieveMetaData(reader, writer, inStream);

            if (filesInTransmission == null) return;

            // Notify metadata received
            ArrayList<TransmissionFile> filesToReceive = new ArrayList<>();
            for (String key : filesInTransmission.keySet())
                filesToReceive.add(filesInTransmission.get(key));
            notifyMetaDataReceived(filesToReceive);


            // Receive Incoming Files
            for (int i = 0; isRunning() && i < filesInTransmission.size(); i++) {
                String cmd = reader.readLine();
                if (cmd.startsWith(FILE.CMD_PREFIX)) {
                    // File Incoming
                    FILE cmdFile = FILE.decodeCommand(cmd);
                    Log.d(LOG_TAG, "Command Received - " + cmdFile);
                    retrieveIncomingFile(cmdFile, filesInTransmission.get(cmdFile.getFileUUID()), writer, inStream);
                } else if (cmd.startsWith(DIR.CMD_PREFIX)) {
                    // Directory Incoming
                    DIR cmdDir = DIR.decodeCommand(cmd);
                    Log.d(LOG_TAG, "Command Received - " + cmdDir);
                    retrieveIncomingDirectory(cmdDir, filesInTransmission.get(cmdDir.getDirUUID()), writer, reader, inStream);
                } else {
                    // Invalid command. Protocol violated.
                    throw new ProtocolViolationException("Unknown command received from client : " + cmd);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            interruptServer();
        } catch (ProtocolViolationException ex) {
            ex.printStackTrace();
            interruptServer();
        } catch (ProtocolVersionMismatch ex) {
            ex.printStackTrace();
            interruptServer();
            notifyProtocolVersionMismatch(ex.getRemoteProtocolVersion(), ex.getLocalProtocolVersion());
        } finally {
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
                if (clientSocket != null) clientSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private LinkedHashMap<String, TransmissionFile> retrieveMetaData(BufferedReader reader, PrintWriter writer, InputStream inStream) {
        ByteArrayOutputStream outStream = null;
        try {
            // Get META request
            META meta = META.decodeCommand(reader.readLine());
            Log.d(LOG_TAG, "Command Received - " + meta);

            // Send READYMETA response
            String readyMetaRes = READYMETA.getRequestString(meta.getSizeInBytes());
            writer.println(readyMetaRes);
            Log.d(LOG_TAG, "Command Sent - " + readyMetaRes);

            // Get META data for transmission
            outStream = new ByteArrayOutputStream();
            for (long i = 1; isRunning() && i <= meta.getSizeInBytes(); i++) {
                outStream.write(inStream.read());
            }
            if (!isRunning()) {
                // Do not send Acknowledgement if Interrupted
                return null;
            }
            // Send Acknowledgement
            String metaAckgmt = OK.getResponseString();
            writer.println(metaAckgmt);
            Log.d(LOG_TAG, "Command Sent - " + metaAckgmt);

            // Decode received meta data from JSON into collection of TransmissionFile objects
            String metaJson = new String(outStream.toByteArray(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<TransmissionFile>>() {
            }.getType();
            ArrayList<TransmissionFile> metaData = gson.fromJson(metaJson, listType);
            LinkedHashMap<String, TransmissionFile> metaMap = new LinkedHashMap<>();
            for (TransmissionFile tf : metaData) {
                switch (tf.getCategory()) {
                    case APP:
                        tf.setFilePath(SaveConfig.getAppSavePath(tf.getFileName()));
                        break;
                    case IMAGE:
                        tf.setFilePath(SaveConfig.getImageSavePath(tf.getFileName()));
                        break;
                    case VIDEO:
                        tf.setFilePath(SaveConfig.getVideoSavePath(tf.getFileName()));
                        break;
                    case AUDIO:
                        tf.setFilePath(SaveConfig.getAudioSavePath(tf.getFileName()));
                        break;
                    case FILE:
                        tf.setFilePath(SaveConfig.getFileOrDirSavePath(tf.getFileName()));
                        break;
                    default:
                        tf.setFilePath(SaveConfig.getRootSavePath(tf.getFileName()));

                }
                metaMap.put(tf.getFileUUID(), tf);
            }
            return metaMap;
        } catch (IOException ex) {
            ex.printStackTrace();
            interruptServer();
        } catch (ProtocolViolationException ex) {
            ex.printStackTrace();
            interruptServer();
        } catch (JsonParseException ex) {
            ex.printStackTrace();
            interruptServer();
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }

    public void retrieveIncomingFile(FILE cmd, TransmissionFile fileToReceive, PrintWriter writer, InputStream inStream) {
        FileOutputStream fos = null;
        long bytesReceived = 0;
        try {

            File saveFile = new File(fileToReceive.getFilePath());

            // Make sure save directory exists
            // Create path directories if not exist
            if (!saveFile.getParentFile().exists()) saveFile.getParentFile().mkdirs();

            // Create output stream for incoming file
            fos = new FileOutputStream(saveFile, false);

            // Send READYFILE response
            String readyFileRes = READYFILE.getResponseString(cmd.getFileUUID(), 0);
            writer.println(readyFileRes);
            Log.d(LOG_TAG, "Command Sent - " + readyFileRes);

            // Notify File Reception started
            notifyTUnitReceptionStarted(fileToReceive, cmd.getSizeInBytes());

            // Read and Save Incoming File
            byte[] buffer = new byte[FILE_BUFFER_SIZE];
            int len = 0;
            while (isRunning() && bytesReceived < cmd.getSizeInBytes()) {
                len = inStream.read(buffer, 0, buffer.length);
                fos.write(buffer, 0, len);
                bytesReceived += len;
                mBytesReceived += len;
                // Publish progress update
                publishProgressUpdate(fileToReceive, bytesReceived, cmd.getSizeInBytes());
            }
            if (isRunning()) { // Send Acknowledgement if not Interrupted
                // Notify File Reception Completed
                notifyTUnitReceptionCompleted(fileToReceive);
                // Send Acknowledgement for FILE
                String fileAckgmt = OK.getResponseString();
                writer.println(fileAckgmt);
                Log.d(LOG_TAG, "Command Sent - " + fileAckgmt);
                // Tell media scanner to include new file in media store
                MediaScannerConnection.scanFile(App.getInstance(), new String[]{saveFile.getAbsolutePath()}, null, null);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            interruptServer();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void retrieveIncomingDirectory(DIR cmd, TransmissionFile directoryToReceive, PrintWriter writer, BufferedReader reader, InputStream inStream) {

        // Create File Object Representing directory
        File directory = new File(directoryToReceive.getFilePath());
        if (!directory.exists()) directory.mkdirs();

        // Send READYDIR response
        String readyDirRes = READYDIR.getResponseString(cmd.getDirUUID());
        writer.println(READYDIR.getResponseString(cmd.getDirUUID()));
        Log.d(LOG_TAG, "Command Sent - " + readyDirRes);

        // Notify Directory reception started
        notifyTUnitReceptionStarted(directoryToReceive, cmd.getFilesCount());

        long filesCount = cmd.getFilesCount();
        long filesReceived = 0;
        while (isRunning() && filesCount > 0) {
            FileOutputStream fos = null;
            try {
                // Get DIRFILE command
                DIRFILE cmdDirFile = DIRFILE.decodeCommand(reader.readLine());
                Log.d(LOG_TAG, "Command Received - " + cmdDirFile);

                // Create File Object for Incoming File
                File file = new File(directory.getAbsolutePath() + cmdDirFile.getRelativePath());
                if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

                // Create File Output Stream
                fos = new FileOutputStream(file, false);

                // Send READYDIRFILE response
                String readyDirFileRes = READYDIRFILE.getResponseString(cmdDirFile.getDirUUID(), cmdDirFile.getRelativePath(), 0);
                writer.println(readyDirFileRes);
                Log.d(LOG_TAG, "Command Sent - " + readyDirFileRes);

                // Read and Save Incoming File
                byte[] buffer = new byte[FILE_BUFFER_SIZE];
                int len = 0;
                long bytesReceived = 0;
                while (isRunning() && bytesReceived < cmdDirFile.getSizeInBytes()) {
                    len = inStream.read(buffer, 0, buffer.length);
                    fos.write(buffer, 0, len);
                    bytesReceived += len;
                    mBytesReceived += len;
                }
                if (isRunning()) { // Send Acknowledgement if not Interrupted
                    filesCount--;
                    filesReceived++;
                    // Publish progress update
                    publishProgressUpdate(directoryToReceive, filesReceived, cmd.getFilesCount());
                    // Send Acknowledgement for DIRFILE
                    String dirFileAckgmt = OK.getResponseString();
                    writer.println(dirFileAckgmt);
                    Log.d(LOG_TAG, "Command Sent - " + dirFileAckgmt);
                    // Tell media scanner to include new file in media store
                    MediaScannerConnection.scanFile(App.getInstance(), new String[]{file.getAbsolutePath()}, null, null);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                interruptServer();
            } catch (ProtocolViolationException ex) {
                ex.printStackTrace();
                interruptServer();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        if (isRunning()) { // Send Acknowledgement if not Interrupted
            // Update UI progress if directory was empty
            if (filesCount == 0) publishProgressUpdate(directoryToReceive, 1, 1);

            // Notify Directory reception completed
            notifyTUnitReceptionCompleted(directoryToReceive);
            // Send Acknowledgement for DIR
            String dirAckgmt = OK.getResponseString();
            writer.println(dirAckgmt);
            Log.d(LOG_TAG, "Command Sent - " + dirAckgmt);
        }

    }


    private void notifyClientConnected() {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(ClientConnected.class))
            bus.post(new ClientConnected());
    }

    private void notifyMetaDataReceived(ArrayList<TransmissionFile> metaData) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(MetaDataReceived.class))
            bus.post(new MetaDataReceived(metaData));
    }

    private void notifyTUnitReceptionStarted(TransmissionFile file, long filesCountOrSizeInBytes) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(TUnitReceptionStarted.class))
            bus.post(new TUnitReceptionStarted(file, filesCountOrSizeInBytes));
    }

    private void notifyTUnitReceptionCompleted(TransmissionFile file) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(TUnitReceptionCompleted.class))
            bus.post(new TUnitReceptionCompleted(file));
    }

    private void notifyServerFinished(boolean success) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(ServerFinished.class))
            bus.post(new ServerFinished(success));
    }

    private void publishProgressUpdate(TransmissionFile file, long filesOrBytesReceived, long totalFilesOrBytes) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(ProgressUpdate.class))
            bus.post(new ProgressUpdate(file, totalFilesOrBytes, filesOrBytesReceived, mBytesReceived));
    }

    public void notifyProtocolVersionMismatch(int remoteVersion, int localVersion) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(ProtocolMismatch.class))
            bus.post(new ProtocolMismatch(remoteVersion, localVersion));
    }
}
