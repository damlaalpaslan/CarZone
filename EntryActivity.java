package com.ece.aractakip;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ece.aractakip.data.AppPreferences;
import com.ece.aractakip.util.ThemeHelper;


public class EntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        if (AppPreferences.isOnboardingCompleted(this)) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, OnboardingActivity.class));
        }
        finish();
    }
}
