package com.cod3rboy.apnashare.activities;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.adapters.PermissionsAdapter;
import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.models.Permission;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class PermissionActivity extends BaseActivity {
    private static final int REQUEST_CODE_LOCATION = 501;
    private static final int REQUEST_CODE_STORAGE = 502;

    public static final int PERMISSION_STORAGE = 0x00000001; // Read-Write storage permission
    public static final int PERMISSION_LOCATION = 0x00000010; // Access find location permission
    public static final int PERMISSION_SYSTEM_SETTINGS = 0x0000100; // Modify system settings permissions
    public static final int PERMISSION_WIFI = 0x00001000; // Turn on wifi permission
    //public static final int PERMISSION_MOBILE_DATA = 0x00010000; // Turn off mobile data permission
    public static final String EXTRA_PERMISSIONS = "permissions_extra";


    public static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView mRvPermissions;
    private MaterialButton mBtnContinue;
    private PermissionsAdapter mAdapter;
    private LocationReceiver mLocationReceiver;
    private WiFiReceiver mWiFiReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Override default status bar color here
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorAccent));

        setContentView(R.layout.activity_permission);
        mRvPermissions = findViewById(R.id.rv_permissions);
        mBtnContinue = findViewById(R.id.btn_continue);
        mBtnContinue.setEnabled(false);

        mRvPermissions.setLayoutManager(new LinearLayoutManager(this));
        mRvPermissions.setHasFixedSize(true);
        mAdapter = new PermissionsAdapter(this);
        mRvPermissions.setAdapter(mAdapter);
        DividerItemDecoration dividerDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        dividerDecoration.setDrawable(getResources().getDrawable(R.drawable.list_divider));
        mRvPermissions.addItemDecoration(dividerDecoration);
        mBtnContinue.setOnClickListener((view) -> {
            setResult(Activity.RESULT_OK);
            finish();
        });

        // Get permissions to display in the list passed in the intent.
        ArrayList<Permission> permissions = new ArrayList<>();
        Intent i = getIntent();
        if (i != null && i.hasExtra(EXTRA_PERMISSIONS)) {
            int permissionMask = i.getIntExtra(EXTRA_PERMISSIONS, 0);
            if ((permissionMask & PERMISSION_LOCATION) != 0) {
                Permission locationPermission = new Permission(
                        getString(R.string.permission_location_name),
                        getString(R.string.permission_location_description),
                        Permission.Type.LOCATION,
                        getResources().getDrawable(R.drawable.ic_gps),
                        getString(R.string.permission_action_turn_on),
                        this::requestLocationPermission);
                permissions.add(locationPermission);
                locationPermission.setGranted(Utilities.isLocationEnabled());
                // Create and register location broadcast receiver
                mLocationReceiver = new LocationReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
                registerReceiver(mLocationReceiver, filter);
            }
            if ((permissionMask & PERMISSION_STORAGE) != 0) {
                permissions.add(
                        new Permission(
                                getString(R.string.permission_storage_name),
                                getString(R.string.permission_storage_description),
                                Permission.Type.STORAGE,
                                getResources().getDrawable(R.drawable.ic_storage),
                                getString(R.string.permission_action_allow),
                                this::requestStoragePermission)
                );
            }
            if ((permissionMask & PERMISSION_SYSTEM_SETTINGS) != 0) {
                permissions.add(
                        new Permission(
                                getString(R.string.permission_modify_settings_name),
                                getString(R.string.permission_modify_settings_description),
                                Permission.Type.SYSTEM_SETTINGS,
                                getResources().getDrawable(R.drawable.ic_settings),
                                getString(R.string.permission_action_allow),
                                this::requestModifySystemSettings)
                );
            }
            if ((permissionMask & PERMISSION_WIFI) != 0) {
                Permission wifiPermission = new Permission(
                        getString(R.string.permission_wifi_name),
                        getString(R.string.permission_wifi_description),
                        Permission.Type.WIFI,
                        getResources().getDrawable(R.drawable.ic_wifi),
                        getString(R.string.permission_action_turn_on),
                        this::requestWiFiEnable);
                permissions.add(wifiPermission);
                wifiPermission.setGranted(Utilities.isWiFiEnabled());
                mWiFiReceiver = new WiFiReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
                registerReceiver(mWiFiReceiver, filter);
            }
        }
        // set permissions in the adapter
        mAdapter.setPermissionModels(permissions);

    }

    public void requestLocationPermission() {
        if (Utilities.isLocationPermissionGranted()) {
            onLocationPermissionGranted();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Show permission request dialog
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
        }
    }

    public void requestStoragePermission() {
        if (Utilities.isStoragePermissionGranted()) {
            // Permission allowed
            onStoragePermissionGranted();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Show permission dialog
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
        }
    }

    public void requestWiFiEnable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                if (isHotspotEnabled(wm)) setHotspotEnabled(wm, false);
                wm.setWifiEnabled(true);
            }
        } else {
            // Show settings panel for WiFi
            startActivity(new Intent(Settings.Panel.ACTION_WIFI));
        }
    }

    private boolean isHotspotEnabled(WifiManager wm) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                Method hotspotEnabledMethod = WifiManager.class.getMethod("isWifiApEnabled");
                return (Boolean) hotspotEnabledMethod.invoke(wm);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    private void setHotspotEnabled(WifiManager wm, boolean enabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                Method disableApMethod = WifiManager.class.getMethod(
                        "setWifiApEnabled",
                        WifiConfiguration.class,
                        boolean.class
                );
                if (!enabled) wm.setWifiEnabled(false);
                disableApMethod.invoke(wm, null, enabled);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void requestModifySystemSettings() {
        if (Utilities.canWriteSystemSettings()) {
            // Modify System Settings is allowed
            mAdapter.setPermissionGranted(Permission.Type.SYSTEM_SETTINGS, true);
            if (mAdapter.allPermissionsGranted()) mBtnContinue.setEnabled(true);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                        .setNegativeButton(R.string.perm_dialog_deny_btn, (dialog, which) -> dialog.dismiss())
                        .setTitle(R.string.permission_dialog_modify_settings_title)
                        .setMessage(R.string.permission_dialog_modify_settings_description_1);
                // Show permission management screen to allow modify system settings
                Intent i1 = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                i1.addCategory(Intent.CATEGORY_DEFAULT);
                i1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent i2 = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + this.getPackageName()));
                i2.addCategory(Intent.CATEGORY_DEFAULT);
                i2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (i1.resolveActivity(getPackageManager()) != null) {
                    startActivity(i1);
                    builder.setPositiveButton(R.string.perm_dialog_settings_btn, (dialog, which) -> {
                        startActivity(i1);
                        dialog.dismiss();
                    }).show();
                } else if (i2.resolveActivity(getPackageManager()) != null) {
                    builder.setPositiveButton(R.string.perm_dialog_settings_btn, (dialog, which) -> {
                        startActivity(i2);
                        dialog.dismiss();
                    }).show();
                } else {
                    builder.setMessage(R.string.permission_dialog_modify_settings_description_2);
                    builder.setPositiveButton(R.string.perm_dialog_settings_btn, (dialog, which) -> {
                        Intent i = new Intent(Settings.ACTION_SETTINGS);
                        i.addCategory(Intent.CATEGORY_DEFAULT);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        dialog.dismiss();
                    }).show();
                }
            }
        }
    }

    private void onLocationPermissionGranted() {
        if (!Utilities.isLocationEnabled()) {
            // Turn on location here
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } else {
            mAdapter.setPermissionGranted(Permission.Type.LOCATION, true);
            if (mAdapter.allPermissionsGranted()) mBtnContinue.setEnabled(true);
        }
    }

    private void onStoragePermissionGranted() {
        // Set permission is granted
        mAdapter.setPermissionGranted(Permission.Type.STORAGE, true);
        if (mAdapter.allPermissionsGranted()) mBtnContinue.setEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onLocationPermissionGranted();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            // User has selected never ask again in last permission dialog.
                            // So system will not show Permission request dialog and deny permission immediately.
                            // We need to ask user to explicitly grant permission from app settings page.
                            new MaterialAlertDialogBuilder(this)
                                    .setPositiveButton(R.string.perm_dialog_settings_btn, (dialog, which) -> {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.parse("package:" + this.getPackageName()));
                                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        this.startActivityForResult(intent, 0);
                                        dialog.dismiss();
                                    })
                                    .setNegativeButton(R.string.perm_dialog_settings_btn, (dialog, which) -> dialog.dismiss())
                                    .setTitle(R.string.permission_dialog_location_title)
                                    .setMessage(R.string.permission_dialog_location_description)
                                    .show();
                        }
                    }
                }
                break;
            case REQUEST_CODE_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onStoragePermissionGranted();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // User has selected never ask again in last permission dialog.
                        // So system will not show Permission request dialog and deny permission immediately.
                        // We need to ask user to explicitly grant permission from app settings page.
                        new MaterialAlertDialogBuilder(this)
                                .setPositiveButton(R.string.perm_dialog_settings_btn, (dialog, which) -> {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:" + this.getPackageName()));
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    this.startActivityForResult(intent, 0);
                                    dialog.dismiss();
                                })
                                .setNegativeButton(R.string.perm_dialog_deny_btn, (dialog, which) -> dialog.dismiss())
                                .setTitle(R.string.permission_dialog_storage_name)
                                .setMessage(R.string.permission_dialog_storage_description)
                                .show();
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // Finish Permission Activity by setting result status to false which indicated that permissions
        // are not fulfilled by user.
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationReceiver != null) {
            unregisterReceiver(mLocationReceiver);
            mLocationReceiver = null;
        }
        if (mWiFiReceiver != null) {
            unregisterReceiver(mWiFiReceiver);
            mWiFiReceiver = null;
        }
    }

    private class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                if (Utilities.isLocationEnabled()) {
                    // Location is successfully enabled
                    mAdapter.setPermissionGranted(Permission.Type.LOCATION, true);
                    if (mAdapter.allPermissionsGranted()) mBtnContinue.setEnabled(true);
                } else {
                    // Location is disabled
                    mAdapter.setPermissionGranted(Permission.Type.LOCATION, false);
                    mBtnContinue.setEnabled(false);
                }
            }
        }
    }

    public class WiFiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().matches(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                    mAdapter.setPermissionGranted(Permission.Type.WIFI, true);
                    if (mAdapter.allPermissionsGranted()) mBtnContinue.setEnabled(true);
                } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                    mAdapter.setPermissionGranted(Permission.Type.WIFI, false);
                    mBtnContinue.setEnabled(false);
                }
            }
        }
    }
}
