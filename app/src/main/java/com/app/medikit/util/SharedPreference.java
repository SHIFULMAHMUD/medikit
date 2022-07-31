package com.app.medikit.util;

import android.content.Context;

import com.google.gson.Gson;
import com.app.medikit.controller.MedkitController;
import com.app.medikit.model.Contact;
import com.app.medikit.model.Feed;

/**
 *  Storing data locally
 **/
public class SharedPreference {

    private Context     context;

    private final       String STORE_VALUE_SP_NAME = "storeValue";
    private final       String EMERGENCY_CONTACT_SP_NAME = "emergencyContact";
    private final       String EMERGENCY_CONTACT_SP_KEY = "emergencyContactKey";
    private final       String FEED_SP_NAME = "feedData";
    private final       String FEED_SP_KEY = "feedDataKey";
    public static final String BT_VALUE_SP_KEY = "btValueKey";
    public static final String PR_VALUE_SP_KEY = "prValueKey";
    public static final String SpO2_VALUE_SP_KEY = "spo2ValueKey";

    private final       String ALLOW_NOTIFICATION_SP_NAME = "allowNotification";
    private final       String AUTO_START_SP_NAME = "allowNotification";
    public static final String ALLOW_SP_KEY = "allowKey";

    public SharedPreference() {
        this.context = MedkitController.getContext();
    }

    public SharedPreference(Context context) {
        this.context = context;
    }

    public void storeEmergencyContact(Contact contact){
        String data = contact == null ? null : new Gson().toJson(contact);
        context.getSharedPreferences(EMERGENCY_CONTACT_SP_NAME, Context.MODE_PRIVATE).edit().putString(EMERGENCY_CONTACT_SP_KEY, data).apply();
    }

    public Contact getEmergencyContact(){
        String contact = context.getSharedPreferences(EMERGENCY_CONTACT_SP_NAME, Context.MODE_PRIVATE).getString(EMERGENCY_CONTACT_SP_KEY,null);
        return contact == null ? null : new Gson().fromJson(contact, Contact.class);
    }

    public void storeValue(String key, float value){
        context.getSharedPreferences(STORE_VALUE_SP_NAME, Context.MODE_PRIVATE).edit().putFloat(key, value).apply();
    }

    public Double getStoredValue(String key){
        return (double) context.getSharedPreferences(STORE_VALUE_SP_NAME, Context.MODE_PRIVATE).getFloat(key,0);
    }

    public void storeValue(Feed data){
        String feed = data == null ? null : new Gson().toJson(data);
        context.getSharedPreferences(FEED_SP_NAME, Context.MODE_PRIVATE).edit().putString(FEED_SP_KEY, feed).apply();
    }

    public Feed getStoredValue(){
        String storedData = context.getSharedPreferences(FEED_SP_NAME, Context.MODE_PRIVATE).getString(FEED_SP_KEY,null);
        return storedData == null ? null : new Gson().fromJson(storedData, Feed.class);
    }

    public void allowNotification(String key, boolean allow){
        context.getSharedPreferences(ALLOW_NOTIFICATION_SP_NAME, Context.MODE_PRIVATE).edit().putBoolean(key, allow).apply();
    }

    public boolean isNotificationAllow(String key){
        return context.getSharedPreferences(ALLOW_NOTIFICATION_SP_NAME, Context.MODE_PRIVATE).getBoolean(key,false);
    }

    public void allowAutoStart(String key, boolean allow){
        context.getSharedPreferences(AUTO_START_SP_NAME, Context.MODE_PRIVATE).edit().putBoolean(key, allow).apply();
    }

    public boolean isAutoStart(String key){
        return context.getSharedPreferences(AUTO_START_SP_NAME, Context.MODE_PRIVATE).getBoolean(key,false);
    }
}
