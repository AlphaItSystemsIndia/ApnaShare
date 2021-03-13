package com.cod3rboy.apnashare.misc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FileNode {
    private File mFile;
    private ArrayList<FileNode> mChildren;
    private FileNode mParent;
    private boolean mLeafNode;

    public FileNode(File file) {
        mFile = file;
        mChildren = new ArrayList<>();
        mLeafNode = false;
        mParent = null;
    }

    public void setParent(FileNode parent) {
        mParent = parent;
    }

    public FileNode getParent() {
        return mParent;
    }

    public void addChildren(FileNode... children) {
        Collections.addAll(mChildren, children);
    }

    public void addChild(FileNode child) {
        mChildren.add(child);
    }

    public FileNode[] getChildren() {
        FileNode[] children = new FileNode[mChildren.size()];
        return mChildren.toArray(children);
    }

    public ArrayList<FileNode> getChildrenList() {
        return mChildren;
    }

    public int getChildCount() {
        return mChildren.size();
    }

    public File getFile() {
        return mFile;
    }

    public void setLeafNode(boolean value) {
        mLeafNode = value;
    }

    public boolean isLeafNode() {
        return mLeafNode;
    }
}
