package com.cod3rboy.apnashare.managers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.cod3rboy.apnashare.misc.FileNode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileManager {
    public interface NavigationListener {
        void backToRoot();

        void subdirectoryVisited(int position);

        void backToParent();

        void forwardChildrenLoaded();
    }

    private Context mContext;
    private ArrayList<FileNode> mDirListing;
    private ArrayList<String> mRootDirectories;
    private ExecutorService mExecutorService; // To load forward children nodes in background
    private FileNode mActiveDir;
    private ArrayList<NavigationListener> mNavListeners;
    private FileNode mRootNode; // root node has null file object inside
    private Handler mMainHandler;

    public FileManager(Context context) {
        mContext = context;
        mDirListing = new ArrayList<>();
        mRootDirectories = new ArrayList<>();
        mNavListeners = new ArrayList<>();
        mMainHandler = new Handler(Looper.getMainLooper());
        initRootDirectories();
    }

    private void initRootDirectories() {
        // Find Paths for available roots storage
        // and Create Empty FileNodes for roots storage
        File[] filesDir = mContext.getExternalFilesDirs(null);
        for (File file : filesDir) {
            if (file == null) continue;
            String[] parts = file.getAbsolutePath().split("/");
            StringBuilder pathBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].equals("Android")) {
                    break;
                } else {
                    pathBuilder.append("/");
                    pathBuilder.append(parts[i]);
                }
            }
            mRootDirectories.add(pathBuilder.toString());
            // Create and add FileNode corresponding to root directory
            File rootDir = new File(pathBuilder.toString());
            if (mRootNode == null) mRootNode = new FileNode(rootDir.getParentFile());
            FileNode node = new FileNode(rootDir);
            node.setParent(mRootNode);
            mRootNode.addChild(node);
            mDirListing.add(node);
        }
        mActiveDir = mRootNode;
        loadForwardChildren();
    }

    public void addNavigationListener(NavigationListener listener) {
        if (listener != null) mNavListeners.add(listener);
    }

    public void removeNavigationListener(NavigationListener listener) {
        if (listener == null) return;
        mNavListeners.remove(listener);
    }

    public File getFileAtIndex(int position) {
        if (position < 0 || position >= mDirListing.size())
            throw new IndexOutOfBoundsException("Accessing children with index beyond directory listing entries.");
        return mDirListing.get(position).getFile();
    }

    public void visitDirectory(int position) {
        if (position < 0 || position >= mDirListing.size())
            throw new IndexOutOfBoundsException("Accessing children with index beyond directory listing entries.");
        FileNode dirToVisit = mDirListing.get(position);
        if (!dirToVisit.getFile().isDirectory())
            throw new RuntimeException("File is not a directory at position : " + position);
        if (dirToVisit == mRootNode) {
            // We have reached back to the start of file system
            mActiveDir = mRootNode;
            mDirListing.clear();
            mDirListing.addAll(mActiveDir.getChildrenList());
            for (int i = 0; i < mNavListeners.size(); i++) mNavListeners.get(i).backToRoot();
        } else {
            mDirListing.clear();
            mDirListing.add(dirToVisit.getParent()); // Add link to parent dir to go back
            mDirListing.addAll(dirToVisit.getChildrenList());
            FileNode oldActiveDir = mActiveDir;
            mActiveDir = dirToVisit;
            if (oldActiveDir.getParent() == mActiveDir) {
                // We have moved back to parent directory
                for (int i = 0; i < mNavListeners.size(); i++) mNavListeners.get(i).backToParent();
            } else {
                // We have moved to a sub directory
                loadForwardChildren();
                for (int i = 0; i < mNavListeners.size(); i++)
                    mNavListeners.get(i).subdirectoryVisited(position);
            }
        }
    }

    public int getChildCountAtIndex(int position) {
        if (position < 0 || position >= mDirListing.size())
            throw new IndexOutOfBoundsException("Accessing children with index beyond directory listing entries.");
        FileNode dirToVisit = mDirListing.get(position);
        if (!dirToVisit.getFile().isDirectory())
            throw new RuntimeException("File is not a directory at position : " + position);
        return mDirListing.get(position).getChildCount();
    }

    private void loadForwardChildren() {
        ArrayList<FileNode> unvisitedDirs = new ArrayList<>();
        for (FileNode node : mDirListing) {
            if (node.getFile().isDirectory() && !node.isLeafNode() && node.getChildCount() == 0)
                unvisitedDirs.add(node); // Non-Leaf Empty Directory = Unvisited Directory
        }
        // Do not load forward children if there are no unvisited directories
        if (!unvisitedDirs.isEmpty()) {
            FileNode[] unvisitedDirsNodes = new FileNode[unvisitedDirs.size()];
            ForwardChildrenLoader loader = new ForwardChildrenLoader(unvisitedDirs.toArray(unvisitedDirsNodes));
            loader.start();
        }
    }

    public int getActiveDirectoryListingCount() {
        if (mActiveDir == mRootNode) return 0;
        return mDirListing.size();
    }

    public ArrayList<String> getAvailableExternalRoots() {
        return new ArrayList<>(mRootDirectories);
    }

    public boolean isRootActiveDirectory() {
        return mActiveDir == mRootNode;
    }

    // @todo clean up tree references to avoid memory leak
    public void dispose() {

    }

    class ForwardChildrenLoader implements Runnable {
        ArrayList<FileNode> mParents;

        /**
         * @param parents FileNodes representing directories only.
         */
        public ForwardChildrenLoader(FileNode... parents) {
            mParents = new ArrayList<>();
            Collections.addAll(mParents, parents);
            mExecutorService = Executors.newCachedThreadPool();
        }

        public void start() {
            mExecutorService.execute(this);
        }

        @Override
        public void run() {
            for (FileNode parent : mParents) {
                // Skip nodes whose children are already loaded
                if (parent.getChildCount() > 0) continue;
                File[] files = parent.getFile().listFiles();
                if (files == null || files.length <= 0) {
                    parent.setLeafNode(true); // Declare parent as leaf node
                    continue; // Skip nodes with no children
                }
                // Apply sorting to files
                Arrays.sort(files);
                ArrayList<FileNode> childDirs = new ArrayList<>();
                ArrayList<FileNode> childFiles = new ArrayList<>();

                for (int i = 0; i < files.length; i++) {
                    FileNode newChild = new FileNode(files[i]);
                    newChild.setParent(parent);
                    if (files[i].isDirectory()) childDirs.add(newChild);
                    else childFiles.add(newChild);
                }
                FileNode[] nodeDirs = new FileNode[childDirs.size()];
                FileNode[] nodeFiles = new FileNode[childFiles.size()];
                parent.addChildren(childDirs.toArray(nodeDirs));
                parent.addChildren(childFiles.toArray(nodeFiles));
            }
            mExecutorService.shutdown();
            mExecutorService = null;
            notifyForwardChildrenLoaded();
        }

        private void notifyForwardChildrenLoaded() {
            mMainHandler.post(() -> {
                for (int i = 0; i < mNavListeners.size(); i++)
                    mNavListeners.get(i).forwardChildrenLoaded();
            });
        }
    }
}
