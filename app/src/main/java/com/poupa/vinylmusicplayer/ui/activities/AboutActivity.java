package com.poupa.vinylmusicplayer.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.resources.TextAppearance;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.poupa.vinylmusicplayer.R;
import com.poupa.vinylmusicplayer.databinding.ActivityAboutBinding;
import com.poupa.vinylmusicplayer.dialogs.ChangelogDialog;
import com.poupa.vinylmusicplayer.dialogs.MarkdownViewDialog;
import com.poupa.vinylmusicplayer.ui.activities.base.AbsBaseActivity;
import com.poupa.vinylmusicplayer.ui.activities.bugreport.BugReportActivity;
import com.poupa.vinylmusicplayer.ui.activities.intro.AppIntroActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Function;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class AboutActivity extends AbsBaseActivity {

    private static final String GITHUB = "https://github.com/VinylMusicPlayer/VinylMusicPlayer";

    private static final String WEBSITE = "https://adrien.poupa.fr/";

    private static final String RATE_ON_GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=com.poupa.vinylmusicplayer";

    private ActivityAboutBinding layoutBinding;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            layoutBinding = ActivityAboutBinding.inflate(LayoutInflater.from(this));
        } catch (InflateException e) {
            new MaterialDialog.Builder(this)
                    .title(android.R.string.dialog_alert_title)
                    .content(R.string.missing_webview_component)
                    .positiveText(android.R.string.ok)
                    .build()
                    .show();
        }

        setContentView(layoutBinding.getRoot());

       

    }

}
