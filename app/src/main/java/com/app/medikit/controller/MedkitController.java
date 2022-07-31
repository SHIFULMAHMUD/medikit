package com.app.medikit.controller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;


@SuppressLint("StaticFieldLeak")
public class MedkitController extends MultiDexApplication {

    public static Context   context = MedkitController.getContext();
    private static Activity activity;

    @Override
    public void onCreate() {
        super.onCreate();
        MedkitController.context = getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static Context getContext() {
        return context;
    }

    public static void setActivity(Activity currentActivity) {
        activity = currentActivity;
    }

    public static Activity getActivity() {
        return activity;
    }
}
