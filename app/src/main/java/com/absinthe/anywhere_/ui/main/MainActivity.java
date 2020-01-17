package com.absinthe.anywhere_.ui.main;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.absinthe.anywhere_.AnywhereApplication;
import com.absinthe.anywhere_.BaseActivity;
import com.absinthe.anywhere_.R;
import com.absinthe.anywhere_.adapter.page.PageListAdapter;
import com.absinthe.anywhere_.adapter.page.PageTitleNode;
import com.absinthe.anywhere_.databinding.ActivityMainBinding;
import com.absinthe.anywhere_.databinding.ActivityMainMd2Binding;
import com.absinthe.anywhere_.model.AnywhereEntity;
import com.absinthe.anywhere_.model.AnywhereType;
import com.absinthe.anywhere_.model.Const;
import com.absinthe.anywhere_.model.GlobalValues;
import com.absinthe.anywhere_.model.OnceTag;
import com.absinthe.anywhere_.model.PageEntity;
import com.absinthe.anywhere_.model.Settings;
import com.absinthe.anywhere_.ui.list.AppListActivity;
import com.absinthe.anywhere_.ui.qrcode.QRCodeCollectionActivity;
import com.absinthe.anywhere_.utils.CommandUtils;
import com.absinthe.anywhere_.utils.FirebaseUtil;
import com.absinthe.anywhere_.utils.SPUtils;
import com.absinthe.anywhere_.utils.TextUtils;
import com.absinthe.anywhere_.utils.UiUtils;
import com.absinthe.anywhere_.utils.manager.Logger;
import com.absinthe.anywhere_.view.AnywhereEditor;
import com.absinthe.anywhere_.view.Editor;
import com.absinthe.anywhere_.viewmodel.AnywhereViewModel;
import com.chad.library.adapter.base.entity.node.BaseNode;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jonathanfinerty.once.Once;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class MainActivity extends BaseActivity {
    @SuppressLint("StaticFieldLeak")
    private static MainActivity sInstance;
    private static boolean isPageInit = false;
    private MainFragment mCurrFragment;

    private AnywhereViewModel mViewModel;
    private FirebaseAnalytics mFirebaseAnalytics;

    /* View */
    public SpeedDialView mFab;

    private ImageView mIvBackground;
    private DrawerLayout mDrawer;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mToggle;

    public static MainActivity getInstance() {
        return sInstance;
    }

    public void setCurrFragment(MainFragment mCurrFragment) {
        this.mCurrFragment = mCurrFragment;
    }

    public MainFragment getCurrFragment() {
        return mCurrFragment;
    }

    public AnywhereViewModel getViewModel() {
        return mViewModel;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sInstance = this;
        mViewModel = ViewModelProviders.of(this).get(AnywhereViewModel.class);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        setLayout();
        initView();

        Observer<List<PageEntity>> observer = new Observer<List<PageEntity>>() {
            @Override
            public void onChanged(List<PageEntity> pageEntities) {
                AnywhereApplication.sRepository.getAllPageEntities().removeObserver(this);

                if (pageEntities.size() == 0 && !isPageInit) {
                    String timeStamp = System.currentTimeMillis() + "";
                    AnywhereApplication.sRepository.insertPage(
                            new PageEntity(timeStamp, GlobalValues.sCategory, 1, timeStamp));
                    isPageInit = true;
                }
            }
        };

        AnywhereApplication.sRepository.getAllPageEntities().observe(this, observer);

        if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FAB_GUIDE) &&
                SPUtils.getBoolean(this, Const.PREF_FIRST_LAUNCH, true)) {
            mFab.setVisibility(View.GONE);
            WelcomeFragment welcomeFragment = WelcomeFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.anim_fade_in, R.anim.anim_fade_out)
                    .replace(R.id.container, welcomeFragment)
                    .commitNow();
        } else {
            mCurrFragment = MainFragment.newInstance(GlobalValues.sCategory);
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.anim_fade_in, R.anim.anim_fade_out)
                    .replace(R.id.container, mCurrFragment)
                    .commitNow();
            initFab();
            initObserver();
            getAnywhereIntent(getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Settings.setTheme(GlobalValues.sDarkMode);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getAnywhereIntent(intent);
    }

    @Override
    protected void onDestroy() {
        sInstance = null;
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        UiUtils.loadBackgroundPic(this, mIvBackground);
        if (mToggle != null) {
            mToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Logger.d("onPrepareOptionsMenu: actionBarType =", GlobalValues.sActionBarType);

        if ((GlobalValues.sActionBarType.equals(Const.ACTION_BAR_TYPE_LIGHT) && !GlobalValues.sIsMd2Toolbar)
                || (UiUtils.isDarkMode(this) && GlobalValues.sBackgroundUri.isEmpty())
                || (UiUtils.isDarkMode(this) && GlobalValues.sIsMd2Toolbar)) {
            UiUtils.tintToolbarIcon(this, menu, mToggle, Const.ACTION_BAR_TYPE_LIGHT);
        } else {
            UiUtils.tintToolbarIcon(this, menu, mToggle, Const.ACTION_BAR_TYPE_DARK);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void setLayout() {
        if (GlobalValues.sIsMd2Toolbar) {
            ActivityMainMd2Binding binding2 = DataBindingUtil.setContentView(this, R.layout.activity_main_md2);
            if (!GlobalValues.sBackgroundUri.isEmpty()) {
                mIvBackground = (ImageView) Objects.requireNonNull(binding2.stubBg.getViewStub()).inflate();
            }
            mToolbar = binding2.toolbar;
            mDrawer = binding2.drawer;
            mFab = binding2.fab;
        } else {
            ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
            if (!GlobalValues.sBackgroundUri.isEmpty()) {
                mIvBackground = (ImageView) Objects.requireNonNull(binding.stubBg.getViewStub()).inflate();
            }
            mToolbar = binding.toolbar;
            mDrawer = binding.drawer;
            mFab = binding.fab;
        }
    }

    private void initView() {
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();

        if (!GlobalValues.sBackgroundUri.isEmpty()) {
            UiUtils.loadBackgroundPic(this, mIvBackground);
            UiUtils.setActionBarTransparent(this);
            UiUtils.setAdaptiveActionBarTitleColor(this, getSupportActionBar(), UiUtils.getActionBarTitle());
        }

        if (actionBar != null) {
            if (GlobalValues.sIsPages) {
                mToggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar, R.string.drawer_open, R.string.drawer_close);
                actionBar.setDisplayHomeAsUpEnabled(true);
                mDrawer.addDrawerListener(mToggle);
                mToggle.syncState();

                AnywhereApplication.sRepository
                        .getAllAnywhereEntities()
                        .observe(this, anywhereEntities -> initDrawer(mDrawer));
            } else {
                actionBar.setHomeButtonEnabled(false);
                mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        }
    }

    private void initDrawer(DrawerLayout drawer) {
        RecyclerView recyclerView = drawer.findViewById(R.id.rv_pages);

        PageListAdapter adapter = new PageListAdapter();
        adapter.setOnItemChildClickListener((adapter1, view, position) -> {
            if (view.getId() == R.id.iv_entry) {
                MainActivity.getInstance().mDrawer.closeDrawer(GravityCompat.START);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    PageTitleNode node = (PageTitleNode) adapter1.getItem(position);
                    if (node != null) {
                        mCurrFragment = MainFragment.newInstance(node.getTitle());
                        MainActivity.getInstance().getSupportFragmentManager()
                                .beginTransaction()
                                .setCustomAnimations(R.anim.anim_fade_in, R.anim.anim_fade_out)
                                .replace(R.id.container, mCurrFragment)
                                .commitNow();
                        GlobalValues.setsCategory(node.getTitle(), position);
                    }
                }, 300);
            }
        });
        AnywhereApplication.sRepository.getAllPageEntities().observe(this, pageEntities -> {
            if (pageEntities != null) {
                setupDrawerData(adapter, pageEntities);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(adapter);
        ((SimpleItemAnimator) Objects.requireNonNull(
                recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);

        drawer.findViewById(R.id.ib_add).setOnClickListener(v -> {
            List<PageEntity> list = AnywhereApplication.sRepository.getAllPageEntities().getValue();
            if (list != null) {
                String timeStamp = System.currentTimeMillis() + "";
                if (list.size() != 0) {
                    int size = list.size();
                    PageEntity pe = new PageEntity(timeStamp, "Page " + (size + 1), size + 1, timeStamp);
                    AnywhereApplication.sRepository.insertPage(pe);
                } else {
                    PageEntity pe = new PageEntity(timeStamp, AnywhereType.DEFAULT_CATEGORY, 1, timeStamp);
                    AnywhereApplication.sRepository.insertPage(pe);
                }
            }
        });
    }

    private void setupDrawerData(PageListAdapter adapter, List<PageEntity> pageEntities) {
        List<BaseNode> list = new ArrayList<>();
        for (PageEntity pe : pageEntities) {
            list.add(mViewModel.getEntity(pe.getTitle()));
        }
        adapter.setNewData(list);
    }

    public void initObserver() {
        mViewModel.getBackground().observe(this, s -> {
            if (!s.isEmpty()) {
                UiUtils.loadBackgroundPic(sInstance, mIvBackground);
                UiUtils.setActionBarTransparent(MainActivity.getInstance());
                UiUtils.setAdaptiveActionBarTitleColor(sInstance, getSupportActionBar(), UiUtils.getActionBarTitle());
            }
            GlobalValues.setsBackgroundUri(s);
        });

        mViewModel.getBackground().setValue(GlobalValues.sBackgroundUri);
        mViewModel.getWorkingMode().observe(this, s -> {
            GlobalValues.setsWorkingMode(s);
            UiUtils.setActionBarTitle(this, getSupportActionBar());
        });
        mViewModel.getWorkingMode().setValue(GlobalValues.sWorkingMode);
        mViewModel.getCommand().observe(this, CommandUtils::execCmd);
    }

    public void initFab() {
        mFab.addActionItem(new SpeedDialActionItem.Builder(R.id.fab_url_scheme, R.drawable.ic_url_scheme)
                .setFabBackgroundColor(getResources().getColor(R.color.white))
                .setLabel(getString(R.string.btn_url_scheme))
                .setLabelClickable(false)
                .create());
        mFab.addActionItem(new SpeedDialActionItem.Builder(R.id.fab_activity_list, R.drawable.ic_activity_list)
                .setFabBackgroundColor(getResources().getColor(R.color.white))
                .setLabel(getString(R.string.btn_activity_list))
                .setLabelClickable(false)
                .create());
        mFab.addActionItem(new SpeedDialActionItem.Builder(R.id.fab_qr_code_collection, R.drawable.ic_qr_code)
                .setFabBackgroundColor(getResources().getColor(R.color.white))
                .setLabel(getString(R.string.btn_qr_code_collection))
                .setLabelClickable(false)
                .create());
        mFab.addActionItem(new SpeedDialActionItem.Builder(R.id.fab_collector, R.drawable.ic_logo)
                .setFabBackgroundColor(getResources().getColor(R.color.white))
                .setLabel(GlobalValues.getCollectorMode())
                .setLabelClickable(false)
                .create());
        mFab.setOnActionSelectedListener(actionItem -> {
            switch (actionItem.getId()) {
                case R.id.fab_url_scheme:
                    MainActivity.getInstance().getViewModel().setUpUrlScheme("");
                    FirebaseUtil.logEvent(mFirebaseAnalytics, "fab_url_scheme", "click_fab_url_scheme");
                    break;
                case R.id.fab_activity_list:
                    startActivity(new Intent(this, AppListActivity.class));
                    FirebaseUtil.logEvent(mFirebaseAnalytics, "fab_activity_list", "click_fab_activity_list");
                    break;
                case R.id.fab_collector:
                    MainActivity.getInstance().getViewModel().checkWorkingPermission(MainActivity.getInstance());
                    FirebaseUtil.logEvent(mFirebaseAnalytics, "fab_collector", "click_fab_collector");
                    break;
                case R.id.fab_qr_code_collection:
                    startActivity(new Intent(this, QRCodeCollectionActivity.class));
                    FirebaseUtil.logEvent(mFirebaseAnalytics, "fab_qr_code_collection", "click_fab_qr_code_collection");
                    break;
                default:
                    return false;
            }
            mFab.close();
            return true;
        });

        if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FAB_GUIDE)) {
            new MaterialTapTargetPrompt.Builder(this)
                    .setTarget(R.id.fab)
                    .setPrimaryText(R.string.first_launch_guide_title)
                    .setBackgroundColour(getResources().getColor(R.color.colorAccent))
                    .show();
            Once.markDone(OnceTag.FAB_GUIDE);
        }
    }

    private void getAnywhereIntent(Intent intent) {
        String action = intent.getAction();

        Logger.d("action = ", action);

        if (action == null || action.equals(Intent.ACTION_VIEW)) {
            Uri uri = intent.getData();

            if (uri == null) {
                return;
            } else {
                Logger.d("Received Url =", uri.toString());
            }

            String param1 = uri.getQueryParameter(Const.INTENT_EXTRA_PARAM_1);
            String param2 = uri.getQueryParameter(Const.INTENT_EXTRA_PARAM_2);
            String param3 = uri.getQueryParameter(Const.INTENT_EXTRA_PARAM_3);

            if (param1 != null && param2 != null && param3 != null) {
                if (param2.isEmpty() && param3.isEmpty()) {
                    mViewModel.setUpUrlScheme(param1);
                } else {
                    String appName;
                    appName = TextUtils.getAppName(this, param1);

                    int exported = 0;
                    if (UiUtils.isActivityExported(this, new ComponentName(param1,
                            param2.charAt(0) == '.' ? param1 + param2 : param2))) {
                        exported = 100;
                    }

                    AnywhereEntity ae = AnywhereEntity.Builder();
                    ae.setAppName(appName);
                    ae.setParam1(param1);
                    ae.setParam2(param2);
                    ae.setParam3(param3);
                    ae.setType(AnywhereType.ACTIVITY + exported);

                    Editor editor = new AnywhereEditor(this)
                            .item(ae)
                            .isEditorMode(false)
                            .isShortcut(false)
                            .build();
                    editor.show();
                }
            }
        } else if (action.equals(Intent.ACTION_SEND)) {
            String sharing = intent.getStringExtra(Intent.EXTRA_TEXT);
            mViewModel.setUpUrlScheme(TextUtils.parseUrlFromSharingText(sharing));
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mToggle != null && mToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Const.REQUEST_CODE_ACTION_MANAGE_OVERLAY_PERMISSION) {
            Logger.d("REQUEST_CODE_ACTION_MANAGE_OVERLAY_PERMISSION");
            if (resultCode == RESULT_OK) {
                mViewModel.checkWorkingPermission(this);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerVisible(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
