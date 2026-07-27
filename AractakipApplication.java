package com.ece.aractakip;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.ece.aractakip.util.LocaleHelper;

public class AractakipApplication extends Application {

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("tr-TR"));
    }
}
