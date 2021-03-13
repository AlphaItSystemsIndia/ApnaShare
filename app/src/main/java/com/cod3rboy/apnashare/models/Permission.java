package com.cod3rboy.apnashare.models;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class Permission {
    public enum Type {
        STORAGE, LOCATION, SYSTEM_SETTINGS, WIFI //, MOBILE_DATA
    }

    public interface ActionCallback {
        void performAction();
    }

    private final String name;
    private final String description;
    private String actionName;
    private boolean granted;
    private Type type;
    private ActionCallback actionCallback;
    private Drawable icon;

    public Permission(@NonNull String name, @NonNull String description, Type type, Drawable icon, @NonNull String actionName, ActionCallback actionCallback) {
        this.name = name;
        this.description = description;
        this.actionName = actionName;
        this.granted = false;
        this.type = type;
        this.icon = icon;
        this.actionCallback = actionCallback;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }


    public boolean isGranted() {
        return granted;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getActionName() {
        return actionName;
    }

    public Type getType() {
        return type;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void executeAction() {
        if (actionCallback != null)
            actionCallback.performAction();
    }
}
