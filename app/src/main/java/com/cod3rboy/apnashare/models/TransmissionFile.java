package com.cod3rboy.apnashare.models;

import android.content.ContentResolver;
import android.webkit.MimeTypeMap;

import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.transmission.events.ProgressUpdate;

import java.util.UUID;

public class TransmissionFile {
    public enum Category {
        IMAGE, APP, AUDIO, VIDEO, FILE
    }

    public enum State {
        WAITING, PROGRESSING, SUCCESS, FAILED
    }

    public enum FilePathType {
        PATH_TYPE_ABSOLUTE, PATH_TYPE_CONTENT
    }

    private String fileName;
    private byte[] fileIcon;
    private long fileItemsOrSizeInBytes;
    private boolean isDirectory;
    private String fileUUID;
    private Category category;

    // Exclude these fields from serialization/deserialization by declaring them transient
    private transient ProgressUpdate progressUpdate;
    private transient State state;
    private transient String filePath;
    private transient FilePathType filePathType;

    public static TransmissionFile createFromSelectedFile(BasicFile selectedFile) {
        TransmissionFile fileToTransmit = new TransmissionFile();
        fileToTransmit.setFileName(selectedFile.getFileName());
        fileToTransmit.setFileIcon(Utilities.bitmapToByteArray(selectedFile.getIcon()));
        fileToTransmit.setFileItemsOrSizeInBytes(selectedFile.getBytesOrItemsCount());
        fileToTransmit.setDirectory((selectedFile instanceof GeneralFile) && (((GeneralFile) selectedFile).isDirectory()));
        fileToTransmit.setFileUUID(UUID.randomUUID().toString());
        fileToTransmit.setFilePath(selectedFile.getPath());
        if (fileToTransmit.getFilePath().startsWith(ContentResolver.SCHEME_CONTENT))
            fileToTransmit.setFilePathType(FilePathType.PATH_TYPE_CONTENT);
        else
            fileToTransmit.setFilePathType(FilePathType.PATH_TYPE_ABSOLUTE);

        if (selectedFile instanceof AppFile)
            fileToTransmit.setCategory(Category.APP);
        else if (selectedFile instanceof ImageFile)
            fileToTransmit.setCategory(Category.IMAGE);
        else if (selectedFile instanceof VideoFile)
            fileToTransmit.setCategory(Category.VIDEO);
        else if (selectedFile instanceof AudioFile)
            fileToTransmit.setCategory(Category.AUDIO);
        else if (selectedFile instanceof GeneralFile) {
            // File Extension
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileToTransmit.getFilePath());
            if (!fileToTransmit.isDirectory() && fileExtension != null) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
                if (mimeType != null) {
                    if (mimeType.startsWith("image/"))
                        fileToTransmit.setCategory(Category.IMAGE);
                    else if (mimeType.startsWith("video/"))
                        fileToTransmit.setCategory(Category.VIDEO);
                    else if (mimeType.startsWith("audio/"))
                        fileToTransmit.setCategory(Category.AUDIO);
                    else if (fileExtension.toLowerCase().equals("apk"))
                        fileToTransmit.setCategory(Category.APP);
                    else fileToTransmit.setCategory(Category.FILE);
                } else {
                    fileToTransmit.setCategory(Category.FILE);
                }
            } else {
                fileToTransmit.setCategory(Category.FILE);
            }
        } else {
            fileToTransmit.setCategory(Category.FILE);
        }
        return fileToTransmit;
    }

    private TransmissionFile() {
        this.state = State.WAITING;
        this.filePath = "";
        this.filePathType = FilePathType.PATH_TYPE_ABSOLUTE;
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public void setProgressUpdate(ProgressUpdate updateEventObject) {
        this.progressUpdate = updateEventObject;
    }

    public ProgressUpdate getProgressUpdate() {
        return progressUpdate;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setFileIcon(byte[] fileIcon) {
        this.fileIcon = fileIcon;
    }

    public void setFileItemsOrSizeInBytes(long value) {
        this.fileItemsOrSizeInBytes = value;
    }

    public void setDirectory(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public void setFileUUID(String fileUUID) {
        this.fileUUID = fileUUID;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFilePathType(FilePathType filePathType) {
        this.filePathType = filePathType;
    }

    public String getFileName() {
        return fileName;
    }

    public Category getCategory() {
        return category;
    }

    public byte[] getFileIcon() {
        return fileIcon;
    }

    public long getFileItemsOrSizeInBytes() {
        return fileItemsOrSizeInBytes;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getFileUUID() {
        return fileUUID;
    }

    public String getFilePath() {
        return filePath;
    }

    public FilePathType getFilePathType() {
        return filePathType;
    }
}
