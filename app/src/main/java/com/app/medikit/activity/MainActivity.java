package com.app.medikit.activity;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.ybq.android.spinkit.style.Circle;
import com.app.medikit.R;
import com.app.medikit.controller.MedkitController;
import com.app.medikit.model.Feed;
import com.app.medikit.model.HealthData;
import com.app.medikit.model.HealthDataModel;
import com.app.medikit.receiver.NetworkStatusChangeReceiver;
import com.app.medikit.receiver.NotifyUserReceiver;
import com.app.medikit.remote.PermissionManager;
import com.app.medikit.util.AppExtensions;
import com.app.medikit.util.Constants;
import com.app.medikit.util.CustomSnackBar;
import com.app.medikit.util.SharedPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private SwipeRefreshLayout          refreshLayout;
    private HealthDataModel             dataModel;
    private AppCompatTextView           pulseValue;
    private AppCompatTextView           lastPulseValue;
    private AppCompatTextView           pulseStatus;
    private AppCompatTextView           oxygenValue;
    private AppCompatTextView           lastOxygenValue;
    private AppCompatTextView           oxygenStatus;
    private AppCompatTextView           temperatureValue;
    private AppCompatTextView           lastTemperatureValue;
    private AppCompatTextView           temperatureStatus;
    private ProgressBar                 loader;
    private AlertDialog                 notificationAlertDialog;
    private AlertDialog                 autoStartAlertDialog;
    private NetworkStatusChangeReceiver networkStatusChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MedkitController.setActivity(MainActivity.this);

        new PermissionManager().showPermissionDialogs();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setLogo(R.drawable.app_logo);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(getString(R.string.app_name));
        }

        init();

        refreshLayout.setOnRefreshListener(this);

        new NotifyUserReceiver().startNotifyService(this);

        getFeedData();
    }

    /**
     * {@link #onStart()} called whenever the application becomes visible
     **/
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mDataReceiver, new IntentFilter(Constants.DATA_LISTENER_KEY));

        /**
         *  Allow Notification ->
         *  Check if my app is allowed to run in background
         **/
        if(!new SharedPreference(MainActivity.this).isAutoStart(SharedPreference.ALLOW_SP_KEY)){
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    showAutoStartDialog(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + this.getPackageName())));
                }
                catch (ActivityNotFoundException e) {
                    try {
                        showAutoStartDialog(new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        }

        /**
         *  Allow Auto Startup ->
         *  Check if my app is allowed to show notification
         **/
        new SharedPreference(MainActivity.this).allowNotification(SharedPreference.ALLOW_SP_KEY, NotificationManagerCompat.from(this).areNotificationsEnabled());

        if(!NotificationManagerCompat.from(this).areNotificationsEnabled()){
            showNotificationDialog();
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mDataReceiver);
    }

    private void init(){
        refreshLayout = findViewById(R.id.swipeRefreshLayout);
        refreshLayout.setColorSchemeResources(R.color.icon_Color_Accent);

        dataModel = new HealthDataModel(getApplication());

        pulseValue = findViewById(R.id.pulseRate_Value);
        lastPulseValue = findViewById(R.id.pulseRate_Last);
        pulseStatus = findViewById(R.id.pulseRate_Status);

        oxygenValue = findViewById(R.id.oxygenSaturation_Value);
        lastOxygenValue = findViewById(R.id.oxygenSaturation_Last);
        oxygenStatus = findViewById(R.id.oxygenSaturation_Status);

        temperatureValue = findViewById(R.id.bodyTemperature_Value);
        lastTemperatureValue = findViewById(R.id.bodyTemperature_Last);
        temperatureStatus = findViewById(R.id.bodyTemperature_Status);

        loader = findViewById(R.id.loader);
        loader.setIndeterminateDrawable(new Circle());

        networkStatusChangeReceiver = new NetworkStatusChangeReceiver();
    }

    /**
     * Get data from https://thingspeak.com/
     **/
    private void getFeedData() {
        double getPR = new SharedPreference().getStoredValue(SharedPreference.PR_VALUE_SP_KEY);
        double getSpO2 = new SharedPreference().getStoredValue(SharedPreference.SpO2_VALUE_SP_KEY);
        double getBT = new SharedPreference().getStoredValue(SharedPreference.BT_VALUE_SP_KEY);

        HealthData healthData = new HealthData();
        List<Feed> feeds = new ArrayList<>();
        feeds.add(new Feed(getPR, getSpO2, getBT));
        feeds.add(new SharedPreference(MainActivity.this).getStoredValue());
        healthData.setFeeds(feeds);
        dataSetup(healthData);

        dataModel.getRefresh().observe(MainActivity.this, o ->
                dataModel.getHealthData().observe(MainActivity.this, healthData1 -> {
                    refreshLayout.setRefreshing(false);
                    loader.setVisibility(View.GONE);
                    if (healthData1 == null || healthData1.getFeeds().size() == 0) return;
                    Collections.reverse(healthData1.getFeeds());
                    dataSetup(healthData1);
                }));
    }

    /**
     * Update data from {@link NotifyUserReceiver}
     **/
    private final BroadcastReceiver mDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            HealthData healthData = (HealthData) intent.getSerializableExtra(Constants.DATA_INTENT_KEY);
            assert healthData != null;
            dataSetup(healthData);
        }
    };

    private void dataSetup(HealthData healthData){
        Feed lastFeed = healthData.getFeeds().size() == 2 && healthData.getFeeds().get(1) != null ? healthData.getFeeds().get(1) : null;

        Feed currentFeed = healthData.getFeeds().get(0);
        if(currentFeed == null) return;

        /**
         * Pulse Rate
         **/
        String getLastPR = AppExtensions.formatValue(lastFeed == null ? null : lastFeed.getPulse(), null);
        if(getLastPR != null){
            lastPulseValue.setVisibility(View.VISIBLE);
            lastPulseValue.setText(getLastPR + " " + AppExtensions.getString(R.string.bpm));
        }
        else lastPulseValue.setVisibility(View.GONE);

        pulseValue.setText(AppExtensions.formatValue(currentFeed.getPulse(), getResources().getString(R.string.nullSymbol)));
        String getCurPR = AppExtensions.formatValue(currentFeed.getPulse(), null);
        if(getCurPR != null) {
            double pulse = Double.parseDouble(getCurPR);
            if(pulse < Constants.PULSE_MIN_VALUE){
                pulseStatus.setText(AppExtensions.getString(R.string.low));
            }
            else if(pulse > Constants.PULSE_MAX_VALUE){
                pulseStatus.setText(AppExtensions.getString(R.string.high));
            }
            else {
                pulseStatus.setText(AppExtensions.getString(R.string.normal));
            }
        }

        /**
         * Oxygen Saturation
         **/
        String getLastSpO2 = AppExtensions.formatValue(lastFeed == null ? null : lastFeed.getOxygen(), null);
        if(getLastSpO2 != null){
            lastOxygenValue.setVisibility(View.VISIBLE);
            lastOxygenValue.setText(getLastSpO2 + " " + AppExtensions.getString(R.string.percent));
        }
        else lastOxygenValue.setVisibility(View.GONE);

        oxygenValue.setText(AppExtensions.formatValue(currentFeed.getOxygen(), getResources().getString(R.string.nullSymbol)));
        String getCurSpO2 = AppExtensions.formatValue(currentFeed.getOxygen(), null);
        if(getCurSpO2 != null) {
            double oxygen = Double.parseDouble(getCurSpO2);
            if(oxygen < Constants.SPO2_NORMAL_VALUE){
                oxygenStatus.setText(AppExtensions.getString(R.string.low));
            }
            else {
                oxygenStatus.setText(AppExtensions.getString(R.string.normal));
            }
        }

        /**
         * Body Temperature
         **/
        String getLastTemp = AppExtensions.formatValue(lastFeed == null ? null : lastFeed.getTemperature(), null);
        if(getLastTemp != null){
            lastTemperatureValue.setVisibility(View.VISIBLE);
            lastTemperatureValue.setText(getLastTemp + " " + AppExtensions.getString(R.string.degreeFerSymbol));
        }
        else lastTemperatureValue.setVisibility(View.GONE);

        temperatureValue.setText(AppExtensions.formatValue(currentFeed.getTemperature(), getResources().getString(R.string.nullSymbol)));
        String getCurTemp = AppExtensions.formatValue(currentFeed.getTemperature(), null);

        if(getCurTemp != null){
            double temperature = Double.parseDouble(getCurTemp);
            if(temperature < Constants.TEMPERATURE_MIN_VALUE){
                temperatureStatus.setText(AppExtensions.getString(R.string.low));
            }
            else if(temperature > Constants.TEMPERATURE_MAX_VALUE){
                temperatureStatus.setText(AppExtensions.getString(R.string.high));
            }
            else {
                temperatureStatus.setText(AppExtensions.getString(R.string.normal));
            }
        }

        new SharedPreference(MainActivity.this).storeValue(lastFeed);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    /**
//     *  Signing Out Users
//     **/
//    @SuppressLint("NonConstantResourceId")
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        switch (id){
//            case R.id.addContact:
//                AddContactFragment.show(null);
//                break;
//            case R.id.phoneContacts:
//                ContactsFragment.show();
//                break;
//            case R.id.emergencyContact:
//                EmergencyContactFragment.show();
//                break;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    /**
     * Swipe to reload data
     **/
    @Override
    public void onRefresh() {
        dataModel.RefreshData();
    }

    /**
     *  prompt permission dialog for notification
     **/
    public void showNotificationDialog(){
        if(notificationAlertDialog != null && notificationAlertDialog.isShowing() ) return;

        notificationAlertDialog = new AlertDialog.Builder(MainActivity.this, R.style.NotificationDialog).create();
        notificationAlertDialog.setCancelable(false);
        notificationAlertDialog.setMessage(getResources().getString(R.string.allowNotificationMessage));

        notificationAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, (getResources().getString(R.string.allow)+"          "),
                (dialog, which) -> {
                    dialog.dismiss();

                    Intent intent = new Intent();
                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");

                    /**
                     * for Android 5-7
                     **/
                    intent.putExtra("app_package", getPackageName());
                    intent.putExtra("app_uid", getApplicationInfo().uid);

                    /**
                     * for Android 8 and above
                     **/
                    intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());

                    startActivity(intent);
                });

        notificationAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, ("          "+getResources().getString(R.string.forbid)),
                (dialog, which) -> dialog.dismiss());

        notificationAlertDialog.show();

        AppCompatTextView messageText = notificationAlertDialog.findViewById(android.R.id.message);
        if (messageText != null) {
            messageText.setGravity(Gravity.CENTER_HORIZONTAL);
            messageText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorSmokeWhite));
        }
    }


    /**
     *  prompt permission dialog for autoStart
     **/
    public void showAutoStartDialog(final Intent intent){
        if(autoStartAlertDialog != null && autoStartAlertDialog.isShowing() ) return;

        autoStartAlertDialog = new AlertDialog.Builder(MainActivity.this, R.style.NotificationDialog).create();
        autoStartAlertDialog.setCancelable(false);
        autoStartAlertDialog.setMessage(getResources().getString(R.string.allowAutoStartupMessage));

        autoStartAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, (getResources().getString(R.string.allow)+"          "),
                (dialog, which) -> {
                    dialog.dismiss();
                    new SharedPreference(MainActivity.this).allowAutoStart(SharedPreference.ALLOW_SP_KEY, true);
                    startActivity(intent);
                });

        autoStartAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, ("            "+getResources().getString(R.string.forbid)),
                (dialog, which) -> dialog.dismiss());

        autoStartAlertDialog.show();

        AppCompatTextView messageText = autoStartAlertDialog.findViewById(android.R.id.message);
        if (messageText != null) {
            messageText.setGravity(Gravity.CENTER_HORIZONTAL);
            messageText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorSmokeWhite));
        }
    }


    /**
     *  Monitor Internet Connection
     **/
    public void updateInternetConnectionStatus(boolean isConnected) {
        if (isConnected) {
            if(CustomSnackBar.snackbar != null) CustomSnackBar.snackbar.dismiss();
        }
        else {
            CustomSnackBar snackbar = new CustomSnackBar(refreshLayout, R.string.network_Error, R.string.retry, CustomSnackBar.Duration.INDEFINITE);
            snackbar.show();
            snackbar.setOnDismissListener(snackbar1 -> {
                networkStatusChangeReceiver.onReceive(MainActivity.this, null);
                snackbar1.dismiss();
            });
        }
    }
}
