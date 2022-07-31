package com.app.medikit.receiver;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.app.medikit.BuildConfig;
import com.google.gson.GsonBuilder;
import com.app.medikit.R;
import com.app.medikit.activity.MainActivity;
import com.app.medikit.api.API;
import com.app.medikit.model.Contact;
import com.app.medikit.model.Feed;
import com.app.medikit.model.HealthData;
import com.app.medikit.remote.PermissionManager;
import com.app.medikit.util.AppExtensions;
import com.app.medikit.util.Constants;
import com.app.medikit.util.SharedPreference;

import java.util.Collections;

/**
 *  Alert Notification Service
 **/
public class NotifyUserReceiver extends BroadcastReceiver {

    public static int NOTIFICATION_ID = 1;
    private LocalBroadcastManager dataBroadcaster;

    @Override public void onReceive(final Context context, Intent intent) {
        dataBroadcaster = LocalBroadcastManager.getInstance(context);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, API.getFeedURL(2),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        HealthData healthData = new GsonBuilder().create().fromJson(response, HealthData.class);
                        if(healthData == null) return;
                        if(healthData.getFeeds() == null || healthData.getFeeds().size() == 0) return;
                        Collections.reverse(healthData.getFeeds());

                        Feed latestFeed = healthData.getFeeds().get(0);
                        if(latestFeed == null) return;

                        String pulseStatus = ""; String oxygenStatus = ""; String temperatureStatus = "";

                        Intent tokenIntent = new Intent(Constants.DATA_LISTENER_KEY);
                        tokenIntent.putExtra(Constants.DATA_INTENT_KEY, healthData);
                        dataBroadcaster.sendBroadcast(tokenIntent);

                        /**
                         *  Checking Pulse Rate (PR)
                         **/
                        String getNewPR = AppExtensions.formatValue(latestFeed.getPulse(), null);
                        String getOldPR = AppExtensions.formatValue(new SharedPreference(context).getStoredValue(SharedPreference.PR_VALUE_SP_KEY).toString(), null);
                        if(getNewPR != null) {
                            double newPR = Double.parseDouble(getNewPR);
                            double oldPR = Double.parseDouble(getOldPR);
                            if(newPR != oldPR) {
                                if(newPR < Constants.PULSE_MIN_VALUE){
                                    pulseStatus = "PULSE RATE:\nLow, its about " + getNewPR + context.getResources().getString(R.string.bpm);
                                    showNotification(context, "Pulse Rate", "Low, its about "+ getNewPR + " " + context.getResources().getString(R.string.bpm));
                                }
                                else if(newPR > Constants.PULSE_MAX_VALUE){
                                    pulseStatus = "PULSE RATE:\nHigh, its about " + getNewPR + context.getResources().getString(R.string.bpm);
                                    showNotification(context, "Pulse Rate", "High, its about "+ getNewPR + " " + context.getResources().getString(R.string.bpm));
                                }

                                new SharedPreference(context).storeValue(SharedPreference.PR_VALUE_SP_KEY, (float) newPR);
                            }
                        }
 
                        /**
                         *  Checking Oxygen Saturation (SpO2)
                         **/
                        String getNewSpO2 = AppExtensions.formatValue(latestFeed.getOxygen(), null);
                        String getOldSpO2 = AppExtensions.formatValue(new SharedPreference(context).getStoredValue(SharedPreference.SpO2_VALUE_SP_KEY).toString(), null);
                        if(getNewSpO2 != null) {
                            double newSpO2 = Double.parseDouble(getNewSpO2);
                            double oldSpO2 = Double.parseDouble(getOldSpO2);
                            if(newSpO2 != oldSpO2){
                                if(newSpO2 < Constants.SPO2_NORMAL_VALUE){
                                    oxygenStatus = "OXYGEN SATURATION:\nLow, its about " + getNewSpO2 + context.getResources().getString(R.string.percent);
                                    showNotification(context, "Oxygen Saturation", "Low, its about "+ getNewSpO2 + " " + context.getResources().getString(R.string.percent));
                                }

                                new SharedPreference(context).storeValue(SharedPreference.SpO2_VALUE_SP_KEY, (float) newSpO2);
                            }
                        }

                        /**
                         *  Checking Body Temperature (BT)
                         **/
                        String getNewTemp = AppExtensions.formatValue(latestFeed.getTemperature(), null);
                        String getOldTemp = AppExtensions.formatValue(new SharedPreference(context).getStoredValue(SharedPreference.BT_VALUE_SP_KEY).toString(), null);
                        if(getNewTemp != null){
                            double newTemp = Double.parseDouble(getNewTemp);
                            double oldTemp = Double.parseDouble(getOldTemp);
                            if(newTemp != oldTemp) {
                                if(newTemp < Constants.TEMPERATURE_MIN_VALUE){
                                    temperatureStatus = "BODY TEMPERATURE:\nLow, its about " + getNewTemp + context.getResources().getString(R.string.degreeFerSymbol);
                                    showNotification(context, "Body Temperature", "Low, its about "+ getNewTemp + " " + context.getResources().getString(R.string.degreeFerSymbol));
                                }
                                else if(newTemp > Constants.TEMPERATURE_MAX_VALUE){
                                    temperatureStatus = "BODY TEMPERATURE:\nHigh, its about " + getNewTemp + context.getResources().getString(R.string.degreeFerSymbol);
                                    showNotification(context, "Body Temperature", "High, its about "+ getNewTemp + " " + context.getResources().getString(R.string.degreeFerSymbol));
                                }

                                new SharedPreference(context).storeValue(SharedPreference.BT_VALUE_SP_KEY, (float) newTemp);
                            }
                        }

                        /**
                         * Send sms to emergency contact
                         **/
                        if(!pulseStatus.isEmpty() || !oxygenStatus.isEmpty() || !temperatureStatus.isEmpty()){
                            String message = "Emergency Alert !!\n\n"
                                    + pulseStatus
                                    + (pulseStatus.isEmpty() || oxygenStatus.isEmpty() ? oxygenStatus : "\n\n" + oxygenStatus)
                                    + (oxygenStatus.isEmpty() || temperatureStatus.isEmpty() ? temperatureStatus : "\n\n" + temperatureStatus);

                            sendSMS(context, message);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        );

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);
    }

    protected void sendSMS(Context context, String message) {
        if (!new PermissionManager(PermissionManager.Permission.SMS, false).isGranted()) return;
        SharedPreference sp = new SharedPreference(context);
        Contact contact = sp.getEmergencyContact();
        if(contact == null || contact.getNumber() == null) return;
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(contact.getNumber(), null, message, null, null);
            Log.e(Constants.TAG, "Message sent successfully");
        }
        catch (Exception ex) {
            Log.e(Constants.TAG, "Message not sent, reason: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void startNotifyService(Context context){
        try {
            final int _id = (int) System.currentTimeMillis();

            Intent myIntent = new Intent(context, NotifyUserReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, _id, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            assert alarmManager != null;
            alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), Constants.REFRESH_TIMING, pendingIntent);
        }
        catch (Exception ex){
            ex.printStackTrace();
            Log.e("Hell", "Sorry, there is some problem in starting TeleKiT, please! reinstall it");
        }
    }

    private void showNotification(Context context, String title, String message) {
        Intent intent= new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context,0,intent,0);

        String CHANNEL_ID = BuildConfig.APPLICATION_ID;
        String CHANNEL_NAME = "TeleKiT";

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setVibrate(new long[] { 100, 100, 100, 100, 100 })
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setDefaults(Notification.DEFAULT_LIGHTS);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(message);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLightColor(Color.CYAN);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            assert notificationManager != null;
            builder.setChannelId(CHANNEL_ID);
            notificationManager.createNotificationChannel(channel);
        }

        if (NOTIFICATION_ID > 1073741824) {
            NOTIFICATION_ID = 0;
        }

        assert notificationManager != null;
        notificationManager.notify(NOTIFICATION_ID++,builder.build());
    }
}
