package com.app.medikit.activity;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.app.medikit.R;
import com.app.medikit.controller.MedkitController;
import com.app.medikit.receiver.NetworkStatusChangeReceiver;
import com.app.medikit.util.Constants;

public class SplashActivity extends AppCompatActivity {

    private boolean                     isSplashDone = false;
    private AppCompatTextView           networkCheckingTv;
    private NetworkStatusChangeReceiver networkStatusChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        MedkitController.setActivity(SplashActivity.this);
        init();
    }


    /**
     * {@link #onResume()} called when the activity is in the resumed state
     **/
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(networkStatusChangeReceiver, new IntentFilter(CONNECTIVITY_ACTION));
    }


    /**
     * {@link #onPause()} called when an activity is about to lose focus
     **/
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkStatusChangeReceiver);
    }


    private void init(){
        networkCheckingTv = findViewById(R.id.networkCheckingTv);
        networkStatusChangeReceiver = new NetworkStatusChangeReceiver();
        splashTimer();
    }

    /**
     *  Show splash screen for 2 seconds
     **/
    private void splashTimer(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(Constants.IS_NETWORK_CONNECTED) {
                    launchNewActivity();
                }
                else {
                    networkCheckingTv.setVisibility(View.VISIBLE);
                }

                isSplashDone = true;
            }
        }, Constants.SPLASH_TIMING);
    }

    /**
     *  Launch Main Activity
     **/
    private void launchNewActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    /**
     *  Monitor Internet Connection
     **/
    public void updateInternetConnectionStatus(boolean isConnected) {
        if (isConnected) {
            if(isSplashDone){
                launchNewActivity();
            }
        }
    }
}
