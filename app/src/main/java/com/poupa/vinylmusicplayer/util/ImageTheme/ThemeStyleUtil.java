package com.poupa.vinylmusicplayer.util.ImageTheme;


import androidx.annotation.NonNull;

import com.poupa.vinylmusicplayer.util.PreferenceUtil;

public class ThemeStyleUtil {
    private static ThemeStyle sInstance = new FlatTheme();


    public static synchronized ThemeStyle getInstance() {
        return sInstance;
    }
}