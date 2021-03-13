package com.cod3rboy.apnashare.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.cod3rboy.apnashare.App;
import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.adapters.SelectedFilesAdapter;
import com.cod3rboy.apnashare.fragments.SelectFilesFragment;
import com.cod3rboy.apnashare.models.BasicFile;
import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.models.ImageFile;
import com.cod3rboy.apnashare.models.AudioFile;
import com.cod3rboy.apnashare.models.GeneralFile;
import com.cod3rboy.apnashare.models.VideoFile;
import com.cod3rboy.apnashare.misc.ViewPagerNestedScrollFixer;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

import com.cod3rboy.apnashare.fragments.SelectAppsFragment;
import com.cod3rboy.apnashare.fragments.SelectImagesFragment;
import com.cod3rboy.apnashare.fragments.SelectMusicFragment;
import com.cod3rboy.apnashare.fragments.SelectVideosFragment;
import com.cod3rboy.apnashare.models.AppFile;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class FilesSelectionActivity extends BaseActivity
        implements SelectedFilesAdapter.IMediator {
    public static final int VIEWPAGER_RETAIN_PAGES = 4;

    private static final int REQUEST_STORAGE_PERMISSION = 10;

    private ViewPager2 mViewPager;
    private FixedFragmentPagesAdapter mViewPagerStateAdapter;
    private TabLayoutMediator mTabLayoutMediator;

    private MaterialButton mSelectedButton;
    private MaterialButton mSendButton;
    private BottomSheetBehavior<View> mSelectionBottomSheet;

    private SelectedFilesAdapter mSelectedFilesAdapter;

    private ConstraintLayout mLayoutViewPager;
    private View mLayoutNoStoragePermission;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Override default status bar color here
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorAccent));

        // Inflate Layout
        setContentView(R.layout.activity_files_selection);

        // Set close btn click listener
        ImageButton closeBtn = findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(v -> finish());

        // Layout containing view pager and a view to display if no storage permission granted
        mLayoutViewPager = findViewById(R.id.layout_view_pager);
        mLayoutNoStoragePermission = findViewById(R.id.layout_no_storage_perm);
        MaterialButton btnGrantStorage = mLayoutNoStoragePermission.findViewById(R.id.btn_grant_storage);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            btnGrantStorage.setOnClickListener(view -> checkStoragePermission());

        mSelectedFilesAdapter = new SelectedFilesAdapter(this, this);

        // Setup Tab Layout and View Pager
        mViewPager = findViewById(R.id.pager);
        mViewPagerStateAdapter = new FixedFragmentPagesAdapter(this);
        mViewPager.setAdapter(mViewPagerStateAdapter);
        mViewPager.setOffscreenPageLimit(VIEWPAGER_RETAIN_PAGES);
        ViewPagerNestedScrollFixer.enforceSingleScrollDirection(mViewPager);

        TabLayout tabLayout = findViewById(R.id.tab_layout);

        mTabLayoutMediator = new TabLayoutMediator(tabLayout, mViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(mViewPagerStateAdapter.getPageTitle(position));
            }
        });

        mTabLayoutMediator.attach();

        // Set Send Button Listener
        mSendButton = findViewById(R.id.btn_send_files);
        mSendButton.setOnClickListener((view) -> startSendActivity());

        mSelectedButton = findViewById(R.id.btn_selected_files);
        mSendButton.setEnabled(false);
        mSelectedButton.setEnabled(false);


        // Load bottom sheet behavior
        View selectionBottomSheet = findViewById(R.id.bs_selected_items);
        mSelectionBottomSheet = BottomSheetBehavior.from(selectionBottomSheet);
        mSelectionBottomSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        ImageButton btnClearBottomSheet = selectionBottomSheet.findViewById(R.id.btn_clear_all);
        btnClearBottomSheet.setOnClickListener(v -> mSelectedFilesAdapter.clearAll());
        RecyclerView selectedItemsRecyclerView = selectionBottomSheet.findViewById(R.id.rv_selection);
        selectedItemsRecyclerView.setHasFixedSize(true);
        selectedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        selectedItemsRecyclerView.setAdapter(mSelectedFilesAdapter);
        mSelectedButton.setOnClickListener((view) -> {
            if (mSelectionBottomSheet.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                // Reset RecyclerView to top before displaying bottom sheet
                selectedItemsRecyclerView.scrollToPosition(0);
                mSelectionBottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                mSelectionBottomSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
        // Set List divider in RecyclerView
        DividerItemDecoration dividerDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        dividerDecoration.setDrawable(getDrawable(R.drawable.list_divider));
        selectedItemsRecyclerView.addItemDecoration(dividerDecoration);
    }

    public void translateThumbnailToSelectedButton(ImageView originalThumbnail) {
        PointF selectedButtonLocation = getSelectedButtonLocation();
        ImageView temporaryThumbnail = makeAndAttachAnimationThumbnail(originalThumbnail);
        // Compute the translation distance between temporary thumbnail and target location
        int[] temporaryThumbnailLocation = new int[2];
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) temporaryThumbnail.getLayoutParams();
        temporaryThumbnail.getLocationInWindow(temporaryThumbnailLocation);
        float translationX = selectedButtonLocation.x - (temporaryThumbnailLocation[0] + layoutParams.leftMargin + layoutParams.width / 2f);
        float translationY = (selectedButtonLocation.y - (temporaryThumbnailLocation[1] + layoutParams.topMargin)) - layoutParams.height / 2f;
        temporaryThumbnail.animate()
                .setDuration(800)
                .alpha(0)
                .scaleXBy(-0.5f)
                .scaleYBy(-0.5f)
                .translationXBy(translationX)
                .translationYBy(translationY)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    temporaryThumbnail.setVisibility(View.GONE);
                    detachAnimationThumbnail(temporaryThumbnail);
                }).start();
    }

    private ImageView makeAndAttachAnimationThumbnail(ImageView originalThumbnail) {
        // Create a temporary thumbnail to be used as animation thumbnail
        ImageView temporaryThumbnail = new ImageView(this);
        temporaryThumbnail.setImageDrawable(originalThumbnail.getDrawable());
        temporaryThumbnail.setScaleType(originalThumbnail.getScaleType());
        temporaryThumbnail.setId(View.generateViewId());

        // Add temporary thumbnail in the viewpager container
        ConstraintLayout.LayoutParams layoutParamsForTemporaryThumbnail = new ConstraintLayout.LayoutParams(
                originalThumbnail.getWidth(), originalThumbnail.getHeight()
        );
        // Get the screen location of original thumbnail
        int[] originalThumbnailLocation = new int[2];
        originalThumbnail.getLocationOnScreen(originalThumbnailLocation);

        // Get the viewpager container location in which temporary thumbnail will be hosted.
        int[] containerLocation = new int[2];
        mLayoutViewPager.getLocationInWindow(containerLocation);

        // Compute left and top margins of temporary thumbnail relative to viewpager container.
        int marginLeft = originalThumbnailLocation[0] - containerLocation[0];
        int marginTop = originalThumbnailLocation[1] - containerLocation[1];

        layoutParamsForTemporaryThumbnail.setMargins(marginLeft, marginTop, 0, 0);
        mLayoutViewPager.addView(temporaryThumbnail, layoutParamsForTemporaryThumbnail);

        // Align temporary thumbnail within viewpager container
        ConstraintSet containerConstraints = new ConstraintSet();
        // Use existing constraints as a base for modification
        containerConstraints.clone(mLayoutViewPager);
        containerConstraints.connect(temporaryThumbnail.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        containerConstraints.connect(temporaryThumbnail.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        containerConstraints.applyTo(mLayoutViewPager);

        return temporaryThumbnail;
    }

    private void detachAnimationThumbnail(ImageView temporaryThumbnail) {
        ConstraintSet containerConstraints = new ConstraintSet();
        containerConstraints.clone(mLayoutViewPager);
        containerConstraints.clear(temporaryThumbnail.getId(), ConstraintSet.START);
        containerConstraints.clear(temporaryThumbnail.getId(), ConstraintSet.TOP);
        containerConstraints.applyTo(mLayoutViewPager);
        mLayoutViewPager.removeView(temporaryThumbnail);
    }

    private PointF getSelectedButtonLocation() {
        int[] location = new int[2];
        mSelectedButton.getLocationInWindow(location);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mSelectedButton.getLayoutParams();
        return new PointF(location[0] + layoutParams.leftMargin + mSelectedButton.getWidth() / 2f,
                location[1] + layoutParams.topMargin);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check for Storage Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            checkStoragePermission();
        else
            onStoragePermissionGranted();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkStoragePermission() {
        if (Utilities.isStoragePermissionGranted()) {
            onStoragePermissionGranted();
        } else {
            // System can show Permission request dialog.
            onStoragePermissionDenied();
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_STORAGE_PERMISSION);
        }
    }


    private void onStoragePermissionGranted() {
        // Show View Pager Layout and Hide No Permission Layout
        mLayoutNoStoragePermission.setVisibility(View.GONE);
        mLayoutViewPager.setVisibility(View.VISIBLE);

        if (!mViewPagerStateAdapter.areFragmentsLoaded()) {
            // Load Fragments into View Pager adapter and set Initially selected fragment
            mViewPagerStateAdapter.loadFragments();
            mViewPager.setCurrentItem(FixedFragmentPagesAdapter.POSITION_APPS, false);
        }
    }

    private void onStoragePermissionDenied() {
        // Show No Permission Layout and Hide View Pager Layout
        mLayoutViewPager.setVisibility(View.GONE);
        mLayoutNoStoragePermission.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStoragePermissionGranted();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                                .setTitle(R.string.grant_storage_permission)
                                .setMessage(R.string.grant_storage_permission_dialog_description)
                                .show();
                    }
                }
            }
        }
    }

    public void startSendActivity() {
        // Send file button is clicked
        Intent i = new Intent(this, SendActivity.class);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        if (mSelectionBottomSheet.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            mSelectionBottomSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            try {
                Fragment mFilesFragment = mViewPagerStateAdapter.getFragmentAtPosition(mViewPager.getCurrentItem());
                if (mFilesFragment instanceof SelectFilesFragment) {
                    ((SelectFilesFragment) mFilesFragment).onBackPressed();
                    return;
                }
            } catch (IndexOutOfBoundsException ex) {
                // ignore
            }
            super.onBackPressed();
        }
    }

    public void fileSelected(BasicFile selectedFile) {
        mSelectedFilesAdapter.addSelectedFile(selectedFile);
        mSelectedButton.setText(getString(R.string.btn_title_selected, mSelectedFilesAdapter.getItemCount()));
        if (!mSelectedButton.isEnabled()) mSelectedButton.setEnabled(true);
        if (!mSendButton.isEnabled()) mSendButton.setEnabled(true);
    }

    public void fileDeselected(BasicFile deselectedFile) {
        mSelectedFilesAdapter.removeSelectedFile(deselectedFile);
        if (mSelectedFilesAdapter.getItemCount() > 0) {
            mSelectedButton.setText(getString(R.string.btn_title_selected, mSelectedFilesAdapter.getItemCount()));
        } else {
            mSelectedButton.setText(R.string.btn_title_not_selected);
            mSelectedButton.setEnabled(false);
            mSendButton.setEnabled(false);
        }
    }

    @Override
    public void requestFileDeselection(BasicFile file) {
        // Identify file kind and forward deselection request to corresponding fragment
        if (file instanceof AppFile) {
            // deselect app in SelectAppsFragment
            Fragment fragment = mViewPagerStateAdapter.getFragmentAtPosition(FixedFragmentPagesAdapter.POSITION_APPS);
            ((SelectAppsFragment) fragment).deselectApp((AppFile) file);
        } else if (file instanceof AudioFile) {
            // deselect music in SelectMusicFragment
            Fragment fragment = mViewPagerStateAdapter.getFragmentAtPosition(FixedFragmentPagesAdapter.POSITION_MUSIC);
            ((SelectMusicFragment) fragment).deselectMusic((AudioFile) file);
        } else if (file instanceof ImageFile) {
            // deselect image in SelectImagesFragment
            Fragment fragment = mViewPagerStateAdapter.getFragmentAtPosition(FixedFragmentPagesAdapter.POSITION_IMAGES);
            ((SelectImagesFragment) fragment).deselectImage((ImageFile) file);
        } else if (file instanceof VideoFile) {
            // deselect video in SelectVideosFragment
            Fragment fragment = mViewPagerStateAdapter.getFragmentAtPosition(FixedFragmentPagesAdapter.POSITION_VIDEOS);
            ((SelectVideosFragment) fragment).deselectVideo((VideoFile) file);
        } else if (file instanceof GeneralFile) {
            // deselect file SelectFilesFragment
            Fragment fragment = mViewPagerStateAdapter.getFragmentAtPosition(FixedFragmentPagesAdapter.POSITION_FILES);
            ((SelectFilesFragment) fragment).deselectFile((GeneralFile) file);
        }
    }

    @Override
    public void deselectionSuccessful() {
        if (mSelectedFilesAdapter.getItemCount() > 0) {
            mSelectedButton.setText(getString(R.string.btn_title_selected, mSelectedFilesAdapter.getItemCount()));
        } else {
            mSelectedButton.setText(R.string.btn_title_not_selected);
            mSelectedButton.setEnabled(false);
            mSendButton.setEnabled(false);
            mSelectionBottomSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }


    class FixedFragmentPagesAdapter extends FragmentStateAdapter {
        public static final int POSITION_FILES = 0;
        public static final int POSITION_MUSIC = 1;
        public static final int POSITION_APPS = 2;
        public static final int POSITION_IMAGES = 3;
        public static final int POSITION_VIDEOS = 4;

        // Create Tab names and their corresponding index position constants.
        private String[] TAB_NAMES = {
                App.getInstance().getString(R.string.selection_tab_files),
                App.getInstance().getString(R.string.selection_tab_music),
                App.getInstance().getString(R.string.selection_tab_apps),
                App.getInstance().getString(R.string.selection_tab_images),
                App.getInstance().getString(R.string.selection_tab_videos)
        };
        private ArrayList<Fragment> mFragments;

        public FixedFragmentPagesAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
            mFragments = new ArrayList<>();
        }

        public void loadFragments() {
            mFragments.clear();
            mFragments.add(new SelectFilesFragment());
            mFragments.add(new SelectMusicFragment());
            mFragments.add(new SelectAppsFragment());
            mFragments.add(new SelectImagesFragment());
            mFragments.add(new SelectVideosFragment());
            notifyDataSetChanged();
        }

        public boolean areFragmentsLoaded() {
            return mFragments.size() > 0;
        }

        @NonNull
        public Fragment getFragmentAtPosition(int position) {
            return mFragments.get(position);
        }

        public CharSequence getPageTitle(int position) {
            return TAB_NAMES[position];
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getItemCount() {
            return mFragments.size();
        }
    }
}
