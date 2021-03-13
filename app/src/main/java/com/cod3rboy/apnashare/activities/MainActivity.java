package com.cod3rboy.apnashare.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.BuildConfig;
import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.misc.Utilities;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Override navigation color for oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            getWindow().setNavigationBarColor(getColor(R.color.secondarySurfaceColor));

        setContentView(R.layout.activity_main);

        // configure toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Button sendBtn = findViewById(R.id.btn_send);
        Button receiveBtn = findViewById(R.id.btn_receive);
        TextView policyLink = findViewById(R.id.policy_link);
        TextView feedbackLink = findViewById(R.id.link_feedback);
        TextView version = findViewById(R.id.tv_version);

        sendBtn.setOnClickListener((view) -> {
            // Start activity to send files
            Intent i = new Intent(this, FilesSelectionActivity.class);
            startActivity(i);
        });

        receiveBtn.setOnClickListener((view) -> {
            // Start activity to receive files
            Intent i = new Intent(this, ReceiveActivity.class);
            startActivity(i);
        });

        policyLink.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_link))));
        });

        feedbackLink.setOnClickListener(this::feedbackLinkClicked);

        version.setText(String.format(Locale.getDefault(), "v%s", getAPKVersionName(this)));

        // Display Rating Reminder
        Utilities.showRatingReminder(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        if (!BuildConfig.DEBUG) menu.removeItem(R.id.item_crash);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_share:
                shareAppMessage();
                return true;
            case R.id.item_crash:
                throwFakeException();
                return true;
        }
        return false;
    }

    private void throwFakeException() {
        throw new NullPointerException("This is custom thrown exception for testing purposes. Just ignore it!");
    }

    private void shareAppMessage() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_share_text, getString(R.string.app_name), getPackageName()));
        intent.setType("text/plain");
        startActivity(intent);
    }

    private void feedbackLinkClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", getString(R.string.developer_email), null));

        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(intent, 0);
        if (resolveInfos.size() == 0) {
            // Toast No Email App Found
            Toast.makeText(this, getString(R.string.feedback_fail_msg), Toast.LENGTH_LONG)
                    .show();
        } else {
            int i = 0;
            for (; i < resolveInfos.size(); i++) {
                ResolveInfo resolveInfo = resolveInfos.get(i);
                String packageName = resolveInfo.activityInfo.packageName;
                String name = resolveInfo.activityInfo.name;
                if (resolveInfo.activityInfo.applicationInfo.enabled && !packageName.equals("com.android.fallback")) {
                    intent = new Intent(Intent.ACTION_SEND);
                    intent.setDataAndType(Uri.parse("mailto:"), "text/plain");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.developer_email)});
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_body, getAPKVersionName(this), Build.VERSION.RELEASE));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.setComponent(new ComponentName(packageName, name));
                    startActivity(intent);
                    break;
                }
            }
            if (i >= resolveInfos.size()) {
                // Toast No Email App Found
                Toast.makeText(this, getString(R.string.feedback_fail_msg), Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    /**
     * Method to determine version name of installed APK.
     *
     * @param context A Context Object
     * @return Version Name or Unknown if fails
     */
    @NonNull
    private static String getAPKVersionName(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
