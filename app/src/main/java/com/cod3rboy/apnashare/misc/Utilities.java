package com.cod3rboy.apnashare.misc;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.cod3rboy.apnashare.App;
import com.mikhaellopez.ratebottomsheet.RateBottomSheet;
import com.mikhaellopez.ratebottomsheet.RateBottomSheetManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {
    /*
     * Used to convert a Drawable Object to bitmap.
     * Source : https://stackoverflow.com/questions/46531073/how-to-create-bitmap-form-drawable-object
     * */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) throw new NullPointerException("Source drawable cannot be null");

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        Bitmap bitmap = null;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap of size 1x1
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bitmapData = stream.toByteArray();
        try {
            stream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return bitmapData;
    }

    public static Bitmap byteArrayToBitmap(byte[] bitmapData) {
        return BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
    }


    public static String randomAlphaNumericString(int n) {
        String alphaNumericString = "0123456789abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int) (alphaNumericString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(alphaNumericString.charAt(index));
        }

        return sb.toString();
    }

    public static String stripQuotes(String ssidStr) {
        if (ssidStr.startsWith("\"") && ssidStr.endsWith("\"")) {
            ssidStr = ssidStr.replaceAll("\"", "");
        }
        return ssidStr;
    }

    private static final int LENGTH_LIST_SHORT_NAME = 38;

    public static String makeShortNameForList(String str) {
        int minLen = Math.min(str.length(), LENGTH_LIST_SHORT_NAME - 3);
        return str.substring(0, minLen) + ((minLen == str.length()) ? "" : "...");
    }

    private static final int LENGTH_GRID_SHORT_NAME = 13;

    public static String makeShortNameForGrid(String str) {
        int minLen = Math.min(str.length(), LENGTH_GRID_SHORT_NAME - 3);
        return str.substring(0, minLen) + ((minLen == str.length()) ? "" : "...");
    }

    private static final long KILOBYTE = 1024;
    private static final long MEGABYTE = KILOBYTE * 1024;
    private static final long GIGABYTE = MEGABYTE * 1024;

    public static String convertBytesToString(long bytes) {
        StringBuilder sb = new StringBuilder();
        if (bytes >= GIGABYTE) {
            double gbs = bytes * 1.0 / GIGABYTE;
            sb.append(String.format(Locale.getDefault(), "%.2f GB", gbs));
        } else if (bytes >= MEGABYTE) {
            double mbs = bytes * 1.0 / MEGABYTE;
            sb.append(String.format(Locale.getDefault(), "%.2f MB", mbs));
        } else if (bytes >= 200) { // convert to kb starting on 200 bytes
            double kbs = bytes * 1.0 / KILOBYTE;
            sb.append(String.format(Locale.getDefault(), "%.2f KB", kbs));
        } else {
            sb.append(String.format(Locale.getDefault(), "%d bytes", bytes));
        }
        return sb.toString();
    }

    public static String convertFolderItemsToString(long items) {
        StringBuilder sb = new StringBuilder();
        if (items > 0) {
            sb.append(items);
            sb.append((items == 1) ? " item" : " items");
        } else {
            sb.append("Empty");
        }
        return sb.toString();
    }

    public static String convertFilesCountToString(long filesCount) {
        return filesCount +
                ((filesCount > 1) ? " files" : " file");
    }

    private static final int SECOND = 1000;
    private static final int MINUTE = 60 * 1000;
    private static final int HOUR = 60 * MINUTE;

    public static String formatVideoDuration(long ms) {
        long hours = ms / HOUR;
        ms %= HOUR;
        long mins = ms / MINUTE;
        ms %= MINUTE;
        long secs = ms / SECOND;
        if (hours == 0)
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs);
    }

    public static void openAppInPlayStore(final Context cntx, final String appId) {
        Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("market://details?id=%s", appId)));
        boolean marketFound = false;

        // Find all the applications able to handle our rateIntent
        final List<ResolveInfo> otherApps = cntx.getPackageManager().queryIntentActivities(rateIntent, 0);
        for (ResolveInfo otherApp : otherApps) {
            // Look for Google Play Application
            if (otherApp.activityInfo.packageName.contentEquals("com.android.vending")) {
                ActivityInfo otherAppActivity = otherApp.activityInfo;
                ComponentName componentName = new ComponentName(otherAppActivity.applicationInfo.packageName, otherAppActivity.name);
                // Make sure it does NOT open in the stack of my own app activity
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // Task reparenting if needed
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                // If Google Play was already open in a search result this make sure it still
                // go to this app page
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                // This make sure only the Google Play app is allowed to intercept the intent
                rateIntent.setComponent(componentName);
                cntx.startActivity(rateIntent);
                marketFound = true;
                break;
            }
        }

        // If Google Play app not present in device, open web browser
        if (!marketFound) {
            cntx.startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(String.format("https://play.google.com/store/apps/details?id=%s", appId))));
        }
    }


    public static boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(
                App.getInstance().getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(
                App.getInstance().getApplicationContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(
                App.getInstance().getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isLocationEnabled() {
        if (isLocationPermissionGranted()) {
            LocationManager lm = (LocationManager) App.getInstance().getSystemService(Context.LOCATION_SERVICE);
            return lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER); // throws exception if permission not granted
        }
        return false;
    }

    public static boolean isWiFiEnabled() {
        WifiManager wm = (WifiManager) App.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wm != null && wm.isWifiEnabled();
    }

    public static boolean canWriteSystemSettings() {
        // Write system settings permission is needed only for android version starting from
        // marshmallow and start from oreo we use local only hotspot which needs no such permission.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return true;
        return Settings.System.canWrite(App.getInstance().getApplicationContext());
    }

    /*public static boolean isMobileDataEnabled() {
        boolean isDataEnabled = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TelephonyManager tm = (TelephonyManager) App.getInstance().getSystemService(Context.TELEPHONY_SERVICE);
            isDataEnabled = tm != null && tm.isDataEnabled();
        } else {
            ConnectionManager cm = (ConnectionManager) App.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
            try {
                Method method = cm.getClass().getDeclaredMethod("getMobileDataEnabled");
                method.setAccessible(true); // Make the method callable
                // get the setting for "mobile data"
                isDataEnabled = (Boolean) method.invoke(cm);
            } catch (Exception e) {
                // Some problem accessible private API
                isDataEnabled = Settings.Global.getInt(App.getInstance().getContentResolver(), "mobile_data", 0) == 1;
            }
        }
        return isDataEnabled;
    }*/

    private static final String QR_BODY_DELIMITER = "#";
    private static final Pattern QR_CODE_PATTERN = Pattern.compile("^AndroidShare(_|\\s)\\d{4}#[a-f0-9]{12}$", Pattern.CASE_INSENSITIVE);

    public static boolean isValidQrBody(String msgBody) {
        return QR_CODE_PATTERN.matcher(msgBody).matches();
    }

    public static String generateQrBody(String receiverName, String receiverPassword) {
        return receiverName + QR_BODY_DELIMITER + receiverPassword;
    }

    @NonNull
    public static String[] splitQrBody(String msgBody) {
        if (!isValidQrBody(msgBody))
            throw new IllegalArgumentException("Cannot split invalid QR msg body : " + msgBody);
        return msgBody.split(QR_BODY_DELIMITER);
    }

    public static String randomSSID() {
        // SSID Pattern - "AndroidShare dddd"
        Random random = new Random(System.currentTimeMillis());
        StringBuilder ssidBuilder = new StringBuilder();
        ssidBuilder.append("AndroidShare ");
        ssidBuilder.append((random.nextInt() & 0x7FFFFFFF) % 10);
        ssidBuilder.append((random.nextInt() & 0x7FFFFFFF) % 10);
        ssidBuilder.append((random.nextInt() & 0x7FFFFFFF) % 10);
        ssidBuilder.append((random.nextInt() & 0x7FFFFFFF) % 10);
        return ssidBuilder.toString();
    }

    private static final int RECEIVER_PASSWORD_LENGTH = 12;

    public static String randomPassword() {
        String alphabet = "0123456789abcdef";
        StringBuilder pwdBuilder = new StringBuilder();
        for (int i = 0; i < RECEIVER_PASSWORD_LENGTH; i++) {
            int index = (int) (alphabet.length() * Math.random());
            pwdBuilder.append(alphabet.charAt(index));
        }
        return pwdBuilder.toString();
    }

    private static final Pattern FILE_EXT_PATTERN = Pattern.compile("(?<=\\.)[a-z0-9]{1,4}$", Pattern.CASE_INSENSITIVE);

    public static String getFileExtName(String fileName) {
        Matcher matcher = FILE_EXT_PATTERN.matcher(fileName);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    public static Uri getFileUri(File file) {
        return FileProvider.getUriForFile(App.getInstance(), App.getInstance().getPackageName() + ".provider", file);
    }

    /**
     * Helper method to show user a rating bottom sheet every period of time until user explicitly
     * disables it.
     *
     * @param activity Activity hosting dialog box
     */
    public static void showRatingReminder(AppCompatActivity activity) {
        new RateBottomSheetManager(activity)
                .setInstallDays(1) // 3 by default
                .setLaunchTimes(3) // 5 by default
                .setRemindInterval(1) // 2 by default
                .setShowAskBottomSheet(true) // True by default
                .setShowLaterButton(true) // True by default
                .setShowCloseButtonIcon(false) // True by default
                .setDebugForceOpenEnable(false) // Set to false in release
                .monitor();

        // Show bottom sheet if meets conditions
        // With AppCompatActivity or Fragment
        RateBottomSheet.Companion.showRateBottomSheetIfMeetsConditions(activity, null);
    }
}
