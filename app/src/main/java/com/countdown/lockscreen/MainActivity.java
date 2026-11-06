package com.countdown.lockscreen;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private WebView mWebView;
    private Button mWallpaperButton;
    private final Handler mHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            if (mWallpaperButton != null) {
                mWallpaperButton.animate().alpha(0.15f).setDuration(500).start();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen awake for countdown lock screen experience
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Allow app to show on top of Lock Screen without unlocking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }

        // Enable full-screen immersive mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        makeFullScreen();

        // Root container
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#020617"));

        // Create WebView dynamically
        mWebView = new WebView(this);
        mWebView.setBackgroundColor(Color.parseColor("#020617"));
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Configure WebSettings
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        // Client handlers
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        request.grant(request.getResources());
                    }
                });
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return super.onConsoleMessage(consoleMessage);
            }
        });

        rootLayout.addView(mWebView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Floating "Set as Lock Screen Wallpaper" button
        mWallpaperButton = new Button(this);
        mWallpaperButton.setText("✨ Set as Lock Screen 3D Wallpaper");
        mWallpaperButton.setTextColor(Color.WHITE);
        mWallpaperButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mWallpaperButton.setPadding(dp(18), dp(10), dp(18), dp(10));

        // Glassmorphic cyan glowing pill background
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#CC020617"));
        btnBg.setCornerRadius(dp(25));
        btnBg.setStroke(dp(1), Color.parseColor("#38BDF8"));
        mWallpaperButton.setBackground(btnBg);

        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        btnParams.bottomMargin = dp(24);
        mWallpaperButton.setLayoutParams(btnParams);

        mWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchWallpaperPicker();
            }
        });

        rootLayout.addView(mWallpaperButton);
        setContentView(rootLayout);

        // Load entry point
        mWebView.loadUrl("file:///android_asset/index.html");

        // Schedule button to gently fade out after 6 seconds so it doesn't obstruct the wallpaper
        scheduleButtonFade();

        rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWallpaperButton != null) {
                    mWallpaperButton.animate().alpha(1.0f).setDuration(250).start();
                    scheduleButtonFade();
                }
            }
        });
    }

    private void launchWallpaperPicker() {
        try {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this, CountdownWallpaperService.class)
            );
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                startActivity(fallback);
            } catch (Exception ignored) {}
        }
    }

    private void scheduleButtonFade() {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, 6000);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            getResources().getDisplayMetrics()
        );
    }

    private void makeFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            makeFullScreen();
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWebView != null) {
            mWebView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWebView != null) {
            mWebView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        mHideHandler.removeCallbacks(mHideRunnable);
        if (mWebView != null) {
            mWebView.destroy();
        }
        super.onDestroy();
    }
}
